# AGENTS.md

## Project

Pixel Pet (PixelPal) — an Android companion app built with Kotlin + Jetpack Compose.
Single module `:app` under `app/`. Root project name is `PixelPal`.

## Architecture: ONE COMPANION (v2)

The app is built around **a single active companion** (DB enforces/expect one row).
Tasks, reminders, AI-agent connection, personality, bond, streaks, activity,
notifications and the overlay are **features of that companion** — never separate
companions. Do not reintroduce multi-companion UI or identity leaks.

## Build & Run

- **Build**: `./gradlew :app:assembleDebug` (or `gradlew.bat` on Windows)
- **Install/device**: `./gradlew :app:installDebug`
- **Run unit tests**: `./gradlew :app:testDebugUnitTest`
- **Clean**: `./gradlew clean`

No lint/typecheck/formatter beyond the standard Gradle build.

## Key source files

- `data/local/db/PixelPalDatabase.kt` — Room **v7**:
  CompanionEntity(+species/color/pattern), BondEntity, PersonalityEntity,
  ReminderEntity(companionId), TaskEntity(companionId),
  **AgentConnectionEntity** (merged config+status), ActivityEventEntity(isRead)
- `data/local/db/DatabaseMigrations.kt` — chain 1_3…6_7. `MIGRATION_6_7` adds
  appearance columns and merges agent_config+agent_status → agent_connection
- `data/local/datastore/CompanionBootstrapInitializer.kt` — startup
  reconciliation: one-time **SingleCompanionFold** (picks primary via stored
  active id → favorite → most-recent; moves pending tasks/reminders, activity,
  agent connection; deletes extras), then fresh-install seeding +
  bond/personality row guarantees. Both steps flag-guarded and idempotent.
- `data/local/datastore/PreferencesManager.kt` — DataStore wrapper; legacy
  `active_companion_id` still used as fold input; per-companion overlay
  position keys (`overlay_x_<id>`)
- `domain/engine/ActiveCompanionManager.kt` — THE companion authority
  (`activeCompanion` = primary row)
- `domain/engine/BondEngine.kt` — bond + streaks: taps grant level only first
  `BOND_GRANTING_TAPS_PER_DAY`(3)/day; feed/play are cosmetic; task +2 /
  reminder +3; level milestones every 5; streaks increment once/day with
  milestone events at 3/7/14/30/60/100
- `domain/engine/CompanionReactionProvider.kt` — contextual message layer;
  weaves in live state ("You still have N tasks left"); agent-state messages
- `domain/repository/AgentConnectionRepository.kt` (+Impl) — connection CRUD
  + `checkNow()` = poll → persist → record meaningful activity → notify
- `data/remote/GenericHttpAgentConnector.kt` — polls user endpoint JSON
  `{status, currentTask?, progress?, message?}`; network→OFFLINE, HTTP≠2xx→ERROR
- `worker/AgentStatusWorker`+`WorkerScheduler` — periodic polling per
  companionId (`AGENT_WORK_PREFIX`); `PersonalityWorker` recalcs primary daily
- `presentation/screens/home/` — greeting header + bell w/ unread badge,
  single hero card (Interact/Feed/Play + Open Workspace), compact stats,
  TODAY summary chips (Tasks/Reminders/Agent)
- `presentation/screens/companions/CompanionWorkspace*` — single route
  (`workspace`, no id): Profile★/Bond/Personality/Tasks/Reminders/AI Agent
  Connection/Activity + Customize link
- `presentation/screens/activity/` — Notification Center (bell target),
  filter chip, mark-read on open; TAP/FEED rows excluded by DAO query
- `presentation/screens/customize/` — transformation picker
  (Species×Color×Pattern via `SpeciesStyle`) — appearance-only
- `overlay/` — `OverlayManager` registry of `OverlaySession`s capped at
  `MAX_SIMULTANEOUS_OVERLAYS = 1`; service syncs sessions to the primary
- `di/RepositoryModule` — binds all repositories incl. `AgentConnector`

## Gotchas

- **A `companion object {}` inside a class with a `companion` property shadows
  it** — `state.companion` resolves to the Kotlin object, not the field.
  Twice-caused silent type chaos (`HomeUiState`, old card VM). Never declare a
  companion object in classes holding a `companion` field.
- **Smart casts on `by viewModel.uiState.collectAsState()` fail** — copy to a
  plain local `val state = uiState` before null-checking nested nullable fields.
- **`combine` supports max 5 typed flows** — nest combines into intermediate
  data classes; dynamic flow lists use `flatMapLatest { combine(list.map{...}) }`.
- **Migrations**: NOT NULL columns need explicit values/values-or-DEFAULT in
  hand-written SQL; migrations run in a transaction (failure rolls back). The
  v7 fold lives in Kotlin (needs DataStore), not migration SQL.
- `BondEngine.bond` follows the primary; use `bondRepository.getBond(id)` for
  direct reads elsewhere.
- Species sprites: full set only for cat; other species have idle vectors and
  fall back to IDLE automatically (`AnimationState.getDrawableResId`).
- kapt warns "doesn't support language version 2.0+" — expected.
