# TITAN-OMEGA

### Autonomous Tri-Agent AI System for Android

> **Three Agents. One Intelligence.**

TITAN-OMEGA is a modular, on-device AI system composed of independent agents that collaborate through a local communication bridge. It is designed for **offline-first intelligence**, **low-latency execution**, and **secure inter-process communication**.

The system runs entirely on a single Android device using **localhost TCP sockets**, ensuring zero external exposure while maintaining high performance.

---

## 🧠 Architecture

TITAN-OMEGA consists of three core agents:

* **GHOST** → Local execution engine (offline AI + task handling)
* **GIPSY** → Communication agent (online AI APIs)
* **IRONGATE** → Central bridge (message routing & orchestration)

### Connection Flow

GHOST ⇄ IRONGATE ⇄ GIPSY

* GHOST connects to IRONGATE via `localhost:8765`
* GIPSY connects via `localhost:8766`
* IRONGATE manages routing, buffering, and health monitoring

---

## ⚙️ Features

* 🔹 Fully local inter-agent communication (TCP over localhost)
* 🔹 Hybrid AI system (offline + online)
* 🔹 Persistent foreground services
* 🔹 Modular agent-based architecture
* 🔹 Secure storage using EncryptedSharedPreferences
* 🔹 Real-time message routing via JSON protocol

---

## 🧰 Tech Stack

* **Language:** Kotlin
* **Platform:** Android (API 26–34)
* **Local AI:** Vosk (speech recognition), Gemma 1B (quantized)
* **Online AI:** Gemini API / Groq API
* **Communication:** TCP sockets (localhost only)
* **Storage:** SQLite + EncryptedSharedPreferences

---

## 🚀 Installation

> ⚠️ This project uses sideloaded APKs and requires manual permission setup.

1. Build all three modules:

   * `ghost/`
   * `gipsy/`
   * `irongate/`

2. Install APKs on the same Android device

3. Grant required permissions:

   * Microphone
   * Accessibility
   * Overlay
   * Notifications
   * (and others depending on module)

4. Start services:

   * Launch IRONGATE first
   * Then GHOST and GIPSY

---

## 🔄 How It Works

1. GHOST processes user input (voice / command)
2. Sends request → IRONGATE (JSON over TCP)
3. IRONGATE routes request:

   * Local execution OR
   * Forward to GIPSY (if online required)
4. GIPSY processes via API and returns response
5. IRONGATE relays back to GHOST

---

## 🧭 Roadmap

* [ ] Improve reconnection & fault tolerance
* [ ] Add multi-agent scaling support
* [ ] Optimize local model performance
* [ ] UI dashboard for monitoring agents
* [ ] Plugin system for custom tasks

---

## ⚠️ Disclaimer

This project requires high-level Android permissions and is intended for **development and research purposes only**.

---

## 📌 Status

Active development 🚧
