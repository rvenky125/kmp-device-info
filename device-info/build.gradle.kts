import com.android.build.gradle.internal.scope.publishBuildArtifacts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libVersion = "v0.0.1-alpha"
val gitName = "kmp-device-info"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)

    id("convention.publication")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "This module provides device info"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "device-info"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation("androidx.startup:startup-runtime:1.1.1")
        }

        commonMain.dependencies {
            //put your multiplatform dependencies here
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.famas.kmp_device_info"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}

//val javadocJar by tasks.registering(Jar::class) {
//    archiveClassifier.set("javadoc")
//}
//
//publishing {
//    publications {
//        register<MavenPublication>("release")  {
//            artifact(javadocJar.get())
//
//            groupId = "com.github.rvenky125"
//            artifactId = gitName
//            version = libVersion
//        }
//    }
//}