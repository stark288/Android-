package com.anonymity.toolkit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        requestAllPermissions()
        checkOverlayPermission()

        startButton.setOnClickListener {
            startAnonymity()
        }
        stopButton.setOnClickListener {
            stopAnonymity()
        }
    }

    private fun requestAllPermissions() {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                startActivity(intent)
            }
        }
    }

    private fun startAnonymity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(AnonymityService.getIntent(this))
        } else {
            startService(AnonymityService.getIntent(this))
        }
        isRunning = true
        statusText.text = "🟢 ACTIVE\nRotating every 1s"
        Toast.makeText(this, "Anonymity service started", Toast.LENGTH_SHORT).show()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work = PeriodicWorkRequestBuilder<AnonymityWorker>(1, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .setInitialDelay(0, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(work)
    }

    private fun stopAnonymity() {
        stopService(AnonymityService.getIntent(this))
        isRunning = false
        statusText.text = "🔴 STOPPED\nTraces erased"
        Toast.makeText(this, "Service stopped. Traces wiped.", Toast.LENGTH_SHORT).show()
        WorkManager.getInstance(this).cancelAllWork()
    }
}
