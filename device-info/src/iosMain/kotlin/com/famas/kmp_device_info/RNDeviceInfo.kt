package com.famas.kmp_device_info

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.ULongVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionPortBluetoothA2DP
import platform.AVFAudio.AVAudioSessionPortBluetoothHFP
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.AVAudioSessionPortHeadphones
import platform.AVFAudio.AVAudioSessionRouteDescription
import platform.AVFAudio.currentRoute
import platform.CoreFoundation.*
import platform.CoreGraphics.CGFloat
import platform.CoreLocation.*
import platform.CoreTelephony.CTTelephonyNetworkInfo
import platform.DeviceCheck.DCDevice
import platform.Foundation.*
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.Security.*
import platform.UIKit.*
import platform.WebKit.WKWebView
import platform.darwin.KERN_SUCCESS
import platform.darwin.NSInteger
import platform.darwin.TARGET_IPHONE_SIMULATOR
import platform.darwin.TARGET_OS_MACCATALYST
import platform.darwin.TASK_BASIC_INFO
import platform.darwin.getifaddrs
import platform.darwin.kern_return_t
import platform.darwin.noErr
import platform.darwin.sysctlbyname
import platform.darwin.task_basic_info
import platform.darwin.task_info_t
import platform.darwin.version_min_command
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.alloca
import platform.posix.calloc
import platform.posix.err
import platform.posix.size_tVar
import platform.posix.sockaddr_in
import platform.posix.uname
import platform.posix.utsname
import platform.zlib.alloc_func
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
object RNDeviceInfo {
     var _uid: String? = null
     val uid: String
        get() {
            _uid?.let { return it }
            val uid = valueForKeychainKey(UIDKey, service = UIDKey)
            _uid = uid
            if (uid != null) return uid
            val uid2 = valueForUserDefaultsKey(UIDKey)
            _uid = uid2
            if (uid2 != null) return uid2
            val uid3 = appleIFV()
            _uid = uid3
            if (uid3 != null) return uid3
            _uid = randomUUID()
            return _uid!!
        }

    fun uid(): String? = uid

    fun syncUid(): String? {
        _uid = appleIFV()
        if (_uid == null) {
            _uid = randomUUID()
        }
        save()
        return _uid
    }

     fun valueForKeychainKey(key: String, service: String): String? {
        val keychainItem = mutableMapOf<Any?, Any?>()
        keychainItem[kSecClass as Any] = kSecClassGenericPassword
        keychainItem[kSecAttrAccessible as Any] = kSecAttrAccessibleAfterFirstUnlock
        keychainItem[kSecAttrAccount as Any] = key
        keychainItem[kSecAttrService as Any] = service
        val result = SecItemCopyMatching(keychainItem as CFDictionaryRef, null)
        if (result.toUInt() != noErr) {
            return null
        }
        val resultDict = result as NSDictionary
        val data = resultDict.valueForKey(kSecValueData.toString()) as NSData
        return data.toString()
    }

    fun setValue(value: String, forUserDefaultsKey: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = forUserDefaultsKey)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

     fun valueForUserDefaultsKey(key: String): String? {
        return NSUserDefaults.standardUserDefaults.objectForKey(key) as? String
    }

     fun keychainItemForKey(key: String, service: String): NSMutableDictionary {
        val keychainItem = mutableMapOf<Any?, Any?>()
        keychainItem[kSecClass as Any] = kSecClassGenericPassword
        keychainItem[kSecAttrAccessible as Any] = kSecAttrAccessibleAfterFirstUnlock
        keychainItem[kSecAttrAccount as Any] = key
        keychainItem[kSecAttrService as Any] = service
        return keychainItem as NSMutableDictionary
    }

    fun setValue(value: String, forKeychainKey: String, inService: String): Boolean {
        val keychainItem = keychainItemForKey(forKeychainKey, service = inService)
        keychainItem.setValue(value, kSecValueData.toString())
        return SecItemAdd(keychainItem as CFDictionaryRef, null) == errSecSuccess
    }

