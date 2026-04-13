package com.ghost.core

import android.content.Context
import android.util.Log
import com.ghost.capabilities.*
import com.ghost.overlay.OverlayManager
import com.ghost.protocols.ProtocolEngine

class ActionDispatcher(
    private val context: Context,
    private val protocolEngine: ProtocolEngine,
    private val overlayManager: OverlayManager,
    private val ttsEngine: TTSEngine
) {
    companion object { const val TAG = "GHOST_DISPATCHER" }

    private val callModule by lazy { CallModule(context) }
    private val smsModule by lazy { SMSModule(context) }
    private val whatsAppModule by lazy { WhatsAppModule(context) }
    private val appControlModule by lazy { AppControlModule(context) }
    private val mediaModule by lazy { MediaModule(context) }
    private val systemModule by lazy { SystemSettingsModule(context) }
    private val cameraModule by lazy { CameraModule(context) }
    private val locationModule by lazy { LocationModule(context) }
    private val alarmModule by lazy { AlarmModule(context) }
    private val systemInfoModule by lazy { SystemInfoModule(context) }
    private val notificationModule by lazy { NotificationModule(context) }

    fun dispatch(action: GhostAction) {
        Log.d(TAG, "Dispatching: ${action.type} → ${action.target}")
        when (action.type) {
            "call" -> callModule.makeCall(action.target)
            "sms" -> smsModule.send(action.target, action.params["message"] ?: "")
            "whatsapp" -> whatsAppModule.send(action.target, action.params["message"] ?: "")
            "open_app" -> appControlModule.open(action.target) { notFound ->
                ttsEngine.speak("$notFound not installed, Boss. Open the website instead?")
            }
            "close_app" -> appControlModule.close(action.target)
            "play_music" -> mediaModule.play(action.target, action.params["playlist"] ?: "")
            "set_volume" -> mediaModule.setVolume(
                action.target.toIntOrNull() ?: 50,
                action.params["stream"] ?: "media"
            )
            "set_brightness" -> systemModule.setBrightness(action.target.toIntOrNull() ?: 50)
            "toggle_wifi" -> systemModule.toggleWifi(action.params["state"] ?: "toggle")
            "toggle_bluetooth" -> systemModule.toggleBluetooth(action.params["state"] ?: "toggle")
            "toggle_data" -> systemModule.toggleMobileData(action.params["state"] ?: "toggle")
            "toggle_flight_mode" -> systemModule.toggleFlightMode(action.params["state"] ?: "toggle")
            "toggle_flashlight" -> systemModule.toggleFlashlight(action.params["state"] ?: "toggle")
            "toggle_dnd" -> systemModule.toggleDND(action.params["state"] ?: "toggle")
            "toggle_hotspot" -> systemModule.toggleHotspot(action.params["state"] ?: "toggle")
            "take_photo" -> cameraModule.takePhoto(action.params["camera"] ?: "rear")
            "record_video" -> cameraModule.recordVideo(action.params["state"] ?: "start")
            "set_alarm" -> alarmModule.setAlarm(
                action.params["hour"]?.toIntOrNull() ?: 7,
                action.params["minute"]?.toIntOrNull() ?: 0,
                action.params["label"] ?: "GHOST Alarm"
            )
            "set_timer" -> alarmModule.setTimer(action.target.toLongOrNull() ?: 60000L)
            "get_location" -> locationModule.getCurrentLocation { addr ->
                ttsEngine.speak("Current location: $addr, Boss.")
            }
            "navigate" -> locationModule.openNavigation(action.target)
            "get_battery" -> {
                val info = systemInfoModule.getBatteryInfo()
                ttsEngine.speak("Battery at $info, Boss.")
            }
            "get_ram" -> {
                val info = systemInfoModule.getRAMInfo()
                ttsEngine.speak("RAM status: $info, Boss.")
            }
            "read_notifications" -> notificationModule.readAll { ttsEngine.speak(it) }
            "lock_screen" -> systemModule.lockScreen()
            "screenshot" -> systemModule.takeScreenshot()
            "protocol" -> {
                val state = action.params["state"] ?: "start"
                val category = action.params["category"] ?: "protocol"
                if (category == "mode") {
                    if (state == "start") protocolEngine.activateMode(action.target)
                    else protocolEngine.deactivateMode(action.target)
                } else {
                    if (state == "start") protocolEngine.commence(action.target)
                    else protocolEngine.standDown(action.target)
                }
            }
            "none" -> Log.d(TAG, "No action.")
            else -> Log.w(TAG, "Unknown action: ${action.type}")
        }
    }
}
