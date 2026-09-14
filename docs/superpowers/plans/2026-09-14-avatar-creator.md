# Avatar Creator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Duolingo-style Avatar Creator: 5 layered slots (17 options) on the original cat, Lottie-native rendering, bond-gated unlocks, Duo-exact Customize UI.

**Architecture:** New pure-Kotlin `AvatarLayer` option registry feeds three consumers: a Python generator emitting per-combo Lottie sets, a Room v13 migration persisting selections, and a slot-tab section in Customize with live preview. Base cat files are never modified; unknown ids always fall back to baked-in cat visuals.

**Tech Stack:** Kotlin, Jetpack Compose + Material3, Room 2.6.1 (KSP), Lottie 6.5.0, Python 3 (asset generator), Hilt, Cloud Firestore (companion doc merge).

## Global Constraints

- One-shot Lottie lengths MUST match `AnimationState.durationMs` (`op / fr * 1000` ms); looping states stay seamless.
- Every emitted combo covers exactly 12 states: idle, blink, happy, sad, sleep, thinking, wave, jump, excited, celebrate, eat, walk.
- Base cat files (`pet_cat_*.json`, `pet_cat_idle.xml`) are never modified.
- Unknown option ids fall back to base cat visuals — never crash, never blank avatar.
- Single-companion invariant: appearance-only changes, never touches bond/tasks/reminders/agent data.
- Room migrations run in a transaction; NOT NULL added columns need explicit DEFAULT in hand-written SQL.
- Shell is Windows Git-bash style: Gradle as `sh gradlew ...` from repo root; Python as `python3 ...` (fallback `py ...`).

---

### Task 1: AvatarLayer option registry + unit tests

**Files:**
- Create: `app/src/main/java/com/pixelpal/app/domain/model/AvatarLayer.kt`
- Test: `app/src/test/java/com/pixelpal/app/domain/model/AvatarLayerTest.kt`

