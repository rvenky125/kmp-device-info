package com.famas.kmp_device_info

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class RNInstallReferrerClient internal constructor(context: Context) {
    private val sharedPreferences: SharedPreferences
    private var mReferrerClient: Any? = null
    private var installReferrerStateListener: Any? = null

    init {
        sharedPreferences =
            context.getSharedPreferences("react-native-device-info", Context.MODE_PRIVATE)
        if (!(InstallReferrerClientClazz == null || InstallReferrerStateListenerClazz == null || ReferrerDetailsClazz == null)) {
            try {
                // Build the InstallReferrerClient instance.
                val newBuilderMethod =
                    InstallReferrerClientClazz!!.getMethod("newBuilder", Context::class.java)
                val builder = newBuilderMethod.invoke(null, context)
                val buildMethod = builder.javaClass.getMethod("build")
                mReferrerClient = buildMethod.invoke(builder)

                // Create the InstallReferrerStateListener instance using a Proxy.
                installReferrerStateListener = Proxy.newProxyInstance(
                    InstallReferrerStateListenerClazz!!.getClassLoader(), arrayOf(
                        InstallReferrerStateListenerClazz
                    ),
                    InstallReferrerStateListenerProxy()
                )

                // Call startConnection on the client instance.
                val startConnectionMethod = InstallReferrerClientClazz!!.getMethod(
                    "startConnection",
                    InstallReferrerStateListenerClazz
                )
                startConnectionMethod.invoke(mReferrerClient, installReferrerStateListener)
            } catch (e: Exception) {
                System.err.println("RNInstallReferrerClient exception. getInstallReferrer will be unavailable: " + e.message)
                e.printStackTrace(System.err)
            }
        }
    }

    private inner class InstallReferrerStateListenerProxy : InvocationHandler {
        @Throws(Throwable::class)
        override fun invoke(o: Any, method: Method, args: Array<Any>): Any? {
            val methodName = method.name
            try {
                if (methodName == "onInstallReferrerSetupFinished" && args[0] is Int) {
                    onInstallReferrerSetupFinished(args[0] as Int)
                } else if (methodName == "onInstallReferrerServiceDisconnected") {
                    onInstallReferrerServiceDisconnected()
                }
            } catch (e: Exception) {
                throw RuntimeException("unexpected invocation exception: " + e.message)
            }
            return null
        }

        fun onInstallReferrerSetupFinished(responseCode: Int) {
            when (responseCode) {
                R_RESPONSE_OK ->           // Connection established
                    try {
                        //if (BuildConfig.DEBUG)
                        Log.d("InstallReferrerState", "OK")
                        val getInstallReferrerMethod =
                            InstallReferrerClientClazz!!.getMethod("getInstallReferrer")
                        val response = getInstallReferrerMethod.invoke(mReferrerClient)
                        val getInstallReferrerMethod2 =
                            ReferrerDetailsClazz!!.getMethod("getInstallReferrer")
                        val referrer = getInstallReferrerMethod2.invoke(response) as String
                        val editor = sharedPreferences.edit()
                        editor.putString("installReferrer", referrer)
                        editor.apply()
                        val endConnectionMethod =
                            InstallReferrerClientClazz!!.getMethod("endConnection")
                        endConnectionMethod.invoke(mReferrerClient)
                    } catch (e: Exception) {
                        System.err.println("RNInstallReferrerClient exception. getInstallReferrer will be unavailable: " + e.message)
                        e.printStackTrace(System.err)
                    }

                R_RESPONSE_FEATURE_NOT_SUPPORTED ->           //if (BuildConfig.DEBUG)
                    Log.d("InstallReferrerState", "FEATURE_NOT_SUPPORTED")

                R_RESPONSE_SERVICE_UNAVAILABLE ->           //if (BuildConfig.DEBUG)
                    Log.d("InstallReferrerState", "SERVICE_UNAVAILABLE")
            }
        }

        fun onInstallReferrerServiceDisconnected() {
            // Documentation indicates the InstallReferrer connection will be maintained
            // So there is really nothing to do here
            //if (BuildConfig.DEBUG)
            Log.d("RNInstallReferrerClient", "InstallReferrerService disconnected")
        }
    }

    companion object {
        private var InstallReferrerClientClazz: Class<*>? = null
        private var InstallReferrerStateListenerClazz: Class<*>? = null
        private var ReferrerDetailsClazz: Class<*>? = null

        init {
            try {
                InstallReferrerClientClazz =
                    Class.forName("com.android.installreferrer.api.InstallReferrerClient")
                InstallReferrerStateListenerClazz =
                    Class.forName("com.android.installreferrer.api.InstallReferrerStateListener")
                ReferrerDetailsClazz =
                    Class.forName("com.android.installreferrer.api.ReferrerDetails")
            } catch (e: Exception) {
                System.err.println("RNInstallReferrerClient exception. 'installreferrer' APIs are unavailable.")
            }
        }

        // From InstallReferrerClient.InstallReferrerResponse
        private const val R_RESPONSE_OK = 0
        private const val R_RESPONSE_SERVICE_UNAVAILABLE = 1
        private const val R_RESPONSE_FEATURE_NOT_SUPPORTED = 2
    }
}
