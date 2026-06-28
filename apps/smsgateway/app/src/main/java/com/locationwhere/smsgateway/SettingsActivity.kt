package com.locationwhere.smsgateway

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.locationwhere.smsgateway.databinding.ActivitySettingsBinding
import com.locationwhere.smsgateway.network.ApiClient
import com.locationwhere.smsgateway.util.Prefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = Prefs.getInstance(this)
        loadSettings()

        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }
    }

    private fun loadSettings() {
        binding.etBackendUrl.setText(prefs.backendUrl)
        binding.etAdminNumber.setText(prefs.adminNumber)
        binding.etGatewaySecret.setText(prefs.gatewaySecret)
        binding.switchForwarding.isChecked = prefs.isForwardingEnabled
    }

    private fun saveSettings() {
        val url = binding.etBackendUrl.text.toString().trim()
        val admin = binding.etAdminNumber.text.toString().trim()
        val secret = binding.etGatewaySecret.text.toString().trim()

        // Validate required fields
        if (url.isEmpty()) {
            binding.etBackendUrl.error = getString(R.string.backend_url_required)
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            binding.etBackendUrl.error = getString(R.string.url_invalid_scheme)
            return
        }
        if (secret.isEmpty()) {
            binding.etGatewaySecret.error = getString(R.string.secret_required)
            return
        }

        prefs.backendUrl = url
        prefs.adminNumber = admin
        prefs.gatewaySecret = secret
        prefs.isForwardingEnabled = binding.switchForwarding.isChecked

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun testConnection() {
        val url = binding.etBackendUrl.text.toString().trim()
        val secret = binding.etGatewaySecret.text.toString().trim()

        if (url.isEmpty() || secret.isEmpty()) {
            Toast.makeText(this, R.string.url_and_secret_required, Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnTestConnection.isEnabled = false
        binding.btnTestConnection.text = getString(R.string.testing)

        ApiClient.testConnection(url, secret) { success, message ->
            runOnUiThread {
                binding.btnTestConnection.isEnabled = true
                binding.btnTestConnection.text = getString(R.string.test_connection)
                val result = if (success) {
                    getString(R.string.connection_successful)
                } else {
                    getString(R.string.connection_failed, message ?: "Unknown error")
                }
                Toast.makeText(this, result, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
