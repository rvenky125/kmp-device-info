package com.famas.kmp_device_info


import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionPortBluetoothA2DP
import platform.AVFAudio.AVAudioSessionPortBluetoothHFP
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.AVAudioSessionPortHeadphones
import platform.AVFAudio.currentRoute
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreLocation.CLLocationManager
import platform.CoreTelephony.CTTelephonyNetworkInfo
import platform.DeviceCheck.DCDevice
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSBundle
import platform.Foundation.NSDataBase64EncodingEndLineWithLineFeed
import platform.Foundation.NSDictionary
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSMutableData
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.isLowPowerModeEnabled
import platform.Foundation.isiOSAppOnMac
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.Security.SecItemAdd
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecClass
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceIdiomMac
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIUserInterfaceIdiomPhone
import platform.UIKit.UIUserInterfaceIdiomTV
import platform.WebKit.WKWebView
import platform.darwin.TARGET_IPHONE_SIMULATOR
import platform.darwin.TARGET_OS_MACCATALYST
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_ntop
import platform.darwin.sysctlbyname
import platform.darwin.task_basic_info
import platform.darwin.task_info_t
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.INET6_ADDRSTRLEN
import platform.posix.INET_ADDRSTRLEN
import platform.posix.sa_family_t
import platform.posix.sockaddr_in
import platform.posix.sockaddr_in6
import platform.posix.uname
import platform.posix.utsname
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.toString

@OptIn(ExperimentalForeignApi::class)
object DeviceInfoFactory {

