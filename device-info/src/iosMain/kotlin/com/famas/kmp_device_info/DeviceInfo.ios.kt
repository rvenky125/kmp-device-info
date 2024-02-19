package com.famas.kmp_device_info

actual class DeviceInfo {
    actual fun isEmulator(): Boolean = RNDeviceInfo.isEmulator()

    actual fun getFontScale(): Float = RNDeviceInfo.getFontScale()

    actual fun isPinOrFingerprintSet(): Boolean = RNDeviceInfo.isPinOrFingerprintSet()

    actual fun getIpAddress(): String? = RNDeviceInfo.getIpAddress()

    actual fun isCameraPresent(): Boolean? {
        throw NotAvailableToPlatformException
    }

    actual fun getMacAddress(): String {
        return RNDeviceInfo.getMacAddress()
    }

    actual fun getCarrier(): String {
        return RNDeviceInfo.getCarrier() ?: ""
    }

    actual fun getTotalDiskCapacity(): Double {
        return RNDeviceInfo.getTotalDiskCapacity()
    }

    actual fun getFreeDiskStorage(): Double {
        return RNDeviceInfo.getFreeDiskStorage()

    }

    actual fun getTotalDiskCapacityOld(): Double {
        return RNDeviceInfo.getTotalDiskCapacityOld()

    }

    actual fun getFreeDiskStorageOld(): Double {
        return RNDeviceInfo.getFreeDiskStorageOld()

    }

    actual fun isBatteryCharging(): Boolean {
        return RNDeviceInfo.isBatteryCharging()

    }

    actual fun getUsedMemory(): Double {
        return RNDeviceInfo.getUsedMemory().toDouble()
    }

    actual fun getPowerState(): Map<String, Any> {
        val map = RNDeviceInfo.getPowerState()
        return map.mapValues {
            it.value ?: ""
        }
    }

    actual fun getBatteryLevel(): Double {
        return RNDeviceInfo.getBatteryLevel().toDouble()

    }

    actual fun isAirplaneMode(): Boolean {
        throw NotAvailableToPlatformException
    }

    actual fun hasGms(): Boolean {
        throw NotAvailableToPlatformException
    }

    actual fun hasHms(): Boolean {
        throw NotAvailableToPlatformException
    }

    actual fun hasSystemFeature(feature: String?): Boolean {
        throw NotAvailableToPlatformException
    }

    actual fun getSystemAvailableFeatures(): List<String> {
        throw NotAvailableToPlatformException
    }

    actual fun isLocationEnabled(): Boolean {
        return RNDeviceInfo.isLocationEnabled()
    }

    actual fun isHeadphonesConnected(): Boolean {
        return RNDeviceInfo.isHeadphonesConnected()

    }

    actual fun getAvailableLocationProviders(): Map<String, Boolean> {
        return RNDeviceInfo.getAvailableLocationProviders() as Map<String, Boolean>

    }

    actual fun getInstallReferrer(): String? {
        throw NotAvailableToPlatformException
    }

    actual fun getInstallerPackageName(): String {
        return RNDeviceInfo.getInstallerPackageName()

    }

    actual fun getFirstInstallTime(): Double {
        return RNDeviceInfo.getFirstInstallTime().toDouble()

    }

    actual fun getLastUpdateTime(): Double {
        throw NotAvailableToPlatformException
    }

    actual fun getDeviceName(): String {
        return RNDeviceInfo.getDeviceName()

    }

    actual fun getSerialNumber(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getDevice(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getBuildId(): String {
        return RNDeviceInfo.getBuildId()

    }

    actual fun getApiLevel(): Int {
        throw NotAvailableToPlatformException
    }

    actual fun getBootloader(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getDisplay(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getFingerprint(): String {

        throw NotAvailableToPlatformException
    }

    actual fun getHardware(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getHost(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getProduct(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getTags(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getType(): String {
        throw NotAvailableToPlatformException
    }

    actual fun isLowRamDevice(): Boolean {
        throw NotAvailableToPlatformException
    }

    actual fun getSystemManufacturer(): String {
        return RNDeviceInfo.getSystemManufacturer()
    }

    actual fun isDisplayZoomed(): Boolean {
        return RNDeviceInfo.isDisplayZoomed()
    }

    actual suspend fun getDeviceToken(): String? {
        return RNDeviceInfo.getDeviceToken()
    }


    actual fun getBrightness(): Float {
        return RNDeviceInfo.getBrightness()
    }

    actual fun getCodename(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getIncremental(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getUniqueId(): String {
        return RNDeviceInfo.getUniqueId().toString()

    }

    actual fun getAndroidId(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getMaxMemory(): Double {
        throw NotAvailableToPlatformException
    }

    actual fun getTotalMemory(): Double {
        return RNDeviceInfo.getTotalMemory()

    }

    actual fun getInstanceId(): String? {
        throw NotAvailableToPlatformException
    }

    actual fun getBaseOs(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getPreviewSdkInt(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getSecurityPatch(): String {
        throw NotAvailableToPlatformException
    }

    actual suspend fun getUserAgent(): String? {
        return RNDeviceInfo.getUserAgent()
    }

    actual fun getPhoneNumber(): String {
        throw NotAvailableToPlatformException
    }

    actual fun getSupportedAbis(): List<String> {
        return RNDeviceInfo.getSupportedAbis().toList()

    }

    actual fun getSupported32BitAbis(): List<String> {
        throw NotAvailableToPlatformException
    }

    actual fun getSupported64BitAbis(): List<String> {
        throw NotAvailableToPlatformException
    }

    actual fun getSupportedMediaTypeList(): List<String> {
        throw NotAvailableToPlatformException
    }
}