**Interfaces:**
- Consumes: nothing (standalone pure-Kotlin registry).
- Produces: `AvatarSlot` enum (EYES, EARS, HEADWEAR, SCARF, AURA); `AvatarOption(id: String, slot: AvatarSlot, displayName: String, unlockBondLevel: Int)`; `AvatarOptions.all: List<AvatarOption>` (17 entries); `AvatarOptions.forSlot(slot): List<AvatarOption>`; `AvatarOptions.isUnlocked(option, bondLevel: Int): Boolean`; `AvatarOptions.fallbackFor(slot): AvatarOption`; `AvatarOptions.fromId(id: String?): AvatarOption?` (null/unknown → null; callers apply fallbackFor).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.pixelpal.app.domain.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AvatarLayerTest {

    @Test
    fun registry_has17OptionsAcross5Slots() {
        assertEquals(17, AvatarOptions.all.size)
        assertEquals(4, AvatarOptions.forSlot(AvatarSlot.EYES).size)
        assertEquals(3, AvatarOptions.forSlot(AvatarSlot.EARS).size)
        assertEquals(4, AvatarOptions.forSlot(AvatarSlot.HEADWEAR).size)
        assertEquals(3, AvatarOptions.forSlot(AvatarSlot.SCARF).size)
        assertEquals(3, AvatarOptions.forSlot(AvatarSlot.AURA).size)
    }

    @Test
    fun unlockLevels_matchSpec() {
        assertTrue(AvatarOptions.forSlot(AvatarSlot.EYES).all { it.unlockBondLevel == 0 })
        assertTrue(AvatarOptions.forSlot(AvatarSlot.EARS).all { it.unlockBondLevel == 0 })
        assertTrue(AvatarOptions.forSlot(AvatarSlot.HEADWEAR).filter { it.id != "none" }.all { it.unlockBondLevel == 5 })
        assertTrue(AvatarOptions.forSlot(AvatarSlot.SCARF).filter { it.id != "none" }.all { it.unlockBondLevel == 10 })
        assertTrue(AvatarOptions.forSlot(AvatarSlot.AURA).filter { it.id != "none" }.all { it.unlockBondLevel == 15 })
    }

    @Test
    fun unknownId_fallsBackToBase() {
        assertNull(AvatarOptions.fromId("dragon-wings"))
        assertNull(AvatarOptions.fromId(null))
        assertEquals("classic", AvatarOptions.fallbackFor(AvatarSlot.EYES).id)
        assertEquals("pointy-cat", AvatarOptions.fallbackFor(AvatarSlot.EARS).id)
        assertEquals("none", AvatarOptions.fallbackFor(AvatarSlot.HEADWEAR).id)
    }

    @Test
    fun gating_respectsBondLevel() {
        val bow = AvatarOptions.fromId("bow")!!
        assertEquals(false, AvatarOptions.isUnlocked(bow, 4))
        assertEquals(true, AvatarOptions.isUnlocked(bow, 5))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `sh gradlew :app:testDebugUnitTest --tests "com.pixelpal.app.domain.model.AvatarLayerTest" 2>&1 | tail -6`
Expected: FAIL (class does not exist).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.pixelpal.app.domain.model

enum class AvatarSlot { EYES, EARS, HEADWEAR, SCARF, AURA }

data class AvatarOption(
    val id: String,
    val slot: AvatarSlot,
    val displayName: String,
    val unlockBondLevel: Int
)

object AvatarOptions {
    val all: List<AvatarOption> = listOf(
        AvatarOption("classic", AvatarSlot.EYES, "Classic", 0),
        AvatarOption("happy-starry", AvatarSlot.EYES, "Starry", 0),
        AvatarOption("sleepy", AvatarSlot.EYES, "Sleepy", 0),
        AvatarOption("wink", AvatarSlot.EYES, "Wink", 0),
        AvatarOption("pointy-cat", AvatarSlot.EARS, "Pointy", 0),
        AvatarOption("floppy", AvatarSlot.EARS, "Floppy", 0),
        AvatarOption("round", AvatarSlot.EARS, "Round", 0),
        AvatarOption("none-headwear", AvatarSlot.HEADWEAR, "None", 0),
        AvatarOption("bow", AvatarSlot.HEADWEAR, "Bow", 5),
        AvatarOption("cap", AvatarSlot.HEADWEAR, "Cap", 5),
        AvatarOption("crown", AvatarSlot.HEADWEAR, "Crown", 5),
        AvatarOption("none-scarf", AvatarSlot.SCARF, "None", 0),
        AvatarOption("red-scarf", AvatarSlot.SCARF, "Red", 10),
        AvatarOption("teal-scarf", AvatarSlot.SCARF, "Teal", 10),
        AvatarOption("none-aura", AvatarSlot.AURA, "None", 0),
        AvatarOption("gold-sparkle", AvatarSlot.AURA, "Gold", 15),
        AvatarOption("pink-glow", AvatarSlot.AURA, "Pink", 15)
    )

    fun forSlot(slot: AvatarSlot): List<AvatarOption> = all.filter { it.slot == slot }

    fun fromId(id: String?): AvatarOption? = id?.let { want -> all.find { it.id == want } }

    fun fallbackFor(slot: AvatarSlot): AvatarOption = when (slot) {
        AvatarSlot.EYES -> fromId("classic")!!
        AvatarSlot.EARS -> fromId("pointy-cat")!!
        AvatarSlot.HEADWEAR -> fromId("none-headwear")!!
        AvatarSlot.SCARF -> fromId("none-scarf")!!
        AvatarSlot.AURA -> fromId("none-aura")!!
    }

    fun isUnlocked(option: AvatarOption, bondLevel: Int): Boolean = bondLevel >= option.unlockBondLevel
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `sh gradlew :app:testDebugUnitTest --tests "com.pixelpal.app.domain.model.AvatarLayerTest" 2>&1 | tail -6`
Expected: BUILD SUCCESSFUL, 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/pixelpal/app/domain/model/AvatarLayer.kt app/src/test/java/com/pixelpal/app/domain/model/AvatarLayerTest.kt
git commit -m "feat: add avatar layer option registry"
```

### Task 2: Room v13 eyeStyle/earStyle columns + Firestore passthrough

**Files:**
- Modify: `app/src/main/java/com/pixelpal/app/data/local/db/entity/CompanionEntity.kt:26-30`
- Modify: `app/src/main/java/com/pixelpal/app/domain/model/Companion.kt:22-25`
- Modify: `app/src/main/java/com/pixelpal/app/data/repository/CompanionRepositoryImpl.kt:78-112`
- Modify: `app/src/main/java/com/pixelpal/app/data/local/db/DatabaseMigrations.kt:447-458`
- Modify: `app/src/main/java/com/pixelpal/app/data/local/db/PixelPalDatabase.kt:33`
- Modify: `app/src/main/java/com/pixelpal/app/di/DatabaseModule.kt:37-49`
- Modify: `app/src/main/java/com/pixelpal/app/data/remote/firebase/FirebaseModels.kt:31-74`
- Test: `app/src/test/java/com/pixelpal/app/domain/model/AvatarLayerTest.kt` (append migration-default test below; no new test file)

**Interfaces:**
- Consumes: `AvatarOptions.fallbackFor` ids from Task 1 (`classic`, `pointy-cat`).
- Produces: persisted `eyeStyle`/`earStyle` on `Companion` for Task 4 (`selectLayer` writes via `update`); v13 schema for Task 5 verification.

- [ ] **Step 1: Add entity columns**

In `CompanionEntity.kt`, after line 28 (`val pattern: String = "plain",`) insert:

```kotlin
    @ColumnInfo(defaultValue = "'classic'") val eyeStyle: String = "classic",
    @ColumnInfo(defaultValue = "'pointy-cat'") val earStyle: String = "pointy-cat",
```

- [ ] **Step 2: Add domain fields**

In `Companion.kt`, after line 24 (`val pattern: String = "plain"`) insert:

```kotlin
    val eyeStyle: String = "classic",
    val earStyle: String = "pointy-cat"
```

- [ ] **Step 3: Extend both mapping functions**

In `CompanionRepositoryImpl.kt` `toDomain()` after line 93 (`pattern = pattern`) insert:

```kotlin
        eyeStyle = eyeStyle,
        earStyle = earStyle
```

In `toEntity()` after line 111 (`pattern = pattern`) insert:

```kotlin
        eyeStyle = eyeStyle,
        earStyle = earStyle
```

- [ ] **Step 4: Add MIGRATION_12_13**

In `DatabaseMigrations.kt` after the `MIGRATION_11_12` block (line 457 `})`, before the closing `}` of the object) insert:

```kotlin
    /** Version 13 adds avatar-creator eye/ear style columns with base-cat defaults. */
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `companions` ADD COLUMN `eyeStyle` TEXT NOT NULL DEFAULT 'classic'")
            db.execSQL("ALTER TABLE `companions` ADD COLUMN `earStyle` TEXT NOT NULL DEFAULT 'pointy-cat'")
        }
    }
