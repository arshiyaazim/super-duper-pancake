package com.locationwhere.smsgateway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.locationwhere.smsgateway.databinding.ActivityMainBinding
import com.locationwhere.smsgateway.service.GatewayService
import com.locationwhere.smsgateway.ui.LogAdapter
import com.locationwhere.smsgateway.ui.LogViewModel
import com.locationwhere.smsgateway.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: LogViewModel
    private lateinit var adapter: LogAdapter
    private lateinit var prefs: Prefs

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startGatewayService()
            checkBatteryOptimization()
        } else {
            showPermissionRationale()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        prefs = Prefs.getInstance(this)
        viewModel = ViewModelProvider(this)[LogViewModel::class.java]
        adapter = LogAdapter()

        setupRecyclerView()
        checkPermissions()
        observeLogs()

        binding.fabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        updateStatusCard()
    }

    override fun onResume() {
        super.onResume()
        updateStatusCard()
    }

    private fun setupRecyclerView() {
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter
    }

    private fun observeLogs() {
        viewModel.allLogs.observe(this) { logs ->
            adapter.submitList(logs)
            binding.emptyStateLayout.visibility = if (logs.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.rvLogs.visibility = if (logs.isNullOrEmpty()) View.GONE else View.VISIBLE
            updateStatusCard()
        }
    }

    private fun updateStatusCard() {
        when {
            !prefs.isConfigured -> {
                binding.tvStatusTitle.text = getString(R.string.gateway_not_configured)
                binding.statusIcon.setImageResource(android.R.drawable.presence_offline)
            }
            prefs.isForwardingEnabled -> {
                binding.tvStatusTitle.text = getString(R.string.gateway_active)
                binding.statusIcon.setImageResource(android.R.drawable.presence_online)
            }
            else -> {
                binding.tvStatusTitle.text = getString(R.string.gateway_inactive)
                binding.statusIcon.setImageResource(android.R.drawable.presence_busy)
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) {
            startGatewayService()
            checkBatteryOptimization()
        } else {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permissions_required_title)
            .setMessage(R.string.permissions_required_message)
            .setPositiveButton(R.string.action_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun checkBatteryOptimization() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.battery_opt_title)
                .setMessage(R.string.battery_opt_message)
                .setPositiveButton(R.string.action_settings) { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun startGatewayService() {
        val serviceIntent = Intent(this, GatewayService::class.java).apply {
            action = GatewayService.ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_logs -> {
                viewModel.deleteAllLogs()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
