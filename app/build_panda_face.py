"""Rebuild the 12 panda Lottie files as the kawaii FACE-only spec.

Keeps every motion keyframe verbatim; only swaps static geometry/fills and
inserts 4 new layers (Head Outline, Left/Right Ear Inner, Nose) whose tracks
are deep-copied from existing layers.

Usage: python3 app/build_panda_face.py
"""
import copy
import json
from pathlib import Path

HERE = Path(__file__).parent
RAW = HERE / "src" / "main" / "res" / "raw"
STATES = ["idle", "blink", "happy", "sad", "sleep", "thinking",
          "wave", "jump", "excited", "celebrate", "eat", "walk"]

CREAM = [1.0, 0.973, 0.925, 1]
DARK = [0.169, 0.137, 0.126, 1]      # dark brown / near-black
DARKER = [0.09, 0.071, 0.066, 1]     # inner ear
BLUSH = [0.961, 0.659, 0.722, 1]     # pastel pink
WHITE = [1, 1, 1, 1]


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


def shift_y(ks_prop, dy):
    """Shift every keyframe s/e y in an animated p track."""
    prop = copy.deepcopy(ks_prop)
    if prop.get("a") == 1:
        for kf in prop["k"]:
            for key in ("s", "e"):
                if key in kf and isinstance(kf[key], list) and len(kf[key]) == 2:
                    kf[key] = [kf[key][0], kf[key][1] + dy]
    else:
        if isinstance(prop["k"], list) and len(prop["k"]) == 2:
            prop["k"] = [prop["k"][0], prop["k"][1] + dy]
    return prop


def w_mouth_shapes():
    return [{"ty": "gr", "nm": "shape", "it": [
        {"ty": "sh", "d": 1, "ks": {"a": 0, "k": {
            "i": [[0, 0], [-4, 4], [-4, -3], [-4, 4], [0, 0]],
            "o": [[0, 0], [4, 4], [4, -3], [4, 4], [0, 0]],
            "v": [[-14, -2], [-7, 6], [0, 0], [7, 6], [14, -2]],
            "c": False}}, "nm": "path"},
        {"ty": "st", "c": {"a": 0, "k": list(DARK)},
         "o": {"a": 0, "k": 100}, "w": {"a": 0, "k": 5},
         "lc": 2, "lj": 2, "nm": "stroke"},
        {"ty": "tr", "p": {"a": 0, "k": [0, 0]},
         "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]},
         "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100},
         "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0},
         "nm": "transform"}]}]


def swap_first_shape(layer, new_items_fn):
    for shape in layer.get("shapes", []):
        shape["it"] = new_items_fn(shape["it"])
        break


def ellipse_items(size, fill):
    return [{"ty": "el", "d": 1, "s": {"a": 0, "k": list(size)},
             "p": {"a": 0, "k": [0, 0]}, "nm": "ellipse"},
            {"ty": "fl", "c": {"a": 0, "k": list(fill)},
             "o": {"a": 0, "k": 100}, "r": 1, "nm": "fill"},
            {"ty": "tr", "p": {"a": 0, "k": [0, 0]},
             "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]},
             "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100},
             "sk": {"a": 0, "k": 0}, "sa": {"a": 0, "k": 0},
             "nm": "transform"}]


def rebuild(doc, op):
    # Idempotent: strip previously added layers so re-runs rebuild cleanly.
    doc["layers"] = [L for L in doc["layers"]
                     if L["nm"] not in ("Head Outline", "Left Ear Inner",
                                        "Right Ear Inner", "Nose")]
    layers = {L["nm"]: L for L in doc["layers"]}
    body = layers["Pixel Body"]
    mouth = layers["Mouth"]
    # HEAD: wide cream oval (wide, NOT circular) + outline below.
    swap_first_shape(body, lambda it: ellipse_items([330, 250], CREAM))

    # EARS: perfect circles, dark brown.
    for nm in ("Left Ear", "Right Ear"):
        swap_first_shape(layers[nm], lambda it: ellipse_items([56, 56], DARK))

    # EYE PATCHES + PUPILS: unify to dark brown (keep sizes/positions).
    for nm in ("Left Eye", "Right Eye", "Left Pupil", "Right Pupil"):
        for shape in layers[nm].get("shapes", []):
            for item in shape.get("it", []):
                if item.get("ty") == "fl":
                    item["c"]["k"] = list(DARK)

    # MOUTH: pink rect -> dark W-smile stroke path (motion tracks kept).
    mouth["shapes"] = w_mouth_shapes()

    # CHEEKS: rect -> horizontal pastel-pink ovals.
    for nm in ("Left Cheek", "Right Cheek"):
        swap_first_shape(layers[nm], lambda it: ellipse_items([34, 16], BLUSH))

    # --- new layers (paint order: file order = front first, so append behind)
    out = list(doc["layers"])

    # Head outline: dark ellipse behind head, copies body motion.
    outline = el_layer("Head Outline", [342, 262], DARK, [0, 0])
    outline["ks"]["p"] = copy.deepcopy(body["ks"]["p"])
    outline["ks"]["s"] = copy.deepcopy(body["ks"]["s"])
    outline["ks"]["r"] = copy.deepcopy(body["ks"]["r"])
    outline["ip"], outline["op"] = 0, op

    # Ear inners: follow outer ear rotation, offset toward head center.
    inners = []
    for nm, pos in (("Left Ear", [145, 100]), ("Right Ear", [255, 100])):
        outer = layers[nm]
        inner = el_layer(nm + " Inner", [28, 28], DARKER, pos)
        inner["ks"]["r"] = copy.deepcopy(outer["ks"]["r"])
        inner["ip"], inner["op"] = 0, op
        inners.append((nm, inner))

    # Nose: follows mouth motion, shifted up 12px.
    nose = el_layer("Nose", [20, 13], DARK, [200, 224])
    nose["ks"]["p"] = shift_y(mouth["ks"]["p"], -12)
    nose["ks"]["s"] = copy.deepcopy(mouth["ks"]["s"])
    nose["ip"], nose["op"] = 0, op

    # Insert inners right after their outer ear; outline + nose go last (back).
    rebuilt = []
    for L in out:
        rebuilt.append(L)
        for nm, inner in inners:
            if L["nm"] == nm:
                rebuilt.append(inner)
    rebuilt.append(outline)
    rebuilt.append(nose)

    # Renumber inds sequentially (Root stays 1).
    for i, L in enumerate(rebuilt):
        L["ind"] = i + 1
    doc["layers"] = rebuilt
    return doc


def main():
    for st in STATES:
        p = RAW / f"pet_panda_{st}.json"
        doc = json.loads(p.read_text())
        doc = rebuild(doc, doc["op"])
        assert len(doc["layers"]) == 20, (st, len(doc["layers"]))
        assert doc["nm"] == f"pet_panda_{st}"
        p.write_text(json.dumps(doc, separators=(",", ":")))
        print(f"{p.name:28s} | op={doc['op']:>4}f | {p.stat().st_size:>6} bytes")


if __name__ == "__main__":
    main()