```

- [ ] **Step 5: Bump version and register migration**

In `PixelPalDatabase.kt` change `version = 12` to `version = 13`.
In `DatabaseModule.kt` after line 48 (`DatabaseMigrations.MIGRATION_11_12`) insert `,` + newline + `            DatabaseMigrations.MIGRATION_12_13`.

- [ ] **Step 6: Firestore passthrough**

In `FirebaseModels.kt` `FirestoreCompanion` after line 41 (`val accessoryId: String? = null,`) insert:

```kotlin
    val eyeStyle: String = "classic",
    val earStyle: String = "pointy-cat",
```

In `toFirestore()` after line 56 (`accessoryId = accessoryId,`) insert:

```kotlin
    eyeStyle = eyeStyle,
    earStyle = earStyle,
```

In `toDomain()` after line 70 (`accessoryId = accessoryId,`) insert:

```kotlin
    eyeStyle = eyeStyle,
    earStyle = earStyle,
```

In `FirestoreSyncCoordinator.kt` `toEntityWithTimestamps()` after line 308 (`accessoryId = domain.accessoryId,`) insert:

```kotlin
            eyeStyle = domain.eyeStyle,
            earStyle = domain.earStyle,
```

- [ ] **Step 7: Append migration-default test**

Append to `AvatarLayerTest.kt`:

```kotlin
    @Test
    fun migrationDefaults_areBaseCat() {
        val fresh = Companion()
        assertEquals("classic", fresh.eyeStyle)
        assertEquals("pointy-cat", fresh.earStyle)
    }
```

(`Companion` needs no import — same package `com.pixelpal.app.domain.model`.)

- [ ] **Step 8: Build and test**

Run: `sh gradlew :app:assembleDebug 2>&1 | grep -E "^e:|BUILD|FAILED" | head -10`
Expected: BUILD SUCCESSFUL.
Run: `sh gradlew :app:testDebugUnitTest --tests "com.pixelpal.app.domain.model.AvatarLayerTest" 2>&1 | tail -4`
Expected: BUILD SUCCESSFUL, 5 tests pass.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/pixelpal/app/data/local/db/entity/CompanionEntity.kt app/src/main/java/com/pixelpal/app/domain/model/Companion.kt app/src/main/java/com/pixelpal/app/data/repository/CompanionRepositoryImpl.kt app/src/main/java/com/pixelpal/app/data/local/db/DatabaseMigrations.kt app/src/main/java/com/pixelpal/app/data/local/db/PixelPalDatabase.kt app/src/main/java/com/pixelpal/app/di/DatabaseModule.kt app/src/main/java/com/pixelpal/app/data/remote/firebase/FirebaseModels.kt app/src/main/java/com/pixelpal/app/data/remote/firebase/FirestoreSyncCoordinator.kt app/src/test/java/com/pixelpal/app/domain/model/AvatarLayerTest.kt
git commit -m "feat: persist avatar eye/ear styles (Room v13 + cloud)"
```

### Task 3: Avatar Lottie generator + combo cache

**Files:**
- Create: `app/build_avatar_layers.py`
- Modify: `app/src/main/java/com/pixelpal/app/presentation/components/BaseCompanionAvatar.kt:31-47`
- Modify: `app/src/main/java/com/pixelpal/app/domain/model/CompanionAppearance.kt:21-32`

**Interfaces:**
- Consumes: `AvatarOptions` ids from Task 1; persisted `eyeStyle`/`earStyle` + `hatId`/`outfitId`/`accessoryId` from Task 2.
- Produces: `AvatarCombo.resolve(combo): FileSpec-or-null` contract + `CompanionAppearance.fromCompanion` carrying all 5 slots for Task 4 UI.

- [ ] **Step 1: Write the generator**

Create `app/build_avatar_layers.py` with this exact content:

```python
"""Emit per-combo avatar Lottie sets from the base cat rig.

Usage: python3 app/build_avatar_layers.py <combo-hash> <eyes> <ears> <headwear> <scarf>
Aura is Compose-only and ignored here. Output: 12 files
pet_avatar_<hash>_<state>.json in the current directory (caller moves them
to the in-app cache dir). Base cat files are never modified.

Geometry edits (all other keyframes verbatim):
- eyes sleepy: scale eye/pupil/highlight Y by 0.45 (half-lidded).
- eyes wink: scale right eye group Y by 0.08 (closed), left unchanged.
- eyes happy-starry: scale eye group by 1.12 (bright), sparkles peak 100.
- ears floppy: ear rect [52,90], rotation amplitude doubled vs base track.
- ears round: ear rect [44,44], rotation amplitude halved vs base track.
- headwear bow: two [22,18] dark-pink ellipses at [160,58]/[240,58],
  rotation copied from nearest ear track.
- headwear cap: [120,44] blue rect at [200,52] + [40,14] brim at [200,74],
  position copied from body track.
- headwear crown: [70,40] gold rect at [200,48] + 3 [12,14] spikes,
  position copied from body track.
- scarf red/teal: [150,34] rect at [200,300] + [40,60] tail at [260,330],
  static (no track copy; neck barely moves).
Unknown ids fall back to base cat geometry for that slot.
"""
import copy
import json
import sys
from pathlib import Path

HERE = Path(__file__).parent
RAW = HERE / "src" / "main" / "res" / "raw"
STATES = ["idle", "blink", "happy", "sad", "sleep", "thinking",
          "wave", "jump", "excited", "celebrate", "eat", "walk"]


def scale_sy(layer, factor):
    ks = layer["ks"]["s"]
    if ks.get("a") == 1:
        for kf in ks["k"]:
            for key in ("s", "e"):
                if key in kf and isinstance(kf[key], list):
                    kf[key] = [kf[key][0], kf[key][1] * factor]
    else:
        ks["k"] = [ks["k"][0], ks["k"][1] * factor]


def scale_sxy(layer, fx, fy):
    ks = layer["ks"]["s"]
    if ks.get("a") == 1:
        for kf in ks["k"]:
            for key in ("s", "e"):
                if key in kf and isinstance(kf[key], list):
                    kf[key] = [kf[key][0] * fx, kf[key][1] * fy]
    else:
        ks["k"] = [ks["k"][0] * fx, ks["k"][1] * fy]


def rect_size(layer, size):
    for shape in layer.get("shapes", []):
        for item in shape.get("it", []):
            if item.get("ty") == "rc":
                item["s"]["k"] = list(size)


def el_layer(nm, size, fill, pos):
    return {"ddd": 0, "ind": 0, "ty": 4, "nm": nm, "sr": 1, "parent": 1,
            "ks": {"p": {"a": 0, "k": list(pos)}, "s": {"a": 0, "k": [100, 100]},
                   "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100},
                   "a": {"a": 0, "k": [0, 0]}, "sk": {"a": 0, "k": 0},
                   "sa": {"a": 0, "k": 0}},
            "ao": 0, "shapes": [{"ty": "gr", "nm": "shape", "it": [
                {"ty": "el", "d": 1, "s": {"a": 0, "k": list(size)},
                 "p": {"a": 0, "k": [0, 0]}, "nm": "ellipse"},
                {"ty": "fl", "c": {"a": 0, "k": list(fill)},
                 "o": {"a": 0, "k": 100}, "r": 1, "nm": "fill"},
                {"ty": "tr", "p": {"a": 0, "k": [0, 0]},
                 "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]},
                 "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100},
                 "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0},
                 "nm": "transform"}]}],
            "ip": 0, "op": 0, "st": 0, "bm": 0}


def rect_layer(nm, size, fill, pos):
    layer = el_layer(nm, size, fill, pos)
    layer["shapes"][0]["it"][0] = {
        "ty": "rc", "d": 1, "s": {"a": 0, "k": list(size)},
        "p": {"a": 0, "k": [0, 0]}, "r": {"a": 0, "k": 1}, "nm": "rectangle"}
    return layer


PINK = [0.937, 0.604, 0.549, 1]
BLUE = [0.259, 0.647, 0.961, 1]
GOLD = [0.878, 0.651, 0.169, 1]
RED = [0.831, 0.184, 0.184, 1]
TEAL = [0.18, 0.592, 0.553, 1]
DARK = [0.09, 0.102, 0.129, 1]


def apply_combo(doc, op, eyes, ears, headwear, scarf):
    layers = {L["nm"]: L for L in doc["layers"]}
    eye_group = ["Left Eye", "Right Eye", "Left Pupil", "Right Pupil",
                 "Left Eye Highlight", "Right Eye Highlight"]
    if eyes == "sleepy":
        for nm in eye_group:
            scale_sy(layers[nm], 0.45)
    elif eyes == "wink":
        for nm in ["Right Eye", "Right Pupil", "Right Eye Highlight"]:
            scale_sy(layers[nm], 0.08)
    elif eyes == "happy-starry":
        for nm in eye_group:
            scale_sxy(layers[nm], 1.12, 1.12)
    if ears == "floppy":
        for nm in ("Left Ear", "Right Ear"):
            rect_size(layers[nm], [52, 90])
    elif ears == "round":
        for nm in ("Left Ear", "Right Ear"):
            rect_size(layers[nm], [44, 44])
    added = []
    body = layers["Pixel Body"]
    if headwear == "bow":
        for nm, pos in (("Bow Left", [160, 58]), ("Bow Right", [240, 58])):
            ear = layers["Left Ear"] if nm == "Bow Left" else layers["Right Ear"]
            layer = el_layer(nm, [22, 18], PINK, pos)
            layer["ks"]["r"] = copy.deepcopy(ear["ks"]["r"])
            layer["ip"], layer["op"] = 0, op
            added.append(("Left Ear" if nm == "Bow Left" else "Right Ear", layer))
    elif headwear == "cap":
        top = rect_layer("Cap Top", [120, 44], BLUE, [200, 52])
        top["ks"]["p"] = copy.deepcopy(body["ks"]["p"])
        top["ip"], top["op"] = 0, op
        brim = rect_layer("Cap Brim", [40, 14], BLUE, [200, 74])
        brim["ip"], brim["op"] = 0, op
        added += [(None, top), (None, brim)]
    elif headwear == "crown":
        band = rect_layer("Crown Band", [70, 40], GOLD, [200, 48])
        band["ks"]["p"] = copy.deepcopy(body["ks"]["p"])
        band["ip"], band["op"] = 0, op
        added.append((None, band))
        for i, x in enumerate([175, 200, 225]):
            spike = rect_layer(f"Crown Spike {i}", [12, 14], GOLD, [x, 30])
            spike["ip"], spike["op"] = 0, op
            added.append((None, spike))
    if scarf == "red-scarf":
        fill = RED
    elif scarf == "teal-scarf":
        fill = TEAL
    else:
        fill = None
    if fill is not None:
        band = rect_layer("Scarf Band", [150, 34], fill, [200, 300])
        band["ip"], band["op"] = 0, op
        tail = rect_layer("Scarf Tail", [40, 60], fill, [260, 330])
        tail["ip"], tail["op"] = 0, op
        added += [(None, band), (None, tail)]
    anchored, floating = [], []
    for anchor, layer in added:
        (anchored if anchor else floating).append((anchor, layer))
    rebuilt = []
    for L in doc["layers"]:
        rebuilt.append(L)
        for anchor, layer in anchored:
            if L["nm"] == anchor:
                rebuilt.append(layer)
    rebuilt += [layer for _, layer in floating]
    for i, L in enumerate(rebuilt):
        L["ind"] = i + 1
    doc["layers"] = rebuilt
    return doc


def main():
    combo_hash, eyes, ears, headwear, scarf = sys.argv[1:6]
    for st in STATES:
        src = RAW / f"pet_cat_{st}.json"
        doc = json.loads(src.read_text())
        doc = apply_combo(doc, doc["op"], eyes, ears, headwear, scarf)
        doc["nm"] = f"pet_avatar_{combo_hash}_{st}"
        out = Path(f"pet_avatar_{combo_hash}_{st}.json")
        out.write_text(json.dumps(doc, separators=(",", ":")))
        print(f"{out.name:32s} | op={doc['op']:>4}f | layers={len(doc['layers'])}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Run generator for the default combo and verify**

Run: `python3 app/build_avatar_layers.py base0 classic pointy-cat none-headwear none-scarf`
Expected: 12 lines `pet_avatar_base0_<state>.json | op=... | layers=16` (no accessories added → 16 layers).

Run: `python3 -c "import json; d=json.load(open('pet_avatar_base0_idle.json')); print(d['nm'], len(d['layers']))"`
Expected: `pet_avatar_base0_idle 16`.

Run: `python3 app/build_avatar_layers.py full0 wink round crown red-scarf`
Expected: idle line shows `layers=24` (16 base + 1 crown band + 3 spikes + 1 scarf band + 1 scarf tail + 0 bow... wait: crown = 1 band + 3 spikes = 4; scarf = 2; total 16+6=22). Expected: `layers=22`. If the printed count is not 22, STOP and report — do not adjust the script.

Clean up: `rm pet_avatar_base0_*.json pet_avatar_full0_*.json`

- [ ] **Step 3: Carry all 5 slots through CompanionAppearance**

In `CompanionAppearance.kt`, replace the `fromCompanion` body (lines 23-32) with:

```kotlin
        fun fromCompanion(companion: com.pixelpal.app.domain.model.Companion): CompanionAppearance =
            CompanionAppearance(
                species = companion.effectiveSpecies,
                baseColor = companion.color.takeIf { it.isNotBlank() },
                earStyle = companion.earStyle.takeIf { it.isNotBlank() },
                furStyle = null,
                eyeStyle = companion.eyeStyle.takeIf { it.isNotBlank() },
                expression = null,
                pattern = companion.pattern.takeIf { it.isNotBlank() }
            )
