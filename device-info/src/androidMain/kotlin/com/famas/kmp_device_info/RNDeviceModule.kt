package com.famas.kmp_device_info

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.hardware.Camera
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
import android.text.TextUtils
import android.webkit.WebSettings
import com.famas.kmp_device_info.resolver.DeviceIdResolver
import com.famas.kmp_device_info.resolver.DeviceTypeResolver
import java.math.BigInteger
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections

class RNDeviceModule(private val context: Context) {
    private val deviceTypeResolver: DeviceTypeResolver = DeviceTypeResolver(context)
    private val deviceIdResolver: DeviceIdResolver = DeviceIdResolver(context)
    private var receiver: BroadcastReceiver? = null
    private var headphoneConnectionReceiver: BroadcastReceiver? = null
    private val installReferrerClient: RNInstallReferrerClient = RNInstallReferrerClient(context)
    private var mLastBatteryLevel = -1.0
    private var mLastBatteryState = ""
    private var mLastPowerSaveState = false

    fun initialize() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        filter.addAction(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val powerState = getPowerStateFromIntent(intent) ?: return
                val batteryState: String = powerState[BATTERY_STATE] as String
                val batteryLevel: Double = powerState[BATTERY_LEVEL] as Double
                val powerSaveState: Boolean = powerState[LOW_POWER_MODE] as Boolean
                if (!mLastBatteryState.equals(
                        batteryState,
                        ignoreCase = true
                    ) || mLastPowerSaveState != powerSaveState
                ) {
                    sendEvent(
                        context,
                        "RNDeviceInfo_powerStateDidChange",
                        batteryState
                    )
                    mLastBatteryState = batteryState
                    mLastPowerSaveState = powerSaveState
                }
                if (mLastBatteryLevel != batteryLevel) {
                    sendEvent(
                        context,
                        "RNDeviceInfo_batteryLevelDidChange",
                        batteryLevel
                    )
                    if (batteryLevel <= .15) {
                        sendEvent(
                            context,
                            "RNDeviceInfo_batteryLevelIsLow",
                            batteryLevel
                        )
                    }
                    mLastBatteryLevel = batteryLevel
                }
            }
        }
        context.registerReceiver(receiver, filter)
        initializeHeadphoneConnectionReceiver()
    }

    private fun initializeHeadphoneConnectionReceiver() {
        val filter = IntentFilter()
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG)
        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        headphoneConnectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val isConnected: Boolean = isHeadphonesConnectedSync
                sendEvent(
                    context,
                    "RNDeviceInfo_headphoneConnectionDidChange",
                    isConnected
                )
            }
        }
        context.registerReceiver(headphoneConnectionReceiver, filter)
    }

    fun onCatalystInstanceDestroy() {
        context.unregisterReceiver(receiver)
        context.unregisterReceiver(headphoneConnectionReceiver)
    }

    private val wifiInfo: WifiInfo?
        get() {
            val manager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            return manager.connectionInfo
        }

    private val isLowRamDevice: Boolean
        get() {
            val am =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            var isLowRamDevice = false
            isLowRamDevice = am.isLowRamDevice
            return isLowRamDevice
        }

    val constants: Map<String, Any>
        get() {
            var appVersion: String
            var buildNumber: String
            var appName: String
            try {
                appVersion = packageInfo.versionName
                buildNumber = Integer.toString(packageInfo.versionCode)
                appName = context.getApplicationInfo()
                    .loadLabel(context.getPackageManager()).toString()
            } catch (e: Exception) {
                appVersion = "unknown"
                buildNumber = "unknown"
                appName = "unknown"
            }
            val constants: MutableMap<String, Any> = HashMap()
            constants["deviceId"] = Build.BOARD
            constants["bundleId"] = context.getPackageName()
            constants["systemName"] = "Android"
            constants["systemVersion"] = Build.VERSION.RELEASE
            constants["appVersion"] = appVersion
            constants["buildNumber"] = buildNumber
            constants["isTablet"] = deviceTypeResolver.isTablet
            constants["isLowRamDevice"] = isLowRamDevice
            constants["appName"] = appName
            constants["brand"] = Build.BRAND
            constants["model"] = Build.MODEL
            constants["deviceType"] = deviceTypeResolver.deviceType.value
            return constants
        }

    fun isEmulator(): Boolean {
        return isEmulatorSync
    }

    val isEmulatorSync: Boolean
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
        get() = context.resources.configuration.fontScale

    fun getFontScale(): Float {
        return fontScaleSync
    }

    private val isPinOrFingerprintSetSync: Boolean
        get() {
            val keyguardManager =
                context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            return keyguardManager.isKeyguardSecure
        }

    fun isPinOrFingerprintSet(): Boolean {
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

    fun getIpAddress(): String? {
        return ipAddressSync
    }


    @get:Suppress("deprecation")
    private val isCameraPresentSync: Boolean?
        get() = try {
            val manager =

                context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                manager.cameraIdList.size > 0
            } catch (e: Exception) {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }


    fun isCameraPresent() = isCameraPresentSync
    
    @get:SuppressLint("HardwareIds")
    val macAddressSync: String
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

    
    fun getMacAddress() = macAddressSync

    
    private val carrierSync: String
        get() {
            val telMgr =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telMgr.networkOperatorName
        }

    
    fun getCarrier() = carrierSync

    
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

    
    fun getTotalDiskCapacity() = totalDiskCapacitySync

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
                BigInteger.valueOf(rootAvailableBlocks).multiply(BigInteger.valueOf(rootBlockSize))
                    .toDouble()
            val dataAvailableBlocks = getTotalAvailableBlocks(dataDir)
            val dataBlockSize = getBlockSize(dataDir)
            val dataFree =
                BigInteger.valueOf(dataAvailableBlocks).multiply(BigInteger.valueOf(dataBlockSize))
                    .toDouble()
            rootFree + dataFree
        } catch (e: Exception) {
            (-1).toDouble()
        }

    
    fun getFreeDiskStorage() = freeDiskStorageSync

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

    
    fun getTotalDiskCapacityOld() = totalDiskCapacityOldSync

    
    val freeDiskStorageOldSync: Double
        get() = try {
            val external = StatFs(Environment.getExternalStorageDirectory().absolutePath)
            val availableBlocks: Long
            val blockSize: Long
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                availableBlocks = external.availableBlocks.toLong()
                blockSize = external.blockSize.toLong()
            } else {
                availableBlocks = external.availableBlocksLong
                blockSize = external.blockSizeLong
            }
            BigInteger.valueOf(availableBlocks).multiply(BigInteger.valueOf(blockSize)).toDouble()
        } catch (e: Exception) {
            (-1).toDouble()
        }

    
    fun getFreeDiskStorageOld() {
        freeDiskStorageOldSync)
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

    
    fun isBatteryCharging() {
        isBatteryChargingSync)
    }

    
    val usedMemorySync: Double
        get() {
            val actMgr =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return if (actMgr != null) {
                val pid = Process.myPid()
                val memInfos = actMgr.getProcessMemoryInfo(intArrayOf(pid))
                if (memInfos.size != 1) {
                    System.err.println("Unable to getProcessMemoryInfo. getProcessMemoryInfo did not return any info for the PID")
                    return (-1).toDouble()
                }
                val memInfo = memInfos[0]
                memInfo.totalPss * 1024.0
            } else {
                System.err.println("Unable to getProcessMemoryInfo. ActivityManager was null")
                (-1).toDouble()
            }
        }

    
    fun getUsedMemory() {
        usedMemorySync)
    }

    
    val powerStateSync: HashMap<String, Any>?
        get() {
            val intent: Intent? = context.registerReceiver(
                null, IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            )
            return getPowerStateFromIntent(intent)
        }

    
    fun getPowerState() = powerStateSync

    
    val batteryLevelSync: Double
        get() {
            val intent: Intent = context.registerReceiver(
                null, IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            )
            val powerState = getPowerStateFromIntent(intent) ?: return 0
            return powerState.getDouble(BATTERY_LEVEL)
        }

    
    fun getBatteryLevel() {
        batteryLevelSync)
    }

    
    val isAirplaneModeSync: Boolean
        get() {
            val isAirplaneMode: Boolean
            isAirplaneMode =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    Settings.System.getInt(
                        context.getContentResolver(),
                        Settings.System.AIRPLANE_MODE_ON,
                        0
                    ) != 0
                } else {
                    Settings.Global.getInt(
                        context.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON,
                        0
                    ) != 0
                }
            return isAirplaneMode
        }

    
    fun isAirplaneMode() {
        isAirplaneModeSync)
    }

    (isBlockingSynchronousMethod = true)
    fun hasGmsSync(): Boolean {
        return try {
            val googleApiAvailability =
                Class.forName("com.google.android.gms.common.GoogleApiAvailability")
            val getInstanceMethod = googleApiAvailability.getMethod("getInstance")
            val gmsObject = getInstanceMethod.invoke(null)
            val isGooglePlayServicesAvailableMethod =
                gmsObject.javaClass.getMethod("isGooglePlayServicesAvailable", Context::class.java)
            val isGMS = isGooglePlayServicesAvailableMethod.invoke(
                gmsObject,
                context
            ) as Int
            isGMS == 0 // ConnectionResult.SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    
    fun hasGms() {
        hasGmsSync())
    }

    (isBlockingSynchronousMethod = true)
    fun hasHmsSync(): Boolean {
        return try {
            val huaweiApiAvailability = Class.forName("com.huawei.hms.api.HuaweiApiAvailability")
            val getInstanceMethod = huaweiApiAvailability.getMethod("getInstance")
            val hmsObject = getInstanceMethod.invoke(null)
            val isHuaweiMobileServicesAvailableMethod = hmsObject.javaClass.getMethod(
                "isHuaweiMobileServicesAvailable",
                Context::class.java
            )
            val isHMS = isHuaweiMobileServicesAvailableMethod.invoke(
                hmsObject,
                context
            ) as Int
            isHMS == 0 // ConnectionResult.SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    
    fun hasHms() {
        hasHmsSync())
    }

    (isBlockingSynchronousMethod = true)
    fun hasSystemFeatureSync(feature: String?): Boolean {
        return if (feature == null || feature == "") {
            false
        } else context.getPackageManager().hasSystemFeature(feature)
    }

    
    fun hasSystemFeature(feature: String?, ) {
        hasSystemFeatureSync(feature))
    }

    
    val systemAvailableFeaturesSync: WritableArray
        get() {
            val featureList: Array<FeatureInfo> =
                context.getPackageManager().getSystemAvailableFeatures()
            val promiseArray: WritableArray = Arguments.createArray()
            for (f in featureList) {
                if (f.name != null) {
                    promiseArray.pushString(f.name)
                }
            }
            return promiseArray
        }

    
    fun getSystemAvailableFeatures() {
        systemAvailableFeaturesSync)
    }

    
    val isLocationEnabledSync: Boolean
        get() {
            val locationEnabled: Boolean
            locationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val mLocationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                try {
                    mLocationManager.isLocationEnabled
                } catch (e: Exception) {
                    System.err.println("Unable to determine if location enabled. LocationManager was null")
                    return false
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val locationMode = Secure.getInt(
                    context.getContentResolver(),
                    Secure.LOCATION_MODE,
                    Secure.LOCATION_MODE_OFF
                )
                locationMode != Secure.LOCATION_MODE_OFF
            } else {
                val locationProviders = Secure.getString(
                    context.getContentResolver(),
                    Secure.LOCATION_PROVIDERS_ALLOWED
                )
                !TextUtils.isEmpty(locationProviders)
            }
            return locationEnabled
        }

    
    fun isLocationEnabled() {
        isLocationEnabledSync)
    }

    
    val isHeadphonesConnectedSync: Boolean
        get() {
            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
        }

    
    fun isHeadphonesConnected() {
        isHeadphonesConnectedSync)
    }

    
    val availableLocationProvidersSync: WritableMap
        get() {
            val mLocationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providersAvailability: WritableMap = Arguments.createMap()
            try {
                val providers = mLocationManager.getProviders(false)
                for (provider in providers) {
                    providersAvailability.putBoolean(
                        provider, mLocationManager.isProviderEnabled(
                            provider!!
                        )
                    )
                }
            } catch (e: Exception) {
                System.err.println("Unable to get location providers. LocationManager was null")
            }
            return providersAvailability
        }

    
    fun getAvailableLocationProviders() {
        availableLocationProvidersSync)
    }

    
    val installReferrerSync: String?
        get() {
            val sharedPref = getRNDISharedPreferences(context)
            return sharedPref.getString("installReferrer", Build.UNKNOWN)
        }

    
    fun getInstallReferrer() {
        installReferrerSync)
    }

    @get:Throws(Exception::class)
    private val packageInfo: PackageInfo
        private get() = context.getPackageManager()
            .getPackageInfo(context.getPackageName(), 0)

    
    val installerPackageNameSync: String
        get() {
            val packageName: String = context.getPackageName()
            return context.getPackageManager()
                .getInstallerPackageName(packageName)
                ?: return "unknown"
        }

    
    fun getInstallerPackageName() {
        installerPackageNameSync)
    }

    
    val firstInstallTimeSync: Double
        get() = try {
            packageInfo.firstInstallTime.toDouble()
        } catch (e: Exception) {
            (-1).toDouble()
        }

    
    fun getFirstInstallTime() {
        firstInstallTimeSync)
    }

    
    val lastUpdateTimeSync: Double
        get() = try {
            packageInfo.lastUpdateTime.toDouble()
        } catch (e: Exception) {
            (-1).toDouble()
        }

    
    fun getLastUpdateTime() {
        lastUpdateTimeSync)
    }

    
    val deviceNameSync: String
        get() {
            try {
                if (Build.VERSION.SDK_INT <= 31) {
                    val bluetoothName = Secure.getString(
                        context.getContentResolver(),
                        "bluetooth_name"
                    )
                    if (bluetoothName != null) {
                        return bluetoothName
                    }
                }
                if (Build.VERSION.SDK_INT >= 25) {
                    val deviceName = Settings.Global.getString(
                        context.getContentResolver(),
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

    
    fun getDeviceName() {
        deviceNameSync)
    }

    
    @get:SuppressLint("HardwareIds", "MissingPermission")
    val serialNumberSync: String
        get() {
            try {
                return if (Build.VERSION.SDK_INT >= 26) {
                    // There are a lot of conditions to access to getSerial api
                    // For details, see https://developer.android.com/reference/android/os/Build#getSerial()
                    // Rather than check each one, just try and rely on the catch below, for discussion on this approach, refer to
                    // https://github.com/react-native-device-info/react-native-device-info/issues/1320
                    Build.getSerial()
                } else {
                    Build.SERIAL
                }
            } catch (e: Exception) {
                // This is almost always a PermissionException. We will log it but return unknown
                System.err.println("getSerialNumber failed, it probably should not be used: " + e.message)
            }
            return "unknown"
        }

    
    fun getSerialNumber() {
        serialNumberSync)
    }

    
    val deviceSync: String
        get() = Build.DEVICE

    
    fun getDevice() {
        deviceSync)
    }

    
    val buildIdSync: String
        get() = Build.ID

    
    fun getBuildId() {
        buildIdSync)
    }

    
    val apiLevelSync: Int
        get() = Build.VERSION.SDK_INT

    
    fun getApiLevel() {
        apiLevelSync)
    }

    
    val bootloaderSync: String
        get() = Build.BOOTLOADER

    
    fun getBootloader() {
        bootloaderSync)
    }

    
    val displaySync: String
        get() = Build.DISPLAY

    
    fun getDisplay() {
        displaySync)
    }

    
    val fingerprintSync: String
        get() = Build.FINGERPRINT

    
    fun getFingerprint() {
        fingerprintSync)
    }

    
    val hardwareSync: String
        get() = Build.HARDWARE

    
    fun getHardware() {
        hardwareSync)
    }

    
    val hostSync: String
        get() = Build.HOST

    
    fun getHost() {
        hostSync)
    }

    
    val productSync: String
        get() = Build.PRODUCT

    
    fun getProduct() {
        productSync)
    }

    
    val tagsSync: String
        get() = Build.TAGS

    
    fun getTags() {
        tagsSync)
    }

    
    val typeSync: String
        get() = Build.TYPE

    
    fun getType() {
        typeSync)
    }

    
    val systemManufacturerSync: String
        get() = Build.MANUFACTURER

    
    fun getSystemManufacturer() {
        systemManufacturerSync)
    }

    
    val codenameSync: String
        get() = Build.VERSION.CODENAME

    
    fun getCodename() {
        codenameSync)
    }

    
    val incrementalSync: String
        get() = Build.VERSION.INCREMENTAL

    
    fun getIncremental() {
        incrementalSync)
    }

    
    @get:SuppressLint("HardwareIds")
    val uniqueIdSync: String
        get() = Secure.getString(
            context.getContentResolver(),
            Secure.ANDROID_ID
        )

    
    fun getUniqueId() {
        uniqueIdSync)
    }

    
    @get:SuppressLint("HardwareIds")
    val androidIdSync: String
        get() = uniqueIdSync

    
    fun getAndroidId() {
        androidIdSync)
    }

    
    val maxMemorySync: Double
        get() = Runtime.getRuntime().maxMemory().toDouble()

    
    fun getMaxMemory() {
        maxMemorySync)
    }

    
    val totalMemorySync: Double
        get() {
            val actMgr =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            if (actMgr != null) {
                actMgr.getMemoryInfo(memInfo)
            } else {
                System.err.println("Unable to getMemoryInfo. ActivityManager was null")
                return (-1).toDouble()
            }
            return memInfo.totalMem.toDouble()
        }

    
    fun getTotalMemory() {
        totalMemorySync)
    }

    @get:Suppress("deprecation")
    
    val instanceIdSync: String
        get() = deviceIdResolver.instanceIdSync

    
    fun getInstanceId() {
        instanceIdSync)
    }

    
    val baseOsSync: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.BASE_OS
        } else "unknown"

    
    fun getBaseOs() {
        baseOsSync)
    }

    
    val previewSdkIntSync: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Integer.toString(Build.VERSION.PREVIEW_SDK_INT)
        } else "unknown"

    
    fun getPreviewSdkInt() {
        previewSdkIntSync)
    }

    
    val securityPatchSync: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else "unknown"

    
    fun getSecurityPatch() {
        securityPatchSync)
    }

    
    val userAgentSync: String
        get() = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                WebSettings.getDefaultUserAgent(context)
            } else {
                System.getProperty("http.agent")
            }
        } catch (e: RuntimeException) {
            System.getProperty("http.agent")
        }

    
    fun getUserAgent() {
        userAgentSync)
    }

    
    @get:SuppressLint("HardwareIds", "MissingPermission")
    val phoneNumberSync: String
        get() {
            if (context != null &&
                (context.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) === PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkCallingOrSelfPermission(
                    Manifest.permission.READ_SMS
                ) === PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.checkCallingOrSelfPermission(
                    Manifest.permission.READ_PHONE_NUMBERS
                ) === PackageManager.PERMISSION_GRANTED)
            ) {
                val telMgr =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (telMgr != null) {
                    try {
                        return telMgr.line1Number
                    } catch (e: SecurityException) {
                        System.err.println("getLine1Number called with permission, but threw anyway: " + e.message)
                    }
                } else {
                    System.err.println("Unable to getPhoneNumber. TelephonyManager was null")
                }
            }
            return "unknown"
        }

    
    fun getPhoneNumber() {
        phoneNumberSync)
    }

    
    val supportedAbisSync: WritableArray
        get() {
            val array: WritableArray = WritableNativeArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (abi in Build.SUPPORTED_ABIS) {
                    array.pushString(abi)
                }
            } else {
                array.pushString(Build.CPU_ABI)
            }
            return array
        }

    
    fun getSupportedAbis() {
        supportedAbisSync)
    }

    
    val supported32BitAbisSync: WritableArray
        get() {
            val array: WritableArray = WritableNativeArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (abi in Build.SUPPORTED_32_BIT_ABIS) {
                    array.pushString(abi)
                }
            }
            return array
        }

    
    fun getSupported32BitAbis() {
        supported32BitAbisSync)
    }

    
    val supported64BitAbisSync: WritableArray
        get() {
            val array: WritableArray = WritableNativeArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (abi in Build.SUPPORTED_64_BIT_ABIS) {
                    array.pushString(abi)
                }
            }
            return array
        }

    
    fun getSupported64BitAbis() {
        supported64BitAbisSync)
    }

    private fun getPowerStateFromIntent(intent: Intent?): HashMap<String, Any>? {
        if (intent == null) {
            return null
        }
        val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val isPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val batteryPercentage = batteryLevel / batteryScale.toFloat()
        var batteryState = "unknown"
        if (isPlugged == 0) {
            batteryState = "unplugged"
        } else if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            batteryState = "charging"
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            batteryState = "full"
        }
        val powerManager =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager
        var powerSaveMode = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerSaveMode = powerManager.isPowerSaveMode
        }
        val powerState = HashMap<String, Any>()
        powerState[BATTERY_STATE] = batteryState
        powerState[BATTERY_LEVEL] = batteryPercentage
        powerState[LOW_POWER_MODE] = powerSaveMode
        return powerState
    }

    private fun sendEvent(
        reactContext: ReactContext,
        eventName: String,
        @Nullable data: Any
    ) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, data)
    }

    
    val supportedMediaTypeListSync: WritableArray
        get() {
            val writableArray: WritableArray = WritableNativeArray()
            for (i in 0 until MediaCodecList.getCodecCount()) {
                val mediaCodecInfo = MediaCodecList.getCodecInfoAt(i)
                val supportedTypes = mediaCodecInfo.supportedTypes
                for (j in supportedTypes.indices) {
                    writableArray.pushString(supportedTypes[j])
                }
            }
            return writableArray
        }

    
    fun getSupportedMediaTypeList(promise: Promise) {
        promise.resolve(supportedMediaTypeListSync)
    }

    companion object {
        @get:Nonnull
        val name = "RNDeviceInfo"
            get() = Companion.field
        private const val BATTERY_STATE = "batteryState"
        private const val BATTERY_LEVEL = "batteryLevel"
        private const val LOW_POWER_MODE = "lowPowerMode"
        @JvmStatic
        fun getRNDISharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("react-native-device-info", Context.MODE_PRIVATE)
        }
    }
}
