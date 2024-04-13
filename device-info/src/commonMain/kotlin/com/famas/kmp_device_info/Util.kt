package com.famas.kmp_device_info

object NotAvailableToPlatformException : Throwable("The feature not available for this platform")


data class InfoConstants(
    val boardName: String,
    val bundleId: String,
    val systemName: String,
    val systemVersion: String,
    val appVersion: String,
    val buildNumber: String,
    val isTablet: Boolean,
    val isLowRamDevice: Boolean?,
    val appName: String,
    val brand: String,
    val model: String,
    val deviceType: DeviceType
)