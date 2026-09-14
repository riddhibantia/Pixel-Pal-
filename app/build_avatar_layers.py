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
