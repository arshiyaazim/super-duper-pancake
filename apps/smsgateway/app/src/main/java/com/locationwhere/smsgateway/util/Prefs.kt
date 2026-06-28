package com.locationwhere.smsgateway.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.locationwhere.smsgateway.BuildConfig

class Prefs private constructor(context: Context) {
    private val masterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "gateway_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var backendUrl: String
        get() {
            val stored = sharedPrefs.getString("backend_url", "") ?: ""
            if (stored.isNotBlank()) {
                return stored
            }
            return BuildConfig.DEFAULT_BACKEND_URL
        }
        set(value) = sharedPrefs.edit().putString("backend_url", value).apply()

    var adminNumber: String
        get() {
            val stored = sharedPrefs.getString("admin_number", "") ?: ""
            if (stored.isNotBlank()) {
                return stored
            }
            return BuildConfig.DEFAULT_ADMIN_NUMBER
        }
        set(value) = sharedPrefs.edit().putString("admin_number", value).apply()

    var gatewaySecret: String
        get() {
            val stored = sharedPrefs.getString("gateway_secret", "") ?: ""
            if (stored.isNotBlank()) {
                return stored
            }
            return BuildConfig.DEFAULT_GATEWAY_SECRET
        }
        set(value) = sharedPrefs.edit().putString("gateway_secret", value).apply()

    var isForwardingEnabled: Boolean
        get() = sharedPrefs.getBoolean("forwarding_enabled", false)
        set(value) = sharedPrefs.edit().putBoolean("forwarding_enabled", value).apply()

    val isConfigured: Boolean
        get() = backendUrl.isNotEmpty() && gatewaySecret.isNotEmpty()

    companion object {
        @Volatile
        private var INSTANCE: Prefs? = null

        fun getInstance(context: Context): Prefs {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Prefs(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
