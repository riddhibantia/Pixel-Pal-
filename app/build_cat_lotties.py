"""Author the full cat Lottie set from the authentic in-repo rig.

Reads the current pet_cat_idle.json as the geometry/palette template
(same 15 layers, same colors, same positions) and writes 12 state files
with modern motion: bezier easing, squash & stretch, secondary ear lag,
whole-cat Root null for jumps/sway, seamless loops.

One-shot file lengths match AnimationState.durationMs (op / 30fps * 1000).
Looping files end exactly where they start.

Usage:  python3 app/build_cat_lotties.py
"""
import copy
import json
from pathlib import Path

HERE = Path(__file__).parent
RAW = HERE / "src" / "main" / "res" / "raw"
TEMPLATE = RAW / "pet_cat_idle.json"
FR = 30

# ---------------------------------------------------------------- helpers

# cubic easings: (in-bezier, out-bezier) per dimension count
_EASE = {
    "io":  ({"x": [0.42, 0.42], "y": [1, 1]}, {"x": [0.58, 0.58], "y": [0, 0]}),
    "out": ({"x": [0.0, 0.0], "y": [1, 1]}, {"x": [0.58, 0.58], "y": [0, 0]}),
    "in":  ({"x": [0.42, 0.42], "y": [1, 1]}, {"x": [1.0, 1.0], "y": [0, 0]}),
    "lin": ({"x": [0.5, 0.5], "y": [1, 1]}, {"x": [0.5, 0.5], "y": [0, 0]}),
}
_EASE1 = {k: ({"x": v[0]["x"][:1], "y": v[0]["y"][:1]},
              {"x": v[1]["x"][:1], "y": v[1]["y"][:1]}) for k, v in _EASE.items()}


def _kf(t, s, e, ease, dim):
    i, o = (_EASE if dim == 2 else _EASE1)[ease]
    s = list(s) if isinstance(s, list) else [s]
    e = list(e) if isinstance(e, list) else [e]
    return {"t": t, "s": s, "e": e, "i": copy.deepcopy(i), "o": copy.deepcopy(o)}


def track(keys, op, loop, dim, default_ease="io"):
    """keys: [(t, value, [ease])...] -> animated Lottie prop.

    Fills segment end-values from the next key; auto-closes at t=op
    (to the first value for loops, to the last value for one-shots).
    """
    keys = sorted(keys, key=lambda k: k[0])
    out = []
    for i, (t, v, *rest) in enumerate(keys):
        ease = rest[0] if rest else default_ease
        if i + 1 < len(keys):
            e = keys[i + 1][1]
        else:
            e = keys[0][1] if loop else v
        out.append(_kf(t, v, e, ease, dim))
    if out[0]["t"] > 0:
        first = keys[0][1]
        out.insert(0, _kf(0, first, first, "lin", dim))
    if out[-1]["t"] < op:
        last = keys[0][1] if loop else keys[-1][1]
        out.append(_kf(op, last, last, "lin", dim))
    else:
        end = keys[0][1] if loop else keys[-1][1]
        out[-1]["e"] = list(end) if isinstance(end, list) else [end]
    return {"a": 1, "k": out}


def static(v):
    return {"a": 0, "k": v}


def base_of(layer, prop, idx):
    """Rest value of a template layer prop (static or first-keyframe s)."""
    v = layer["ks"][prop]["k"]
    if isinstance(v, list) and v and isinstance(v[0], dict):
        return copy.deepcopy(v[0]["s"])
    return copy.deepcopy(v)


# ------------------------------------------------------------ rig loading

