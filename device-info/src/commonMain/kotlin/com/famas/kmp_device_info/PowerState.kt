package com.famas.kmp_device_info

data class PowerState(
    val batteryLevel: Float,
    val batteryState: BatterState,
    val isLowPowerMode: Boolean
)

enum class BatterState {
    UNKNOWN, UNPLUGGED, CHARGING, FULL
}