package com.famas.kmp_device_info.resolver

import android.content.Context
import android.os.Build
import com.famas.kmp_device_info.DeviceInfo
import com.famas.kmp_device_info.DeviceInfo.Companion.getSharePreferences
import java.lang.reflect.InvocationTargetException
import java.util.UUID


class DeviceIdResolver(private val context: Context) {
    val instanceIdSync: String?
        get() {
            var instanceId = instanceIdFromPrefs
            if (instanceId != Build.UNKNOWN) {
                return instanceId
            }
            try {
                instanceId = firebaseInstanceId
                setInstanceIdInPrefs(instanceId)
                return instanceId
            } catch (ignored: ClassNotFoundException) {
            } catch (e: NoSuchMethodException) {
                System.err.println("N/A: Unsupported version of com.google.firebase:firebase-iid in your project.")
            } catch (e: SecurityException) {
                System.err.println("N/A: Unsupported version of com.google.firebase:firebase-iid in your project.")
            } catch (e: IllegalAccessException) {
                System.err.println("N/A: Unsupported version of com.google.firebase:firebase-iid in your project.")
            } catch (e: InvocationTargetException) {
                System.err.println("N/A: Unsupported version of com.google.firebase:firebase-iid in your project.")
            }
            try {
                instanceId = gmsInstanceId
                setInstanceIdInPrefs(instanceId)
                return instanceId
            } catch (ignored: ClassNotFoundException) {
            } catch (e: NoSuchMethodException) {
                System.err.println("N/A: Unsupported version of com.google.android.gms.iid in your project.")
            } catch (e: SecurityException) {
                System.err.println("N/A: Unsupported version of com.google.android.gms.iid in your project.")
            } catch (e: IllegalAccessException) {
                System.err.println("N/A: Unsupported version of com.google.android.gms.iid in your project.")
            } catch (e: InvocationTargetException) {
                System.err.println("N/A: Unsupported version of com.google.android.gms.iid in your project.")
            }
            instanceId = uUIDInstanceId
            setInstanceIdInPrefs(instanceId)
            return instanceId
        }
    private val uUIDInstanceId: String
        get() = UUID.randomUUID().toString()
    private val instanceIdFromPrefs: String?
        get() {
            val prefs = getSharePreferences(
                context
            )
            return prefs.getString("instanceId", Build.UNKNOWN)
        }

    private fun setInstanceIdInPrefs(instanceId: String?) {
        val editor = getSharePreferences(
            context
        ).edit()
        editor.putString("instanceId", instanceId)
        editor.apply()
    }

    @get:Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class
    )
    val gmsInstanceId: String
        get() {
            val clazz = Class.forName("com.google.android.gms.iid.InstanceID")
            val method = clazz.getDeclaredMethod("getInstance", Context::class.java)
            val obj = method.invoke(null, context.applicationContext)!!
            val method1 = obj.javaClass.getMethod("getId")
            return method1.invoke(obj) as String
        }

    @get:Throws(
        ClassNotFoundException::class,
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class
    )
    val firebaseInstanceId: String
        get() {
            val clazz = Class.forName("com.google.firebase.iid.FirebaseInstanceId")
            val method = clazz.getDeclaredMethod("getInstance")
            val obj = method.invoke(null)!!
            val method1 = obj.javaClass.getMethod("getId")
            return method1.invoke(obj) as String
        }
}