def load_rig():
    tpl = json.loads(TEMPLATE.read_text())
    by_name = {}
    for L in tpl["layers"]:
        if L.get("ty") == 3:  # skip any prior Root
            continue
        by_name[L["nm"]] = L
    # Canonical front-to-back emission order (Lottie paints layers[0]
    # front). NEVER derive order from the template file: the template is
    # our own output, so file order flips on every regeneration.
    FRONT_TO_BACK = [
        "Sparkle 1", "Sparkle 2", "Sparkle 3",
        "Mouth",
        "Left Cheek", "Right Cheek",
        "Left Eye Highlight", "Right Eye Highlight",
        "Left Pupil", "Right Pupil",
        "Left Eye", "Right Eye",
        "Left Ear", "Right Ear",
        "Pixel Body",
    ]
    # Face tweak: tall eye ovals read "long". Widen X a touch, same
    # centers and heights — character unchanged, face less narrow.
    WIDER = {
        "Left Eye": [38.0, 56], "Right Eye": [38.0, 56],
        "Left Pupil": [14.5, 22], "Right Pupil": [14.5, 22],
        "Left Eye Highlight": [7.4, 10], "Right Eye Highlight": [7.4, 10],
    }
    layers = []
    for nm in FRONT_TO_BACK:
        L = by_name[nm]
        shapes = copy.deepcopy(L.get("shapes", []))
        if nm in WIDER:
            for shape in shapes:
                for item in shape.get("it", []):
                    if item.get("ty") == "el":
                        item["s"] = {"a": 0, "k": list(WIDER[nm])}
        layers.append({
            "nm": nm,
            "shapes": shapes,
            "p": base_of(L, "p", 0),
            "s": base_of(L, "s", 0),
            "r": base_of(L, "r", 0),
            "o": base_of(L, "o", 0),
        })
    return tpl, layers
def assemble(name, op, motions, tpl, rig, root_motion=None):
    """motions: {layer_nm: {prop: [(t, val, [ease])...]}} using rig rest values.

    Values may be absolute, or {"d": delta} / {"dx":.., "dy":..} /
    {"sx":.., "sy":..} relative to the rig rest value.
    """
    root_motion = root_motion or {}
    out_layers = [{
        "ddd": 0, "ind": 1, "ty": 3, "nm": "Root", "sr": 1,
        "ks": {
            "o": static(100),
            "r": track(root_motion.get("r", [(0, 0)]), op,
                       root_motion.get("loop", False), 1),
            "p": track(root_motion.get("p", [(0, [200, 200])]), op,
                       root_motion.get("loop", False), 2),
            "a": static([200, 200]),
            "s": track(root_motion.get("s", [(0, [100, 100])]), op,
                       root_motion.get("loop", False), 2),
        },
        "ao": 0, "ip": 0, "op": op, "st": 0, "bm": 0,
    }]

    # Rig arrives front-to-back (see load_rig); emit as-is.
    for ind, base in enumerate(rig, start=2):
        prog = motions.get(base["nm"], {})
        ks = {}
        for prop, dim in (("p", 2), ("s", 2), ("r", 1), ("o", 1)):
            if prop in prog:
                keys = []
                for t, v, *rest in prog[prop]:
                    e = rest[0] if rest else "io"
                    keys.append((t, resolve(base, prop, v), e))
                ks[prop] = track(keys, op, prog.get("loop", False), dim)
            else:
                ks[prop] = static(copy.deepcopy(base[prop]))
        ks.update({"a": static([0, 0]), "sk": static(0), "sa": static(0)})
        out_layers.append({
            "ddd": 0, "ind": ind, "ty": 4, "nm": base["nm"], "sr": 1,
            "parent": 1, "ks": ks, "ao": 0, "shapes": copy.deepcopy(base["shapes"]),
            "ip": 0, "op": op, "st": 0, "bm": 0,
        })

    return {"v": tpl["v"], "fr": FR, "ip": 0, "op": op,
            "w": tpl["w"], "h": tpl["h"], "nm": name,
            "ddd": 0, "assets": [], "layers": out_layers}


def resolve(base, prop, v):
    """Absolute value, or delta dict relative to the rig rest value."""
    b = base[prop]
    if not isinstance(v, dict):
        return v
    if prop in ("p", "s"):
        out = list(b)
        if prop == "p":
            out[0] += v.get("dx", 0) + v.get("d", [0, 0])[0] if "d" in v else v.get("dx", 0)
            out[1] += v.get("d", [0, 0])[1] if "d" in v else v.get("dy", 0)
        else:
            out[0] += v.get("dsx", 0) + v.get("ds", [0, 0])[0] if "ds" in v else v.get("dsx", 0)
            out[1] += v.get("ds", [0, 0])[1] if "ds" in v else v.get("dsy", 0)
        return out
    out = b[0] if isinstance(b, list) else b
    return out + v.get("d", 0)


