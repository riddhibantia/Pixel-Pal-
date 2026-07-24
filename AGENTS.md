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

- `PixelPalApplication` — `@HiltAndroidApp` Application subclass; plants Timber.DebugTree in DEBUG
- `MainActivity` — `@AndroidEntryPoint ComponentActivity`; Compose UI with `PixelPalTheme(darkTheme = true)`
- `di/` — Hilt modules (`AppModule`, `DatabaseModule`, `ServiceModule`) scoped to `SingletonComponent`
- `data/local/db/` — Room database (`PixelPalDatabase`, entities list currently empty)
- `data/repository/`, `data/datastore/`, `data/dialogue/` — directories present but mostly empty
- `domain/model/` — data models (`Bond`, `Companion`, `Emotion`, `Personality`, `PetType`, `Reminder`)
- `domain/usecase/` — use cases (`companion`, `personality`, `reminder` subdirs)
- `domain/engine/`, `domain/repository/` — present but empty
- `presentation/` — `MainActivity`, `theme/` (Color, Shape, Theme, Type), `screens/` (companion, customize, home, onboarding, reminders, settings), `navigation/`, `components/`
- `overlay/`, `receiver/`, `worker/` — referenced in Manifest but Kotlin files not yet created
- `util/Constants.kt` — shared constants (DB name, preferences keys, notification channels, overlay defaults)

## Key Dependencies (versions.toml)

- AGP 8.5.0, Kotlin 2.0.0, Compose BOM 2024.09.00, Compose compiler 1.5.14
- Hilt 2.51.1, Room 2.6.1, DataStore 1.1.1, Coil 2.7.0, Timber 5.0.1
- Coroutines 1.8.1, WorkManager 2.9.1, Navigation Compose 2.8.2
- Testing: JUnit 4.13.2, androidx.test 1.1.5, espresso 3.5.1, compose-ui-test 1.3.0

## Manifest Permissions

`SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `VIBRATE`.

## Gotchas

- `data_extraction_rules.xml` is referenced in the Manifest (`android:dataExtractionRules`) but does not exist in `res/xml/` — this will cause a build error until the file is created.
- `overlay/OverlayService`, `receiver/AlarmReceiver`, `receiver/BootReceiver` are declared in the Manifest but have no Kotlin source files yet.
- `RoomDatabase.entities` is empty — Room will not compile until entities are added.
- `ServiceModule` is a stub placeholder.
- Compose `darkTheme = true` is hardcoded in `MainActivity`; no dynamic theme switching yet.