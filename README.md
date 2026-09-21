# 🐾 PixelPal

**A pixel-art screen companion that watches your AI coding agents for you.**

PixelPal is an Android app that lives as a small floating pet on top of your phone screen. Underneath the cute pixel-art exterior, its real job is to be a **live window into your AI agents** — Gemini, custom HTTP-based agents, or anything that streams status over a WebSocket — so you can glance at your pet and instantly know what your agent is doing, without opening a terminal, a laptop, or a dashboard.

Tasks, reminders, a bond/personality system, and character customization round out the companion experience, but the agent-monitoring layer is the core idea: **turn "is my AI agent still working?" into a one-glance question.**

---

## ✨ Why PixelPal

Anyone running long AI coding sessions (Claude Code, OpenCode, Gemini CLI, custom agent runners, CI-style agent jobs, etc.) knows the pattern: kick off a task, then repeatedly tab back in just to check "is it done yet?" PixelPal moves that check off your laptop and onto your phone — your companion reacts, updates, and notifies you as the agent's state changes, so checking in feels like glancing at a pet instead of babysitting a process.

---

## 🧠 Core Feature: Agentic AI Connection

This is what PixelPal is actually built around:

- **Multiple connector types** so PixelPal can talk to almost any agent setup:
  - `GeminiAgentConnector` — direct integration with Google's Generative AI SDK, with token-level streaming (`Flow<String>`) and your companion's personality injected into the prompt.
  - `WebSocketAgentConnector` — a persistent WebSocket listener for agents that push live events in real time.
  - `GenericHttpAgentConnector` — polls any agent endpoint that returns a simple JSON contract (`status`, `currentTask`, `progress`, `message`), making it possible to hook up custom agents, self-hosted tools, or coding-agent CLIs that expose a status endpoint.
- **Background polling** via `AgentStatusWorker`, so your companion's status stays current even when the app isn't open — network issues surface as OFFLINE, non-2xx responses surface as ERROR.
- **Companion reacts to agent state**: `CompanionReactionProvider` weaves live agent state into what your pet actually says ("still working on it," task progress, completion), instead of just showing a raw status field.
- **Persisted connection config + status** in a single `AgentConnectionEntity`, so your agent link survives restarts.
- **QR pairing** — point the in-app scanner (CameraX + ML Kit) at a QR code holding the endpoint URL; private-LAN `http://` endpoints are allowed so a laptop agent on the same Wi-Fi just works.
- **Approval gate** — when the agent sends `pendingApproval`, you get an Approve/Deny notification (`AgentApprovalReceiver`); each approval notifies once, deduplicated by id.
- **Notification Center** surfaces meaningful agent activity (not just taps/feeds) so you get pinged when something actually changes.

In short: connect an agent once, and your on-screen pet becomes a live status readout — expressive, glanceable, and always running in the background.

---

## 🐱 The Companion Experience

- **Floating overlay pet** that sits on top of any app, draggable, with idle/blink/sleep animation states rendered via Lottie.
- **One active companion** by design — tasks, reminders, agent connection, personality, and bond are all *features of your one pet*, not separate profiles.
- **Tasks & Reminders** — create reminders and tasks; completing them feeds directly into your companion's reactions and bond growth rather than just sitting in a plain list. Tasks support subtasks, progress, swipe-to-delete, and optional **photo proof** on the detail screen.
- **Bond & Personality system** — a friendship level that grows through real interaction (taps, completed tasks, completed reminders — capped per day to avoid grinding), streaks with milestone celebrations, and a personality that gradually adapts to how you use the app.
- **Character customization** — appearance picker across species, color, and pattern combinations.
- **Cloud sync** — optional Firebase Auth (including guest/anonymous mode) with two-way Firestore sync, so your companion, tasks, and agent connection aren't stuck on one device.
- **Offline-first** — Room (v13, 13 migrations) is the source of truth; writes sync via a WorkManager retry queue, so the app works with no network.
- **Home-screen widgets + desktop mini widget** — Tasks and Home widgets read the same database, and a tiny floating desktop window (`agent-endpoint/mini_pet.py`) mirrors agent status on your laptop.

---

## 🛠️ Tech Stack

- **Language:** Kotlin 2.0, Jetpack Compose, Material 3
- **Architecture:** Clean architecture (presentation → domain → data), single source of truth via `ActiveCompanionManager`
- **DI:** Hilt
- **Local persistence:** Room (with versioned migrations) + DataStore for preferences
- **Background work:** WorkManager (`AgentStatusWorker`, `PersonalityWorker`)
- **Networking / streaming:** OkHttp WebSocket, Google Generative AI SDK (Gemini)
- **Cloud:** Firebase Auth + Cloud Firestore
- **Animation:** Lottie Compose with vector-drawable fallback
- **Build:** Gradle (Kotlin DSL), AGP 8.5.0, KSP for annotation processing

---

## 🏗️ Architecture Overview

```
presentation/   → Compose UI: home, companion workspace, activity/notifications,
                  customization, auth screens
domain/         → engines & use cases: ActiveCompanionManager, BondEngine,
                  CompanionReactionProvider, agent connection repository contracts
data/           → Room DB (companion, bond, personality, tasks, reminders,
                  agent connection, activity events), DataStore, Firebase sync,
                  agent connectors (Gemini / WebSocket / Generic HTTP)
overlay/        → floating pet window management (WindowManager overlay,
                  single active overlay session)
worker/         → periodic background jobs (agent polling, personality recalculation)
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest stable)
- JDK 17
- An Android device/emulator — `minSdk 26`, `targetSdk 35`, package `com.pixelpal.app`

### Setup
```bash
git clone https://github.com/riddhibantia/Pixel-Pal-.git
cd Pixel-Pal-
```

Add your Gemini API key to `local.properties`:
```properties
GEMINI_API_KEY=your_api_key_here
```

Build and run:
```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

### Grant the overlay permission
On first launch, PixelPal will ask for the "Display over other apps" permission — this is required for the floating companion to appear.

---

## 📱 Usage

1. **Onboard** your companion — choose a species and name it.
2. **Enable the overlay** so your pet appears on top of other apps.
3. **Connect an agent** from the AI Agent Connection screen — pick Gemini, a WebSocket endpoint, or a generic HTTP status endpoint.
4. Go about your day — your companion will animate, speak, and notify you as your agent's status changes.
5. Add **tasks and reminders**; completing them grows your bond level and unlocks personality shifts and cosmetic milestones.

---

## 🗺️ Roadmap

- [ ] Additional first-class agent integrations (beyond Gemini/generic HTTP/WebSocket)
- [ ] Richer agent-progress visualizations in the overlay itself
- [ ] More species with full animated sprite sets (currently full coverage for cat; others fall back to idle)
- [ ] Wear OS companion view for agent status at a glance
- [ ] Expanded dialogue and reaction library

---

## 🤝 Contributing

This is currently a solo/early-stage project. Issues and suggestions are welcome — feel free to open an issue to discuss before submitting a PR.

---

## 📄 License

MIT — see [LICENSE](LICENSE). Copyright © 2026 Riddhi Bantia.

---

## 📬 Contact

**Riddhi Bantia**
[Add: email / LinkedIn / portfolio link]
