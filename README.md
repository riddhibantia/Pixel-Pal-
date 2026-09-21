# 🐾 PixelPal — Your AI Companion for Daily Life

[![CI](https://github.com/riddhibantia/Pixel-Pal-/actions/workflows/ci.yml/badge.svg)](https://github.com/riddhibantia/Pixel-Pal-/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7f52ff?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?logo=android)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> One pixel pet that lives with you — your tasks keep it alive, your AI talks through it.

## Overview

PixelPal is an Android app built around **one companion, forever**. Tasks, reminders, streaks, personality, overlay, widgets, and AI are not separate screens — they are the same pet's life. It solves the "todo app you abandon in a week" problem: every meaningful action grows the companion, and that living state is the context your connected AI agent sees and reacts to.

## Demo

[Add screenshot here — Home screen with companion hero]

[Add GIF here — 30s flow: New Task → photo proof → agent Approve/Deny (optional)]

> Gallery temporarily removed — recapturing from a real device into `docs/screenshots/`.

## Features

- 🤖 **Pluggable AI agent** — Generic HTTP, WebSocket live stream, or Gemini; `WORKING / IDLE / ERROR / OFFLINE` status UI
- 🔔 **Approval gate** — agent requests arrive as Approve/Deny notifications, deduplicated per `approvalId`
- 📷 **QR pairing** — CameraX + ML Kit scanner connects a LAN-hosted agent in seconds
- ✅ **Tasks that matter** — tasks + subtasks + progress + swipe-to-delete, with **photo proof** on detail
- ⏰ **Reminders that fire** — exact alarms, boot restore, snooze; completion keeps the streak
- 💗 **Progress without gates** — all 7 species unlocked day one; streaks (3/7/14/30/60/100) + interactions kept
- 🪟 **Overlay & widgets** — floating one-session overlay, Home + Tasks widgets on the same Room DB
- ☁️ **Offline-first** — Room v13 is truth; Firestore syncs with stable `cloudId` + WorkManager retry

## Tech Stack

- **Language / Build:** Kotlin 2.0, AGP 8.5, KSP, Java 17, Gradle version catalog
- **UI:** Jetpack Compose (BOM), Material 3, Navigation Compose, Lottie Compose, Coil
- **Architecture:** Hilt DI, Coroutines + Flow, WorkManager, AlarmManager, DataStore
- **Data:** Room v13 (8 entities, 13 migrations), Firebase Auth (anonymous + email), Cloud Firestore (offline cache)
- **AI / Agent:** Gemini streaming SDK, OkHttp HTTP + WebSocket, QR (CameraX + ML Kit)
- **Desktop:** Python + PyWebView mini widget (`agent-endpoint/`)
- **Quality:** JUnit + Espresso + Room testing, GitHub Actions (build / instrumented api-35 / rules)

## Architecture / Folder Structure

Single-companion invariant: `ActiveCompanionManager.activeCompanion == getPrimary()`. Room is the source of truth; Firestore syncs async with last-write-wins on `updatedAt`.

```text
Pixel-Pal/
├── app/                          # :app module (minSdk 26, targetSdk 35)
│   └── src/main/java/com/pixelpal/app/
│       ├── data/local/db/        # Room v13, entities, DAOs, migrations 1→13
│       ├── data/local/datastore/ # PreferencesManager, bootstrap fold
│       ├── data/remote/          # HTTP / WebSocket / Gemini connectors
│       ├── data/remote/firebase/ # Auth, FirestoreSyncEngine, push workers
│       ├── domain/engine/        # ActiveCompanionManager, BondEngine
│       ├── presentation/         # Compose screens, components, navigation
│       ├── overlay/ / worker/    # floating pet, WorkManager jobs
│       ├── widget/ / receiver/   # home+tasks widgets, alarms, approvals
│       └── di/                   # Hilt modules
├── agent-endpoint/               # server.py, mini_pet.py, status.json
├── docs/                         # specs, plans (screenshots return here)
├── .github/workflows/ci.yml
├── firestore.rules
└── README.md / AGENTS.md / LICENSE
```

## Getting Started

### Prerequisites

- JDK 17, Android SDK 35 (via Android Studio), Git
- A Firebase project (for sync) and optionally a Gemini API key

### Installation

```bash
# 1. Clone
git clone https://github.com/riddhibantia/Pixel-Pal-.git
cd Pixel-Pal-

# 2. Firebase config (package: com.pixelpal.app)
#    Download google-services.json from Firebase console → place at:
#    app/google-services.json

# 3. Gemini key (optional — app runs without it)
echo "GEMINI_API_KEY=your_key_here" > local.properties

# 4. Build + install (device or emulator attached)
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

### Environment variables / config

| Key | Where | Required |
|---|---|---|
| `google-services.json` | `app/` | Yes for Firebase sync |
| `GEMINI_API_KEY` | `local.properties` (or env/gradle prop) | No — falls back to `""`, CI uses `dummy` |
| `RELEASE_STORE_FILE` etc. | gradle props | Only for signed release builds |

## Usage

```bash
# Run unit tests (34)
./gradlew :app:testDebugUnitTest

# Run instrumented tests (needs emulator/device)
./gradlew :app:connectedDebugAndroidTest

# Lint
./gradlew :app:lintDebug

# Point the app at your agent: Agent tab → paste endpoint, e.g.
http://192.168.1.10:8765        # generic HTTP status envelope
ws://192.168.1.10:8765          # live WebSocket stream
# ...or Scan QR from the laptop dashboard
```

Agent envelope contract:

```json
{
  "status": "WORKING",
  "currentTask": "Run the test suite",
  "progress": 72,
  "message": "Almost done",
  "pendingApproval": { "id": "42", "action": "Deploy?", "detail": "..." }
}
```

Desktop companion widget:

```bash
python agent-endpoint/server.py 8765
pythonw agent-endpoint/mini_pet.py cat
```

## My Role / Key Contributions

Built end-to-end as a solo developer. Highlights:

- **Single-companion architecture** — enforced one-pet invariant across DB (`CompanionBootstrapInitializer` fold), domain (`ActiveCompanionManager`), and navigation (single `workspace` route); eliminated multi-companion identity leaks.
- **Offline-first sync** — Room-as-truth with stable `cloudId` UUIDs, last-write-wins, plus a `FirestorePushWorker` retry queue (`NetworkType.CONNECTED` + exponential backoff) replacing fire-and-forget pushes.
- **Fixed silent task loss** — `TasksViewModel.createTask` dropped inserts when the companion `StateFlow` hadn't emitted; added direct-DB fallback so creation never fails.
- **AI agent layer** — generic HTTP poller with private-LAN allowlist, WebSocket live-typing banner in Activity Center, QR pairing (CameraX + ML Kit), and notification Approve/Deny with per-`approvalId` dedup.
- **Lottie as source of truth** — launcher icon vector sampled pixel-for-pixel from `pet_cat_idle.json`; all 7 species ship full animation sets with drawable fallback.
- **CI ownership** — stabilized the emulator job (api-35 `pixel_7_pro` + KVM + boot diagnostics) and lint gates; `build ✅ / instrumented ✅ / rules ✅`.

## Roadmap

- [ ] Recaptured screenshot gallery + 30s demo video in this README
- [ ] Photo-proof cloud sync (Firebase Storage) — currently local-first
- [ ] Signed release + Play internal track (`RELEASE_STORE_FILE` wiring exists)
- [ ] WebSocket auto-reconnect with backoff for flaky networks
- [ ] Baseline profiles + Compose screenshot tests for all 7 species

## Contributing

This is a personal portfolio project — issues and suggestions are welcome via GitHub Issues. PRs: please keep the single-companion invariant (see `AGENTS.md`).

## License

MIT — see [LICENSE](LICENSE). Copyright © 2026 Riddhi Bantia.

## Contact

- **Name:** Riddhi Bantia
- **Email:** [your email here]
- **LinkedIn:** [your LinkedIn URL here]
- **Portfolio:** [your portfolio URL here]
- **GitHub:** [github.com/riddhibantia](https://github.com/riddhibantia)
