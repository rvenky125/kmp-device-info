package com.famas.kmp_device_info

import android.content.Context
import androidx.startup.Initializer

@Suppress("UNUSED")
class ModuleInitializer: Initializer<Int> {
    override fun create(context: Context): Int {
        DeviceInfo(context)
        return 0
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}