package com.famas.kmp_device_info

import kotlin.coroutines.cancellation.CancellationException

expect class DeviceInfo {
    @Throws(NotAvailableToPlatformException::class)
    fun isEmulator(): Boolean

    fun getFontScale(): Float

    fun isPinOrFingerprintSet(): Boolean

    fun getIpAddress(): String?

    @Throws(NotAvailableToPlatformException::class)
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

    @Throws(NotAvailableToPlatformException::class)
    fun hasGms(): Boolean

    @Throws(NotAvailableToPlatformException::class)
    fun hasHms(): Boolean

    fun hasSystemFeature(feature: String?): Boolean

    fun getSystemAvailableFeatures(): List<String>

    fun isLocationEnabled(): Boolean

    fun isHeadphonesConnected(): Boolean

    fun getAvailableLocationProviders(): Map<String, Boolean>

    @Throws(NotAvailableToPlatformException::class)
    fun getInstallReferrer(): String?

    fun getInstallerPackageName(): String

    fun getFirstInstallTime(): Double

    @Throws(NotAvailableToPlatformException::class)
    fun getLastUpdateTime(): Double

    fun getDeviceName(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getSerialNumber(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getDevice(): String

    fun getBuildId(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getApiLevel(): Int

    @Throws(NotAvailableToPlatformException::class)
    fun getBootloader(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getDisplay(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getFingerprint(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getHardware(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getHost(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getProduct(): String

    fun getTags(): String

    fun getType(): String

    fun getSystemManufacturer(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getCodename(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getIncremental(): String

    fun getUniqueId(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getAndroidId(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getMaxMemory(): Double

    fun getTotalMemory(): Double

    @Throws(NotAvailableToPlatformException::class)
    fun getInstanceId(): String?

    @Throws(NotAvailableToPlatformException::class)
    fun getBaseOs(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getPreviewSdkInt(): String

    @Throws(NotAvailableToPlatformException::class)
    fun getSecurityPatch(): String

    @Throws(NotAvailableToPlatformException::class, CancellationException::class)
    suspend fun getUserAgent(): String?

    @Throws(NotAvailableToPlatformException::class)
    fun getPhoneNumber(): String

    fun getSupportedAbis(): List<String>

    @Throws(NotAvailableToPlatformException::class)
    fun getSupported32BitAbis(): List<String>

    @Throws(NotAvailableToPlatformException::class)
    fun getSupported64BitAbis(): List<String>

    @Throws(NotAvailableToPlatformException::class)
    fun getSupportedMediaTypeList(): List<String>

    @Throws(NotAvailableToPlatformException::class)
    fun isLowRamDevice(): Boolean

    fun isDisplayZoomed(): Boolean

    @Throws(NotAvailableToPlatformException::class)
    fun getBrightness(): Float

    @Throws(NotAvailableToPlatformException::class, CancellationException::class)
    suspend fun getDeviceToken(): String?

    fun getInfoConstants(): InfoConstants
}