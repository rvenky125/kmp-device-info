package com.famas.kmp_device_info

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFUUIDCreate
import platform.CoreFoundation.CFUUIDCreateString
import platform.Foundation.NSClassFromString
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue
import platform.Foundation.valueForKey
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.UIKit.UIDevice

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class DeviceUID {
    private var _uid: String? = null

    private var uid: String? = null
        get() {
            if (field == null) {
                field = valueForKeychainKey(UIDKey, service = UIDKey)
                if (field == null) {
                    field = valueForUserDefaultsKey(UIDKey)
                    if (field == null) {
                        field = appleIFV()
                        if (field == null) {
                            field = randomUUID()
                        }
                        saveIfNeed()
                    }
                }
            }
            return field
        }


    fun uid(): String? {
        return uid
    }

    fun syncUid(): String? {
        _uid = appleIFV()
        if (_uid == null) {
            _uid = randomUUID()
        }
        save()
        return _uid
    }

    private fun valueForKeychainKey(key: String, service: String): String? {
        val keychainItem = keychainItemForKey(key, service = service)
        keychainItem.setValue(kCFBooleanTrue, kSecReturnData.toString())
        keychainItem.setValue(kCFBooleanTrue, kSecReturnAttributes.toString())
        val result = SecItemCopyMatching(keychainItem as CFDictionaryRef, null)
        if (result != noErr) {
            return null
        }
        val resultDict = result as NSDictionary
        val data = resultDict.valueForKey(kSecValueData.toString()) as NSData
        return data.toString()
    }

    fun setValue(value: String, forUserDefaultsKey: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = forUserDefaultsKey)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    private fun valueForUserDefaultsKey(key: String): String? {
        return NSUserDefaults.standardUserDefaults.objectForKey(key) as? String
    }

    private fun keychainItemForKey(key: String, service: String): NSMutableDictionary {
        val keychainItem = mutableMapOf<Any?, Any?>()
        keychainItem[kSecClass as Any] = kSecClassGenericPassword
        keychainItem[kSecAttrAccessible as Any] = kSecAttrAccessibleAfterFirstUnlock
        keychainItem[kSecAttrAccount as Any] = key
        keychainItem[kSecAttrService as Any] = service
        return keychainItem as NSMutableDictionary
    }

    fun setValue(value: String, forKeychainKey: String, inService: String): Boolean {
        val keychainItem = keychainItemForKey(forKeychainKey, service = inService)
        keychainItem.setValue(value, kSecValueData.toString())
        return SecItemAdd(keychainItem as CFDictionaryRef, null) == errSecSuccess
    }

    fun updateValue(value: String, forKeychainKey: String, inService: String): Boolean {
        val query = mutableMapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to forKeychainKey,
            kSecAttrService to inService
        )
        val attributesToUpdate = mutableMapOf(
            kSecValueData to value
        )
        return SecItemUpdate(query as CFDictionaryRef, attributesToUpdate as CFDictionaryRef) == errSecSuccess
    }

    fun deleteValue(forKeychainKey: String, inService: String): Boolean {
        val query = mutableMapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to forKeychainKey,
            kSecAttrService to inService
        )
        return SecItemDelete(query as CFDictionaryRef) == errSecSuccess
    }

    private fun save() {
        uid?.let { setValue(it, forUserDefaultsKey = UIDKey) }
        uid?.let { setValue(it, forKeychainKey = UIDKey, inService = UIDKey) }
    }

    private fun saveIfNeed() {
        if (valueForUserDefaultsKey(UIDKey) == null) {
            uid?.let { setValue(it, forUserDefaultsKey = UIDKey) }
        }
        if (valueForKeychainKey(UIDKey, service = UIDKey) == null) {
            uid?.let { setValue(it, forKeychainKey = UIDKey, inService = UIDKey) }
        }
    }

    private fun randomUUID(): String {
        if (NSClassFromString("NSUUID") != null) {
            return NSUUID.UUID().UUIDString
        }
        val uuidRef = CFUUIDCreate(null)
        val cfuuid = CFUUIDCreateString(null, uuidRef)
        CFRelease(uuidRef)
        val uuid = cfuuid.toString()
        CFRelease(cfuuid)
        return uuid
    }

    private fun appleIFV(): String? {
        if (NSClassFromString("UIDevice") != null && UIDevice.instancesRespondToSelector(
                NSSelectorFromString("identifierForVendor")
            )) {
            // only available in iOS >= 6.0
            return UIDevice.currentDevice.identifierForVendor?.UUIDString
        }
        return null
    }
}

private val UIDKey = "deviceUID"
private val kCFBooleanTrue = true
@OptIn(ExperimentalForeignApi::class)
private val kSecClassGenericPassword = kSecClass
@OptIn(ExperimentalForeignApi::class)
private val kSecAttrAccessibleAfterFirstUnlock = kSecAttrAccessible
private const val noErr = 0