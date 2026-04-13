# IRONGATE — Bridge v1.0

> The neutral middleman. Silent by default. Deadly when opened.

IRONGATE is the communication bridge between **GHOST** and **GIPSY** — two Android AI assistants. It routes all JSON messages between them via local TCP sockets, runs health diagnostics every hour, and exposes a full command center UI when opened.

---

## Architecture

```
GHOST (Client)              IRONGATE (Server)             GIPSY (Client)
localhost:8765  ──────────► PORT 8765
                             PORT 8766 ◄────────────────── localhost:8766

All three run on the same physical device.
No internet required. JSON over TCP. Local only.
```

---

## Features

### Core Bridge
- TCP socket server on ports 8765 (GHOST) and 8766 (GIPSY)
- Routes all 5 JSON message types: SYNC, CONFIRM, DATA, COMMAND, STATUS
- Buffers up to 100 messages per queue if recipient is offline
- Delivers buffered messages in order on reconnect
- Heartbeat every 10 seconds, 30-second timeout before marking offline
- Auto-reconnect on disconnect

### IRONGATE Protocol
- Runs automatically every **60 minutes**
- Manual trigger: "Commence IRONGATE Protocol"
- Checks handshake with both GHOST and GIPSY
- Validates bridge health, measures latency, writes log
- Fires one silent notification:
  - `IRONGATE Protocol Complete — All clear.`
  - `IRONGATE Protocol Complete — Something's down.`
  - `IRONGATE Protocol Complete — Blackout.`

### UI (Command Center)
- Pure black and white aesthetic
- Minecraft-style pixel font
- TARS-inspired smooth mechanical animations
- App logo: Autobots insignia

### Screens
| Screen | Description |
|--------|-------------|
| Dashboard | Live connection cards, protocol card, bridge health |
| Interface | Chat with GHOST, GIPSY, or BOTH (text only, no voice) |
| Logs | Live message feed with type tags and routing info |
| Settings | OpenRouter API key + CALLSIGN module |
| Nuke | Multi-layer factory reset with WARHAWK nuclear code |

### CALLSIGN Module
Set custom display names and wake words for GHOST, GIPSY, and IRONGATE. Changes pushed to assistants in real-time via bridge.

### Nuke Sequence
1. Warning display
2. Target selection: GHOST / GIPSY / BOTH
3. On-screen confirm button
4. Enter nuclear code: `WARHAWK`
5. `destroyer-of-world.mp3` plays in full — cannot skip
6. *"Code accepted. Initiating wipe, Sir."*
7. Wipe executes
8. *"GHOST online. Ready for initialization, Sir."*

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Service | Android Foreground Service |
| Sockets | Java TCP ServerSocket |
| Storage | EncryptedSharedPreferences |
| Serialization | Gson |
| Architecture | MVVM |

---

## Build Info

```
Min SDK     : Android 8.0 (API 26)
Target SDK  : Android 14 (API 34)
Architecture: ARM64-v8a
Package     : com.irongate
```

---

## Install Order

```
1. Install IRONGATE.apk  → launch → confirm running
2. Install GHOST.apk     → launch → connects port 8765
3. Install GIPSY.apk     → launch → connects port 8766
4. "Commence IRONGATE Protocol" → confirm ALL CLEAR
5. Ecosystem live.
```

---

## Assets Required

Place the following in `app/src/main/assets/`:
- `destroyer-of-world.mp3` — plays during nuclear wipe sequence

---

## Part of the GHOST Ecosystem

```
GHOST     — Execution engine (com.ghost)
GIPSY     — Conversational AI (com.gipsy)
IRONGATE  — Bridge (com.irongate)
```

---

*Built by Hari. One shot. No mistakes.*
