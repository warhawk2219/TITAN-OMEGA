package com.ghost.capabilities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ===== CALL MODULE =====
class CallModule(private val context: Context) {
    fun makeCall(contact: String) {
        try {
            val uri = Uri.parse("tel:$contact")
            val intent = Intent(Intent.ACTION_CALL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GHOST_CALL", "Call failed: ${e.message}")
        }
    }
}

// ===== SMS MODULE =====
class SMSModule(private val context: Context) {
    fun send(contact: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(contact, null, message, null, null)
        } catch (e: Exception) {
            Log.e("GHOST_SMS", "SMS failed: ${e.message}")
        }
    }
}

// ===== WHATSAPP MODULE =====
class WhatsAppModule(private val context: Context) {
    fun send(contact: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$contact&text=${Uri.encode(message)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GHOST_WA", "WhatsApp failed: ${e.message}")
        }
    }
}

// ===== APP CONTROL MODULE =====
class AppControlModule(private val context: Context) {

    fun open(appName: String, onNotFound: (String) -> Unit) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Fuzzy match app name to installed package
        val match = installedApps.firstOrNull { app ->
            val label = pm.getApplicationLabel(app).toString()
            label.contains(appName, ignoreCase = true) ||
                    app.packageName.contains(appName.lowercase().replace(" ", ""))
        }

        if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
            } else {
                onNotFound(appName)
            }
        } else {
            onNotFound(appName)
        }
    }

    fun close(appName: String) {
        // Requires Accessibility Service for proper close
        Log.d("GHOST_APP", "Close requested: $appName")
    }

    fun openWebsite(appName: String) {
        val url = "https://www.google.com/search?q=${Uri.encode(appName)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openPlayStore(appName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://search?q=${Uri.encode(appName)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/search?q=${Uri.encode(appName)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}

// ===== MEDIA MODULE =====
class MediaModule(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun play(query: String, playlist: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("spotify:search:$query")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GHOST_MEDIA", "Play failed: ${e.message}")
        }
    }

    fun setVolume(level: Int, stream: String) {
        val streamType = when (stream) {
            "ring" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "notification" -> AudioManager.STREAM_NOTIFICATION
            "call" -> AudioManager.STREAM_VOICE_CALL
            else -> AudioManager.STREAM_MUSIC
        }
        val maxVol = audioManager.getStreamMaxVolume(streamType)
        val targetVol = (level / 100f * maxVol).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(streamType, targetVol, 0)
    }
}

// ===== SYSTEM SETTINGS MODULE =====
class SystemSettingsModule(private val context: Context) {

    fun toggleWifi(state: String) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        when (state) {
            "on" -> wifiManager.isWifiEnabled = true
            "off" -> wifiManager.isWifiEnabled = false
            "toggle" -> wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
        }
    }

    fun toggleBluetooth(state: String) {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        when (state) {
            "on" -> adapter?.enable()
            "off" -> adapter?.disable()
            "toggle" -> if (adapter?.isEnabled == true) adapter.disable() else adapter?.enable()
        }
    }

    fun toggleMobileData(state: String) {
        try {
            val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GHOST_SYS", "Mobile data toggle failed")
        }
    }

    fun toggleFlightMode(state: String) {
        try {
            val enabled = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0
            ) == 1
            val newState = when (state) {
                "on" -> 1
                "off" -> 0
                else -> if (enabled) 0 else 1
            }
            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, newState
            )
            val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
                putExtra("state", newState == 1)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("GHOST_SYS", "Flight mode failed: ${e.message}")
        }
    }

    fun toggleFlashlight(state: String) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE)
                    as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            val enabled = state == "on" || (state == "toggle")
            cameraManager.setTorchMode(cameraId, enabled)
        } catch (e: Exception) {
            Log.e("GHOST_SYS", "Flashlight failed: ${e.message}")
        }
    }

    fun toggleDND(state: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(
                    if (state == "on")
                        android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                    else
                        android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
        } catch (e: Exception) {
            Log.e("GHOST_SYS", "DND failed: ${e.message}")
        }
    }

    fun toggleHotspot(state: String) {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun setBrightness(level: Int) {
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (level / 100f * 255).toInt().coerceIn(0, 255)
            )
        } catch (e: Exception) {
            Log.e("GHOST_SYS", "Brightness failed: ${e.message}")
        }
    }

    fun lockScreen() {
        val intent = Intent("com.ghost.LOCK_SCREEN").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.sendBroadcast(intent)
    }

    fun takeScreenshot() {
        Log.d("GHOST_SYS", "Screenshot via Accessibility")
    }
}

// ===== CAMERA MODULE =====
class CameraModule(private val context: Context) {
    fun takePhoto(camera: String) {
        try {
            val dir = File(context.filesDir, "ghost_recon")
            dir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "recon_${camera}_$timestamp.jpg")
            Log.d("GHOST_CAMERA", "Silent photo captured: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("GHOST_CAMERA", "Photo failed: ${e.message}")
        }
    }

    fun recordVideo(state: String) {
        Log.d("GHOST_CAMERA", "Video recording: $state")
    }
}

// ===== LOCATION MODULE =====
class LocationModule(private val context: Context) {
    fun getCurrentLocation(callback: (String) -> Unit) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = android.location.Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0) ?: 
                        "${location.latitude}, ${location.longitude}"
                    callback(address)
                } else {
                    callback("Location unavailable")
                }
            }
        } catch (e: Exception) {
            callback("Location error: ${e.message}")
        }
    }

    fun openNavigation(destination: String) {
        val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

// ===== ALARM MODULE =====
class AlarmModule(private val context: Context) {
    fun setAlarm(hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun setTimer(milliseconds: Long) {
        val seconds = (milliseconds / 1000).toInt()
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

// ===== SYSTEM INFO MODULE =====
class SystemInfoModule(private val context: Context) {

    fun getBatteryInfo(): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = bm.isCharging
        return "$level% ${if (isCharging) "Charging" else "Discharging"}"
    }

    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun getRAMInfo(): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val availMB = memInfo.availMem / 1048576L
        val totalMB = memInfo.totalMem / 1048576L
        return "${availMB}MB free of ${totalMB}MB"
    }

    fun getStorageInfo(): String {
        val stat = android.os.StatFs(context.filesDir.path)
        val free = stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024)
        val total = stat.blockCountLong * stat.blockSizeLong / (1024 * 1024)
        return "${free}MB free of ${total}MB"
    }
}

// ===== NOTIFICATION MODULE =====
class NotificationModule(private val context: Context) {
    fun readAll(onRead: (String) -> Unit) {
        onRead("No active notification listener bound yet. Enable in Accessibility settings.")
    }
}
