# GIPSY

> *The voice of the operation.*

GIPSY is a Kotlin Android AI assistant — the conversational and cognitive layer of a two-part system paired with GHOST. It communicates like TARS from Interstellar. Dry, deadpan, zero filler. Calls you Cooper. Always.

---

## SYSTEM ARCHITECTURE

```
GIPSY (this app)         BRIDGE                   GHOST
    Client           ←→  Server  ←→               Client
localhost:8766           :8765/:8766               localhost:8765

All three on same device. Local TCP. No internet required for connection.
```

---

## INSTALL ORDER

```
1. Install Bridge.apk  → launch → confirm running
2. Install GHOST.apk   → launch → connects to Bridge
3. Install GIPSY.apk   → launch → connects to Bridge
4. Say "Commence IRONGATE" → confirm ALL CLEAR
5. Ecosystem live.
```

---

## SETUP IN ANDROID STUDIO

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK API 34
- Device: Redmi Note 8 Pro (ARM64-v8a)

### Steps

1. **Clone / open project**
   ```
   Open Android Studio → Open → select /GIPSY folder
   ```

2. **Add font file**
   - Download `Press Start 2P` font from Google Fonts
   - Place `press_start_2p.ttf` in `app/src/main/res/font/`
   - Rename to `press_start_2p.ttf`

3. **Add logo**
   - Take the GIPSY logo image
   - Convert to PNG, name it `ic_gipsy_logo.png`
   - Place in `app/src/main/res/drawable/`

4. **Add nuke audio**
   - Take `destroyer-of-world.mp3`
   - Place in `app/src/main/res/raw/`
   - Rename to `nuke_audio.mp3`

5. **Sync Gradle**
   ```
   File → Sync Project with Gradle Files
   ```

6. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

7. **Sideload to device**
   ```
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## FIRST LAUNCH

1. Grant all permissions when prompted:
   - Microphone
   - Overlay (for floating logo)
   - Notifications
   - Accessibility (for some protocol support)

2. Open Settings → API KEYS → enter at least one:
   - Gemini API key (from Google AI Studio)
   - Groq API key (from console.groq.com)
   - OpenRouter API key (from openrouter.ai)

3. Say **"GIPSY"** — logo appears top left.

4. Say your command.

---

## FEATURES

- **Dual/Triple API** — Gemini, Groq, OpenRouter. Hot-swappable. Auto-fallback.
- **TARS personality** — Calls you Cooper. Dry. Deadpan. Loyal.
- **31 Protocols** — Verbal acknowledgment for all 31. Delegates execution to GHOST.
- **10 Modes** — Changes how GIPSY communicates.
- **Long-term memory** — Room database, persistent across sessions.
- **Delete Memory** — Single confirmation. Wipes conversation history.
- **Factory Reset** — 3-layer: Warning speech → Target selection (GHOST/GIPSY/BOTH) → Confirm button → WARHAWK code → Nuke audio → Wipe.
- **IRONGATE** — Bridge health check every 1 hour. Manual trigger available. Returns: All clear / Something's down / Black out.
- **Floating logo** — Appears top-left when wake word detected. Pulses while listening. Fades after 5 seconds of silence.
- **TTS toggle** — VOX on/off in top bar.
- **CALLSIGN module** — Rename GIPSY, GHOST, IRONGATE and change wake words from Bridge app settings.
- **Bridge connection** — TCP localhost:8766. Auto-reconnect. Heartbeat every 10s. Timeout at 30s.

---

## NUCLEAR CODE

Default: `WARHAWK`

Change in Settings → Security → Change Nuclear Code.

Keep it to yourself. Obviously.

---

## PROJECT STRUCTURE

```
GIPSY/
├── app/src/main/java/com/gipsy/
│   ├── GipsyApp.kt              # Application, MainActivity, BootReceiver, DI
│   ├── ai/
│   │   └── AIClients.kt         # Gemini, Groq, OpenRouter, AIRouter
│   ├── bridge/
│   │   └── BridgeManager.kt     # TCP client, heartbeat, message routing
│   ├── data/
│   │   ├── local/
│   │   │   ├── Database.kt      # Room DB, DAOs
│   │   │   └── PreferencesManager.kt  # EncryptedSharedPreferences
│   │   └── models/
│   │       └── Models.kt        # All data models and enums
│   ├── service/
│   │   ├── GipsyService.kt      # Foreground service, TTS, wake word, IRONGATE scheduler
│   │   └── FloatingLogoService.kt  # Overlay logo
│   └── ui/
│       ├── GipsyViewModel.kt    # Main state management
│       ├── theme/
│       │   └── Theme.kt         # Black/white, Minecraft font, typography
│       └── screens/
│           ├── ChatScreen.kt    # Main chat UI
│           └── SettingsScreen.kt  # All settings including CALLSIGN
└── app/src/main/res/
    ├── drawable/                # ic_gipsy_logo.png
    ├── font/                    # press_start_2p.ttf
    ├── raw/                     # nuke_audio.mp3
    └── values/                  # styles.xml
```

---

## GITHUB

Safe to push. Just ensure these are in `.gitignore`:

```
# API keys — NEVER commit these
local.properties
*.keystore
app/src/main/assets/config.json
```

Keys are stored in `EncryptedSharedPreferences` on device only. Nothing in the repo.

---

*GIPSY is the voice. GHOST is the hand. Cooper commands.*