    fun updateValue(value: String, forKeychainKey: String, inService: String): Boolean {
        val query = mutableMapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to forKeychainKey,
            kSecAttrService to inService
        )
        val attributesToUpdate = mutableMapOf(
            kSecValueData to value
        )
        return SecItemUpdate(
            query as CFDictionaryRef,
            attributesToUpdate as CFDictionaryRef
        ) == errSecSuccess
    }

    fun deleteValue(forKeychainKey: String, inService: String): Boolean {
        val query = mutableMapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to forKeychainKey,
            kSecAttrService to inService
        )
        return SecItemDelete(query as CFDictionaryRef) == errSecSuccess
    }

     fun save() {
        uid?.let { setValue(it, forUserDefaultsKey = UIDKey) }
        uid?.let { setValue(it, forKeychainKey = UIDKey, inService = UIDKey) }
    }

     fun saveIfNeed() {
        if (valueForUserDefaultsKey(UIDKey) == null) {
            uid?.let { setValue(it, forUserDefaultsKey = UIDKey) }
        }
        if (valueForKeychainKey(UIDKey, service = UIDKey) == null) {
            uid?.let { setValue(it, forKeychainKey = UIDKey, inService = UIDKey) }
        }
    }

     fun randomUUID(): String {
        if (NSClassFromString("NSUUID") != null) {
            return NSUUID.UUID().UUIDString
        }
        val uuidRef = CFUUIDCreate(null)
        val cfuuid = CFUUIDCreateString(null, uuidRef)
        CFRelease(uuidRef)
        val uuid = cfuuid.toString()
        CFRelease(cfuuid)
        return uuid
    }

     fun appleIFV(): String? {
        if (NSClassFromString("UIDevice") != null && UIDevice.instancesRespondToSelector(
                NSSelectorFromString("identifierForVendor")
            )
        ) {
            // only available in iOS >= 6.0
            return UIDevice.currentDevice.identifierForVendor?.UUIDString
        }
        return null
    }

     var hasListeners: Boolean = false

    fun requiresMainQueueSetup() = false

    fun supportedEvents(): Array<String> = arrayOf(
        "RNDeviceInfo_batteryLevelDidChange",
        "RNDeviceInfo_batteryLevelIsLow",
        "RNDeviceInfo_powerStateDidChange",
        "RNDeviceInfo_headphoneConnectionDidChange",
        "RNDeviceInfo_brightnessDidChange"
    )

    fun constantsToExport(): Map<String, Any?> {
        return mapOf(
            "deviceId" to getDeviceId(),
            "bundleId" to getBundleId(),
            "systemName" to getSystemName(),
            "systemVersion" to getSystemVersion(),
            "appVersion" to getAppVersion(),
            "buildNumber" to getBuildNumber(),
            "isTablet" to isTablet(),
            "appName" to getAppName(),
            "brand" to "Apple",
            "model" to getModel(),
            "deviceType" to getDeviceTypeName(),
            "isDisplayZoomed" to isDisplayZoomed(),
        )
    }

     fun getDeviceType(): DeviceType {
        return when (UIDevice.currentDevice.userInterfaceIdiom) {
            UIUserInterfaceIdiomPhone -> DeviceType.DeviceTypeHandset
            UIUserInterfaceIdiomPad -> {
                if (TARGET_OS_MACCATALYST == 1) {
                    return DeviceType.DeviceTypeDesktop
                }

                return if (UIDevice.currentDevice.systemVersion.toFloat() <= 14.0f) {
                    if (NSProcessInfo.processInfo.isiOSAppOnMac()) {
                        DeviceType.DeviceTypeDesktop
                    } else {
                        DeviceType.DeviceTypeTablet
                    }
                } else {
                    DeviceType.DeviceTypeTablet
                }
            }

            UIUserInterfaceIdiomTV -> DeviceType.DeviceTypeTv
            UIUserInterfaceIdiomMac -> DeviceType.DeviceTypeDesktop
            else -> DeviceType.DeviceTypeUnknown
        }
    }

     fun getStorageDictionary(): NSDictionary? {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        return (NSFileManager.defaultManager.attributesOfFileSystemForPath(
            paths.last().toString(),
            error = null
        ) as? NSDictionary)
    }

     fun getSystemName(): String {
        return UIDevice.currentDevice.systemName
    }

     fun getSystemVersion(): String {
        return UIDevice.currentDevice.systemVersion
    }

    fun getDeviceName(): String {
        return UIDevice.currentDevice.name
    }

    fun isDisplayZoomed(): Boolean {
        return UIScreen.mainScreen.scale != UIScreen.mainScreen.nativeScale
    }

    fun getAppName(): String? {
        val displayName =
            NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleDisplayName") as String?
        val bundleName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleName") as String?
        return displayName ?: bundleName
    }

    fun getBundleId(): String {
        return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleIdentifier") as String
    }

    fun getAppVersion(): String {
        return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as String
    }

    fun getBuildNumber(): String {
        return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as String
    }

    fun getDeviceNamesByCode(): Map<String, String> {
        return mapOf(
            "iPod1,1" to "iPod Touch", // (Original)
            "iPod2,1" to "iPod Touch", // (Second Generation)
            "iPod3,1" to "iPod Touch", // (Third Generation)
            "iPod4,1" to "iPod Touch", // (Fourth Generation)
            "iPod5,1" to "iPod Touch", // (Fifth Generation)
            "iPod7,1" to "iPod Touch", // (Sixth Generation)
            "iPod9,1" to "iPod Touch", // (Seventh Generation)
            "iPhone1,1" to "iPhone", // (Original)
            "iPhone1,2" to "iPhone 3G", // (3G)
            "iPhone2,1" to "iPhone 3GS", // (3GS)
            "iPad1,1" to "iPad", // (Original)
            "iPad2,1" to "iPad 2", //
            "iPad2,2" to "iPad 2", //
            "iPad2,3" to "iPad 2", //
            "iPad2,4" to "iPad 2", //
            "iPad3,1" to "iPad", // (3rd Generation)
            "iPad3,2" to "iPad", // (3rd Generation)
            "iPad3,3" to "iPad", // (3rd Generation)
            "iPhone3,1" to "iPhone 4", // (GSM)
            "iPhone3,2" to "iPhone 4", // iPhone 4
            "iPhone3,3" to "iPhone 4", // (CDMA/Verizon/Sprint)
            "iPhone4,1" to "iPhone 4S", //
            "iPhone5,1" to "iPhone 5", // (model A1428, AT&T/Canada)
            "iPhone5,2" to "iPhone 5", // (model A1429, everything else)
            "iPad3,4" to "iPad", // (4th Generation)
            "iPad3,5" to "iPad", // (4th Generation)
            "iPad3,6" to "iPad", // (4th Generation)
            "iPad2,5" to "iPad Mini", // (Original)
            "iPad2,6" to "iPad Mini", // (Original)
            "iPad2,7" to "iPad Mini", // (Original)
            "iPhone5,3" to "iPhone 5c", // (model A1456, A1532 | GSM)
            "iPhone5,4" to "iPhone 5c", // (model A1507, A1516, A1526 (China), A1529 | Global)
            "iPhone6,1" to "iPhone 5s", // (model A1433, A1533 | GSM)
            "iPhone6,2" to "iPhone 5s", // (model A1457, A1518, A1528 (China), A1530 | Global)
            "iPhone7,1" to "iPhone 6 Plus", //
            "iPhone7,2" to "iPhone 6", //
            "iPhone8,1" to "iPhone 6s", //
            "iPhone8,2" to "iPhone 6s Plus", //
            "iPhone8,4" to "iPhone SE", //
            "iPhone9,1" to "iPhone 7", // (model A1660 | CDMA)
            "iPhone9,3" to "iPhone 7", // (model A1778 | Global)
            "iPhone9,2" to "iPhone 7 Plus", // (model A1661 | CDMA)
            "iPhone9,4" to "iPhone 7 Plus", // (model A1784 | Global)
            "iPhone10,3" to "iPhone X", // (model A1865, A1902)
            "iPhone10,6" to "iPhone X", // (model A1901)
            "iPhone10,1" to "iPhone 8", // (model A1863, A1906, A1907)
            "iPhone10,4" to "iPhone 8", // (model A1905)
            "iPhone10,2" to "iPhone 8 Plus", // (model A1864, A1898, A1899)
            "iPhone10,5" to "iPhone 8 Plus", // (model A1897)
            "iPhone11,2" to "iPhone XS", // (model A2097, A2098)
            "iPhone11,4" to "iPhone XS Max", // (model A1921, A2103)
            "iPhone11,6" to "iPhone XS Max", // (model A2104)
            "iPhone11,8" to "iPhone XR", // (model A1882, A1719, A2105)
            "iPhone12,1" to "iPhone 11",
            "iPhone12,3" to "iPhone 11 Pro",
            "iPhone12,5" to "iPhone 11 Pro Max",
            "iPhone12,8" to "iPhone SE", // (2nd Generation iPhone SE),
            "iPhone13,1" to "iPhone 12 mini",
            "iPhone13,2" to "iPhone 12",
            "iPhone13,3" to "iPhone 12 Pro",
            "iPhone13,4" to "iPhone 12 Pro Max",
            "iPhone14,4" to "iPhone 13 mini",
            "iPhone14,5" to "iPhone 13",
            "iPhone14,2" to "iPhone 13 Pro",
            "iPhone14,3" to "iPhone 13 Pro Max",
            "iPhone14,6" to "iPhone SE", // (3nd Generation iPhone SE),
            "iPhone14,7" to "iPhone 14",
            "iPhone14,8" to "iPhone 14 Plus",
            "iPhone15,2" to "iPhone 14 Pro",
            "iPhone15,3" to "iPhone 14 Pro Max",
            "iPhone15,4" to "iPhone 15",
            "iPhone15,5" to "iPhone 15 Plus",
            "iPhone16,1" to "iPhone 15 Pro",
            "iPhone16,2" to "iPhone 15 Pro Max",
            "iPad4,1" to "iPad Air", // 5th Generation iPad (iPad Air) - Wifi
            "iPad4,2" to "iPad Air", // 5th Generation iPad (iPad Air) - Cellular
            "iPad4,3" to "iPad Air", // 5th Generation iPad (iPad Air)
            "iPad4,4" to "iPad Mini 2", // (2nd Generation iPad Mini - Wifi)
            "iPad4,5" to "iPad Mini 2", // (2nd Generation iPad Mini - Cellular)
            "iPad4,6" to "iPad Mini 2", // (2nd Generation iPad Mini)
            "iPad4,7" to "iPad Mini 3", // (3rd Generation iPad Mini)
            "iPad4,8" to "iPad Mini 3", // (3rd Generation iPad Mini)
            "iPad4,9" to "iPad Mini 3", // (3rd Generation iPad Mini)
            "iPad5,1" to "iPad Mini 4", // (4th Generation iPad Mini)
            "iPad5,2" to "iPad Mini 4", // (4th Generation iPad Mini)
            "iPad5,3" to "iPad Air 2", // 6th Generation iPad (iPad Air 2)
            "iPad5,4" to "iPad Air 2", // 6th Generation iPad (iPad Air 2)
            "iPad6,3" to "iPad Pro 9.7-inch", // iPad Pro 9.7-inch
            "iPad6,4" to "iPad Pro 9.7-inch", // iPad Pro 9.7-inch
            "iPad6,7" to "iPad Pro 12.9-inch", // iPad Pro 12.9-inch
            "iPad6,8" to "iPad Pro 12.9-inch", // iPad Pro 12.9-inch
            "iPad6,11" to "iPad (5th generation)", // Apple iPad 9.7 inch (5th generation) - WiFi
            "iPad6,12" to "iPad (5th generation)", // Apple iPad 9.7 inch (5th generation) - WiFi + cellular
            "iPad7,1" to "iPad Pro 12.9-inch", // 2nd Generation iPad Pro 12.5-inch - Wifi
            "iPad7,2" to "iPad Pro 12.9-inch", // 2nd Generation iPad Pro 12.5-inch - Cellular
            "iPad7,3" to "iPad Pro 10.5-inch", // iPad Pro 10.5-inch - Wifi
            "iPad7,4" to "iPad Pro 10.5-inch", // iPad Pro 10.5-inch - Cellular
            "iPad7,5" to "iPad (6th generation)", // iPad (6th generation) - Wifi
            "iPad7,6" to "iPad (6th generation)", // iPad (6th generation) - Cellular
            "iPad7,11" to "iPad (7th generation)", // iPad 10.2 inch (7th generation) - Wifi
            "iPad7,12" to "iPad (7th generation)", // iPad 10.2 inch (7th generation) - Wifi + cellular
            "iPad8,1" to "iPad Pro 11-inch (3rd generation)", // iPad Pro 11 inch (3rd generation) - Wifi
            "iPad8,2" to "iPad Pro 11-inch (3rd generation)", // iPad Pro 11 inch (3rd generation) - 1TB - Wifi
            "iPad8,3" to "iPad Pro 11-inch (3rd generation)", // iPad Pro 11 inch (3rd generation) - Wifi + cellular
            "iPad8,4" to "iPad Pro 11-inch (3rd generation)", // iPad Pro 11 inch (3rd generation) - 1TB - Wifi + cellular
            "iPad8,5" to "iPad Pro 12.9-inch (3rd generation)", // iPad Pro 12.9 inch (3rd generation) - Wifi
            "iPad8,6" to "iPad Pro 12.9-inch (3rd generation)", // iPad Pro 12.9 inch (3rd generation) - 1TB - Wifi
            "iPad8,7" to "iPad Pro 12.9-inch (3rd generation)", // iPad Pro 12.9 inch (3rd generation) - Wifi + cellular
            "iPad8,8" to "iPad Pro 12.9-inch (3rd generation)", // iPad Pro 12.9 inch (3rd generation) - 1TB - Wifi + cellular
            "iPad11,1" to "iPad Mini 5", // (5th Generation iPad Mini)
            "iPad11,2" to "iPad Mini 5", // (5th Generation iPad Mini)
            "iPad11,3" to "iPad Air (3rd generation)",
            "iPad11,4" to "iPad Air (3rd generation)",
            "iPad13,1" to "iPad Air (4th generation)",
            "iPad13,2" to "iPad Air (4th generation)",
            "AppleTV2,1" to "Apple TV", // Apple TV (2nd Generation)
            "AppleTV3,1" to "Apple TV", // Apple TV (3rd Generation)
            "AppleTV3,2" to "Apple TV", // Apple TV (3rd Generation - Rev A)
            "AppleTV5,3" to "Apple TV", // Apple TV (4th Generation)
            "AppleTV6,2" to "Apple TV 4K" // Apple TV 4K
        )
    }

     fun getModel(): String {
        val deviceId = getDeviceId()
        val deviceNamesByCode = getDeviceNamesByCode()
        val deviceName = deviceNamesByCode[deviceId]
        // Return the real device name if we have it
        if (deviceName != null) return deviceName
        // If we don't have the real device name, try a generic
        return when {
            deviceId.startsWith("iPod") -> "iPod Touch"
            deviceId.startsWith("iPad") -> "iPad"
            deviceId.startsWith("iPhone") -> "iPhone"
            deviceId.startsWith("AppleTV") -> "Apple TV"
            else -> "unknown"
        }
    }

    fun getCarrier(): String? {
        val nettinfo = CTTelephonyNetworkInfo()
        val netinfo = CTTelephonyNetworkInfo()
        val carrier = nettinfo.subscriberCellularProvider ?: netinfo.subscriberCellularProvider
        return carrier?.carrierName
    }

    fun getBuildId(): String {
        val buffer = NSMutableData()
        val numBytes = cValue<ULongVar>()

        val status = sysctlbyname("kern.osversion", buffer.mutableBytes, numBytes, null, 0u)
        return if (status != 0) "unknown" else buffer.bytes().toString()
    }

     fun uniqueId(): String? {
        return DeviceUID().uid()
    }

    fun getUniqueId(): String? {
        return uniqueId()
    }

    fun syncUniqueId(): String? {
        return DeviceUID().syncUid()
    }

     fun getDeviceId(): String {
        memScoped {
            val systemInfo: utsname = alloc()
            uname(systemInfo.ptr)
            return systemInfo.machine.toKString()
        }
    }

    fun isEmulator(): Boolean {
        return TARGET_IPHONE_SIMULATOR == 1
    }

     fun isTablet(): Boolean {
        return getDeviceType() == DeviceType.DeviceTypeTablet
    }

    suspend fun getDeviceToken(): String? {
        if (UIDevice.currentDevice.systemVersion.toFloat() > 11.0f) {
            if (TARGET_IPHONE_SIMULATOR == 1) {
                throw IllegalStateException("Device check is only available for physical devices")
            }
            val device = DCDevice.currentDevice
            if (device.isSupported()) {
                return suspendCoroutine { continuation ->
                    device.generateTokenWithCompletionHandler { token, error ->
                        if (error != null) {
                            continuation.resumeWithException(Exception(error.localizedDescription))
                        } else {
                            continuation.resume(
                                token?.base64EncodedStringWithOptions(
                                    NSDataBase64EncodingEndLineWithLineFeed
                                )
                            )
                        }
                    }
                }
            } else {
                throw IllegalStateException("Device check is not supported by this device")
            }
        } else {
            throw IllegalStateException("Device check is only available for iOS > 11")
        }
    }

    fun getFontScale(): Float {
        // Font scales based on font sizes from https://developer.apple.com/ios/human-interface-guidelines/visual-design/typography/
        var fontScale = 1.0f
        val traitCollection = UIScreen.mainScreen.traitCollection
        var contentSize: String? = null
        val deviceType = getDeviceType()
        val isPreferredContentSizeCategory =
            ((deviceType == DeviceType.DeviceTypeHandset || deviceType == DeviceType.DeviceTypeTv || deviceType == DeviceType.DeviceTypeTablet) && UIDevice.currentDevice.systemVersion.toFloat() >= 10.0) || ((deviceType == DeviceType.DeviceTypeUnknown || deviceType == DeviceType.DeviceTypeDesktop) && UIDevice.currentDevice.systemVersion.toFloat() >= 13.0)
        if (isPreferredContentSizeCategory) {
            contentSize = traitCollection.preferredContentSizeCategory
        }

        when (contentSize) {
            "UICTContentSizeCategoryXS" -> fontScale = 0.82f
            "UICTContentSizeCategoryS" -> fontScale = 0.88f
            "UICTContentSizeCategoryM" -> fontScale = 0.95f
            "UICTContentSizeCategoryL" -> fontScale = 1.0f
            "UICTContentSizeCategoryXL" -> fontScale = 1.12f
            "UICTContentSizeCategoryXXL" -> fontScale = 1.23f
            "UICTContentSizeCategoryXXXL" -> fontScale = 1.35f
            "UICTContentSizeCategoryAccessibilityM" -> fontScale = 1.64f
            "UICTContentSizeCategoryAccessibilityL" -> fontScale = 1.95f
            "UICTContentSizeCategoryAccessibilityXL" -> fontScale = 2.35f
            "UICTContentSizeCategoryAccessibilityXXL" -> fontScale = 2.76f
            "UICTContentSizeCategoryAccessibilityXXXL" -> fontScale = 3.12f
        }
        return fontScale
    }

    fun getTotalMemory(): Double {
        return NSProcessInfo.processInfo.physicalMemory.toDouble()
    }

    fun getTotalDiskCapacity(): Double {
        val storage = getStorageDictionary()
        return (storage?.objectForKey(NSFileSystemSize) as NSNumber).doubleValue
    }

    fun getFreeDiskStorage(): Double {
        val storage = getStorageDictionary()
        return (storage?.objectForKey(NSFileSystemFreeSize) as NSNumber).doubleValue
    }

     fun getDeviceTypeName(): String {
        return getDeviceType().name
    }

    fun getSupportedAbis(): Array<String> {
        /* https://stackoverflow.com/questions/19859388/how-can-i-get-the-ios-device-cpu-architecture-in-runtime */
//        val info = NXGetLocalArchInfo()
//        val typeOfCpu = String(info.description.toByteArray())
//        return arrayOf(typeOfCpu)
        return arrayOf()
    }

    fun getIpAddress(): String {
        var address = "0.0.0.0"
//        val interfaces: COpaquePointer? = null
//        var temp_addr: COpaquePointer? = null
//        var success = 0
//        // retrieve the current interfaces - returns 0 on success
//        success = getifaddrs(interfaces)
//        if (success == 0) {
//            // Loop through linked list of interfaces
//            temp_addr = interfaces
//            while (temp_addr != null) {
//                val addr_family = (temp_addr.pointed.ifa_addr.pointed.sa_family).toUInt()
//                // Check for IPv4 or IPv6-only interfaces
//                if (addr_family == AF_INET.toUInt() || addr_family == AF_INET6.toUInt()) {
//                    val ifname = String(temp_addr.pointed.ifa_name)
//                    if (ifname == "en0" || ifname == "en1") {
//                        val addr = (temp_addr.pointed.ifa_addr.pointed as sockaddr_in).sin_addr
//                        val addr_len =
//                            if (addr_family == AF_INET.toUInt()) INET_ADDRSTRLEN else INET6_ADDRSTRLEN
//                        val addr_buffer = ByteArray(addr_len)
//                        // We use inet_ntop because it also supports getting an address from
//                        // interfaces that are IPv6-only
//                        val netname: COpaquePointer? =
//                            inet_ntop(addr_family.toInt(), addr, addr_buffer, addr_len.toUInt())
//                        // Get NSString from C String
//                        address = String(netname!!.toUtf8())
//                    }
//                }
//                temp_addr = temp_addr.pointed.ifa_next
//            }
//        }
//        // Free memory
//        freeifaddrs(interfaces)
        return address
    }

    fun isPinOrFingerprintSet(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)
    }

    fun getBatteryLevel(): Float {
        return UIDevice.currentDevice.batteryLevel
    }

