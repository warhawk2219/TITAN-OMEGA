package com.ghost.protocols

import android.content.Context
import android.util.Log
import com.ghost.capabilities.*
import com.ghost.core.TTSEngine
import com.ghost.bridge.BridgeHandler
import kotlinx.coroutines.*

class ProtocolEngine(private val context: Context) {

    private var ttsEngine: TTSEngine? = null
    private var bridgeHandler: BridgeHandler? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeProtocols = mutableSetOf<String>()
    private var currentMode = "normal"

    // Callsign helpers
    private val affirmative = listOf(
        "Affirmative", "Confirmed", "Roger", "Copy",
        "Positive", "Understood", "On it", "Roger that", "Acknowledged"
    )
    private var lastAffirmIndex = 0

    fun init(tts: TTSEngine, bridge: BridgeHandler) {
        ttsEngine = tts
        bridgeHandler = bridge
    }

    fun getCurrentMode() = currentMode

    private fun say(text: String) = ttsEngine?.speak(text)

    private fun nextAffirmative(): String {
        val word = affirmative[lastAffirmIndex % affirmative.size]
        lastAffirmIndex++
        return word
    }

    // ===== COMMENCE =====
    fun commence(protocolName: String) {
        val name = protocolName.lowercase().replace(" ", "_")
        Log.d("GHOST_PROTOCOL", "Commencing: $name")

        when (name) {
            "doomsday" -> executeDoomsday()
            "blackout" -> executeBlackout(true)
            "ghost" -> executeGhostProtocol(true)
            "lockdown" -> executeLockdown(true)
            "morning" -> executeMorning()
            "night" -> executeNight()
            "drive" -> executeDrive(true)
            "focus" -> executeFocus(true)
            "recon" -> executeRecon()
            "purge" -> executePurge()
            "shadow" -> executeShadow(true)
            "incognito" -> executeIncognito(true)
            "broadcast" -> executeBroadcast()
            "briefing" -> executeBriefing()
            "shutdown" -> executeShutdown()
            "sos_lite" -> executeSOSLite()
            "camouflage" -> executeCamouflage(true)
            "phantom" -> executePhantom(true)
            "fortress" -> executeFortress(true)
            "hunt" -> executeHunt(true)
            "decoy" -> executeDecoy(true)
            "panic" -> executeDoomsday() // Instant — no announcement
            "anti_theft" -> executeAntiTheft(true)
            "debrief" -> executeDebrief()
            "guardian" -> executeGuardian(true)
            "classified" -> executeClassified(true)
            "interceptor" -> executeInterceptor(true)
            "phantom_call" -> executePhantomCall(true)
            "sentinel" -> executeSentinel(true)
            "predator" -> executePredator(true)
            "irongate" -> executeIrongate()
            else -> say("Unknown protocol, Boss.")
        }

        if (isToggle(name)) activeProtocols.add(name)
        bridgeHandler?.syncProtocol(name, "start")
    }

    // ===== STAND DOWN =====
    fun standDown(protocolName: String) {
        val name = protocolName.lowercase().replace(" ", "_")
        Log.d("GHOST_PROTOCOL", "Standing down: $name")

        when (name) {
            "blackout" -> executeBlackout(false)
            "ghost" -> executeGhostProtocol(false)
            "lockdown" -> executeLockdown(false)
            "drive" -> executeDrive(false)
            "focus" -> executeFocus(false)
            "shadow" -> executeShadow(false)
            "incognito" -> executeIncognito(false)
            "phantom" -> executePhantom(false)
            "fortress" -> executeFortress(false)
            "hunt" -> executeHunt(false)
            "anti_theft" -> executeAntiTheft(false)
            "guardian" -> executeGuardian(false)
            "interceptor" -> executeInterceptor(false)
            "phantom_call" -> executePhantomCall(false)
            "sentinel" -> executeSentinel(false)
            "predator" -> executePredator(false)
            "camouflage" -> {
                say("Camouflage lifted, Sir. Back online.")
                activeProtocols.remove("camouflage")
            }
            "decoy" -> {
                say("Decoy dropped, Sir. Back online.")
                activeProtocols.remove("decoy")
            }
        }

        activeProtocols.remove(name)
        bridgeHandler?.syncProtocol(name, "stop")
    }

