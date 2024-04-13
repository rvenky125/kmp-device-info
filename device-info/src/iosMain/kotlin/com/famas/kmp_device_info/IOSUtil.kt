package com.famas.arrow.kmp_device_info

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFStringRef
import platform.Foundation.CFBridgingRelease

@OptIn(ExperimentalForeignApi::class)
fun CFStringRef.cfToKotlinString(): String = CFBridgingRelease(this) as String