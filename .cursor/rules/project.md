# Cursor Rules for PixelPal

## Project
Android companion app (Kotlin + Jetpack Compose). Single module `:app`.

## Build
```bash
./gradlew :app:assembleDebug
```

## Key Constraints
- Hilt for DI — always use `@HiltAndroidApp` and `@AndroidEntryPoint`
- Room DB — entities must be added before Room will compile
- Compose BOM `2024.09.00` — use `platform()` for BOM dependencies
- `darkTheme = true` hardcoded in `MainActivity`
- `data_extraction_rules.xml` is missing from `res/xml/` — will break build