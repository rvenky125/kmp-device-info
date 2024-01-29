package com.famas.kmp_device_info

import platform.Foundation.NSBundle
import platform.Foundation.lastPathComponent
import platform.darwin.TARGET_OS_MACCATALYST
import platform.darwin.TARGET_OS_OSX
import platform.darwin.TARGET_OS_SIMULATOR

object EnvironmentUtil {
    fun currentAppEnvironment(): MSACEnvironment {
        val isSimulator =
            TARGET_OS_SIMULATOR == 1 || TARGET_OS_OSX == 0 || TARGET_OS_MACCATALYST == 0
        return if (isSimulator) {
            MSACEnvironment.Other
        } else {
            if (hasEmbeddedMobileProvision()) {
                MSACEnvironment.Other
            } else {
                if (isAppStoreReceiptSandbox()) {
                    MSACEnvironment.TestFlight
                } else {
                    MSACEnvironment.AppStore
                }
            }
        }
    }

    private fun hasEmbeddedMobileProvision(): Boolean {
        return NSBundle.mainBundle.pathForResource("embedded", "mobileprovision") != null
    }

    private fun isAppStoreReceiptSandbox(): Boolean {
        if (TARGET_OS_SIMULATOR == 1) {
            return false
        }

        if (NSBundle.mainBundle.appStoreReceiptURL == null) {
            return false
        }

        val appStoreReceiptURL = NSBundle.mainBundle.appStoreReceiptURL
        val appStoreReceiptLastComponent = appStoreReceiptURL?.lastPathComponent

        return appStoreReceiptLastComponent == "sandboxReceipt"
    }
}

enum class MSACEnvironment(val id: Int) {
    AppStore(0),
    TestFlight(1),
    Other(2)
}