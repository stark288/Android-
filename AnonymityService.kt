package com.anonymity.toolkit

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import java.util.*
import kotlin.concurrent.thread

class AnonymityService : Service() {
    private lateinit var locationManager: LocationManager
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private var isRunning = true
    private val tag = "AnonymityService"

    companion object {
        private const val NOTIF_ID = 999
        fun getIntent(context: Context): Intent = Intent(context, AnonymityService::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        startForeground(NOTIF_ID, createNotification())
        thread { runLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "anonymity_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Anonymity Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Anonymity Toolkit")
            .setContentText("Rotating identity every second")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
    }

    private fun runLoop() {
        while (isRunning) {
            try {
                rotateIP()
                rotateDNS()
                rotateMAC()
                rotateGPS()
                wipeTraces()
                Thread.sleep(1000)
            } catch (e: Exception) {
                Log.e(tag, "Rotation error: ${e.message}")
            }
        }
    }

    private fun rotateIP() {
        try {
            val proxy = "proxy${Random.nextInt(1000)}.local"
            val port = 8080 + Random.nextInt(100)
            Settings.Global.putString(contentResolver, "http_proxy", "$proxy:$port")
            Settings.Global.putString(contentResolver, "global_http_proxy_host", proxy)
            Settings.Global.putInt(contentResolver, "global_http_proxy_port", port)
            Log.d(tag, "IP rotated via proxy $proxy:$port")
        } catch (e: Exception) {
            Log.e(tag, "IP rotate failed: ${e.message}")
        }
    }

    private fun rotateDNS() {
        try {
            val dnsServers = listOf(
                "1.1.1.1", "8.8.8.8", "9.9.9.9", "1.0.0.1",
                "208.67.222.222", "208.67.220.220", "76.76.19.19",
                "185.228.168.168", "185.228.169.168"
            )
            val dns = dnsServers.random()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Settings.Global.putString(contentResolver, "private_dns_mode", "hostname")
                Settings.Global.putString(contentResolver, "private_dns_specifier", dns)
                Log.d(tag, "DNS changed to $dns")
            }
        } catch (e: Exception) {
            Log.e(tag, "DNS rotate failed: ${e.message}")
        }
    }

    private fun rotateMAC() {
        try {
            val wifiLock = wifiManager.createWifiLock("MACRotator")
            wifiLock.setReferenceCounted(false)
            wifiLock.acquire()
            Thread.sleep(50)
            wifiLock.release()
            try {
                val method = wifiManager.javaClass.getMethod("setMacAddress", String::class.java)
                method.invoke(wifiManager, randomMAC())
                Log.d(tag, "MAC randomized")
            } catch (e: Exception) {
                // fallback
            }
        } catch (e: Exception) {
            Log.e(tag, "MAC rotate failed: ${e.message}")
        }
    }

    private fun randomMAC(): String {
        return (1..6).joinToString(":") { "%02x".format(Random.nextInt(256)) }
    }

    private fun rotateGPS() {
        try {
            Settings.Secure.putString(contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION, "1")
            val lat = Random.nextDouble() * 180 - 90
            val lon = Random.nextDouble() * 360 - 180
            val mockLoc = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = lat
                longitude = lon
                time = System.currentTimeMillis()
                accuracy = Random.nextFloat() * 10 + 1
            }
            try {
                locationManager.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    false, false, false, false,
                    true, true, true, 0, 5
                )
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            } catch (e: Exception) {}
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLoc)
            Log.d(tag, "GPS spoofed to $lat, $lon")
        } catch (e: Exception) {
            Log.e(tag, "GPS rotate failed: ${e.message}")
        }
    }

    private fun wipeTraces() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).clearPrimaryClip()
            }
            locationManager.removeUpdates { }
            val cacheDir = cacheDir
            if (cacheDir.exists()) { cacheDir.deleteRecursively() }
            val extCache = getExternalCacheDir()
            if (extCache != null && extCache.exists()) { extCache.deleteRecursively() }
            Log.d(tag, "Traces wiped")
        } catch (e: Exception) {
            Log.e(tag, "Trace wipe failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        isRunning = false
        wipeTraces()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