//     fun batteryLevelDidChange(notification: NSNotification) {
//        if (!hasListeners) {
//            return
//        }
//        notification.send
//        val batteryLevel = getBatteryLevel()
//        sendEventWithName("RNDeviceInfo_batteryLevelDidChange", batteryLevel)
//        if (batteryLevel <= _lowBatteryThreshold) {
//            sendEventWithName("RNDeviceInfo_batteryLevelIsLow", batteryLevel)
//        }
//    }

//     fun powerStateDidChange(notification: NSNotification) {
//        if (!hasListeners) {
//            return
//        }
//        sendEventWithName("RNDeviceInfo_powerStateDidChange", powerState)
//    }

     fun headphoneConnectionDidChange(notification: NSNotification): Boolean {
        val isConnected = isHeadphonesConnected()
        return isConnected
    }

     fun brightnessDidChange(notification: NSNotification): Float {
        if (!hasListeners) {
            return 0f
        }
        return getBrightness()
    }

     val powerState: Map<String, Any?>
        get() {
            floatArrayOf(getBatteryLevel())
            return mapOf(
                "batteryLevel" to getBatteryLevel(),
                "batteryState" to when (UIDevice.currentDevice.batteryState) {
                    UIDeviceBatteryState.UIDeviceBatteryStateUnknown -> "unknown"
                    UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> "unplugged"
                    UIDeviceBatteryState.UIDeviceBatteryStateCharging -> "charging"
                    UIDeviceBatteryState.UIDeviceBatteryStateFull -> "full"
                    else -> "none"
                },
                "lowPowerMode" to NSProcessInfo.processInfo.isLowPowerModeEnabled()
            )
        }

    fun isBatteryCharging(): Boolean {
        return powerState["batteryState"] == "charging"
    }

    fun isLocationEnabled(): Boolean {
        return CLLocationManager.locationServicesEnabled()
    }

    fun isHeadphonesConnected(): Boolean {
        val route = AVAudioSession.sharedInstance().currentRoute
        for (desc in route.outputs as List<AVAudioSessionPortDescription>) {
            if (desc.portType == AVAudioSessionPortHeadphones) {
                return true
            }
            if (desc.portType == AVAudioSessionPortBluetoothA2DP) {
                return true
            }
            if (desc.portType == AVAudioSessionPortBluetoothHFP) {
                return true
            }
        }
        return false
    }

    fun getUsedMemory(): Long {
//        val info = task_basic_info()
//        val size = task_info_t(info).size
//        val kerr: kern_return_t = task_info(
//            mach_task_self(),
//            TASK_BASIC_INFO,
//            info,
//            size.also { it.toLong() }
//        )
//        return if (kerr != KERN_SUCCESS) {
//            -1
//        } else {
//            info.resident_size.toLong()
//        }

        //TODO: need to implement
        return 0L
    }

    suspend fun getUserAgent(): String {
        if (UIDevice.currentDevice.systemName == "tvOS") {
            throw Exception("not available on tvOS")
        }
        val webView = WKWebView()
        return suspendCoroutine {
            webView.evaluateJavaScript("window.navigator.userAgent;") { result, error ->
                if (error != null) {
                    it.resumeWithException(Exception(error.localizedDescription))
                } else {
                    it.resume(result?.toString() ?: "")
                }
                // TODO: Need to check this
//                webView?.close() // Destroy the WKWebView after task is complete
            }
        }
    }

    fun getAvailableLocationProviders(): Map<String, Boolean> {
        return mapOf(
            "locationServicesEnabled" to CLLocationManager.locationServicesEnabled(),
            "significantLocationChangeMonitoringAvailable" to CLLocationManager
                .significantLocationChangeMonitoringAvailable(),
            "headingAvailable" to CLLocationManager.headingAvailable(),
            "isRangingAvailable" to CLLocationManager.isRangingAvailable()
        )
    }

    fun getBrightness(): Float {
        return UIScreen.mainScreen.brightness.toFloat()
    }

    fun getFirstInstallTime(): Long {
        val documentsFolderUrl = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).last() as NSURL

        val error = ObjCObjectVar<NSError?>(NSError.objcPtr()).ptr

        val installDate = documentsFolderUrl.path?.let {
            NSFileManager.defaultManager.attributesOfItemAtPath(
                it,
                error
            )
        }
        return (installDate?.get(NSFileAttributeKey) as? NSDate)?.timeIntervalSince1970?.times(
            1000
        )?.toLong() ?: -1
    }

    fun dealloc() {
        NSNotificationCenter.defaultCenter.removeObserver(this)
    }
    
    fun getPowerState(): Map<String, Any?> {
        return powerState
    }

    fun getSystemManufacturer(): String {
        TODO("Not yet implemented")
    }

    fun getInstallerPackageName(): String {
        TODO("Not yet implemented")
    }

    fun getFreeDiskStorageOld(): Double {
        TODO("Not yet implemented")
    }

    fun getTotalDiskCapacityOld(): Double {
        TODO("Not yet implemented")
    }

    fun getMacAddress(): String {
        TODO("Not yet implemented")
    }

    val UIDKey = "deviceUID"
     val kCFBooleanTrue: Boolean = true
}