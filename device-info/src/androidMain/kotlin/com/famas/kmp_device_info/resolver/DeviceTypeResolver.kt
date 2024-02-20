package com.famas.kmp_device_info.resolver

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.WindowManager
import com.famas.kmp_device_info.DeviceType
import kotlin.math.pow
import kotlin.math.sqrt

class DeviceTypeResolver(private val context: Context) {
    val isTablet: Boolean
        get() = deviceType == DeviceType.TABLET
    val deviceType: DeviceType
        get() {
            // Detect TVs via ui mode (Android TVs) or system features (Fire TV).
            if (context.packageManager.hasSystemFeature("amazon.hardware.fire_tv")) {
                return DeviceType.TV
            }
            val uiManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            if (uiManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
                return DeviceType.TV
            }
            val deviceTypeFromConfig = deviceTypeFromResourceConfiguration
            return if (deviceTypeFromConfig != DeviceType.UNKNOWN) {
                deviceTypeFromConfig
            } else deviceTypeFromPhysicalSize
        }
    private val deviceTypeFromResourceConfiguration: DeviceType
        // Use `smallestScreenWidthDp` to determine the screen size
        get() {
            val smallestScreenWidthDp = context.resources.configuration.smallestScreenWidthDp
            if (smallestScreenWidthDp == Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED) {
                return DeviceType.UNKNOWN
            }
            return if (smallestScreenWidthDp >= 600) DeviceType.TABLET else DeviceType.HANDSET
        }
    private val deviceTypeFromPhysicalSize: DeviceType
        get() {
            // Find the current window manager, if none is found we can't measure the device physical size.
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // Get display metrics to see if we can differentiate handsets and tablets.
            // NOTE: for API level 16 the metrics will exclude window decor.
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(metrics)

            // Calculate physical size.
            val widthInches = metrics.widthPixels / metrics.xdpi.toDouble()
            val heightInches = metrics.heightPixels / metrics.ydpi.toDouble()
            val diagonalSizeInches = sqrt(widthInches.pow(2.0) + heightInches.pow(2.0))
            return if (diagonalSizeInches in 3.0..6.9) {
                // Devices in a sane range for phones are considered to be Handsets.
                DeviceType.HANDSET
            } else if (diagonalSizeInches > 6.9 && diagonalSizeInches <= 18.0) {
                // Devices larger than handset and in a sane range for tablets are tablets.
                DeviceType.TABLET
            } else {
                // Otherwise, we don't know what device type we're on/
                DeviceType.UNKNOWN
            }
        }
}
