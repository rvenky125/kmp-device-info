import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost

val libVersion = "0.0.11-alpha"
val artifactId = "kmp-device-info"
val groupId = "io.github.rvenky125"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
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

        }
        iosMain.dependencies {

        }
        iosTest.dependencies {

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}


mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
            androidVariantsToPublish = listOf("debug", "release")
        )
    )

    coordinates(groupId, artifactId, libVersion)

    pom {
        name.set("KMP Device Info Library")
        description.set("It brings all device info functions.")
        inceptionYear.set("2024")
        url.set("https://github.com/rvenky125/kmp-device-info/")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("rvenky125")
                name.set("Venkatesh Paithireddy")
                url.set("https://github.com/rvenky125/")
            }
        }
        scm {
            url.set("https://github.com/rvenky125/kmp-device-info")
            connection.set("scm:git:git://github.com/rvenky125/kmp-device-info.git")
            developerConnection.set("scm:git:ssh://git@github.com/rvenky125/kmp-device-info.git")
        }
    }

    signAllPublications()
}