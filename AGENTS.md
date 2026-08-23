# AGENTS.md

## Project

PixelPal — an Android companion app built with Kotlin + Jetpack Compose.
Single module `:app` under `app/`. Root project name is `PixelPal`.

## Build & Run

- **Build**: `./gradlew :app:assembleDebug` (or `gradlew.bat` on Windows)
- **Install/device**: `./gradlew :app:installDebug`
- **Run unit tests**: `./gradlew :app:testDebugUnitTest`
- **Run instrumentation tests**: `./gradlew :app:connectedDebugAndroidTest`
- **Clean**: `./gradlew clean`

No lint, typecheck, or formatter tasks are configured beyond the standard Gradle build.

## Architecture

Package: `com.pixelpal.app` (namespace in `app/build.gradle.kts`). Multi-companion
architecture: users create up to `Constants.MAX_ACTIVE_COMPANIONS` (3) companions,
each with a role, bond, personality, reminders, tasks, and optional AI-agent hookup.

Key source files (all Kotlin):

- `PixelPalApplication` — `@HiltAndroidApp`; plants Timber in DEBUG; calls
  `CompanionBootstrapInitializer.ensureInitialized()` at startup
- `MainActivity` — `@AndroidEntryPoint ComponentActivity`; Compose UI with `PixelPalTheme(darkTheme = true)`
- `di/` — Hilt modules: `AppModule` (DataStore), `DatabaseModule` (Room + DAOs),
  `RepositoryModule` (@Binds for all repositories + `AgentConnector`),
  `ServiceModule` (stub, no providers yet)
- `data/local/db/PixelPalDatabase.kt` — Room v5 with entities:
  CompanionEntity, BondEntity, PersonalityEntity, ReminderEntity, TaskEntity,
  AgentConfigEntity, AgentStatusEntity, ActivityEventEntity
- `data/local/db/DatabaseMigrations.kt` — MIGRATION_1_3/2_3/3_4/4_5;
  MIGRATION_4_5 builds the multi-companion schema from the legacy single-pet tables
- `data/local/datastore/PreferencesManager.kt` — DataStore wrapper (`@Singleton`);
  also holds `active_companion_id` + bootstrap-done flags
- `data/local/datastore/CompanionBootstrapInitializer.kt` — idempotent post-migration
  bootstrap: creates default companion from legacy DataStore identity, adopts legacy
  name/type into the migrated placeholder row, ensures bond/personality rows exist,
  selects a valid active companion
- `data/repository/` — Room-backed implementations of all domain repository interfaces
- `data/remote/AgentConnector.kt` + `GenericHttpAgentConnector.kt` — polls a
  user-configured JSON status endpoint (`{"status": "...", "message": "..."}`),
  maps failures to AgentState.OFFLINE/FAILED; OkHttp + kotlinx.serialization
- `domain/model/` — `Companion` (id, name, petType, role, isFavorite/isArchived,
  hatId/outfitId/accessoryId), `CompanionRole` (GENERAL/REMINDER/TASK/AI_AGENT/CUSTOM),
  `Bond`, `Personality`, `Reminder` (nullable `companionId`), `Task`,
  `AgentConfig`/`AgentStatus`/`AgentState`, `ActivityEvent`/`ActivityType`,
  `PetType`, `Emotion`
- `domain/engine/` — `ActiveCompanionManager` (source of truth for active companion,
  exposes `activeCompanionId`/`activeCompanion` flows), `BondEngine` (per-companion
  bond mutations; `bond` flow follows the active companion only),
  `PersonalityEngine` (per-companion traits), `EmotionEngine` (global emotion state),
  `CompanionEngine` (tap/feed orchestration for the active companion),
  `AgentMonitorEngine` (poll → persist → record → notify)
- `domain/usecase/` — `companion/` (Create/Get/SetActive/Archive/Restore/ToggleFavorite/
  Update/Tap/Feed...), `reminder/`, `task/`, `agent/`, `activity/`, `personality/`
- `domain/repository/` — interfaces incl. `CompanionRepository`
  (`create`/`restore` return `CompanionActionResult`, enforcing MAX_ACTIVE_COMPANIONS)
- `worker/` — `ReminderScheduler`, `WorkerScheduler` (per-companion agent polling via
  WorkManager unique names `AGENT_WORK_PREFIX + companionId`), `AgentStatusWorker`,
  `PersonalityWorker`; Hilt worker factory wired in `PixelPalApplication`
