package com.example.location_where

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.location_where.databinding.ActivityLoginBinding
import com.example.location_where.ui.LoginUiState
import com.example.location_where.ui.LoginViewModel
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkRoot()

        binding.loginBtn.setOnClickListener {
            val code = binding.employeeCode.text.toString()
            val pass = binding.password.text.toString()
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            
            if (code.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.login(code, pass, androidId)
            } else {
                Toast.makeText(this, "সবগুলো ঘর পূরণ করুন", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginUiState.Loading -> {
                    binding.loading.visibility = View.VISIBLE
                    binding.loginBtn.isEnabled = false
                }
                is LoginUiState.Success -> {
                    binding.loading.visibility = View.GONE
                    routeAfterLogin()
                    finish()
                }
                is LoginUiState.Error -> {
                    binding.loading.visibility = View.GONE
                    binding.loginBtn.isEnabled = true
                    binding.errorText.text = state.message
                }
            }
        }
    }

    private fun checkRoot() {
        val rootBeer = RootBeer(this)
        if (rootBeer.isRooted) {
            Toast.makeText(this, "সতর্কবার্তা: ডিভাইসটি রুটেড! এটি কোম্পানির নীতির পরিপন্থী।", Toast.LENGTH_LONG).show()
        }
    }
    private fun routeAfterLogin() {
        val sharedPrefs = getSharedPreferences("MonitoringPrefs", Context.MODE_PRIVATE)
        val consentSigned = sharedPrefs.getBoolean("consent_signed", false)
        val nextActivity = if (consentSigned) MainActivity::class.java else ConsentActivity::class.java
        startActivity(Intent(this, nextActivity))
    }
}
