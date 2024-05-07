# kmp-device-info

![Frame 3](https://github.com/rvenky125/kmp-device-info/assets/58197145/8aaf50a4-6b7b-4226-a40d-4ce366dca6b1)

[![Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-darkgreen.svg)](https://opensource.org/licenses/Apache-2.0)
[![BuildPassing](https://shields.io/badge/build-passing-blue)](https://github.com/rvenky125/kmp-device-info/actions)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.rvenky125/kmp-device-info)](https://search.maven.org/search?q=g:%22io.github.rvenky125%22%20AND%20a:%kmp-device-info%22)

Device Information for KMP applications. Thanks to [react-native-device-info](https://github.com/react-native-device-info/react-native-device-info) for providing inspiration and resources that were utilized in this project.

getting unique id:
```kotlin
//use like this in common main
import com.famas.kmp_device_info.DeviceInfo

val uniqueId = DeviceInfo.getUniqueId()

```


## TOC

- [Installation](#installation)
- [Usage](#usage)
- [API](#api)
- [Troubleshooting](#troubleshooting)

## Installation

<details open>
  <summary>build.gradle.kts:</summary>

  ```kotlin
  api("io.github.rvenky125:kmp-device-info:0.0.15-alpha")
  ```
</details>

<details>
  <summary>build.gradle:</summary>

  ```kotlin
  api "io.github.rvenky125:kmp-device-info:0.0.15-alpha"
  ```
</details>

<br />
Call the initialize function in Application class and it should be called before calling other methods from device info.

```kotlin
import com.famas.kmp_device_info.DeviceInfo

class MainApplication : Application() {
  override fun onCreate() { 
      super.onCreate()
      DeviceInfo.initialize(this)
  }
}
```

## Usage
In the common main of shared code:
```kotlin
import com.famas.kmp_device_info.DeviceInfo

val isEmulator = DeviceInfo.isEmulator()
```

## API

Note that many APIs are platform-specific. If there is no implementation for a platform, then you’ll get [NotAvailableToPlatformException](https://github.com/rvenky125/kmp-device-info/blob/182336070f48e8db4169cc3c4f8a1ce0daf97541/device-info/src/commonMain/kotlin/com/famas/kmp_device_info/Util.kt#L3).

| Method                                                         | iOS | Android | Windows (work in progress) | Web (work in progress) | visionOS (work in progress) |
|----------------------------------------------------------------| --- | --- | --- | --- | --- |
| [getandroidid](#getandroidid)                                  | ❌ | ✅ |  |  |  | 
| getPlatFormType                                               | ✅ | ✅ |  |  |  | 
| [getapilevel](#getapilevel)                                    | ❌ | ✅ |  |  |  |
| [getapplicationname](#getapplicationname)                      | ✅ | ✅ |  |  |  |
| [getAvailableLocationProviders](#getAvailableLocationProviders) | ✅ | ✅ |  |  |  |
| [getbaseOs](#getbaseOs)                                        | ❌ | ✅ |  |  |  |
| [getbuildid](#getbuildid)                                      | ✅ | ✅ |  |  |  |
| [getbatterylevel](#getbatterylevel)                            | ✅ | ✅ |  |  |  |
| [getbootloader](#getbootloader)                                | ❌ | ✅ |  |  |  |
| [getbrand](#getbrand)                                          | ✅ | ✅ |  |  |  |
| [getbuildnumber](#getbuildnumber)                              | ✅ | ✅ |  |  |  |
| [getbundleid](#getbundleid)                                    | ✅ | ✅ |  |  |  |
| [iscamerapresent](#iscamerapresent)                            | ❌ | ✅ |  |  |  |
| [getcarrier](#getcarrier)                                      | ✅ | ✅ |  |  |  |
| [getcodename](#getcodename)                                    | ❌ | ✅ |  |  |  |
| [getdevice](#getdevice)                                        | ❌ | ✅ |  |  |  |
| [getdeviceid](#getdeviceid)                                    | ✅ | ✅ |  |  |  |
| [getDeviceType](#getDeviceType)                                | ✅ | ✅ |  |  |  |
| [getdisplay](#getdisplay)                                      | ❌ | ✅ |  |  |  |
| [getdevicename](#getdevicename)                                | ✅ | ✅ |  |  |  |
| [getdevicetoken](#getdevicetoken)                              | ✅ | ❌ |  |  |  |
| [getfirstinstalltime](#getfirstinstalltime)                    | ✅ | ✅ |  |  |  |
| [getfingerprint](#getfingerprint)                              | ❌ | ✅ |  |  |  |
| [getfontscale](#getfontscale)                                  | ✅ | ✅ |  |  |  |
| [getfreediskstorage](#getfreediskstorage)                      | ✅ | ✅ |  |  |  |
| [getfreediskstorageold](#getfreediskstorageold)                | ✅ | ✅ |  |  |  |
| [gethardware](#gethardware)                                    | ❌ | ✅ |  |  |  |
| [gethost](#gethost)                                            | ❌ | ✅ |  |  |  |
| [getHostNames](#getHostNames)                                  | ❌ | ❌ |  |  |  |
| [getipaddress](#getipaddress)                                  | ✅ | ✅ |  |  |  |
| [getincremental](#getincremental)                              | ❌ | ✅ |  |  |  |
| [getinstallerpackagename](#getinstallerpackagename)            | ✅ | ✅ |  |  |  |
| [getlastupdatetime](#getlastupdatetime)                        | ❌ | ✅ |  |  |  |
| [getmacaddress](#getmacaddress)                                | ✅ | ✅ |  |  |  |
| [getmanufacturer](#getmanufacturer)                            | ✅ | ✅ |  |  |  |
| [getmaxmemory](#getmaxmemory)                                  | ❌ | ✅ |  |  |  |
| [getmodel](#getmodel)                                          | ✅ | ✅ |  |  |  |
| [getphonenumber](#getphonenumber)                              | ❌ | ✅ |  |  |  |
| [getpowerstate](#getpowerstate)                                | ✅ | ✅ |  |  |  |
| [getproduct](#getproduct)                                      | ❌ | ✅ |  |  |  |
| [getPreviewSdkInt](#getPreviewSdkInt)                          | ❌ | ✅ |  |  |  |
| [getreadableversion](#getreadableversion)                      | ✅ | ✅ |  |  |  |
| [getserialnumber](#getserialnumber)                            | ❌ | ✅ |  |  |  |
| [getsecuritypatch](#getsecuritypatch)                          | ❌ | ✅ |  |  |  |
| [getSystemAvailableFeatures](#getSystemAvailableFeatures)      | ❌ | ✅ |  |  |  |
| [getsystemname](#getsystemname)                                | ✅ | ✅ |  |  |  |
| [getsystemversion](#getsystemversion)                          | ✅ | ✅ |  |  |  |
| [gettags](#gettags)                                            | ❌ | ✅ |  |  |  |
| [gettype](#gettype)                                            | ❌ | ✅ |  |  |  |
| [gettotaldiskcapacity](#gettotaldiskcapacity)                  | ✅ | ✅ |  |  |  |
| [gettotaldiskcapacityold](#gettotaldiskcapacityold)            | ✅ | ✅ |  |  |  |
| [gettotalmemory](#gettotalmemory)                              | ✅ | ✅ |  |  |  |
| [getuniqueid](#getuniqueid)                                    | ✅ | ✅ |  |  |  |
| [getusedmemory](#getusedmemory)                                | ✅ | ✅ |  |  |  |
| [getuseragent](#getuseragent)                                  | ✅ | ✅ |  |  |  |
| [getversion](#getversion)                                      | ✅ | ✅ |  |  |  |
| [getBrightness](#getBrightness)                                | ✅ | ❌ |  |  |  |
| [hasNotch](#hasNotch)                                          | ✅ | ✅ |  |  |  |
| [hasDynamicIsland](#hasDynamicIsland)                          | ✅ | ✅ |  |  |  |
| [hassystemfeaturefeature](#hassystemfeaturefeature)            | ❌ | ✅ |  |  |  |
| [isairplanemode](#isairplanemode)                              | ❌ | ✅ |  |  |  |
| [isbatterycharging](#isbatterycharging)                        | ✅ | ✅ |  |  |  |
| [isemulator](#isemulator)                                      | ✅ | ✅ |  |  |  |
| [iskeyboardconnected](#iskeyboardconnected)                    | ❌ | ❌ |  |  |  |
| [isLandscape](#isLandscape)                                    | ✅ | ✅ |  |  |  |
| [isLocationEnabled](#isLocationEnabled)                        | ✅ | ✅ |  |  |  |
| [ismouseconneted](#ismouseconneted)                            | ❌ | ❌ |  |  |  |
| [isHeadphonesConnected](#isHeadphonesConnected)                | ✅ | ✅ |  |  |  |
| [ispinorfingerprintset](#ispinorfingerprintset)                | ✅ | ✅ |  |  |  |
| [istablet](#istablet)                                          | ✅ | ✅ |  |  |  |
| [istabletmode](#istabletmode)                                  | ❌ | ❌ |  |  |  |
| [supported32BitAbis](#supported32BitAbis)                      | ❌ | ✅ |  |  |  |
| [supported64BitAbis](#supported64BitAbis)                      | ❌ | ✅ |  |  |  |
| [supportedAbis](#supportedAbis)                                | ✅ | ✅ |  |  |  |
| [getSupportedMediaTypeList](#getSupportedMediaTypeList)        | ❌ | ✅ |  |  |  |

### getApiLevel()

Gets the API level.

---

### getAndroidId()

Gets the ANDROID_ID. See [API documentation](https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID) for appropriate use.

---

### getApplicationName()

Gets the application name.

---

### getBaseOs()

The base OS build the product is based on.

---

### getBatteryLevel()

Gets the battery level of the device as a float comprised between 0 and 1.

### Notes

> To be able to get actual battery level enable battery monitoring mode for application.
Add this code:
> 

```objectivec
[UIDevice currentDevice].batteryMonitoringEnabled = true;
```

> to AppDelegate.m application:didFinishLaunchingWithOptions:
> 
> 
> Returns -1 on the iOS Simulator
> 

---

### getBootloader()

The system bootloader version number.

---

### getBrand()

Gets the device brand.

---

### getBuildNumber()

Gets the application build number.

---

### getBundleId()

Gets the application bundle identifier.

---

### isCameraPresent()

Tells if the device has any camera now.

### Notes

> Hot add/remove of camera is supported.Returns the status of the physical presence of the camera. If camera present but your app don’t have permissions to use it, isCameraPresent will still return the true
> 

---

### getCarrier()

Gets the carrier name (network operator).

---

### getCodename()

The current development codename, or the string “REL” if this is a release build.

---

### getDevice()

The name of the industrial design.

---

### getDeviceId()

Gets the device ID.

---

### getDisplay()

A build ID string meant for displaying to the user.

---

### getDeviceName()

Gets the device name.

This used to require the android.permission.BLUETOOTH but the new implementation in v3 does not need it. You may remove that from your AndroidManifest.xml if you had it for this API. iOS 16 and greater [require entitlements](%5Burl%5D(https://developer.apple.com/documentation/bundleresources/entitlements/com_apple_developer_device-information_user-assigned-device-name)) to access user-defined device name, otherwise a generic value is returned (ie. ‘iPad’, ‘iPhone’)

---

### getDeviceToken()

Gets the device token (see [DeviceCheck](https://developer.apple.com/documentation/devicecheck)). Only available for iOS 11.0+ on real devices.
This will reject the promise when getDeviceToken is not supported, be careful with exception handling.

---

### getFirstInstallTime()

Gets the time at which the app was first installed, in milliseconds.

---

### getFingerprint()

A string that uniquely identifies this build.

---

### getFontScale()

Gets the device font scale.
The font scale is the ratio of the current system font to the “normal” font size, so if normal text is 10pt and the system font is currently 15pt, the font scale would be 1.5
This can be used to determine if accessability settings has been changed for the device; you may want to re-layout certain views if the font scale is significantly larger ( > 2.0 )

---

### getFreeDiskStorage()

Method that gets available storage size, in bytes, taking into account both root and data file systems calculation.


### getFreeDiskStorageOld()

Old implementation of method that gets available storage size, in bytes.

### Notes

> From developer.android.com:
> 
> 
> This method was deprecated in API level 29.
> 
> Return the primary shared/external storage directory.
> 
> Note: don’t be confused by the word “external” here. This directory can better be thought as
> media/shared storage. It is a filesystem that can hold a relatively large amount of data and
> that is shared across all applications (does not enforce permissions). Traditionally this is
> an SD card, but it may also be implemented as built-in storage in a device that is distinct
> from the protected internal storage and can be mounted as a filesystem on a computer.

---

### getHardware()

The name of the hardware (from the kernel command line or /proc).

---

### getHost()

Hostname

---

### getIpAddress()

**Deprecated** Gets the device current IP address. (of wifi only)

### Android Permissions

- [android.permission.ACCESS_WIFI_STATE](https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_WIFI_STATE)

### Notes

> Support for iOS was added in 0.22.0
> 

---

### getIncremental()

The internal value used by the underlying source control to represent this build.

---

### getInstallerPackageName()

The internal value used by the underlying source control to represent this build.

---

### getLastUpdateTime()

Gets the time at which the app was last updated, in milliseconds.

---

### getMacAddress()

Gets the network adapter MAC address.

### Android Permissions

- [android.permission.ACCESS_WIFI_STATE](https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_WIFI_STATE)

### Notes

> iOS: This method always return “02:00:00:00:00:00” as retrieving the MAC address is disabled since iOS 7
> 

---

### getManufacturer()

Gets the device manufacturer.

---

### getMaxMemory()

Returns the maximum amount of memory that the VM will attempt to use, in bytes.

---

### getModel()

Gets the device model.

**iOS warning:** The list with device names is maintained by the community and could lag new devices. It is recommended to use `getDeviceId()` since it’s more reliable and always up-to-date with new iOS devices. We do accept pull requests that add new iOS devices to the list with device names.

---

### getPhoneNumber()

Gets the device phone number.

### Android Permissions

Please refer to the [Android docs](https://developer.android.com/about/versions/11/privacy/permissions#phone-numbers) for information about which permission you need, depending on which version of Android you are supporting. Read the note below for more information.

### Notes

> This can return undefined in certain cases and should not be relied on. SO entry on the subject.
> 

> If the above permissions do not work, you can try using android.permission.READ_SMS. However, this is not in the Android docs. If you are supporting Android 10 and below: android.permission.READ_PHONE_STATE. If you are supporting Android 11 and up: android.permission.READ_SMS
> 

---

### getPowerState()

Gets the power state of the device including the battery level, whether it is plugged in, and if the system is currently operating in low power mode.
Displays a warning on iOS if battery monitoring not enabled, or if attempted on an emulator (where monitoring is not possible)

---

### getProduct()

The name of the overall product.

---

### getPreviewSdkInt()

The developer preview revision of a prerelease SDK.

---

### getReadableVersion()

Gets the application human readable version (same as getVersion() + ‘.’ + getBuildNumber())

---

### getSerialNumber()

Gets the device serial number. Will be ‘unknown’ in almost all cases [unless you have a privileged app and you know what you’re doing](https://developer.android.com/reference/android/os/Build.html#getSerial()).

## Notes

### capability smbios

If you want to use this method in windows, you have to add smbios capability in your aplication. Please following this [documentation](https://docs.microsoft.com/en-us/windows/win32/sysinfo/access-smbios-information-from-a-universal-windows-app) for add the capability in your manifest file.

---

### getSecurityPatch()

The user-visible security patch level.

---

### getSystemName()

Gets the device OS name.

---

### getSystemVersion()

Gets the device OS version.

---

### getBuildId()

Gets build number of the operating system.

---

### getTags()

Comma-separated tags describing the build.

---

### getType()

The type of build.

---

### getTotalDiskCapacity()
Method that gets full disk storage size, in bytes, taking into account both root and data file systems calculation.

---

### getTotalDiskCapacityOld()

Old implementation of method that gets full disk storage size, in bytes.

---

### getTotalMemory()

Gets the device total memory, in bytes.

---

### getUniqueId()

*This identifier is considered sensitive information in some app stores (e.g. Huawei or Google Play) and may lead to your app being removed or rejected, if used without user consent or for unapproved purposes. Refer to store policies for more information (see notes below).*

Gets the device unique ID.
On Android it is currently identical to `getAndroidId()` in this module.
On iOS it uses the `DeviceUID` uid identifier.
On Windows it uses `Windows.Security.ExchangeActiveSyncProvisioning.EasClientDeviceInformation.id`.

### Notes

> iOS: This is IDFV or a random string if IDFV is unavaliable. Once UID is generated it is stored in iOS Keychain and NSUserDefaults. So it would stay the same even if you delete the app or reset IDFV. You can carefully consider it a persistent, cross-install unique ID. It can be changed only in case someone manually override values in Keychain/NSUserDefaults or if Apple would change Keychain and NSUserDefaults implementations.
Beware: The IDFV is calculated using your bundle identifier and thus will be different in app extensions.android: Prior to Oreo, this id (ANDROID_ID) will always be the same once you set up your phone.android: Google Play policy, see “persistent device identifiers”. Huawei - AppGallery Review Guidelines see “permanent device identifier” and “obtaining user consent”.
> 

---
### Notes

> If user moved or restored data from one iOS device to second iOS device then he will have two different devices with same uniqueId in Keychain/NSUserDefaults. User can call syncUniqueId() on new iOS device. That will update his uniqueId from IDFV or a random string.
> 

---

### getUsedMemory()

Gets the app memory usage, in bytes.

⚠️ [A note from the Android docs.](https://developer.android.com/reference/android/app/ActivityManager#getProcessMemoryInfo(int%5B%5D))
> Note: this method is only intended for debugging or building a user-facing process management UI.

---

### getSupportedMediaTypeList()

This method gets the list of supported media codecs.

---

### getUserAgent()

Gets the device User Agent.

---

### getVersion()

Gets the application version.
Take into account that a version string is device/OS formatted and can contain any additional data (such as build number etc.). If you want to be sure about version format, you can use a regular expression to get the desired portion of the returned version string.

---

### isAirplaneMode()

Tells if the device is in Airplane Mode.

### Notes

> This only works if the remote debugger is disabled.

---

### isBatteryCharging()

Tells if the battery is currently charging.

---

### isEmulator()

Tells if the application is running in an emulator.

---

### isKeyboardConnected()

Tells if the device has a keyboard connected.

---

### isPinOrFingerprintSet()

Tells if a PIN number or a fingerprint was set for the device.

---

### isTablet()

Tells if the device is a tablet.

---

### isLowRamDevice()

Tells if the device has low RAM.

---

### isDisplayZoomed()

Tells if the user changed Display Zoom to Zoomed

---

### isTabletMode()

Tells if the device is in tablet mode.

---

### isLandscape()

Tells if the device is currently in landscape mode.

---

### isMouseConnected()

Tells if the device has a mouse connected.

---

### hasNotch()

Tells if the device has a notch.

---

### hasDynamicIsland()

Tells if the device has a dynamic island.

---

### getDeviceType()

Returns the device’s type class, which will be one of:


---

### supported32BitAbis()

An ordered list of 32 bit ABIs supported by this device.

---

### supported64BitAbis()

An ordered list of 64 bit ABIs supported by this device.

---

### supportedAbis()

Returns a list of supported processor architecture version

---

### hasSystemFeature(feature)

Tells if the device has a specific system feature.

---

### getSystemAvailableFeatures()

Returns a list of available system features on Android.

---

### isLocationEnabled()

Tells if the device has location services turned off at the device-level (NOT related to app-specific permissions)

---

### isHeadphonesConnected()

Tells if the device is connected to wired headset or bluetooth headphones

---

### getAvailableLocationProviders()

Returns an object of **platform-specfic** location providers/servcies, with `boolean` value whether or not they are currently available.

> NOTE: This function requires access to the Location permission on Android 

---

### getBrightness()

Gets the current brightness level of the device’s main screen. Currently iOS only. Returns a number between 0.0 and 1.0, inclusive.

---


## Troubleshooting
    
- [ios] - [NetworkInfo] Descriptors query returned error: Error Domain=NSCocoaErrorDomain Code=4099
“The connection to service named com.apple.commcenter.coretelephony.xpc was invalidated.”
    
    This is a system level log that may be turned off by executing:
    `xcrun simctl spawn booted log config --mode "level:off" --subsystem com.apple.CoreTelephony`.
    To undo the command, you can execute:
    `xcrun simctl spawn booted log config --mode "level:info" --subsystem com.apple.CoreTelephony`
  
    Checkout the example project for more information.
    
- [warnings] - I get too many warnings (battery state, etc)
    
    Some of the APIs (like getBatteryState) will throw warnings in certain conditions like on tvOS or the iOS emulator. This won’t be visible in production but even in development it may be irritating. It is useful to have the warnings because these devices return no state, and that can be surprising, leading to github support issues. The warnings is intended to educate you as a developer. If the warnings are troublesome you may try this in your code to suppress them:


## Contributing
There aren't any particular guidelines for sharing your expertise and strengthening the open-source community. Just remember to raise an issue or initiate a discussion beforehand, so there are no surprises when you submit a pull request.
