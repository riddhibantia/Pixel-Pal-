# Android Build Skill

## Build Commands

| Command | Purpose |
|---------|---------|
| `./gradlew :app:assembleDebug` | Build debug APK |
| `./gradlew :app:installDebug` | Install on connected device |
| `./gradlew :app:testDebugUnitTest` | Run unit tests |
| `./gradlew :app:connectedDebugAndroidTest` | Run instrumentation tests |
| `./gradlew clean` | Remove build artifacts |

## Key Files

- `app/build.gradle.kts` — module build config, dependencies
- `build.gradle.kts` — project build config, buildscript deps
- `gradle/libs/versions.toml` — centralized dependency version catalog
- `gradle.properties` — Gradle JVM args and Android flags

## Gotchas

- `data_extraction_rules.xml` is missing from `res/xml/` — will break the build. Create it or remove the reference from `AndroidManifest.xml`.
- Room database has no entities — Room will not compile until entities are added.
- `OverlayService`, `AlarmReceiver`, `BootReceiver` are declared in the Manifest but have no source files.