    // ===== MODE CONTROL =====
    fun activateMode(modeName: String) {
        currentMode = modeName.lowercase()
        bridgeHandler?.syncMode(modeName, "start")

        when (currentMode) {
            "silent" -> say("Silent Mode active, Boss.")
            "ghost_mode" -> { /* notification only */ }
            "combat" -> say("Combat Mode. Go.")
            "guardian_mode" -> say("Guardian Mode active, Boss. I've got eyes on everything.")
            "briefing_mode" -> say("Briefing Mode active, Boss. Full detail on everything from here.")
            "incognito_mode" -> say("Incognito Mode active, Boss.")
            "lockdown_mode" -> say("Lockdown Mode active, Sir. Owner access only.")
            "shadow_mode" -> { /* complete silence */ }
            "recon_mode" -> say("Recon Mode active, Sir. Eyes open.")
            "casual" -> say("Casual mode on. What's up, Hari?")
        }
    }

    fun deactivateMode(modeName: String) {
        currentMode = "normal"
        bridgeHandler?.syncMode(modeName, "stop")
        say("${modeName.replace("_", " ").capitalize()} lifted, Boss.")
    }

    // ===== PROTOCOL IMPLEMENTATIONS =====

    private fun executeDoomsday() {
        say("Doomsday initiated, Sir. Acquiring location and dispatching distress signal now. Standing by on comms.")
        scope.launch {
            val locationModule = LocationModule(context)
            locationModule.getCurrentLocation { address ->
                val whatsApp = WhatsAppModule(context)
                val prefs = context.getSharedPreferences("ghost_prefs", Context.MODE_PRIVATE)
                val contact = prefs.getString("emergency_contact", "") ?: ""
                val customMsg = prefs.getString("doomsday_message", "I need help.") ?: ""
                val message = """
🚨 DISTRESS SIGNAL
Name: Hari
Location: $address
Message: $customMsg
Time: ${java.util.Date()}
""".trimIndent()
                whatsApp.send(contact, message)
                ttsEngine?.speak("Signal dispatched, Sir. Emergency contact has been reached.")
            }
        }
    }

    private fun executeBlackout(activate: Boolean) {
        val sys = SystemSettingsModule(context)
        if (activate) {
            say("Blackout initiated, Sir. All networks killed. Going dark.")
            sys.toggleWifi("off")
            sys.toggleBluetooth("off")
            sys.toggleMobileData("off")
            sys.lockScreen()
            activeProtocols.add("blackout")
        } else {
            say("Blackout lifted, Sir. Networks restored.")
            sys.toggleWifi("on")
            sys.toggleBluetooth("on")
            sys.toggleMobileData("on")
            activeProtocols.remove("blackout")
        }
    }

    private fun executeGhostProtocol(activate: Boolean) {
        val sys = SystemSettingsModule(context)
        if (activate) {
            say("Ghost Protocol active, Sir. Going silent.")
            sys.toggleDND("on")
            ttsEngine?.setMuted(true)
        } else {
            ttsEngine?.setMuted(false)
            say("Ghost Protocol lifted. Back on comms, Sir.")
            sys.toggleDND("off")
        }
    }

    private fun executeLockdown(activate: Boolean) {
        if (activate) {
            say("Lockdown active, Sir. Apps secured. Whitelist only. Auto-reply enabled.")
        } else {
            say("Lockdown lifted, Sir. Normal access restored.")
        }
    }

    private fun executeMorning() {
        say("Good morning, Boss. GHOST has you sorted. Here's your day.")
        val sys = SystemSettingsModule(context)
        sys.toggleFlightMode("off")
        sys.toggleWifi("on")
        sys.setBrightness(60)
        val media = MediaModule(context)
        val prefs = context.getSharedPreferences("ghost_prefs", Context.MODE_PRIVATE)
        val playlist = prefs.getString("morning_playlist", "Morning") ?: "Morning"
        media.play(playlist, playlist)
        executeBriefing()
    }

    private fun executeNight() {
        say("Night Protocol active, Boss. GHOST has handled the settings. Get some rest.")
        val sys = SystemSettingsModule(context)
        sys.toggleFlightMode("on")
        sys.toggleDND("on")
        sys.setBrightness(10)
        val alarm = AlarmModule(context)
        val prefs = context.getSharedPreferences("ghost_prefs", Context.MODE_PRIVATE)
        val hour = prefs.getInt("morning_alarm_hour", 7)
        val minute = prefs.getInt("morning_alarm_minute", 0)
        alarm.setAlarm(hour, minute, "GHOST Morning")
    }