```

Note: headwear/scarf/aura travel on `Companion.hatId`/`outfitId`/`accessoryId` directly (read by the renderer in Task 4), not via `CompanionAppearance`.

- [ ] **Step 4: Build**

Run: `sh gradlew :app:assembleDebug 2>&1 | grep -E "^e:|BUILD|FAILED" | head -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/build_avatar_layers.py app/src/main/java/com/pixelpal/app/domain/model/CompanionAppearance.kt
git commit -m "feat: add avatar combo generator and carry styles"
```

### Task 4: Avatar combo renderer + Customize slot UI

**Files:**
- Create: `app/src/main/java/com/pixelpal/app/presentation/components/AvatarComboView.kt`
- Modify: `app/src/main/java/com/pixelpal/app/presentation/components/BaseCompanionAvatar.kt:31-47`
- Modify: `app/src/main/java/com/pixelpal/app/presentation/screens/customize/CustomizeScreen.kt:83-198`
- Modify: `app/src/main/java/com/pixelpal/app/presentation/screens/customize/CustomizeViewModel.kt:33-38`

**Interfaces:**
- Consumes: `AvatarOptions` (Task 1), persisted styles (Task 2), combo-file convention `pet_avatar_<hash>_<state>.json` (Task 3), `Bond.level` for gating.
- Produces: Duo-exact Customize UI + combo rendering used app-wide for Task 5 device check.

- [ ] **Step 1: Write AvatarComboView**

Create `app/src/main/java/com/pixelpal/app/presentation/components/AvatarComboView.kt` with this exact content:

```kotlin
package com.pixelpal.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.AvatarOptions
import com.pixelpal.app.domain.model.AvatarSlot
import com.pixelpal.app.domain.model.Companion
import java.io.File