EYES = ["Left Eye", "Right Eye"]
PUPILS = ["Left Pupil", "Right Pupil"]
HIGHLIGHTS = ["Left Eye Highlight", "Right Eye Highlight"]


def blink_keys(op):
    m = {}
    for nm in EYES + PUPILS + HIGHLIGHTS:
        m[nm] = {"s": [(0, {"dsy": 0}), (4, {"dsy": -94}, "in"),
                       (8, {"dsy": 0}, "out")]}
    return m


def sparkle_pop(m, op, peak=100, base_t=8, step=6):
    # Absolute scales: the rig base scale is [0,0], so deltas can't be used.
    for i, nm in enumerate(["Sparkle 1", "Sparkle 2", "Sparkle 3"]):
        t0 = base_t + i * step
        m[nm] = {"s": [(0, [0, 0]), (t0, [0, 0], "out"),
                       (t0 + 5, [peak, peak], "out"),
                       (op - 6, [peak, peak], "in"),
                       (op, [0, 0])],
                 "o": [(0, [0]), (t0, [0], "out"), (t0 + 5, [100], "out"),
                       (op - 6, [100], "in"), (op, [0])]}
    return m


def PROGRAMS():
    P = {}

    # ---- idle: 120f seamless loop -------------------------------------
    m = {
        "Pixel Body": {
            "loop": True,
            "p": [(0, {"dy": 0}), (60, {"dy": -7}), (120, {"dy": 0})],
            "s": [(0, {"ds": [0, 0]}), (60, {"ds": [4, 4]}), (120, {"ds": [0, 0]})],
        },
        "Left Ear": {"loop": True,
                     "r": [(0, {"d": 0}), (60, {"d": -4}), (120, {"d": 0})]},
        "Right Ear": {"loop": True,
                      "r": [(0, {"d": 0}), (60, {"d": 4}), (120, {"d": 0})]},
    }
    m.update(blink_keys_loop(120, at=(30, 90)))
    P["pet_cat_idle"] = (120, m, {"loop": True,
                                  "p": [(0, [200, 200]), (60, [200, 197]), (120, [200, 200])]})

    # ---- blink: 12f one-shot (400ms) ----------------------------------
    P["pet_cat_blink"] = (12, blink_keys(12), {})

    # ---- happy: 60f one-shot (2000ms) ---------------------------------
    m = {
        "Pixel Body": {
            "p": [(0, {"dy": 0}), (20, {"dy": -12}, "out"), (35, {"dy": 0}, "in"),
                  (45, {"dy": -5}, "out"), (60, {"dy": 0}, "in")],
            "s": [(0, {"ds": [0, 0]}), (20, {"ds": [5, 5]}, "out"),
                  (35, {"ds": [0, 0]}, "in"), (60, {"ds": [0, 0]})],
        },
        "Left Cheek": {"s": [(0, {"ds": [0, 0]}), (20, {"ds": [35, 35]}, "out"),
                             (60, {"ds": [0, 0]}, "in")]},
        "Right Cheek": {"s": [(0, {"ds": [0, 0]}), (20, {"ds": [35, 35]}, "out"),
                              (60, {"ds": [0, 0]}, "in")]},
        "Mouth": {"s": [(0, {"ds": [0, 0]}), (20, {"ds": [70, 25]}, "out"),
                        (60, {"ds": [0, 0]}, "in")]},
    }
    for nm in EYES + PUPILS + HIGHLIGHTS:  # joy squint
        m[nm] = {"s": [(0, {"dsy": 0}), (20, {"dsy": -55}, "out"),
                       (45, {"dsy": -55}, "in"), (60, {"dsy": 0}, "out")]}
    m = sparkle_pop(m, 60)
    P["pet_cat_happy"] = (60, m, {})

    # ---- sad: 90f seamless loop (3000ms window) -----------------------
    m = {
        "Pixel Body": {
            "loop": True,
            "p": [(0, {"dy": 6}), (45, {"dy": 9}), (90, {"dy": 6})],
            "s": [(0, {"ds": [2, -2]}), (45, {"ds": [2, -3]}), (90, {"ds": [2, -2]})],
        },
        "Left Ear": {"loop": True, "r": [(0, {"d": -16}), (45, {"d": -19}), (90, {"d": -16})]},
        "Right Ear": {"loop": True, "r": [(0, {"d": 16}), (45, {"d": 19}), (90, {"d": 16})]},
        "Mouth": {"s": [(0, {"ds": [-35, -25]})]},
    }
    for nm in EYES + PUPILS + HIGHLIGHTS:  # heavy half-mast lids
        m[nm] = {"s": [(0, {"dsy": -45})], "loop": True}
    P["pet_cat_sad"] = (90, m, {"loop": True,
                                "p": [(0, [197, 200]), (45, [203, 200]), (90, [197, 200])]})

    # ---- sleep: 60f seamless loop -------------------------------------
    m = {
        "Pixel Body": {
            "loop": True,
            "p": [(0, {"dy": 2}), (30, {"dy": -3}), (60, {"dy": 2})],
            "s": [(0, {"ds": [0, 0]}), (30, {"ds": [3, 3]}), (60, {"ds": [0, 0]})],
        },
        "Left Ear": {"loop": True, "r": [(0, {"d": -7}), (30, {"d": -9}), (60, {"d": -7})]},
        "Right Ear": {"loop": True, "r": [(0, {"d": 7}), (30, {"d": 9}), (60, {"d": 7})]},
    }
    for nm in EYES + PUPILS + HIGHLIGHTS:  # shut
        m[nm] = {"s": [(0, {"dsy": -94})], "loop": True}
    m["Sparkle 1"] = {"loop": True,  # slow dream bubble
                      "o": [(0, [25]), (30, [55]), (60, [25])],
                      "s": [(0, [70, 70]), (30, [95, 95]), (60, [70, 70])],
                      "p": [(0, {"dy": 0}), (30, {"dy": -8}), (60, {"dy": 0})]}
    P["pet_cat_sleep"] = (60, m, {})

    # ---- thinking: 90f seamless loop ----------------------------------
    m = {
        "Pixel Body": {"loop": True,
                       "p": [(0, {"dx": 0}), (22, {"dx": 6}), (45, {"dx": 0}),
                             (67, {"dx": -6}), (90, {"dx": 0})]},
        "Left Ear": {"loop": True,  # lag behind the lean
                     "r": [(0, {"d": 0}), (30, {"d": 6}), (60, {"d": -6}), (90, {"d": 0})]},
        "Right Ear": {"loop": True,
                      "r": [(0, {"d": 0}), (30, {"d": -6}), (60, {"d": 6}), (90, {"d": 0})]},
    }
    for nm in ("Left Pupil", "Right Pupil",
               "Left Eye Highlight", "Right Eye Highlight"):
        m[nm] = {"loop": True,
                 "p": [(0, {"dx": 0}), (22, {"dx": 6}), (45, {"dx": 0}),
                       (67, {"dx": -6}), (90, {"dx": 0})]}
    for nm in EYES:  # narrowed, pondering
        m[nm] = {"s": [(0, {"dsy": -15})], "loop": True}
    m["Sparkle 2"] = {"loop": True,  # thought dot pulsing overhead
                      "o": [(0, [20]), (45, [90]), (90, [20])],
                      "s": [(0, [55, 55]), (45, [110, 110]),
                            (90, [55, 55])]}
    P["pet_cat_thinking"] = (90, m, {})

    # ---- wave: 45f one-shot (1500ms) ----------------------------------
    m = {
        "Pixel Body": {
            "p": [(0, {"dy": 0}), (12, {"dy": -8}, "out"), (30, {"dy": -4}, "in"),
                  (45, {"dy": 0}, "out")],
        },
        "Right Ear": {"r": [(0, {"d": 0}), (8, {"d": -28}, "out"),
                            (16, {"d": 18}, "io"), (24, {"d": -22}, "io"),
                            (32, {"d": 12}, "io"), (45, {"d": 0}, "out")]},
        "Left Ear": {"r": [(0, {"d": 0}), (12, {"d": -10}, "out"), (45, {"d": 0}, "in")]},
    }
    for nm in EYES + PUPILS + HIGHLIGHTS:
        m[nm] = {"s": [(0, {"dsy": 0}), (12, {"dsy": -30}, "out"),
                       (32, {"dsy": -30}, "in"), (45, {"dsy": 0}, "out")]}
    m = sparkle_pop(m, 45, step=4)
    P["pet_cat_wave"] = (45, m, {"p": [(0, [200, 200]), (12, [208, 200], "out"),
                                       (45, [200, 200], "in")]})

    # ---- jump: 24f one-shot (800ms) -----------------------------------
    m = {
        "Pixel Body": {
            "s": [(0, {"ds": [0, 0]}), (4, {"ds": [13, -13]}, "in"),
                  (12, {"ds": [-6, 11]}, "out"), (18, {"ds": [4, -4]}, "in"),
                  (21, {"ds": [11, -11]}, "in"), (24, {"ds": [0, 0]}, "out")],
        },
        "Left Ear": {"r": [(0, {"d": 0}), (4, {"d": -12}, "in"),
                           (12, {"d": 8}, "out"), (24, {"d": 0}, "in")]},
        "Right Ear": {"r": [(0, {"d": 0}), (4, {"d": 12}, "in"),
                            (12, {"d": -8}, "out"), (24, {"d": 0}, "in")]},
    }
    for nm in EYES + PUPILS + HIGHLIGHTS:  # wide mid-air
        m[nm] = {"s": [(0, {"dsy": 0}), (4, {"dsy": -20}, "in"),
                       (12, {"dsy": 12}, "out"), (24, {"dsy": 0}, "in")]}
    P["pet_cat_jump"] = (24, m, {"p": [(0, [200, 200]), (4, [200, 208], "in"),
                                       (12, [200, 132], "out"), (18, [200, 196], "in"),
                                       (21, [200, 204], "in"), (24, [200, 200], "out")]})

    # ---- excited: 42f one-shot (1400ms) -------------------------------
    m = {
        "Pixel Body": {
            "p": [(0, {"dy": 0}), (10, {"dy": -14}, "out"), (20, {"dy": 0}, "in"),
                  (30, {"dy": -12}, "out"), (42, {"dy": 0}, "in")],
            "s": [(0, {"ds": [0, 0]}), (10, {"ds": [6, 6]}, "out"),
                  (20, {"ds": [0, 0]}, "in"), (30, {"ds": [6, 6]}, "out"),
                  (42, {"ds": [0, 0]}, "in")],
        },
        "Left Ear": {"r": [(0, {"d": 0}), (10, {"d": -14}, "out"),
                           (20, {"d": 0}, "in"), (30, {"d": -14}, "out"),
                           (42, {"d": 0}, "in")]},
        "Right Ear": {"r": [(0, {"d": 0}), (10, {"d": 14}, "out"),
                            (20, {"d": 0}, "in"), (30, {"d": 14}, "out"),
                            (42, {"d": 0}, "in")]},
        "Mouth": {"s": [(0, {"ds": [0, 0]}), (10, {"ds": [60, 30]}, "out"),
                        (20, {"ds": [0, 0]}, "in"), (30, {"ds": [60, 30]}, "out"),
                        (42, {"ds": [0, 0]}, "in")]},
    }
    for nm in EYES + PUPILS + HIGHLIGHTS:  # sparkling wide
        m[nm] = {"s": [(0, {"dsy": 0}), (10, {"dsy": 14}, "out"),
                       (20, {"dsy": 0}, "in"), (30, {"dsy": 14}, "out"),
                       (42, {"dsy": 0}, "in")]}
    m = sparkle_pop(m, 42, peak=120, step=4)
    P["pet_cat_excited"] = (42, m, {})

    # ---- celebrate: 90f one-shot (3000ms) -----------------------------
    hops = [(0, 0), (8, -26, "out"), (16, 0, "in"),
            (30, 0), (38, -26, "out"), (46, 0, "in"),
            (60, 0), (68, -26, "out"), (76, 0, "in"), (90, 0)]
    m = {
        "Left Ear": {"r": [(0, {"d": 0}), (15, {"d": -16}, "out"),
                           (30, {"d": 14}, "io"), (45, {"d": -16}, "io"),
                           (60, {"d": 14}, "io"), (75, {"d": -10}, "io"),
                           (90, {"d": 0}, "out")]},
        "Right Ear": {"r": [(0, {"d": 0}), (15, {"d": 16}, "out"),
                            (30, {"d": -14}, "io"), (45, {"d": 16}, "io"),
                            (60, {"d": -14}, "io"), (75, {"d": 10}, "io"),
                            (90, {"d": 0}, "out")]},
        "Mouth": {"s": [(0, {"ds": [0, 0]}), (8, {"ds": [50, 20]}, "out"),
                        (80, {"ds": [50, 20]}, "in"), (90, {"ds": [0, 0]}, "out")]},
    }
    for nm in EYES + PUPILS + HIGHLIGHTS:
        m[nm] = {"s": [(0, {"dsy": 0}), (8, {"dsy": -45}, "out"),
                       (80, {"dsy": -45}, "in"), (90, {"dsy": 0}, "out")]}
    m = sparkle_pop(m, 90, peak=130, base_t=6, step=8)
    P["pet_cat_celebrate"] = (90, m, {"p": [(t, [200, 200 + dy], *r)
                                            for t, dy, *r in hops]})

    # ---- eat: 60f one-shot (2000ms) -----------------------------------
    chomp = [(0, 0), (6, -72, "in"), (12, 0, "out"),
             (20, 0), (26, -72, "in"), (32, 0, "out"),
             (40, 0), (46, -72, "in"), (52, 0, "out"), (60, 0)]
    m = {
        "Mouth": {"s": [(t, {"ds": [25, v]}, *r) for t, v, *r in chomp]},
        "Left Cheek": {"s": [(t, {"ds": [12 if v else 0, 0]}, *r)
                             for t, v, *r in chomp]},
        "Right Cheek": {"s": [(t, {"ds": [12 if v else 0, 0]}, *r)
                              for t, v, *r in chomp]},
        "Pixel Body": {
            "p": [(0, {"dy": 0}), (12, {"dy": 3}, "in"), (24, {"dy": 0}, "out"),
                  (32, {"dy": 3}, "in"), (44, {"dy": 0}, "out"), (60, {"dy": 0})],
        },
    }
    for nm in EYES + PUPILS + HIGHLIGHTS:  # content, half-lidded
        m[nm] = {"s": [(0, {"dsy": 0}), (6, {"dsy": -30}, "out"),
                       (52, {"dsy": -30}, "in"), (60, {"dsy": 0}, "out")]}
    m = sparkle_pop(m, 60, peak=70, base_t=10, step=12)  # crumbs
    P["pet_cat_eat"] = (60, m, {})

    # ---- walk: 60f seamless loop (2000ms window) ----------------------
    m = {
        "Left Ear": {"loop": True,
                     "r": [(0, {"d": 0}), (15, {"d": -7}), (30, {"d": 0}),
                           (45, {"d": 7}), (60, {"d": 0})]},
        "Right Ear": {"loop": True,
                      "r": [(0, {"d": 0}), (15, {"d": 7}), (30, {"d": 0}),
                            (45, {"d": -7}), (60, {"d": 0})]},
        "Pixel Body": {"loop": True,
                       "s": [(0, {"ds": [0, 0]}), (15, {"ds": [2, -2]}),
                             (30, {"ds": [0, 0]}), (45, {"ds": [2, -2]}), (60, {"ds": [0, 0]})]},
    }
    m.update(blink_keys_loop(60, at=(30,)))
    root = {"loop": True,
            "p": [(0, [188, 200]), (15, [200, 195]), (30, [212, 200]),
                  (45, [200, 195]), (60, [188, 200])]}
    P["pet_cat_walk"] = (60, m, root)

    return P


def blink_keys_loop(op, at):
    """Seamless-loop blinks at given frame times (5f close-open each)."""
    m = {}
    for nm in EYES + PUPILS + HIGHLIGHTS:
        keys = [(0, {"dsy": 0})]
        for t in at:
            keys += [(t, {"dsy": 0}, "in"), (t + 2, {"dsy": -94}, "in"),
                     (t + 4, {"dsy": 0}, "out")]
        keys.append((op, {"dsy": 0}))
        m[nm] = {"s": keys, "loop": True}
    return m


# ------------------------------------------------------------------ main

def main():
    tpl, rig = load_rig()
    for name, (op, motions, root) in PROGRAMS().items():
        seg = assemble(name, op, motions, tpl, rig, root)
        p = RAW / f"{name}.json"
        p.write_text(json.dumps(seg, separators=(",", ":")))
        print(f"{p.name:28s} | op={op:>4}f | {p.stat().st_size:>6} bytes")


if __name__ == "__main__":
    main()
