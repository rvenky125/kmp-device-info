package com.famas.kmp_device_info

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform