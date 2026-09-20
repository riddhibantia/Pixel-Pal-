"""PixelPal corner pet for the laptop.

A tiny always-on-top pixel face that sits in a screen corner while you work
(e.g. beside your coding agent) and mirrors the agent endpoint live:
bounces while WORKING, blinks, sleeps with Zzz while IDLE/OFFLINE, shakes
on ERROR. Green dot = connected to PixelPal, red = endpoint unreachable.

Run:  pythonw corner_pet.py            (no console window)
      pythonw corner_pet.py 9000       (if the endpoint runs elsewhere)
Stop: right-click the pet -> Quit. Drag it anywhere with the left button.

Needs the agent endpoint running (python server.py).
"""
import json
import sys
import tkinter as tk
import urllib.request

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
URL = f"http://localhost:{PORT}/"

CELL = 11
FACE = [
    "...OO....OO...",
    "...OOO..OOO...",
    "...OOOOOOOO...",
    "..OOOOOOOOOO..",
    "..OOOOOOOOOO..",
    "...ODD..DDO...",
    "...ODW..WDO...",
    "...OOOOOOOO...",
    "...OPOOOOPO...",
    "......MM......",
    "....OOOOOO....",
]
BLINK_FACE = [r.replace("D", "O").replace("W", "O") for r in FACE]
COLORS = {"O": "#DF6C50", "D": "#1b1b22", "W": "#ffffff",
          "P": "#F0977F", "M": "#8E4438"}
W = len(FACE[0]) * CELL
H = len(FACE) * CELL


class CornerPet:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("PixelPal")
        self.root.overrideredirect(True)
        self.root.attributes("-topmost", True)
        self.root.configure(bg="#14141f")
        # Bottom-right corner, above the taskbar area.
        self.root.geometry(f"+{self.root.winfo_screenwidth() - W - 60}+" f"{self.root.winfo_screenheight() - H - 160}")
        self.canvas = tk.Canvas(self.root, width=W, height=H + 44,
                                bg="#14141f", highlightthickness=0)
        self.canvas.pack()
        self.dot = self.canvas.create_oval(8, H + 26, 20, H + 38, fill="#555", outline="")
        self.label = self.canvas.create_text(W // 2 + 8, H + 32, text="…",
                                             fill="#f5f0e8", font=("Segoe UI", 9),
                                             anchor="w", width=W - 40)
        self.zzz = self.canvas.create_text(W - 24, 16, text="", fill="#9db4ff",
                                           font=("Segoe UI", 14))
        self.menu = tk.Menu(self.root, tearoff=0)
        self.menu.add_command(label="Quit", command=self.root.destroy)
        self.canvas.bind("<Button-3>", lambda e: self.menu.post(e.x_root, e.y_root))
        self.canvas.bind("<Button-1>", self._drag_start)
        self.canvas.bind("<B1-Motion>", self._drag_move)
        self.pixels = {}
        self.frame = 0
        self.state = "IDLE"
        self.task = ""
        self.blink_in = 4
        self.root.after(100, self.tick)
        self.root.after(500, self.poll)
        self.root.mainloop()

    def _drag_start(self, e):
        self._dx, self._dy = e.x, e.y

    def _drag_move(self, e):
        self.root.geometry(f"+{e.x_root - self._dx}+{e.y_root - self._dy}")

    def draw(self, blink=False):
        rows = BLINK_FACE if blink else FACE
        bob = 0
        if self.state == "WORKING":
            bob = -8 if (self.frame % 8) < 4 else 0
        elif self.state in ("IDLE", "OFFLINE"):
            bob = -3 if (self.frame % 24) < 12 else 0
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                key = (x, y)
                color = COLORS[ch]
                if self.state == "OFFLINE":
                    color = "#5a5a6e" if ch == "O" else ("#33333f" if ch != "W" else "#777788")
                if key not in self.pixels:
                    self.pixels[key] = self.canvas.create_rectangle(0, 0, 0, 0, outline="", fill=color)
                self.canvas.coords(self.pixels[key], x * CELL, y * CELL + bob,
                                   x * CELL + CELL - 1, y * CELL + CELL - 1 + bob)
                self.canvas.itemconfig(self.pixels[key], fill=color)

    def poll(self):
        try:
            with urllib.request.urlopen(URL, timeout=4) as r:
                s = json.load(r)
            self.state = str(s.get("status", "IDLE")).upper()
            self.task = str(s.get("currentTask") or s.get("message") or "idle")
            dot = {"WORKING": "#4caf50", "ERROR": "#e53935"}.get(self.state, "#42a5f5")
            if self.state == "OFFLINE":
                dot = "#555"
            self.canvas.itemconfig(self.dot, fill=dot)
        except Exception:
            self.state = "OFFLINE"
            self.task = "endpoint down"
            self.canvas.itemconfig(self.dot, fill="#e53935")
        short = self.task if len(self.task) <= 22 else self.task[:21] + "…"
        self.canvas.itemconfig(self.label, text=short)
        sleeping = self.state in ("IDLE", "OFFLINE")
        self.canvas.itemconfig(self.zzz, text="z" if sleeping and (self.frame % 24) < 14 else "")
        self.root.after(3000, self.poll)

    def tick(self):
        self.frame += 1
        blink = False
        if self.frame % 40 == 0:
            self.blink_in = 2
        if self.blink_in > 0:
            self.blink_in -= 1
            blink = True
        shake = 0
        if self.state == "ERROR":
            shake = 4 if (self.frame % 6) < 3 else -4
        self.draw(blink=blink)
        for key, pid in self.pixels.items():
            x0, y0, x1, y1 = self.canvas.coords(pid)
            self.canvas.coords(pid, x0 + shake * 0.2, y0, x1 + shake * 0.2, y1)
        self.root.after(120, self.tick)


if __name__ == "__main__":
    CornerPet()
