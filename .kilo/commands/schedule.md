# Agent Schedule

Run commands in this order:

1. `./gradlew clean` — clear stale artifacts
2. `./gradlew :app:assembleDebug` — build the project
3. `./gradlew :app:testDebugUnitTest` — run unit tests
4. `./gradlew :app:installDebug` — install on device (optional)

Do not run instrumentation tests without a connected device or emulator.