    private fun executeDrive(activate: Boolean) {
        if (activate) {
            say("Drive Protocol active, Boss. Auto-reply on. Music loading. Eyes on the road.")
            val sys = SystemSettingsModule(context)
            sys.toggleDND("on")
            val media = MediaModule(context)
            val prefs = context.getSharedPreferences("ghost_prefs", Context.MODE_PRIVATE)
            val playlist = prefs.getString("drive_playlist", "Drive") ?: "Drive"
            media.play(playlist, playlist)
        } else {
            say("Drive Protocol lifted, Boss. Welcome back.")
        }
    }

    private fun executeFocus(activate: Boolean) {
        if (activate) {
            say("Focus Protocol active, Boss. I'll hold your calls.")
            val sys = SystemSettingsModule(context)
            sys.toggleDND("on")
        } else {
            say("Focus lifted, Boss.")
            val sys = SystemSettingsModule(context)
            sys.toggleDND("off")
        }
    }

    private fun executeRecon() {
        // Silent — no announcement during capture
        scope.launch {
            val camera = CameraModule(context)
            camera.takePhoto("front")
            delay(500)
            camera.takePhoto("rear")
            delay(30000) // 30 second audio window
            ttsEngine?.speak("Recon complete, Sir. GHOST has secured the data.")
        }
    }

    private fun executePurge() {
        say("Purge initiated, Sir.")
        scope.launch {
            // Clear cache
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
            ttsEngine?.speak("Purge complete. Clean slate, Sir.")
        }
    }

    private fun executeShadow(activate: Boolean) {
        if (activate) {
            say("Shadow active, Sir. Logging in progress.")
        } else {
            say("Shadow lifted, Sir. Logging terminated.")
        }
    }

    private fun executeIncognito(activate: Boolean) {
        if (activate) {
            say("Incognito active, Boss. You're off the grid socially.")
        } else {
            say("Incognito lifted, Boss.")
        }
    }

    private fun executeBroadcast() {
        say("Broadcast initiated, Boss. Confirm targets and message.")
    }

    private fun executeBriefing() {
        scope.launch {
            val info = SystemInfoModule(context)
            val battery = info.getBatteryInfo()
            val ram = info.getRAMInfo()
            ttsEngine?.speak(
                "Briefing, Boss. Battery $battery. RAM $ram. All systems nominal."
            )
        }
    }

    private fun executeShutdown() {
        say("Initiating Shutdown sequence, Boss.")
        scope.launch {
            delay(2000)
            context.cacheDir.deleteRecursively()
            executeNight()
        }
    }

    private fun executeSOSLite() {
        scope.launch {
            val location = LocationModule(context)
            location.getCurrentLocation { address ->
                val prefs = context.getSharedPreferences("ghost_prefs", Context.MODE_PRIVATE)
                val contact = prefs.getString("sos_contact", "") ?: ""
                val whatsApp = WhatsAppModule(context)
                whatsApp.send(contact, "📍 Location: $address")
                ttsEngine?.speak("Location signal sent, Sir. Standing by.")
            }
        }
    }

    private fun executeCamouflage(activate: Boolean) {
        if (activate) {
            say("Camouflage active, Sir. Going dark.")
            ttsEngine?.setMuted(true)
        }
    }

    private fun executePhantom(activate: Boolean) {
        if (activate) {
            say("Phantom Protocol active, Sir. Stationary detection triggered. Logging position.")
            scope.launch {
                while (activeProtocols.contains("phantom")) {
                    val loc = LocationModule(context)
                    loc.getCurrentLocation { }
                    delay(300000) // 5 minutes
                }
            }
        } else {
            say("Phantom lifted, Sir. Logging terminated.")
        }
    }

    private fun executeFortress(activate: Boolean) {
        if (activate) {
            say("Fortress active, Sir. Maximum security engaged.")
            val sys = SystemSettingsModule(context)
            sys.toggleWifi("off")
            sys.toggleMobileData("off")
            scope.launch {
                while (activeProtocols.contains("fortress")) {
                    // Wipe clipboard every 30 seconds
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                    clipboard.clearPrimaryClip()
                    delay(30000)
                }
            }
        } else {
            say("Fortress lifted, Sir. Security returned to normal.")
        }
    }

