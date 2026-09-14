# Panda replaces Rabbit — companion species redesign

Date: 2026-09-14. Approved approach A (clean replace). User approved panda look:
white body, black ears, black eye patches, pink cheeks.

## Problem

- `rabbit` and `bunny` Lottie sets are visually identical (white body
  `[1,1,1,1]`, long ears `[34,110]`, everything equal except the `nm` tag).
  They render as twins in the Customize species row.
- Tapping a species card glitches: `LottiePetView` restarts on
  `key(lottieRawRes)` (raw id only), so same-state retaps reuse the loaded
  composition without replaying, and every species switch flashes the
  vector-drawable fallback while the new composition loads.

## Roster change

- Delete: `PetType.RABBIT`, `"rabbit"` in `SpeciesStyle.SPECIES`, 12×
  `res/raw/pet_rabbit_*.json`, 3× `res/drawable/pet_rabbit_*.xml`
  (blink/happy/idle).
- Add: `PetType.PANDA("panda", "Panda", 30, "<description>", true)` in the
  same slot (unlock level 30 unchanged, progression order unchanged).
- Update `Companion.kt` KDoc species list (`rabbit` → `panda`).
- Migration: stored `"rabbit"` (companion row `species`/`petType`,
  DataStore `SELECTED_PET_TYPE`) maps to `"panda"` on load. No
  missing-sprite crash. `bunny` assets untouched.

## Panda look (same 16-layer rig, recolored fills only)

- Body fill white `[1,1,1,1]`; ears fill near-black `#2B2B33` (`[0.169,
  0.169, 0.2, 1]`), standard cat-ear rect (NOT the long `[34,110]`
  bunny ears — silhouette differs from bunny at a glance).
- Eye layers fill black (eye patches); cheeks/mouth keep existing pink.
- Motion programs identical to all species (generator recolors fills
  only): all 12 states animate the same, durations match
  `AnimationState.durationMs` (`op / 30fps * 1000`).
- Distinctness check: panda files must differ in hash from every other
  species' same-state file; ears rect must not equal `[34,110]`.

## Glitch fix (`LottiePetView.kt`)

- Restart key becomes `key(lottieRawRes, animationState)` so retapping the
  same species/state replays cleanly.
- Drawable fallback renders only while the composition is null on first
  load — never flashed over a loaded animation on species switch.

## Verification

- 12 `res/raw/pet_panda_*.json` parse, `nm == pet_panda_<state>`, 16
  layers each, one-shot durations match `AnimationState`.
- `:app:assembleDebug` + `:app:testDebugUnitTest` green.
- On device (ZA222Y47FN): Customize row shows Cat/Dog/Bunny/Whale/Llama/
  Fox/Axolotl/Panda (no Rabbit, no twins); tapping Panda renders the panda
  on Home with no flash; `logcat` shows no `FATAL EXCEPTION` from
  `com.pixelpal.app`.

## Out of scope

- No bunny reskin, no template geometry changes, no new animation states,
  no color/pattern body-recolor work (still future layer per
  `CompanionAppearance` docs).
