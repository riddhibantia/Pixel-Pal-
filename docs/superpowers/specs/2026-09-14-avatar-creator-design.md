# Avatar Creator (Duolingo-style, Lottie-native on the original cat) — Design

Date: 2026-09-14
Status: approved design (approach B), pending spec review
Base: the original square pixel cat (`pet_cat_idle.xml`, `pet_cat_*.json`) is the untouched fallback and art idiom reference.

## Goal

Replicate Duolingo Avatar Creator mechanics exactly, restyled to Pixel Pet:
big live preview on top, slot tabs below, instant update on every tap,
locked options gated by progression, billions-of-combos feel from stacked
slots. All accessory art ships as native Lottie shape layers so it rides
body squash/stretch perfectly (approach B).

## Slots and options (17 total)

- Eyes (4): classic / happy-starry / sleepy / wink. Redraw of the existing
  eye + highlight layer group only. Free (unlock level 0).
- Ears (3): pointy-cat / floppy / round. Redraw of the ear rects only.
  Free (unlock level 0).
- Headwear (4): none / bow / cap / crown. New layers parented to Root so
  they ride head motion. Unlock bond level 5 (none always free).
- Scarf (3): none / red / teal. New layers anchored at neck y~95.
  Unlock bond level 10 (none always free).
- Aura (3): none / gold-sparkle / pink-glow. The single non-Lottie piece:
  Compose Modifier ring behind the avatar (glows do not squash).
  Unlock bond level 15 (none always free).

Art rules: flat square-pixel idiom matching the cat (rect/ellipse shapes,
thick dark outlines, no gradients on features, no realistic fur, strict
left-right symmetry). Locked cards show "Lv X" and grey out, exactly like
Duo locked items. Bond level read from the existing Bond flow.

## Rendering architecture

- Generator script (sibling of `app/build_panda_face.py`, e.g.
  `app/build_avatar_layers.py`): input = base `pet_cat_<state>.json` +
  selected option ids; output = `pet_avatar_<hash>_<state>.json` for all
  12 states, cached in-app (files, not res/ — generated combos cannot ship
  as resources; resolved at runtime via file spec, drawable fallback
  untouched).
- Every accessory is native Lottie shape layers reusing the body's
  keyframe tracks, so jump/walk squash carries accessories perfectly.
- One-shot lengths MUST keep matching `AnimationState.durationMs`
  (`op / fr * 1000`); looping states stay seamless.
- Preview in Customize replays on every tap via the landed
  `key(lottieRawRes, animationState)` fix. Base cat files are never
  modified; null/unselected slots fall back to baked-in cat visuals.

## Data flow and persistence

- `Companion` row reuse: `hatId` / `outfitId` / `accessoryId` (exist today,
  unused) map to headwear / scarf / aura. Two new columns `eyeStyle` and
  `earStyle` via a Room migration (NOT NULL needs explicit DEFAULT in
  hand-written SQL; migrations run in a transaction).
- `CustomizeViewModel` gains `selectLayer(slot, optionId)` writing through
  `CompanionRepository.update`. Single-companion invariant untouched:
  appearance-only, never touches bond/tasks/reminders/agent data.
- Firestore sync: new appearance fields join the existing companion
  document merge (last-write-wins, same as species/color/pattern).

## UI (Duo-exact)

- Customize screen: big 170dp live preview on top, slot tab row
  (Eyes/Ears/Hat/Scarf/Aura) below, option cards per slot with selected /
  locked states. Tapping any option instantly re-renders the preview.
- Existing species × color × pattern rows stay as-is above or below the
  new slot section (layout order decided in plan).
- Across the app (`BaseCompanionAvatar` / home hero / overlay), the
  avatar resolves to the generated combo when options are set, else the
  plain cat.

## Error handling

- Missing/unknown option id → fall back to base cat layer (never crash,
  never blank avatar).
- Failed combo generation (bad hash, IO) → render base cat + log; retry
  on next selection change.
- Migration: unknown stored strings map to defaults (`classic`,
  `pointy-cat`, `none`); never a missing-sprite crash.

## Testing

- Unit: option registry (17 ids, slots, unlock levels), fallback on
  unknown id, migration defaults.
- Generator: 12 emitted files parse, `nm` correct, layer counts match
  base + added layers, one-shot durations match `AnimationState`.
- Build gates: `:app:assembleDebug`, `:app:testDebugUnitTest`.
- Device (ZA222Y47FN): Customize shows all slots/options with correct
  locks; tapping options updates preview instantly; Home renders combo;
  logcat shows no FATAL from `com.pixelpal.app`.

## Out of scope

- No body outfits (hoodie/cape) in v1 — face/head/neck + aura only.
- No new animation states, no species geometry changes, no bunny/panda
  reskin, no color/pattern body-recolor (still future layer).
- No photo upload (Duo forbids it post-avatar; same here: built avatar
  only).
