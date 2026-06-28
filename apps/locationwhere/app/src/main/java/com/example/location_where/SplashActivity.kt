package com.example.location_where

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.location_where.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("SplashActivity", "onCreate started")
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuth()
        }, 2000)
    }

    private fun checkAuth() {
        CoroutineScope(Dispatchers.Main).launch {
            val accessToken = tokenManager.getAccessToken()
            val isExpired = tokenManager.isTokenExpired()

            if (accessToken != null && !isExpired) {
                val sharedPrefs = getSharedPreferences("MonitoringPrefs", Context.MODE_PRIVATE)
                val consentSigned = sharedPrefs.getBoolean("consent_signed", false)
                
                if (consentSigned) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                } else {
                    startActivity(Intent(this@SplashActivity, ConsentActivity::class.java))
                }
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}