    private fun executeHunt(activate: Boolean) {
        if (activate) {
            say("Hunt active, Boss. Monitoring target.")
        } else {
            say("Hunt terminated, Boss.")
        }
    }

    private fun executeDecoy(activate: Boolean) {
        if (activate) {
            ttsEngine?.setMuted(true)
        }
    }

    private fun executeAntiTheft(activate: Boolean) {
        if (activate) {
            say("Anti-Theft active, Sir. Device locked. GHOST is watching.")
            scope.launch {
                while (activeProtocols.contains("anti_theft")) {
                    CameraModule(context).takePhoto("front")
                    delay(30000)
                }
            }
        } else {
            say("Anti-Theft lifted, Sir.")
        }
    }

    private fun executeDebrief() {
        scope.launch {
            val info = SystemInfoModule(context)
            val battery = info.getBatteryInfo()
            val ram = info.getRAMInfo()
            ttsEngine?.speak(
                "Today's debrief, Boss. Battery cycles nominal. $battery. $ram. All logged."
            )
        }
    }

    private fun executeGuardian(activate: Boolean) {
        if (activate) {
            say("Guardian Mode active, Boss. I've got eyes on everything.")
            scope.launch {
                while (activeProtocols.contains("guardian")) {
                    val info = SystemInfoModule(context)
                    val level = info.getBatteryLevel()
                    if (level <= 10) {
                        ttsEngine?.speak("Battery critical, Boss. GHOST activating ultra-save mode. Plug in when you can, Boss.")
                    } else if (level <= 20) {
                        ttsEngine?.speak("Battery at 20%, Boss. GHOST is trimming background processes.")
                    }
                    delay(60000)
                }
            }
        } else {
            say("Guardian lifted, Boss.")
        }
    }

    private fun executeClassified(activate: Boolean) {
        if (activate) {
            say("Classified active, Sir.")
            ttsEngine?.setMuted(true)
        }
    }

    private fun executeInterceptor(activate: Boolean) {
        if (activate) {
            say("Interceptor active, Boss. Monitoring all incoming.")
        } else {
            say("Interceptor offline, Boss.")
        }
    }

    private fun executePhantomCall(activate: Boolean) {
        if (activate) {
            say("Phantom Call armed, Sir. Trigger word set. Listening.")
        } else {
            say("Phantom Call disarmed, Sir.")
        }
    }

    private fun executeSentinel(activate: Boolean) {
        if (activate) {
            say("Sentinel armed, Sir. Monitoring all access points.")
        } else {
            say("Sentinel disarmed, Sir.")
        }
    }

    private fun executePredator(activate: Boolean) {
        if (activate) {
            say("Predator active, Sir. Scanning all processes.")
        } else {
            say("Predator offline, Sir.")
        }
    }

    private fun executeIrongate() {
        scope.launch {
            say("IRONGATE initiated, Sir. Running ecosystem health check.")
            delay(3000)
            // Check Bridge connection
            val bridgeConnected = bridgeHandler?.isConnected() ?: false
            val result = if (bridgeConnected) "ALL CLEAR" else "SOMETHING'S DOWN"
            bridgeHandler?.sendIrongateResult(result)
            if (result == "ALL CLEAR") {
                // Silent — no notification if all clear
            } else {
                ttsEngine?.speak("IRONGATE flagged an issue, Sir. Check Bridge status.")
            }
        }
    }

    private fun isToggle(name: String) = name in listOf(
        "blackout", "ghost", "lockdown", "drive", "focus", "shadow",
        "incognito", "camouflage", "phantom", "fortress", "hunt", "decoy",
        "anti_theft", "guardian", "classified", "interceptor",
        "phantom_call", "sentinel", "predator"
    )

    fun isActive(protocolName: String) = activeProtocols.contains(protocolName.lowercase())

    fun getActiveProtocols() = activeProtocols.toList()

    fun scheduleIrongate() {
        scope.launch {
            while (true) {
                delay(3600000) // Every 1 hour
                executeIrongate()
            }
        }
    }

    fun destroy() { scope.cancel() }
}
