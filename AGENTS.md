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

Package: `com.pixelpal.app` (namespace in `app/build.gradle.kts`).

Key source files (all Kotlin):

- `PixelPalApplication` — `@HiltAndroidApp` Application subclass; plants Timber.DebugTree in DEBUG
- `MainActivity` — `@AndroidEntryPoint ComponentActivity`; Compose UI with `PixelPalTheme(darkTheme = true)`
- `di/` — Hilt modules (`AppModule`, `DatabaseModule`, `ServiceModule`) scoped to `SingletonComponent`
- `data/local/db/PixelPalDatabase.kt` — Room database (entities list currently empty)
- `data/local/datastore/PreferencesManager.kt` — DataStore Preferences wrapper, injected as `@Singleton`
- `data/remote/` — empty; no Retrofit/API layer yet
- `domain/model/` — data models (`Bond`, `Companion`, `Emotion`, `Personality`, `PetType`, `Reminder`)
- `domain/usecase/` — use cases (`companion`, `personality`, `reminder` subdirs, mostly empty)
- `domain/engine/`, `domain/repository/` — present but empty
- `presentation/theme/` — `Color`, `Shape`, `Theme`, `Type` (Compose theme)
- `presentation/navigation/NavGraph.kt` — `PixelPalNavGraph` with `Screen` sealed class (Onboarding, Home, Reminders, Customize, Settings)
- `presentation/components/PetRenderer.kt` — Compose component rendering pet sprite via Coil
- `presentation/screens/` — `Home` (with `HomeViewModel`), `Onboarding`, `Customize`, `Reminders`, `Settings`
- `overlay/` — `OverlayService`, `OverlayManager`, `CompanionOverlayView`, `OverlayTouchHandler` (system overlay window)
- `receiver/` — `AlarmReceiver`, `BootReceiver` (Android broadcast receivers)
- `animation/` — `AnimationEngine`, `AnimationState`, `AnimationConfig`, `SpriteAnimator` (state machine + sprite management)
- `util/Constants.kt` — shared constants (DB name, preferences keys, notification channels, overlay defaults, permission actions)
- `util/PermissionHelper.kt` — overlay permission and exact alarm permission helpers

## Key Dependencies (versions.toml)

- AGP 8.5.0, Kotlin 2.0.0, Compose BOM 2024.09.00, Compose compiler 1.5.14
- Hilt 2.51.1, Room 2.6.1, DataStore 1.1.1, Coil 2.7.0, Timber 5.0.1
- Coroutines 1.8.1, WorkManager 2.9.1, Navigation Compose 2.8.2
- Kotlin Serialization 2.0.0, kotlinx-coroutines-play-services 1.8.1
- Testing: JUnit 4.13.2, androidx.test 1.1.5, espresso 3.5.1, compose-ui-test 1.3.0

## Manifest Permissions

`SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `VIBRATE`.

## Gotchas

- `RoomDatabase.entities` is empty — Room will not compile until entities are added.
- `ServiceModule` is a stub placeholder with no providers yet.
- Compose `darkTheme = true` is hardcoded in `MainActivity`; no dynamic theme switching yet.
- `data_extraction_rules.xml` is referenced in the Manifest — verify it exists at `app/src/main/res/xml/data_extraction_rules.xml` before building.
- `overlay/OverlayService` uses `TYPE_APPLICATION_OVERLAY` and `FLAG_NOT_TOUCH_MODAL` — requires `SYSTEM_ALERT_WINDOW` permission at runtime on API 23+.
- `AnimationState.getDrawableResId()` uses `context.resources.getIdentifier()` with naming pattern `pet_{type}_{state}` — drawable resources must follow this convention or return 0 silently.
- `BootReceiver` re-reads `preferencesManager.overlayEnabled` to decide whether to restart `OverlayService` after reboot.
- `HomeViewModel` toggles `OverlayService` start/stop via `OverlayService.start()`/`stop()` static methods.