# Architecture Skill

## Package Structure

```
com.pixelpal.app
├── di/              — Hilt modules (AppModule, DatabaseModule, ServiceModule)
├── data/
│   ├── local/db/    — Room database (PixelPalDatabase, entities currently empty)
│   ├── repository/  — repositories (mostly empty)
│   ├── datastore/   — preference data store
│   └── dialogue/    — dialogue data (mostly empty)
├── domain/
│   ├── model/       — data models (Bond, Companion, Emotion, Personality, PetType, Reminder)
│   ├── usecase/     — use cases (companion, personality, reminder subdirs)
│   ├── engine/      — business logic engine (empty)
│   └── repository/  — repository interfaces (empty)
├── presentation/
│   ├── theme/       — Color, Shape, Theme, Type
│   ├── screens/     — companion, customize, home, onboarding, reminders, settings
│   ├── navigation/  — nav graph (empty)
│   └── components/  — shared composables (empty)
├── overlay/         — OverlayService (declared in Manifest, no source)
├── receiver/        — AlarmReceiver, BootReceiver (declared in Manifest, no source)
├── worker/          — Worker classes (empty)
└── util/            — Constants.kt
```

## DI

Hilt with `@HiltAndroidApp` on `PixelPalApplication` and `@AndroidEntryPoint` on `MainActivity`. All Hilt modules are scoped to `SingletonComponent`.

## State Management

Jetpack Compose with Material3. `PixelPalTheme` hardcodes `darkTheme = true`. DataStore Preferences for simple state.