- `util/AgentNotificationHelper.kt` — agent attention notifications on CHANNEL_AGENT
- `presentation/navigation/NavGraph.kt` — `Screen` sealed class; routes include
  `Companions`, `CreateCompanion`, and parameterized `workspace/{companionId}`
  (use `Screen.companionWorkspace(id)` helper)
- `presentation/screens/home/` — dashboard: companion switcher row, hero card with
  role-specific actions, Today stats, activity feed, overlay toggle
- `presentation/screens/companions/` — `CompanionsScreen` (+VM) manager list,
  `CreateCompanionScreen` (+VM) Role→Details→Review→Done wizard,
  `CompanionWorkspaceScreen` (+VM) per-companion detail: bond, personality,
  reminders, tasks, agent config/status, activity
- `presentation/screens/reminders/` — CreateReminderScreen has a companion picker
  ("Who" chips); ReminderViewModel exposes `companions`/`activeCompanionId`
- `presentation/components/PetRenderer.kt` — renders pet sprite via Coil
- `overlay/` — `OverlayService` (foreground session-sync host), `OverlayManager`
  (registry of `OverlaySession`s), `OverlaySession` (per-companion view,
  position, bubble, own `SessionSpriteRenderer`), `OverlayTouchHandler`;
  supports up to `Constants.MAX_SIMULTANEOUS_OVERLAYS` (2) simultaneous overlays,
  one per companionId, positions persisted per companion
- `receiver/` — `AlarmReceiver` (fires reminders), `BootReceiver` (restarts overlay)
- `animation/` — `AnimationEngine`, `AnimationState`, `AnimationConfig`, `SpriteAnimator`
- `util/Constants.kt` — DB name/version, preference keys, notification channels,
  `MAX_ACTIVE_COMPANIONS = 3`, `DEFAULT_AGENT_POLL_INTERVAL_MIN`, overlay defaults

## Key Dependencies (versions.toml)

- AGP 8.5.0, Kotlin 2.0.0, Compose BOM 2024.09.00
- Hilt 2.51.1, Room 2.6.1, DataStore 1.1.1, Coil 2.7.0, Timber 5.0.1
- Coroutines 1.8.1, WorkManager 2.9.1, Navigation Compose 2.8.2
- Kotlin Serialization 1.7.1, OkHttp 4.12.0 (agent connector)
- Testing: JUnit 4.13.2, androidx.test 1.1.5, espresso 3.5.1, compose-ui-test 1.3.0

## Manifest Permissions

`SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `VIBRATE`, `INTERNET`.

## Gotchas

- **Smart casts on `by viewModel.uiState.collectAsState()` fail** — delegated
  properties can't be smart-cast; copy to a plain local `val state = uiState` before
  null-checking nested nullable fields.
- **A `companion object {}` inside a class with a `companion` property shadows it** —
  `card.companion` then resolves to the Kotlin companion object, not the field
  (caused silent type chaos in `HomeScreen`); avoid the block or rename the field.
- **`combine` supports max 5 typed flows** — for more, nest combines into intermediate
  data classes (see `CompanionWorkspaceViewModel` Core/Extras pattern); dynamic lists
  of flows use `flatMapLatest { combine(list.map{...}) }` (see `HomeViewModel`).
- **MIGRATION_4_5 ordering matters**: companions must exist before bond/personality
  re-keying (their INSERTs select `(SELECT id FROM companions ...)`); NOT NULL columns
  need explicit values in every hand-written INSERT (SQLite has no DEFAULT on new
  columns unless declared). The migration runs inside a transaction, so failure rolls back.
- `BondEngine.bond` follows the ACTIVE companion only; use
  `bondRepository.getBond(companionId)` for per-companion reads elsewhere.
- `EmotionEngine` is global/shared, not per-companion.
- `ServiceModule` is still an empty stub.
- Compose `darkTheme = true` is hardcoded in `MainActivity`.
- `data_extraction_rules.xml` must exist at `app/src/main/res/xml/` (referenced by Manifest).
- `AnimationState.getDrawableResId()` uses `getIdentifier()` with pattern
  `pet_{type}_{state}` — missing drawables return 0 silently.
- `BootReceiver` re-reads `preferencesManager.overlayEnabled` to decide whether to
  restart `OverlayService` after reboot.
- `HomeViewModel` toggles the overlay via `OverlayService.start()/stop()`.
- kapt warns "doesn't support language version 2.0+" — expected, falls back to 1.9.