    fun setValue(value: String, forUserDefaultsKey: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = forUserDefaultsKey)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    private fun keychainItemForKey(key: String, service: String): CFMutableDictionaryRef? {
        memScoped {
            val keychainItem = CFDictionaryCreateMutable(null, 4, null, null)
            val cfKey = CFBridgingRetain(key as NSString) as CFStringRef
            val cfService = CFBridgingRetain(service as NSString) as CFStringRef

            CFDictionaryAddValue(keychainItem, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(
                keychainItem,
                kSecAttrAccessible,
                kSecAttrAccessibleAfterFirstUnlock
            )
            CFDictionaryAddValue(keychainItem, kSecReturnData, cfKey)
            CFDictionaryAddValue(keychainItem, kSecReturnAttributes, cfService)

            return keychainItem
        }
    }

    fun setValue(value: String, forKeychainKey: String, inService: String): Boolean {
        val keychainItem = keychainItemForKey(forKeychainKey, service = inService)
        CFDictionaryAddValue(
            keychainItem,
            kSecValueData,
            CFBridgingRetain(value as NSString) as CFStringRef
        )
        return SecItemAdd(keychainItem as CFDictionaryRef, null) == errSecSuccess
    }

    private var hasListeners: Boolean = false

    fun getInfoConstants(): InfoConstants {
        return InfoConstants(
            boardName = getDeviceId(),
            bundleId = getBundleId(),
            systemName = getSystemName(),
            systemVersion = getSystemVersion(),
            appVersion = getAppVersion(),
            buildNumber = getBuildNumber(),
            isTablet = isTablet(),
            isLowRamDevice = null,
            appName = getAppName() ?: "",
            brand = "Apple",
            model = getModel(),
            deviceType = getDeviceType(),
        )
    }

    private fun getDeviceType(): DeviceType {
        return when (UIDevice.currentDevice.userInterfaceIdiom) {
            UIUserInterfaceIdiomPhone -> DeviceType.HANDSET
            UIUserInterfaceIdiomPad -> {
                if (TARGET_OS_MACCATALYST == 1) {
                    return DeviceType.DESKTOP
                }

                return if (UIDevice.currentDevice.systemVersion.toFloat() <= 14.0f) {
                    if (NSProcessInfo.processInfo.isiOSAppOnMac()) {
                        DeviceType.DESKTOP
                    } else {
                        DeviceType.TABLET
                    }
                } else {
                    DeviceType.TABLET
                }
            }

            UIUserInterfaceIdiomTV -> DeviceType.TV
            UIUserInterfaceIdiomMac -> DeviceType.DESKTOP
            else -> DeviceType.UNKNOWN
        }
    }

    private fun getStorageDictionary(): NSDictionary? {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        return (NSFileManager.defaultManager.attributesOfFileSystemForPath(
            paths.last().toString(),
            error = null
        ) as? NSDictionary)
    }

    private fun getSystemName(): String {
        return UIDevice.currentDevice.systemName
    }

    private fun getSystemVersion(): String {
        return UIDevice.currentDevice.systemVersion
    }

    fun getDeviceName(): String {
        return UIDevice.currentDevice.name
    }

    fun isDisplayZoomed(): Boolean {
        return UIScreen.mainScreen.scale != UIScreen.mainScreen.nativeScale
    }

    private fun getAppName(): String? {
        val displayName =
            NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleDisplayName") as String?
        val bundleName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleName") as String?
        return displayName ?: bundleName
    }

    private fun getBundleId(): String {
        return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleIdentifier") as String
    }

    private fun getAppVersion(): String {
        return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as String
    }

    private fun getBuildNumber(): String {
        return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as String
    }

    private fun getDeviceNamesByCode(): Map<String, String> {
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
        return getDeviceType() == DeviceType.TABLET
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
            ((deviceType == DeviceType.HANDSET || deviceType == DeviceType.TV || deviceType == DeviceType.TABLET) && UIDevice.currentDevice.systemVersion.toFloat() >= 10.0) || ((deviceType == DeviceType.UNKNOWN || deviceType == DeviceType.DESKTOP) && UIDevice.currentDevice.systemVersion.toFloat() >= 13.0)
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

    fun getIpAddress(): String? {
        val (status, interfaces) = memScoped {
            val ifap = allocPointerTo<ifaddrs>()
            getifaddrs(ifap.ptr) to ifap.value
        }
        return try {
            generateSequence(interfaces.takeIf { status == 0 }) { it.pointed.ifa_next }
                .mapNotNull { it.pointed.ifa_addr }
                .mapNotNull {
                    val addr = when (it.pointed.sa_family) {
                        AF_INET.convert<sa_family_t>() -> it.reinterpret<sockaddr_in>().pointed.sin_addr
                        AF_INET6.convert<sa_family_t>() -> it.reinterpret<sockaddr_in6>().pointed.sin6_addr
                        else -> return@mapNotNull null
                    }
                    memScoped {
                        val len = maxOf(INET_ADDRSTRLEN, INET6_ADDRSTRLEN)
                        val dst = allocArray<ByteVar>(len)
                        inet_ntop(
                            it.pointed.sa_family.convert(),
                            addr.ptr,
                            dst,
                            len.convert()
                        )?.toKString()
                    }
                }
                .toList().joinToString(",")
        } catch (e: Exception) {
            null
        } finally {
            freeifaddrs(interfaces)
        }
    }

    fun isPinOrFingerprintSet(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)
    }

    fun getBatteryLevel(): Float {
        return UIDevice.currentDevice.batteryLevel
    }

    fun brightnessDidChange(notification: NSNotification): Float {
        if (!hasListeners) {
            return 0f
        }
        return getBrightness()
    }

    private val powerState: PowerState
        get() {
            floatArrayOf(getBatteryLevel())

            return PowerState(
                getBatteryLevel(),
                when (UIDevice.currentDevice.batteryState) {
                    UIDeviceBatteryState.UIDeviceBatteryStateUnknown -> BatterState.UNKNOWN
                    UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> BatterState.UNPLUGGED
                    UIDeviceBatteryState.UIDeviceBatteryStateCharging -> BatterState.CHARGING
                    UIDeviceBatteryState.UIDeviceBatteryStateFull -> BatterState.FULL
                    else -> BatterState.UNKNOWN
                },
                NSProcessInfo.processInfo.isLowPowerModeEnabled()
            )
        }

    fun isBatteryCharging(): Boolean {
        return powerState.batteryState == BatterState.CHARGING
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
//        val documentsFolderUrl = NSFileManager.defaultManager.URLsForDirectory(
//            NSDocumentDirectory,
//            NSUserDomainMask
//        ).last() as NSURL
//
//        val error = ObjCObjectVar<NSError?>(NSError.objcPtr()).ptr
//
//        val installDate = documentsFolderUrl.path?.let {
//            NSFileManager.defaultManager.attributesOfItemAtPath(
//                it,
//                error
//            )
//        }
//        return (installDate?.get(NSFileAttributeKey) as? NSDate)?.timeIntervalSince1970?.times(
//            1000
//        )?.toLong() ?: -1
        return 0L
    }

    fun dealloc() {
        NSNotificationCenter.defaultCenter.removeObserver(this)
    }

    fun getPowerState(): PowerState {
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