package com.famas.kmp_device_info

actual class DeviceInfo {
    actual companion object {
        actual fun isEmulator(): Boolean = DeviceInfoFactory.isEmulator()

        actual fun getFontScale(): Float = DeviceInfoFactory.getFontScale()

        actual fun isPinOrFingerprintSet(): Boolean = DeviceInfoFactory.isPinOrFingerprintSet()

        actual fun getIpAddress(): String? = DeviceInfoFactory.getIpAddress()

        actual fun isCameraPresent(): Boolean? {
            throw NotAvailableToPlatformException
        }

        actual fun getMacAddress(): String {
            return DeviceInfoFactory.getMacAddress()
        }

        actual fun getCarrier(): String {
            return DeviceInfoFactory.getCarrier() ?: ""
        }

        actual fun getTotalDiskCapacity(): Double {
            return DeviceInfoFactory.getTotalDiskCapacity()
        }

        actual fun getFreeDiskStorage(): Double {
            return DeviceInfoFactory.getFreeDiskStorage()

        }

        actual fun getTotalDiskCapacityOld(): Double {
            return DeviceInfoFactory.getTotalDiskCapacityOld()

        }

        actual fun getFreeDiskStorageOld(): Double {
            return DeviceInfoFactory.getFreeDiskStorageOld()

        }

        actual fun isBatteryCharging(): Boolean {
            return DeviceInfoFactory.isBatteryCharging()

        }

        actual fun getUsedMemory(): Double {
            return DeviceInfoFactory.getUsedMemory().toDouble()
        }

        actual fun getPowerState(): PowerState? {
            return DeviceInfoFactory.getPowerState()
        }

        actual fun getBatteryLevel(): Double {
            return DeviceInfoFactory.getBatteryLevel().toDouble()
        }

        actual fun isAirplaneMode(): Boolean {
            throw NotAvailableToPlatformException
        }

        actual fun hasSystemFeature(feature: String?): Boolean {
            throw NotAvailableToPlatformException
        }

        actual fun getSystemAvailableFeatures(): List<String> {
            throw NotAvailableToPlatformException
        }

        actual fun isLocationEnabled(): Boolean {
            return DeviceInfoFactory.isLocationEnabled()
        }

        actual fun isHeadphonesConnected(): Boolean {
            return DeviceInfoFactory.isHeadphonesConnected()

        }

        actual fun getAvailableLocationProviders(): Map<String, Boolean> {
            return DeviceInfoFactory.getAvailableLocationProviders() as Map<String, Boolean>

        }

        actual fun getInstallerPackageName(): String {
            return DeviceInfoFactory.getInstallerPackageName()

        }

        actual fun getFirstInstallTime(): Double {
            return DeviceInfoFactory.getFirstInstallTime().toDouble()

        }

        actual fun getLastUpdateTime(): Double {
            throw NotAvailableToPlatformException
        }

        actual fun getDeviceName(): String {
            return DeviceInfoFactory.getDeviceName()

        }

        actual fun getSerialNumber(): String {
            throw NotAvailableToPlatformException
        }

        actual fun getDevice(): String {
            throw NotAvailableToPlatformException
        }

        actual fun getBuildId(): String {
            return DeviceInfoFactory.getBuildId()

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
            return DeviceInfoFactory.getSystemManufacturer()
        }

        actual fun isDisplayZoomed(): Boolean {
            return DeviceInfoFactory.isDisplayZoomed()
        }

        actual suspend fun getDeviceToken(): String? {
            return DeviceInfoFactory.getDeviceToken()
        }


        actual fun getBrightness(): Float {
            return DeviceInfoFactory.getBrightness()
        }

        actual fun getCodename(): String {
            throw NotAvailableToPlatformException
        }

        actual fun getIncremental(): String {
            throw NotAvailableToPlatformException
        }

        actual fun getUniqueId(): String {
            return DeviceInfoFactory.getUniqueId().toString()

        }

        actual fun getAndroidId(): String {
            throw NotAvailableToPlatformException
        }

        actual fun getMaxMemory(): Double {
            throw NotAvailableToPlatformException
        }

        actual fun getTotalMemory(): Double {
            return DeviceInfoFactory.getTotalMemory()

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
            return DeviceInfoFactory.getUserAgent()
        }

        actual fun getPhoneNumber(): String {
            throw NotAvailableToPlatformException
        }

        actual fun getSupportedAbis(): List<String> {
            return DeviceInfoFactory.getSupportedAbis().toList()

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

        actual fun getInfoConstants(): InfoConstants {
            return DeviceInfoFactory.getInfoConstants()
        }

        actual fun getPlatFormType(): PlatformType {
            return PlatformType.IOS
        }
    }
}