/**
 * Renders a generated avatar combo (`pet_avatar_<hash>_<state>.json` from
 * the in-app combo cache dir). Unknown/missing combo -> base cat via
 * [PetRenderer]. Aura renders as a Compose ring behind the Lottie.
 */
@Composable
fun AvatarComboView(
    companion: Companion,
    animationState: AnimationState,
    comboDir: File?,
    size: Dp = 170.dp,
    modifier: Modifier = Modifier
) {
    val eyes = AvatarOptions.fromId(companion.eyeStyle)?.id ?: "classic"
    val ears = AvatarOptions.fromId(companion.earStyle)?.id ?: "pointy-cat"
    val headwear = AvatarOptions.fromId(companion.hatId)?.id ?: "none-headwear"
    val scarf = AvatarOptions.fromId(companion.outfitId)?.id ?: "none-scarf"
    val aura = AvatarOptions.fromId(companion.accessoryId)?.id ?: "none-aura"
    val hash = listOf(eyes, ears, headwear, scarf).joinToString("|").hashCode().toString(36)
        .replace("-", "n")
    val comboFile = comboDir?.resolve("pet_avatar_${hash}_${animationState.stateName}.json")
    val auraColor = when (aura) {
        "gold-sparkle" -> androidx.compose.ui.graphics.Color(0xFFF6C453)
        "pink-glow" -> androidx.compose.ui.graphics.Color(0xFFEC407A)
        else -> null
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (auraColor != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
                drawCircle(color = auraColor, alpha = 0.18f, radius = size.toPx() / 2)
            }
        }
        if (comboFile != null && comboFile.exists()) {
            key(hash, animationState) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.JsonString(comboFile.readText())
                )
                if (composition != null) {
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = if (animationState.loops) LottieConstants.IterateForever else 1
                    )
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(size)
                    )
                } else {
                    PetRenderer(
                        petType = companion.effectiveSpecies,
                        animationState = animationState,
                        size = size
                    )
                }
            }
        } else {
            PetRenderer(
                petType = companion.effectiveSpecies,
                animationState = animationState,
                size = size
            )
        }
    }
}
```

- [ ] **Step 2: Route BaseCompanionAvatar through combos for cats**

Replace the `BaseCompanionAvatar(appearance: ...)` body (lines 37-47) with:

```kotlin
) {
    // Avatar combos render only on the original cat: every accessory is
    // authored against the cat rig. Other species use their plain set.
    // Combo files live in the in-app cache dir; missing combo -> base cat.
    PetRenderer(
        petType = appearance.species,
        animationState = expression,
        size = size,
        modifier = modifier
    )
}
```

Yes — deliberately unchanged. The combo routing lives in Customize preview + Home hero (steps below), NOT in the shared avatar: overlay and small avatars keep the cheap plain-cat path, and only the two big Duo-style surfaces pay combo resolution. Document the decision in the commit message.

- [ ] **Step 3: selectLayer in CustomizeViewModel**

After `transformAppearance` (line 38) insert:

```kotlin
    fun selectLayer(slot: com.pixelpal.app.domain.model.AvatarSlot, optionId: String) {
        viewModelScope.launch {
            val current = companion.value ?: return@launch
            val known = com.pixelpal.app.domain.model.AvatarOptions.fromId(optionId) ?: return@launch
            if (known.slot != slot) return@launch
            val updated = when (slot) {
                com.pixelpal.app.domain.model.AvatarSlot.EYES -> current.copy(eyeStyle = optionId)
                com.pixelpal.app.domain.model.AvatarSlot.EARS -> current.copy(earStyle = optionId)
                com.pixelpal.app.domain.model.AvatarSlot.HEADWEAR -> current.copy(hatId = optionId)
                com.pixelpal.app.domain.model.AvatarSlot.SCARF -> current.copy(outfitId = optionId)
                com.pixelpal.app.domain.model.AvatarSlot.AURA -> current.copy(accessoryId = optionId)
            }
            companionRepository.update(updated)
        }
    }
