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
