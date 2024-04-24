package com.famas.kmp_device_info

import kotlin.concurrent.Volatile

expect class DeviceInfo {
    companion object {
        fun isEmulator(): Boolean

        fun getPlatFormType(): PlatformType

        fun getFontScale(): Float

        fun isPinOrFingerprintSet(): Boolean

        fun getIpAddress(): String?

        fun isCameraPresent(): Boolean?

        fun getMacAddress(): String

        fun getCarrier(): String

        fun getTotalDiskCapacity(): Double

        fun getFreeDiskStorage(): Double

        fun getTotalDiskCapacityOld(): Double

        fun getFreeDiskStorageOld(): Double

        fun isBatteryCharging(): Boolean

        fun getUsedMemory(): Double

        fun getPowerState(): PowerState?

        fun getBatteryLevel(): Double

        fun isAirplaneMode(): Boolean

        fun hasGms(): Boolean

        fun hasHms(): Boolean

        fun hasSystemFeature(feature: String?): Boolean

        fun getSystemAvailableFeatures(): List<String>

        fun isLocationEnabled(): Boolean

        fun isHeadphonesConnected(): Boolean

        fun getAvailableLocationProviders(): Map<String, Boolean>

        fun getInstallReferrer(): String?

        fun getInstallerPackageName(): String

        fun getFirstInstallTime(): Double

        fun getLastUpdateTime(): Double

        fun getDeviceName(): String

        fun getSerialNumber(): String

        fun getDevice(): String

        fun getBuildId(): String

        fun getApiLevel(): Int

        fun getBootloader(): String

        fun getDisplay(): String

        fun getFingerprint(): String

        fun getHardware(): String

        fun getHost(): String

        fun getProduct(): String

        fun getTags(): String

        fun getType(): String

        fun getSystemManufacturer(): String

        fun getCodename(): String

        fun getIncremental(): String

        fun getUniqueId(): String

        fun getAndroidId(): String

        fun getMaxMemory(): Double

        fun getTotalMemory(): Double

        fun getInstanceId(): String?

        fun getBaseOs(): String

        fun getPreviewSdkInt(): String

        fun getSecurityPatch(): String

        suspend fun getUserAgent(): String?

        fun getPhoneNumber(): String

        fun getSupportedAbis(): List<String>

        fun getSupported32BitAbis(): List<String>

        fun getSupported64BitAbis(): List<String>

        fun getSupportedMediaTypeList(): List<String>

        fun isLowRamDevice(): Boolean

        fun isDisplayZoomed(): Boolean

        fun getBrightness(): Float

        suspend fun getDeviceToken(): String?

        fun getInfoConstants(): InfoConstants
    }
}