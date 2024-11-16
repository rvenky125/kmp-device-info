plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinCocoapods).apply(false)
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    id("org.jetbrains.dokka").version("1.9.10").apply(false)
    id("com.vanniktech.maven.publish") version "0.28.0" apply false
//    id("com.louiscad.complete-kotlin") version "1.1.0" apply false
}