package com.example.location_where

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.location_where.api.ApiService
import com.example.location_where.api.ConsentRequest
import com.example.location_where.databinding.ActivityConsentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class ConsentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConsentBinding

    @Inject
    lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.agreeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.continueBtn.isEnabled = isChecked
        }

        binding.continueBtn.setOnClickListener {
            saveConsent()
        }
    }

    private fun saveConsent() {
        val sharedPrefs = getSharedPreferences("MonitoringPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("consent_signed", true).apply()

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.submitConsent(ConsentRequest(true, date))
            } catch (e: Exception) {
                // Background submission failed, handled by sync logic if implemented
            }
        }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
