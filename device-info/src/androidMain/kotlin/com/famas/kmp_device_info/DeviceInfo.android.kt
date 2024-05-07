package com.famas.kmp_device_info

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaCodecList
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import android.provider.Settings
import android.provider.Settings.Secure
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import com.famas.kmp_device_info.resolver.DeviceTypeResolver
import java.math.BigInteger
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections

actual class DeviceInfo {
    actual companion object {
        private lateinit var deviceTypeResolver: DeviceTypeResolver
        private lateinit var context: Context
        private lateinit var wifiManager: WifiManager
        private lateinit var activityManager: ActivityManager
        private lateinit var keyguardManager: KeyguardManager
        private lateinit var cameraManager: CameraManager
        private lateinit var telephonyManager: TelephonyManager
        private lateinit var audioManager: AudioManager
        private lateinit var locationManager: LocationManager
        private lateinit var powerManager: PowerManager
        private lateinit var packageManager: PackageManager
        private lateinit var contentResolver: ContentResolver
        private lateinit var receiver: BroadcastReceiver
        private lateinit var headphoneConnectionReceiver: BroadcastReceiver
        private var mLastBatteryLevel = -1.0
        private var mLastBatteryState = BatterState.UNKNOWN
        private var mLastPowerSaveState = false
        private lateinit var resources: Resources

        fun initialize(context: Context) {
            this.context = context

            deviceTypeResolver = DeviceTypeResolver(context)
            wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            packageManager = context.packageManager
            contentResolver = context.contentResolver
            resources = context.resources

            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_BATTERY_CHANGED)
            filter.addAction(Intent.ACTION_POWER_CONNECTED)
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val powerState = getPowerStateFromIntent(intent) ?: return
                    val batteryState = powerState.batteryState
                    val batteryLevel = powerState.batteryLevel.toDouble()
                    val powerSaveState: Boolean = powerState.isLowPowerMode
                    if ((mLastBatteryState != batteryState) || mLastPowerSaveState != powerSaveState
                    ) {
                        mLastBatteryState = batteryState
                        mLastPowerSaveState = powerSaveState
                    }
                    if (mLastBatteryLevel != batteryLevel) {
                        mLastBatteryLevel = batteryLevel
                    }
                }
            }
            context.registerReceiver(receiver, filter)
            initializeHeadphoneConnectionReceiver(context)
        }

        private fun initializeHeadphoneConnectionReceiver(context: Context) {
            val filter = IntentFilter()
            filter.addAction(AudioManager.ACTION_HEADSET_PLUG)
            filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            headphoneConnectionReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val isConnected: Boolean = isHeadphonesConnectedSync
                    print(isConnected)
                }
            }
            context.registerReceiver(headphoneConnectionReceiver, filter)
        }

        private val wifiInfo: WifiInfo?
            get() = wifiManager.connectionInfo

        actual fun isLowRamDevice(): Boolean {
            return activityManager.isLowRamDevice
        }

        actual fun getInfoConstants(): InfoConstants {
            var appVersion: String
            var buildNumber: String
            var appName: String
            try {
                appVersion = packageInfo.versionName
                buildNumber = packageInfo.versionCode.toString()
                appName = packageManager
                    .getApplicationLabel(packageManager.getApplicationInfo(packageInfo.packageName, 0)).toString()
            } catch (e: Exception) {
                appVersion = "unknown"
                buildNumber = "unknown"
                appName = "unknown"
            }

            return InfoConstants(
                boardName = Build.BOARD,
                bundleId = packageInfo.packageName,
                systemName = "Android",
                systemVersion = Build.VERSION.RELEASE,
                appVersion = appVersion,
                buildNumber = buildNumber,
                isTablet = deviceTypeResolver.isTablet,
                isLowRamDevice = isLowRamDevice(),
                appName = appName,
                brand = Build.BRAND,
                model = Build.MODEL,
                deviceType = deviceTypeResolver.deviceType,
            )
        }

        actual fun isEmulator(): Boolean {
            return isEmulatorSync
        }

        val isEmulatorSync: Boolean
            @SuppressLint("HardwareIds")
            get() = (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.lowercase().contains("droid4x")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.HARDWARE.contains("vbox86")
                    || Build.PRODUCT.contains("sdk")
                    || Build.PRODUCT.contains("google_sdk")
                    || Build.PRODUCT.contains("sdk_google")
                    || Build.PRODUCT.contains("sdk_x86")
                    || Build.PRODUCT.contains("vbox86p")
                    || Build.PRODUCT.contains("emulator")
                    || Build.PRODUCT.contains("simulator")
                    || Build.BOARD.lowercase().contains("nox")
                    || Build.BOOTLOADER.lowercase().contains("nox")
                    || Build.HARDWARE.lowercase().contains("nox")
                    || Build.PRODUCT.lowercase().contains("nox")
                    || Build.SERIAL.lowercase()
                .contains("nox") || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))

        private val fontScaleSync: Float
            get() = resources.configuration?.fontScale ?: 1f

        actual fun getFontScale(): Float {
            return fontScaleSync
        }

        private val isPinOrFingerprintSetSync: Boolean
            get() = keyguardManager.isKeyguardSecure

        actual fun isPinOrFingerprintSet(): Boolean {
            return isPinOrFingerprintSetSync
        }

        val ipAddressSync: String?
            get() = try {
                InetAddress.getByAddress(
                    ByteBuffer
                        .allocate(4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(wifiInfo!!.ipAddress)
                        .array()
                ).hostAddress
            } catch (e: Exception) {
                null
            }

        actual fun getIpAddress(): String? {
            return ipAddressSync
        }

        private val isCameraPresentSync: Boolean?
            get() = try {
                try {
                    cameraManager.cameraIdList.isNotEmpty()
                } catch (e: Exception) {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        actual fun isCameraPresent() = isCameraPresentSync

        private val macAddressSync: String
            @SuppressLint("HardwareIds")
            get() {
                val wifiInfo = wifiInfo
                var macAddress = ""
                if (wifiInfo != null) {
                    macAddress = wifiInfo.macAddress
                }
                val permission = "android.permission.INTERNET"
                val res: Int = context.checkCallingOrSelfPermission(permission)
                if (res == PackageManager.PERMISSION_GRANTED) {
                    try {
                        val all: List<NetworkInterface> =
                            Collections.list(NetworkInterface.getNetworkInterfaces())
                        for (nif in all) {
                            if (!nif.name.equals("wlan0", ignoreCase = true)) continue
                            val macBytes = nif.hardwareAddress
                            macAddress = if (macBytes == null) {
                                ""
                            } else {
                                val res1 = StringBuilder()
                                for (b in macBytes) {
                                    res1.append(String.format("%02X:", b))
                                }
                                if (res1.isNotEmpty()) {
                                    res1.deleteCharAt(res1.length - 1)
                                }
                                res1.toString()
                            }
                        }
                    } catch (ex: Exception) {
                        // do nothing
                    }
                }
                return macAddress
            }


        actual fun getMacAddress() = macAddressSync


        private val carrierSync: String
            get() = telephonyManager.networkOperatorName


        actual fun getCarrier() = carrierSync


        private val totalDiskCapacitySync: Double
            get() = try {
                val rootDir = StatFs(Environment.getRootDirectory().absolutePath)
                val dataDir = StatFs(Environment.getDataDirectory().absolutePath)
                val rootDirCapacity = getDirTotalCapacity(rootDir)
                val dataDirCapacity = getDirTotalCapacity(dataDir)
                rootDirCapacity.add(dataDirCapacity).toDouble()
            } catch (e: Exception) {
                (-1).toDouble()
            }


        actual fun getTotalDiskCapacity() = totalDiskCapacitySync

        private fun getDirTotalCapacity(dir: StatFs): BigInteger {
            val blockCount = dir.blockCountLong
            val blockSize = dir.blockSizeLong
            return BigInteger.valueOf(blockCount).multiply(BigInteger.valueOf(blockSize))
        }


        private val freeDiskStorageSync: Double
            get() = try {
                val rootDir = StatFs(Environment.getRootDirectory().absolutePath)
                val dataDir = StatFs(Environment.getDataDirectory().absolutePath)
                val rootAvailableBlocks = getTotalAvailableBlocks(rootDir)
                val rootBlockSize = getBlockSize(rootDir)
                val rootFree =
                    BigInteger.valueOf(rootAvailableBlocks)
                        .multiply(BigInteger.valueOf(rootBlockSize))
                        .toDouble()
                val dataAvailableBlocks = getTotalAvailableBlocks(dataDir)
                val dataBlockSize = getBlockSize(dataDir)
                val dataFree =
                    BigInteger.valueOf(dataAvailableBlocks)
                        .multiply(BigInteger.valueOf(dataBlockSize))
                        .toDouble()
                rootFree + dataFree
            } catch (e: Exception) {
                (-1).toDouble()
            }


        actual fun getFreeDiskStorage() = freeDiskStorageSync

        private fun getTotalAvailableBlocks(dir: StatFs): Long {
            return dir.availableBlocksLong
        }

        private fun getBlockSize(dir: StatFs): Long {
            return dir.blockSizeLong
        }

        private val totalDiskCapacityOldSync: Double
            get() = try {
                val root = StatFs(Environment.getRootDirectory().absolutePath)
                BigInteger.valueOf(root.blockCount.toLong())
                    .multiply(BigInteger.valueOf(root.blockSize.toLong())).toDouble()
            } catch (e: Exception) {
                (-1).toDouble()
            }


        actual fun getTotalDiskCapacityOld() = totalDiskCapacityOldSync


        private val freeDiskStorageOldSync: Double
            get() = try {
                val external = StatFs(Environment.getExternalStorageDirectory().absolutePath)
                val availableBlocks: Long = external.availableBlocksLong
                val blockSize: Long = external.blockSizeLong
                BigInteger.valueOf(availableBlocks).multiply(BigInteger.valueOf(blockSize))
                    .toDouble()
            } catch (e: Exception) {
                (-1).toDouble()
            }


        actual fun getFreeDiskStorageOld(): Double {
            return freeDiskStorageOldSync
        }


        val isBatteryChargingSync: Boolean
            get() {
                val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
                var status = 0
                if (batteryStatus != null) {
                    status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                }
                return status == BatteryManager.BATTERY_STATUS_CHARGING
            }


        actual fun isBatteryCharging(): Boolean {
            return isBatteryChargingSync
        }

        private val usedMemorySync: Double
            get() {
                return try {
                    val pid = Process.myPid()
                    val memInfos = activityManager.getProcessMemoryInfo(intArrayOf(pid))
                    if (memInfos.size != 1) {
                        System.err.println("Unable to getProcessMemoryInfo. getProcessMemoryInfo did not return any info for the PID")
                        return (-1).toDouble()
                    }
                    val memInfo = memInfos[0]
                    memInfo.totalPss * 1024.0
                } catch (e: Exception) {
                    System.err.println("Unable to getProcessMemoryInfo. ActivityManager was null")
                    (-1).toDouble()
                }
            }

        actual fun getUsedMemory(): Double {
            return usedMemorySync
        }

        actual fun getPowerState(): PowerState? {
            val intent: Intent? = context.registerReceiver(
                null, IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            )
            return getPowerStateFromIntent(intent)
        }

        actual fun getBatteryLevel(): Double {
            val intent: Intent? = context.registerReceiver(
                null, IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            )
            val powerState = getPowerStateFromIntent(intent) ?: return 0.0
            return powerState.batteryLevel.toDouble()
        }

        private val isAirplaneModeSync: Boolean
            get() {
                return Settings.Global.getInt(
                    contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    0
                ) != 0
            }

        actual fun isAirplaneMode() = isAirplaneModeSync

        private fun hasSystemFeatureSync(feature: String?): Boolean {
            return if (feature == null || feature == "") {
                false
            } else packageManager.hasSystemFeature(feature)
        }

        actual fun hasSystemFeature(feature: String?): Boolean {
            return hasSystemFeatureSync(feature)
        }

        private val systemAvailableFeaturesSync: List<String>
            get() {
                val featureList: Array<FeatureInfo> =
                    packageManager.systemAvailableFeatures
                val promiseArray = listOf<String>()
                for (f in featureList) {
                    if (f.name != null) {
                        promiseArray.plus(f.name)
                    }
                }
                return promiseArray
            }

        actual fun getSystemAvailableFeatures(): List<String> {
            return systemAvailableFeaturesSync
        }

        private val isLocationEnabledSync: Boolean
            get() {
                val locationEnabled: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        locationManager.isLocationEnabled
                    } catch (e: Exception) {
                        System.err.println("Unable to determine if location enabled. LocationManager was null")
                        return false
                    }
                } else {
                    val locationMode = Secure.getInt(
                        contentResolver,
                        Secure.LOCATION_MODE,
                        Secure.LOCATION_MODE_OFF
                    )
                    locationMode != Secure.LOCATION_MODE_OFF
                }
                return locationEnabled
            }

        actual fun isLocationEnabled(): Boolean {
            return isLocationEnabledSync
        }

        val isHeadphonesConnectedSync: Boolean
            get() {
                return audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
            }

        actual fun isHeadphonesConnected(): Boolean {
            return isHeadphonesConnectedSync
        }

        private val availableLocationProvidersSync: Map<String, Boolean>
            get() {
                val providersAvailability = mutableMapOf<String, Boolean>()
                try {
                    val providers = locationManager.getProviders(false)
                    for (provider in providers) {
                        providersAvailability[provider] = locationManager.isProviderEnabled(
                            provider!!
                        )
                    }
                } catch (e: Exception) {
                    System.err.println("Unable to get location providers. LocationManager was null")
                }
                return providersAvailability
            }

        actual fun getAvailableLocationProviders(): Map<String, Boolean> {
            return availableLocationProvidersSync
        }

        private val packageInfo: PackageInfo
            get() = packageManager
                .getPackageInfo(packageInfo.packageName, 0)

        private val installerPackageNameSync: String
            get() {
                val packageName: String = packageInfo.packageName
                return packageManager
                    .getInstallerPackageName(packageName)
                    ?: return "unknown"
            }

        actual fun getInstallerPackageName(): String {
            return installerPackageNameSync
        }

        private val firstInstallTimeSync: Double
            get() = try {
                packageInfo.firstInstallTime.toDouble()
            } catch (e: Exception) {
                (-1).toDouble()
            }

        actual fun getFirstInstallTime(): Double {
            return firstInstallTimeSync
        }

        private val lastUpdateTimeSync: Double
            get() = try {
                packageInfo.lastUpdateTime.toDouble()
            } catch (e: Exception) {
                (-1).toDouble()
            }

        actual fun getLastUpdateTime(): Double {
            return lastUpdateTimeSync
        }

        val deviceNameSync: String
            get() {
                try {
                    if (Build.VERSION.SDK_INT <= 31) {
                        val bluetoothName = Secure.getString(
                            contentResolver,
                            "bluetooth_name"
                        )
                        if (bluetoothName != null) {
                            return bluetoothName
                        }
                    }
                    if (Build.VERSION.SDK_INT >= 25) {
                        val deviceName = Settings.Global.getString(
                            contentResolver,
                            Settings.Global.DEVICE_NAME
                        )
                        if (deviceName != null) {
                            return deviceName
                        }
                    }
                } catch (e: Exception) {
                    // same as default unknown return
                }
                return "unknown"
            }

        actual fun getDeviceName(): String {
            return deviceNameSync
        }

        @get:SuppressLint("HardwareIds", "MissingPermission")
        val serialNumberSync: String
            get() {
                try {
                    return if (Build.VERSION.SDK_INT >= 26) {
                        Build.getSerial()
                    } else {
                        Build.SERIAL
                    }
                } catch (e: Exception) {
                    System.err.println("getSerialNumber failed, it probably should not be used: " + e.message)
                }
                return "unknown"
            }

        actual fun getSerialNumber(): String {
            return serialNumberSync
        }

        private val deviceSync: String
            get() = Build.DEVICE

        actual fun getDevice(): String {
            return deviceSync
        }

        val buildIdSync: String
            get() = Build.ID

        actual fun getBuildId(): String {
            return buildIdSync
        }

        private val apiLevelSync: Int
            get() = Build.VERSION.SDK_INT

        actual fun getApiLevel(): Int {
            return apiLevelSync
        }

        private val bootloaderSync: String
            get() = Build.BOOTLOADER

        actual fun getBootloader(): String {
            return bootloaderSync
        }

        private val displaySync: String
            get() = Build.DISPLAY

        actual fun getDisplay(): String {
            return displaySync
        }

        val fingerprintSync: String
            get() = Build.FINGERPRINT

        actual fun getFingerprint(): String {
            return fingerprintSync
        }

        val hardwareSync: String
            get() = Build.HARDWARE

        actual fun getHardware(): String {
            return hardwareSync
        }

        val hostSync: String
            get() = Build.HOST

        actual fun getHost(): String {
            return hostSync
        }

        val productSync: String
            get() = Build.PRODUCT

        actual fun getProduct(): String {
            return productSync
        }

        val tagsSync: String
            get() = Build.TAGS

        actual fun getTags(): String {
            return tagsSync
        }

        val typeSync: String
            get() = Build.TYPE

        actual fun getType(): String {
            return typeSync
        }

        val systemManufacturerSync: String
            get() = Build.MANUFACTURER

        actual fun getSystemManufacturer(): String {
            return systemManufacturerSync
        }

        val codenameSync: String
            get() = Build.VERSION.CODENAME

        actual fun getCodename(): String {
            return codenameSync
        }

        val incrementalSync: String
            get() = Build.VERSION.INCREMENTAL

        actual fun getIncremental(): String {
            return incrementalSync
        }

        @get:SuppressLint("HardwareIds")
        val uniqueIdSync: String
            get() = Secure.getString(
                contentResolver,
                Secure.ANDROID_ID
            )

        actual fun getUniqueId(): String {
            return uniqueIdSync
        }

        @get:SuppressLint("HardwareIds")
        val androidIdSync: String
            get() = uniqueIdSync

        actual fun getAndroidId(): String {
            return androidIdSync
        }

        private val maxMemorySync: Double
            get() = Runtime.getRuntime().maxMemory().toDouble()

        actual fun getMaxMemory(): Double {
            return maxMemorySync
        }

        private val totalMemorySync: Double
            get() {
                val memInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                return memInfo.totalMem.toDouble()
            }

        actual fun getTotalMemory(): Double {
            return totalMemorySync
        }

        private val baseOsSync: String
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.BASE_OS
            } else "unknown"

        actual fun getBaseOs(): String {
            return baseOsSync
        }

        private val previewSdkIntSync: String
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.PREVIEW_SDK_INT.toString()
            } else "unknown"

        actual fun getPreviewSdkInt(): String {
            return previewSdkIntSync
        }

        private val securityPatchSync: String
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH
            } else "unknown"

        actual fun getSecurityPatch(): String {
            return securityPatchSync
        }

        private val userAgentSync: String?
            get() = try {
                WebSettings.getDefaultUserAgent(context)
            } catch (e: RuntimeException) {
                System.getProperty("http.agent")?.toString()
            }

        actual suspend fun getUserAgent(): String? {
            return userAgentSync
        }

        @get:SuppressLint("HardwareIds", "MissingPermission")
        val phoneNumberSync: String
            get() {
                if (context.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkCallingOrSelfPermission(
                        Manifest.permission.READ_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        return telephonyManager.line1Number
                    } catch (e: SecurityException) {
                        System.err.println("getLine1Number called with permission, but threw anyway: " + e.message)
                    }
                }
                return "unknown"
            }

        actual fun getPhoneNumber(): String {
            return phoneNumberSync
        }

        val supportedAbisSync: List<String>
            get() {
                val array = listOf<String>()
                for (abi in Build.SUPPORTED_ABIS) {
                    array.plus(abi)
                }
                return array
            }

        actual fun getSupportedAbis(): List<String> {
            return supportedAbisSync
        }

        private val supported32BitAbisSync: List<String>
            get() {
                val array = listOf<String>()
                for (abi in Build.SUPPORTED_32_BIT_ABIS) {
                    array.plus(abi)
                }
                return array
            }

        actual fun getSupported32BitAbis(): List<String> {
            return supported32BitAbisSync
        }

        private val supported64BitAbisSync: List<String>
            get() {
                val array = listOf<String>()
                for (abi in Build.SUPPORTED_64_BIT_ABIS) {
                    array.plus(abi)
                }
                return array
            }

        actual fun getSupported64BitAbis(): List<String> {
            return supported64BitAbisSync
        }

        private fun getPowerStateFromIntent(intent: Intent?): PowerState? {
            if (intent == null) {
                return null
            }
            val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val isPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val batteryPercentage = batteryLevel / batteryScale.toFloat()
            var batteryState = BatterState.UNKNOWN
            if (isPlugged == 0) {
                batteryState = BatterState.UNPLUGGED
            } else if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                batteryState = BatterState.CHARGING
            } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                batteryState = BatterState.FULL
            }
            val powerSaveMode = powerManager.isPowerSaveMode
            return PowerState(
                batteryPercentage,
                batteryState = batteryState,
                isLowPowerMode = powerSaveMode
            )
        }

        private val supportedMediaTypeListSync: List<String>
            get() {
                val writableArray = listOf<String>()
                for (i in 0 until MediaCodecList.getCodecCount()) {
                    val mediaCodecInfo = MediaCodecList.getCodecInfoAt(i)
                    val supportedTypes = mediaCodecInfo.supportedTypes
                    for (j in supportedTypes.indices) {
                        writableArray.plus(supportedTypes[j])
                    }
                }
                return writableArray
            }


        actual fun getSupportedMediaTypeList(): List<String> {
            return supportedMediaTypeListSync
        }


        actual fun isDisplayZoomed(): Boolean {
            throw NotAvailableToPlatformException
        }

        actual fun getBrightness(): Float {
            throw NotAvailableToPlatformException
        }

        actual suspend fun getDeviceToken(): String? {
            throw NotAvailableToPlatformException
        }

        actual fun getPlatFormType(): PlatformType {
            return PlatformType.ANDROID
        }
    }
}
