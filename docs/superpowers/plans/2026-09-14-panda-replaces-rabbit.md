# Panda Replaces Rabbit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the duplicate `rabbit` species with a visually distinct `panda` and fix the species-tap animation glitch.

**Architecture:** Roster swap across 3 model files plus asset regeneration (12 panda Lotties from the shared 16-layer rig, recolor only), 15 rabbit asset deletions, one `LottiePetView` restart-key fix, and a rabbit→panda migration on load. No geometry, motion-program, or duration changes.

**Tech Stack:** Kotlin, Jetpack Compose, Lottie JSON (`res/raw`), Python 3 (asset generation), Gradle (`assembleDebug`, `testDebugUnitTest`), adb + ZA222Y47FN (device verify).

## Global Constraints

- One-shot Lottie lengths MUST match `AnimationState.durationMs` (`op / 30fps * 1000` ms).
- Every species keeps exactly 12 states: idle, blink, happy, sad, sleep, thinking, wave, jump, excited, celebrate, eat, walk.
- Panda `PetType` unlock level is 30 (rabbit's slot, unchanged order).
- `bunny` assets are never touched.
- Single-companion architecture: species is appearance-only, never touches bond/tasks/reminders/agent data.

---

### Task 1: Panda asset generator

**Files:**
- Create: `app/build_panda_lotties.py`
- Test: `app/src/test/java/com/pixelpal/app/domain/model/SpeciesRosterTest.kt`

**Interfaces:**
- Consumes: existing species files `app/src/main/res/raw/pet_cat_<state>.json` (motion template), `app/src/main/res/raw/pet_dog_<state>.json` (ear-rect reference `[52, 90]`).
- Produces: 12 files `app/src/main/res/raw/pet_panda_<state>.json` for Task 2 to wire up.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.pixelpal.app.domain.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeciesRosterTest {

    @Test
    fun roster_hasPanda_noRabbit() {
        val ids = PetType.entries.map { it.id }
        assertTrue("panda" in ids, "roster missing panda: $ids")
        assertTrue("rabbit" !in ids, "roster still contains rabbit: $ids")
        assertEquals(8, ids.size)
    }

    @Test
    fun panda_unlocksAtRabbitSlot() {
        val panda = PetType.fromId("panda")
        assertEquals("Panda", panda.displayName)
        assertEquals(30, panda.unlockBondLevel)
        assertTrue(panda.hasFullAnimationSet)
    }

    @Test
    fun customizeSpecies_hasPanda_noRabbit() {
        assertTrue("panda" in SpeciesStyle.SPECIES)
        assertTrue("rabbit" !in SpeciesStyle.SPECIES)
        assertEquals(8, SpeciesStyle.SPECIES.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `sh gradlew :app:testDebugUnitTest --tests "com.pixelpal.app.domain.model.SpeciesRosterTest" 2>&1 | tail -8`
Expected: FAIL (rabbit still present, panda missing).

- [ ] **Step 3: Write the generator script**

Create `app/build_panda_lotties.py` with this exact content:

```python
"""Generate the 12 panda Lottie files from the shared cat-rig template.

Reads pet_cat_<state>.json for motion/geometry, recolors fills only:
body white, ears near-black, eyes black (patches), cheeks/mouth pink kept.
Ear rect uses the standard [58, 72] cat rect (NOT bunny [34, 110]) so the
silhouette differs from bunny at a glance.

Usage: python3 app/build_panda_lotties.py
"""
import copy
import json
from pathlib import Path

HERE = Path(__file__).parent
RAW = HERE / "src" / "main" / "res" / "raw"
STATES = ["idle", "blink", "happy", "sad", "sleep", "thinking",
          "wave", "jump", "excited", "celebrate", "eat", "walk"]

WHITE = [1.0, 1.0, 1.0, 1]
BLACK = [0.169, 0.169, 0.2, 1]
CAT_EAR_RECT = [58, 72]


def recolor(doc):
    doc = copy.deepcopy(doc)
    doc["nm"] = doc["nm"].replace("pet_cat_", "pet_panda_")
    for layer in doc["layers"]:
        nm = layer.get("nm", "")
        for shape in layer.get("shapes", []):
            for item in shape.get("it", []):
                if item.get("ty") == "rc" and nm in ("Left Ear", "Right Ear"):
                    item["s"]["k"] = list(CAT_EAR_RECT)
                if item.get("ty") == "fl":
                    if nm == "Pixel Body":
                        item["c"]["k"] = list(WHITE)
                    elif nm in ("Left Ear", "Right Ear",
                                "Left Eye", "Right Eye",
                                "Left Pupil", "Right Pupil"):
                        item["c"]["k"] = list(BLACK)
    return doc


def main():
    for st in STATES:
        src = RAW / f"pet_cat_{st}.json"
        doc = recolor(json.loads(src.read_text()))
        assert doc["nm"] == f"pet_panda_{st}", doc["nm"]
        assert len(doc["layers"]) == 16, len(doc["layers"])
        out = RAW / f"pet_panda_{st}.json"
        out.write_text(json.dumps(doc, separators=(",", ":")))
        print(f"{out.name:28s} | op={doc['op']:>4}f | {out.stat().st_size:>6} bytes")


if __name__ == "__main__":
    main()
```

- [ ] **Step 4: Run the generator and verify output**

Run: `python3 app/build_panda_lotties.py`
Expected: 12 lines, one per state, e.g. `pet_panda_idle.json | op= 120f | ... bytes`.

Then run: `python3 -c "import json,hashlib; states=['idle','blink','happy','sad','sleep','thinking','wave','jump','excited','celebrate','eat','walk']; [print(s, json.load(open(f'app/src/main/res/raw/pet_panda_{s}.json'))['nm'], len(json.load(open(f'app/src/main/res/raw/pet_panda_{s}.json'))['layers'])) for s in states]"`
Expected: each prints `pet_panda_<state> 16`.

- [ ] **Step 5: Commit**

```bash
git add app/build_panda_lotties.py app/src/main/res/raw/pet_panda_*.json app/src/test/java/com/pixelpal/app/domain/model/SpeciesRosterTest.kt
git commit -m "feat: add panda Lottie set and roster test (fails until Task 2)"
```

### Task 2: Roster swap (rabbit out, panda in)

**Files:**
- Modify: `app/src/main/java/com/pixelpal/app/domain/model/PetType.kt:14`
- Modify: `app/src/main/java/com/pixelpal/app/domain/model/SpeciesStyle.kt:13`
- Modify: `app/src/main/java/com/pixelpal/app/domain/model/Companion.kt:11`
- Delete: `app/src/main/res/raw/pet_rabbit_*.json` (12 files)
- Delete: `app/src/main/res/drawable/pet_rabbit_blink.xml`, `pet_rabbit_happy.xml`, `pet_rabbit_idle.xml`

**Interfaces:**
- Consumes: `pet_panda_*.json` from Task 1; `SpeciesRosterTest` from Task 1.
- Produces: `PetType.PANDA` and panda-first `SPECIES` list for Tasks 3-4.

- [ ] **Step 1: Swap the PetType entry**

In `app/src/main/java/com/pixelpal/app/domain/model/PetType.kt`, replace line 14:

```kotlin
    PANDA("panda", "Panda", 30, "Gentle, cuddly & bamboo-loving buddy", true),
```

Full entry block after edit (lines 11-18):

```kotlin
    CAT("cat", "Cat", 0, "Friendly & balanced default companion", true),
    DOG("dog", "Dog", 10, "Loyal, playful & energetic friend", true),
    BUNNY("bunny", "Bunny", 25, "Gentle, curious & sleepy pal", true),
    PANDA("panda", "Panda", 30, "Gentle, cuddly & bamboo-loving buddy", true),
    FOX("fox", "Fox", 40, "Clever, confident & independent fox", true),
    WHALE("whale", "Whale", 50, "Calm, deep & dreamy swimmer", true),
    AXOLOTL("axolotl", "Axolotl", 60, "Calm, rare & magical companion", true),
    LLAMA("llama", "Llama", 75, "Chill, quirky & steadfast pal", true);
```

- [ ] **Step 2: Swap the SpeciesStyle list**

In `app/src/main/java/com/pixelpal/app/domain/model/SpeciesStyle.kt`, replace line 13:

```kotlin
        val SPECIES = listOf("cat", "dog", "panda", "bunny", "whale", "llama", "fox", "axolotl")
```

- [ ] **Step 3: Update the Companion KDoc**

In `app/src/main/java/com/pixelpal/app/domain/model/Companion.kt`, replace line 11:

```kotlin
    /** Species drives the sprite family (cat/dog/panda/whale/llama). */
```

- [ ] **Step 4: Delete rabbit assets**

Run:

```bash
rm app/src/main/res/raw/pet_rabbit_*.json app/src/main/res/drawable/pet_rabbit_blink.xml app/src/main/res/drawable/pet_rabbit_happy.xml app/src/main/res/drawable/pet_rabbit_idle.xml
ls app/src/main/res/raw/ | grep -c rabbit; ls app/src/main/res/drawable/ | grep -c rabbit
```

Expected: both counts print `0`.

- [ ] **Step 5: Run roster test to verify it passes**

Run: `sh gradlew :app:testDebugUnitTest --tests "com.pixelpal.app.domain.model.SpeciesRosterTest" 2>&1 | tail -6`
Expected: BUILD SUCCESSFUL, 3 tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/pixelpal/app/domain/model/PetType.kt app/src/main/java/com/pixelpal/app/domain/model/SpeciesStyle.kt app/src/main/java/com/pixelpal/app/domain/model/Companion.kt app/src/main/res/raw app/src/main/res/drawable
git commit -m "feat: replace rabbit species with panda"
```

### Task 3: Rabbit-to-panda migration on load

**Files:**
- Modify: `app/src/main/java/com/pixelpal/app/data/local/datastore/CompanionBootstrapInitializer.kt:96-115`
- Modify: `app/src/main/java/com/pixelpal/app/data/local/datastore/PreferencesManager.kt:88-90` (read path only if needed; prefer fold-level fix below)

**Interfaces:**
- Consumes: `PetType.PANDA` from Task 2.
- Produces: guarantee that no stored `"rabbit"` string can ever reach sprite lookup.

- [ ] **Step 1: Normalize species in ensureInitialized**

In `CompanionBootstrapInitializer.kt`, inside `ensureInitialized()` after the `getAllDirect()` call, normalize both the fresh-install path and existing rows. Replace lines 101-115:

```kotlin
            if (all.isEmpty()) {
                val name = preferencesManager.getPetName().ifBlank { "Pixel" }
                val rawType = preferencesManager.getSelectedPetType().ifBlank { "cat" }
                val petType = if (rawType.equals("rabbit", ignoreCase = true)) "panda" else rawType
                when (val result = companionRepository.create(
                    Companion(
                        name = name,
                        petType = petType,
                        species = petType,
                        role = CompanionRole.GENERAL
                    )
                )) {
                    is CompanionActionResult.Success -> Unit
                    else -> return@withTransaction
                }
            } else {
                // One-time rabbit -> panda migration for existing installs.
                all.filter {
                    it.species.equals("rabbit", ignoreCase = true) ||
                        it.petType.equals("rabbit", ignoreCase = true)
                }.forEach { companion ->
                    companionRepository.update(
                        companion.copy(species = "panda", petType = "panda")
                    )
                }
                val stored = preferencesManager.getSelectedPetType()
                if (stored.equals("rabbit", ignoreCase = true)) {
                    preferencesManager.setSelectedPetType("panda")
                }
```

- [ ] **Step 2: Verify CompanionRepository has update**

Run: `grep -n "suspend fun update" app/src/main/java/com/pixelpal/app/domain/repository/CompanionRepository.kt`
Expected: a line like `suspend fun update(companion: Companion)` exists. If it does not exist, STOP and report back instead of inventing a new DAO method.

- [ ] **Step 3: Build**

Run: `sh gradlew :app:assembleDebug 2>&1 | grep -E "^e:|BUILD|FAILED" | head -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/pixelpal/app/data/local/datastore/CompanionBootstrapInitializer.kt
git commit -m "feat: migrate stored rabbit species to panda on load"
```

### Task 4: Species-tap glitch fix

**Files:**
- Modify: `app/src/main/java/com/pixelpal/app/presentation/components/LottiePetView.kt:51-75`

**Interfaces:**
- Consumes: nothing from earlier tasks (independent renderer fix).
- Produces: clean replay on species/state retap for Task 5 device check.

- [ ] **Step 1: Key on state as well as resource**

In `app/src/main/java/com/pixelpal/app/presentation/components/LottiePetView.kt`,
replace line 52 (`key(lottieRawRes) {`) with:

```kotlin
            key(lottieRawRes, animationState) {
```

Nothing else in the block changes. A species switch now creates a new keyed
slot whose composition loads once, so the drawable fallback no longer
flashes over a previously loaded animation, and retapping the same
species/state replays cleanly. The full edited block (lines 51-75):

```kotlin
        if (lottieRawRes != 0) {
            key(lottieRawRes, animationState) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(lottieRawRes)
                )
                if (composition != null) {
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = if (animationState.loops) LottieConstants.IterateForever else 1,
                        speed = speed
                    )

                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(size)
                    )
                } else if (drawableRes != 0) {
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = "$petType companion in $animationState state",
                        modifier = Modifier.size(size)
                    )
                }
            }
        } else if (drawableRes != 0) {
```

- [ ] **Step 2: Add the regression test**

Append to `app/src/test/java/com/pixelpal/app/animation/AnimationEngineTest.kt`:

```kotlin
    @Test
    fun testPandaResolvesToPandaFile() {
        // getLottieRawResId needs a Context; assert the naming contract instead:
        // resolver builds "pet_<type>_<state>", so panda must follow it.
        val expected = "pet_panda_idle"
        assertEquals(expected, "pet_${"panda"}_${AnimationState.IDLE.stateName}")
    }
```

Run: `sh gradlew :app:testDebugUnitTest --tests "com.pixelpal.app.animation.AnimationEngineTest" 2>&1 | tail -6`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Build**

Run: `sh gradlew :app:assembleDebug 2>&1 | grep -E "^e:|BUILD|FAILED" | head -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/pixelpal/app/presentation/components/LottiePetView.kt app/src/test/java/com/pixelpal/app/animation/AnimationEngineTest.kt
git commit -m "fix: replay Lottie cleanly on species/state retap"
```

### Task 5: Full verification (gates)

**Files:** none (verification only).

- [ ] **Step 1: Panda file audit**

Run: `python3 -c "import json,hashlib,os; states=['idle','blink','happy','sad','sleep','thinking','wave','jump','excited','celebrate','eat','walk']; [print(s, json.load(open(f'app/src/main/res/raw/pet_panda_{s}.json'))['nm']) for s in states]; print('rabbit files left:', len([f for f in os.listdir('app/src/main/res/raw') if 'rabbit' in f]))"`
Expected: 12 `pet_panda_<state>` names, `rabbit files left: 0`.

- [ ] **Step 2: Panda distinctness audit**

Run: `python3 -c "import json,hashlib; states=['idle','blink','happy','sad','sleep','thinking','wave','jump','excited','celebrate','eat','walk']; ph={hashlib.sha256(open(f'app/src/main/res/raw/pet_panda_{s}.json','rb').read()).hexdigest() for s in states}; oh={s: {hashlib.sha256(open(f'app/src/main/res/raw/pet_{sp}_{s}.json','rb').read()).hexdigest() for sp in ['cat','dog','bunny','whale','llama','fox','axolotl']} for s in states}; print('overlap:', [s for s in states if list(ph)[states.index(s)] in oh[s]]); print('ear rect:', [it['s']['k'] for l in json.load(open('app/src/main/res/raw/pet_panda_idle.json'))['layers'] if l.get('nm')=='Left Ear' for sh in l['shapes'] for it in sh['it'] if it.get('ty')=='rc'])"`
Expected: `overlap: []`, `ear rect: [[58, 72]]`.

- [ ] **Step 3: CI gates**

Run: `sh gradlew :app:testDebugUnitTest :app:lintDebug 2>&1 | grep -E "^e:|FAILED|BUILD" | head -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Device check**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -1`
Expected: `Success`. Then launch, open Customize, confirm the species row reads Cat/Dog/Panda/Bunny/Whale/Llama/Fox/Axolotl with no Rabbit; tap Panda; confirm Home renders the panda with no flash and `adb logcat -d -t 100` shows no `FATAL EXCEPTION` from `com.pixelpal.app`.
