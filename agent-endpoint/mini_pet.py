"""PixelPal floating corner pet (laptop).

Chromeless always-on-top mini window playing your companion's REAL lottie
animations from the agent endpoint (/mini page). No taskbar button clutter,
no min/max/close chrome — just the pet. Hover the top-right × to quit.

Run:  pythonw mini_pet.py [species]       (default cat)
Stop: hover the pet, click ×.
Needs: agent endpoint running (python server.py), pywebview installed.
"""
import sys
import webview


class Api:
    def close(self):
        import os
        os._exit(0)
    def resize(self, w, h):
        try:
            # w/h from JS are logical CSS pixels
            win.resize(int(w), int(h))
        except Exception:
            pass


SPECIES = sys.argv[1] if len(sys.argv) > 1 else "cat"

screen = webview.screens[0]
# Clean square ~105x108 (~22% larger than 86x94), cat 80 occupies ~76% width
# No JS auto-fit; body is flex 100% with overflow:hidden so background ends exactly at window edge
W, H = 104, 112
X = screen.width - W - 16
Y = screen.height - H - 60

win = webview.create_window(
    "PixelPal",
    f"http://localhost:8765/mini?species={SPECIES}",
    width=W,
    height=H,
    x=X,
    y=Y,
    frameless=True,
    on_top=True,
    resizable=False,
    shadow=False,
    transparent=False,
    background_color='#14141f',
    js_api=Api(),
)
webview.start()