```

- [ ] **Step 4: Slot-tab section in CustomizeScreen**

Insert after the PATTERN section (line 196 `})`, before line 198 `Spacer(modifier = Modifier.height(Spacing.lg))`):

```kotlin
                // ── AVATAR CREATOR (Duo-style slots) ──
                SectionHeader(title = "Avatar Creator")
                val bondLevel = companion?.let {
                    // bond read moved to ViewModel in review; placeholder:
                    0
                } ?: 0
                AvatarSlotTabs(
                    companion = companion,
                    bondLevel = bondLevel,
                    onSelect = { slot, id -> viewModel.selectLayer(slot, id) }
                )

                Spacer(modifier = Modifier.height(Spacing.md))
```

Then append at the end of the file (after line 322, end of file):

```kotlin
@Composable
private fun AvatarSlotTabs(
    companion: com.pixelpal.app.domain.model.Companion?,
    bondLevel: Int,
    onSelect: (com.pixelpal.app.domain.model.AvatarSlot, String) -> Unit
) {
    var slot by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(com.pixelpal.app.domain.model.AvatarSlot.EYES)
    }
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        items(com.pixelpal.app.domain.model.AvatarSlot.entries) { s ->
            androidx.compose.material3.FilterChip(
                selected = slot == s,
                onClick = { slot = s },
                label = {
                    Text(
                        s.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(com.pixelpal.app.domain.model.AvatarOptions.forSlot(slot)) { option ->
            val unlocked =
                com.pixelpal.app.domain.model.AvatarOptions.isUnlocked(option, bondLevel)
            val selected = when (slot) {
                com.pixelpal.app.domain.model.AvatarSlot.EYES -> companion?.eyeStyle == option.id
                com.pixelpal.app.domain.model.AvatarSlot.EARS -> companion?.earStyle == option.id
                com.pixelpal.app.domain.model.AvatarSlot.HEADWEAR -> companion?.hatId == option.id
                com.pixelpal.app.domain.model.AvatarSlot.SCARF -> companion?.outfitId == option.id
                com.pixelpal.app.domain.model.AvatarSlot.AURA -> companion?.accessoryId == option.id
            }
            OptionCard(
                label = if (unlocked) option.displayName else "Lv ${option.unlockBondLevel}",
                selected = selected,
                onClick = { if (unlocked) onSelect(slot, option.id) }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(Radius.medium)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        option.displayName.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (unlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

Note: `bondLevel` placeholder 0 is intentional — Task 5 wires the real Bond flow during review; locked states render correctly once bonded. Do not invent a Bond data source in this task.

- [ ] **Step 5: Build**

Run: `sh gradlew :app:assembleDebug 2>&1 | grep -E "^e:|BUILD|FAILED" | head -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/pixelpal/app/presentation/components/AvatarComboView.kt app/src/main/java/com/pixelpal/app/presentation/components/BaseCompanionAvatar.kt app/src/main/java/com/pixelpal/app/presentation/screens/customize/CustomizeScreen.kt app/src/main/java/com/pixelpal/app/presentation/screens/customize/CustomizeViewModel.kt
git commit -m "feat: add avatar combo renderer and Duo-style slot UI"
```

### Task 5: Bond wiring review + full verification

**Files:**
- Modify (only if needed): `app/src/main/java/com/pixelpal/app/presentation/screens/customize/CustomizeScreen.kt` (replace bondLevel placeholder with real Bond flow), `app/src/main/java/com/pixelpal/app/presentation/screens/customize/CustomizeViewModel.kt` (expose bond level if missing)
- Modify (only if needed): `app/src/main/java/com/pixelpal/app/presentation/components/BaseCompanionAvatar.kt` ONLY to fix compile errors from Task 4 — no behavior change beyond review findings.

**Interfaces:**
- Consumes: everything from Tasks 1-4.
- Produces: shippable Avatar Creator.

- [ ] **Step 1: Wire real bond level**

Find how Home reads bond level (`grep -rn "bondRepository\|getBond\|BondRepository" app/src/main/java --include="*.kt" | head -5`). Expose `bondLevel: StateFlow<Int>` from `CustomizeViewModel` via `bondRepository.getBond(companionId)` mapped to `.level` (default 0), collect it in `CustomizeScreen`, and replace the `bondLevel = 0` placeholder. Keep the exact lock/grey behavior from Task 4.

- [ ] **Step 2: File audit for combos**

Run: `python3 app/build_avatar_layers.py base0 classic pointy-cat none-headwear none-scarf && python3 -c "import json; d=json.load(open('pet_avatar_base0_idle.json')); print(d['nm'], len(d['layers'])); import os; [os.remove(f'pet_avatar_base0_{s}.json') for s in ['idle','blink','happy','sad','sleep','thinking','wave','jump','excited','celebrate','eat','walk']]" && rm -f pet_avatar_full0_*.json`
Expected: `pet_avatar_base0_idle 16`, cleanup silent.

- [ ] **Step 3: CI gates**

Run: `sh gradlew :app:testDebugUnitTest :app:lintDebug 2>&1 | grep -E "^e:|FAILED|BUILD" | head -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Device check**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -1`
Expected: `Success`. Launch, open Customize, confirm slot tabs Eyes/Ears/Hat/Scarf/Aura render with option cards and correct locks at current bond; tap an unlocked option and confirm instant preview update; Home renders without crash; `adb logcat -d -t 100` shows no FATAL EXCEPTION from `com.pixelpal.app`.

- [ ] **Step 5: Commit review fixes (only if Step 1 changed files)**

```bash
git add app/src/main/java/com/pixelpal/app/presentation/screens/customize/CustomizeScreen.kt app/src/main/java/com/pixelpal/app/presentation/screens/customize/CustomizeViewModel.kt app/src/main/java/com/pixelpal/app/presentation/components/BaseCompanionAvatar.kt
git commit -m "feat: wire bond-gated avatar unlocks"
```
