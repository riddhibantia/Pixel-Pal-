"""PixelPal agent status + command endpoint.

GET  /         -> JSON status envelope the app polls:
                   {"status": "WORKING", "currentTask": "...", "progress": 65, "message": "..."}
GET  /pet      -> laptop dashboard: your companion's face with live
                   animations driven by the agent status above, plus a
                   command box. Query params pick the pet, e.g.
                   /pet?species=whale&color=blue&name=Riru
                   (also remembers your pick in localStorage).
POST /         -> body {"command": "..."} is accepted as a command from the
                   app (or the /pet dashboard); status.json is rewritten so
                   the next poll reflects it (status WORKING, currentTask =
                   the command), and the command is appended to commands.log.
POST /         -> body {"approvalId": "...", "decision": "approve"|"deny"}
                   answers a pendingApproval gate advertised in the envelope
                   (see pendingApproval below); a matching approval is
                   cleared and the decision is logged.

The agent asks the user by advertising this in the envelope:
  "pendingApproval": {"id": "abc123", "action": "delete 40 files",
                      "detail": "optional one-liner"}
The phone notifies once per id with Approve/Deny and POSTs the decision.

The payload is read fresh from status.json on every request, so you can also
edit it by hand while the server runs.

Run:  python server.py            (port defaults to 8765)
      python server.py 9000
      open http://localhost:8765/pet for the dashboard.
"""
import json
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
STATUS_FILE = Path(__file__).with_name("status.json")
COMMANDS_LOG = Path(__file__).with_name("commands.log")
# Optional: copy pet_*.json lottie files here and the /pet dashboard
# renders your companion's REAL animations instead of the SVG fallback.
# e.g. pet_art/pet_cat_idle.json, pet_art/pet_whale_excited.json, ...
ART_DIR = Path(__file__).with_name("pet_art")

DEFAULT_STATUS = {
    "status": "WORKING",
    "currentTask": "Pairing with PixelPal",
    "progress": 65,
    "message": "Agent endpoint is live — edit status.json to change my state.",
}

lock = threading.Lock()


def load_status() -> dict:
    try:
        return json.loads(STATUS_FILE.read_text(encoding="utf-8"))
    except Exception:
        return DEFAULT_STATUS


def save_status(status: dict) -> None:
    STATUS_FILE.write_text(json.dumps(status, indent=2), encoding="utf-8")


PET_PAGE = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>PixelPal Agent Dashboard</title>
<script src="https://cdnjs.cloudflare.com/ajax/libs/lottie-web/5.12.2/lottie.min.js"></script>
<style>
  :root { color-scheme: dark; }
  body { font-family: system-ui, sans-serif; background: #1a1a2e; color: #f5f0e8;
         display: flex; flex-direction: column; align-items: center;
         min-height: 100vh; margin: 0; padding: 24px; box-sizing: border-box; }
  .badge { background: #2e7d32; border-radius: 999px; padding: 4px 14px;
           font-size: 13px; margin-bottom: 8px; }
  h1 { font-size: 22px; margin: 4px 0 0; }
  .sub { opacity: .7; font-size: 14px; margin-bottom: 12px; }
  #stage { width: 240px; height: 240px; }
  #pet { transform-origin: 200px 220px; }
  #pet.working { animation: bounce .8s ease-in-out infinite; }
  #pet.idle { animation: bob 3s ease-in-out infinite; }
  #pet.error { animation: shake .5s ease-in-out infinite; }
  #pet.offline { filter: grayscale(1); opacity: .6; }
  @keyframes bounce { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-26px); } }
  @keyframes bob { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
  @keyframes shake { 0%,100% { transform: translateX(0); } 25% { transform: translateX(-8px); } 75% { transform: translateX(8px); } }
  .zzz { font-size: 28px; opacity: 0; }
  .zzz.on { animation: zzz 3s ease-in-out infinite; }
  @keyframes zzz { 0% { opacity: 0; transform: translateY(0); } 30% { opacity: 1; } 100% { opacity: 0; transform: translateY(-30px); } }
  .card { background: #262640; border-radius: 12px; padding: 16px 20px;
          width: min(440px, 90vw); margin-top: 12px; }
  .pill { display: inline-block; border-radius: 999px; padding: 2px 12px;
          font-size: 13px; font-weight: 600; background: #555; }
  .pill.working { background: #2e7d32; } .pill.error { background: #c62828; }
  .pill.idle { background: #1565c0; } .pill.offline { background: #555; }
  .bar { height: 8px; background: #111; border-radius: 4px; margin: 10px 0; overflow: hidden; }
  .bar > div { height: 100%; background: #de6f53; width: 0%; transition: width .5s; }
  .row { display: flex; gap: 8px; margin-top: 10px; }
  input, select { flex: 1; padding: 8px; border-radius: 8px; border: 1px solid #555;
                  background: #1a1a2e; color: inherit; }
  button { padding: 8px 16px; border-radius: 8px; border: 0; cursor: pointer;
           background: #de6f53; color: #1a1a2e; font-weight: 700; }
  .hint { font-size: 12px; opacity: .6; margin-top: 12px; text-align: center; }
</style>
</head>
<body>
  <div class="badge">Connected to PixelPal</div>
  <h1 id="petname">Pixel</h1>
  <div class="sub" id="species">cat</div>
  <div id="stage">
    <div id="lottie" style="width:240px;height:240px;display:none"></div>
    <svg id="svgpet" viewBox="0 0 400 400" width="240" height="240">
      <g id="pet" class="idle">
        <g id="ears" fill="#de6f53"></g>
        <ellipse id="body" cx="200" cy="220" rx="117" ry="82" fill="#de6f53"/>
        <ellipse id="belly" cx="200" cy="252" rx="70" ry="34" fill="#ffffff" opacity="0.35"/>
        <ellipse cx="161" cy="196" rx="19" ry="28" fill="#1a1a2e"/>
        <ellipse cx="239" cy="196" rx="19" ry="28" fill="#1a1a2e"/>
        <ellipse cx="163" cy="192" rx="4" ry="5" fill="#fff"/>
        <ellipse cx="241" cy="192" rx="4" ry="5" fill="#fff"/>
        <rect x="194" y="232" width="12" height="9" rx="3" fill="#f0977f"/>
        <rect x="116" y="229" width="27" height="13" rx="6" fill="#f0977f"/>
        <rect x="257" y="229" width="27" height="13" rx="6" fill="#f0977f"/>
        <g id="extras" fill="#de6f53"></g>
      </g>
      <text id="z1" class="zzz" x="270" y="120">z</text>
      <text id="z2" class="zzz" x="292" y="100" style="animation-delay:1s">z</text>
      <text id="z3" class="zzz" x="314" y="80" style="animation-delay:2s">Z</text>
    </svg>
  </div>
  <div class="card">
    <span class="pill idle" id="status">IDLE</span>
    <div class="bar"><div id="progress"></div></div>
    <div id="task" style="font-weight:600">Waiting for work…</div>
    <div id="message" style="opacity:.75;font-size:14px;margin-top:4px"></div>
    <div class="row">
      <input id="cmd" placeholder="Send a command… e.g. run the test suite">
      <button onclick="send()">Send</button>
    </div>
    <div class="row">
      <select id="refreshPick" onchange="setRefresh()" title="Dashboard refresh rate">
        <option value="3000">Refresh 3s</option>
        <option value="5000">Refresh 5s</option>
        <option value="10000">Refresh 10s</option>
      </select>
      <button onclick="showHistory()" style="background:#555;color:#fff">History</button>
    </div>
    <div class="row">
      <select id="speciesPick" onchange="pick()">
        <option>cat</option><option>dog</option><option>panda</option>
        <option>bunny</option><option>whale</option><option>llama</option>
        <option>axolotl</option>
      </select>
      <select id="colorPick" onchange="pick()">
        <option>orange</option><option>blue</option><option>purple</option>
        <option>pink</option><option>green</option>
      </select>
      <input id="namePick" placeholder="Name" onchange="pick()" style="flex:.7">
    </div>
  </div>
  <div class="card">
    <div style="font-weight:600;margin-bottom:6px">Set status (testing)</div>
    <div class="row">
      <select id="ovStatus">
        <option>WORKING</option><option>IDLE</option><option>ERROR</option><option>OFFLINE</option>
      </select>
      <input id="ovTask" placeholder="currentTask">
    </div>
    <div class="row">
      <input id="ovMsg" placeholder="message">
      <input id="ovProg" placeholder="progress 0-100" style="flex:.5">
      <button onclick="sendOverride()">Apply</button>
    </div>
    <div id="hist" style="font-size:13px;opacity:.8;margin-top:8px;white-space:pre-wrap"></div>
  </div>
  <div class="hint">Bookmark with ?species=&amp;color=&amp;name= to keep your pet. Same envelope the phone polls.</div>
<script>
const COLORS = { orange:'#de6f53', blue:'#42a5f5', purple:'#ab47bc', pink:'#ec407a', green:'#66bb6a' };
const q = new URLSearchParams(location.search);
let species = localStorage.getItem('pp_species') || q.get('species') || 'cat';
let color = localStorage.getItem('pp_color') || q.get('color') || 'orange';
let petname = localStorage.getItem('pp_name') || q.get('name') || 'Pixel';
document.getElementById('speciesPick').value = species;
document.getElementById('colorPick').value = color;
document.getElementById('namePick').value = petname === 'Pixel' ? '' : petname;

function drawPet() {
  const c = COLORS[color] || COLORS.orange;
  document.getElementById('body').setAttribute('fill', c);
  document.getElementById('petname').textContent = petname;
  document.getElementById('species').textContent = species;
  const ears = document.getElementById('ears');
  const extras = document.getElementById('extras');
  ears.innerHTML = ''; extras.innerHTML = '';
  const tri = (x1,y1,x2,y2,x3,y3) =>
    `<polygon points="${x1},${y1} ${x2},${y2} ${x3},${y3}"/>`;
  if (species === 'bunny') {
    ears.innerHTML = `<rect x="150" y="40" width="30" height="90" rx="14"/>
                      <rect x="220" y="40" width="30" height="90" rx="14"/>`;
  } else if (species === 'llama') {
    ears.innerHTML = `<rect x="150" y="50" width="28" height="85" rx="13"/>
                      <rect x="222" y="50" width="28" height="85" rx="13"/>` +
      `<ellipse cx="170" cy="105" rx="17" ry="15"/><ellipse cx="200" cy="96" rx="19" ry="16"/><ellipse cx="230" cy="105" rx="17" ry="15"/>`;
  } else if (species === 'panda') {
    ears.innerHTML = `<circle cx="120" cy="150" r="26"/><circle cx="280" cy="150" r="26"/>`;
  } else if (species === 'whale') {
    ears.innerHTML = `<ellipse cx="95" cy="255" rx="22" ry="12" transform="rotate(-25 95 255)"/>
                      <ellipse cx="305" cy="255" rx="22" ry="12" transform="rotate(25 305 255)"/>`;
    extras.innerHTML = `<rect x="310" y="205" width="36" height="20" rx="8"/>
      <ellipse cx="352" cy="195" rx="24" ry="15"/><ellipse cx="352" cy="230" rx="24" ry="15"/>`;
  } else if (species === 'axolotl') {
    ears.innerHTML = [0,1,2].map(i =>
      `<rect x="${118+i*14}" y="120" width="10" height="34" rx="5" transform="rotate(-20 ${123+i*14} 120)"/>` +
      `<rect x="${272-i*14}" y="120" width="10" height="34" rx="5" transform="rotate(20 ${277-i*14} 120)"/>`).join('');
  } else { // cat + dog: triangle ears
    ears.innerHTML = tri(128,150, 122,90, 162,128) + tri(272,150, 278,90, 238,128);
  }
}
function pick() {
  species = document.getElementById('speciesPick').value;
  color = document.getElementById('colorPick').value;
  petname = document.getElementById('namePick').value.trim() || 'Pixel';
  localStorage.setItem('pp_species', species);
  localStorage.setItem('pp_color', color);
  localStorage.setItem('pp_name', petname);
  loadedArt = '';
  drawPet();
}
function stateFor(status) {
  const st = (status || 'IDLE').toUpperCase();
  if (st === 'WORKING') return 'excited';
  if (st === 'ERROR') return 'sad';
  if (st === 'OFFLINE') return 'sleep';
  return 'idle';
}
let lottieAnim = null, loadedArt = '';
function loadArt(state) {
  const box = document.getElementById('lottie');
  const svg = document.getElementById('svgpet');
  if (typeof lottie === 'undefined') return; // offline CDN: SVG fallback stays
  const key = species + '/' + state;
  if (key === loadedArt) return;
  loadedArt = key;
  if (lottieAnim) { lottieAnim.destroy(); lottieAnim = null; }
  // Probe first so a missing pet_art file keeps the SVG fallback visible.
  fetch('/art/pet_' + species + '_' + state + '.json', {cache: 'force-cache'})
    .then(r => { if (!r.ok) throw 0; return r.json(); })
    .then(data => {
      box.style.display = 'block'; svg.style.display = 'none';
      lottieAnim = lottie.loadAnimation({container: box, renderer: 'svg',
        loop: true, autoplay: true, animationData: data});
    })
    .catch(() => { loadedArt = ''; box.style.display = 'none'; svg.style.display = 'block'; });
}
async function poll() {
  try {
    const r = await fetch('/', {cache: 'no-store'});
    const s = await r.json();
    const st = (s.status || 'IDLE').toUpperCase();
    const pill = document.getElementById('status');
    pill.textContent = st;
    pill.className = 'pill ' + (st === 'WORKING' ? 'working' : st === 'ERROR' ? 'error' : st === 'OFFLINE' ? 'offline' : 'idle');
    const pet = document.getElementById('pet');
    pet.setAttribute('class', st === 'WORKING' ? 'working' : st === 'ERROR' ? 'error' : st === 'OFFLINE' ? 'offline' : 'idle');
    const sleeping = (st === 'IDLE' || st === 'OFFLINE');
    ['z1','z2','z3'].forEach(id => document.getElementById(id).setAttribute('class', 'zzz' + (sleeping ? ' on' : '')));
    document.getElementById('task').textContent = s.currentTask || 'Waiting for work…';
    document.getElementById('message').textContent = s.message || '';
    document.getElementById('progress').style.width = (s.progress || 0) + '%';
    loadArt(stateFor(st));
  } catch (e) { /* endpoint restarting; retry next tick */ }
}
async function send() {
  const box = document.getElementById('cmd');
  const command = box.value.trim();
  if (!command) return;
  box.value = '';
  await fetch('/', {method: 'POST', headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({command})});
  poll();
}
document.getElementById('cmd').addEventListener('keydown', e => { if (e.key === 'Enter') send(); });
let refreshTimer = null;
function setRefresh() {
  const ms = parseInt(document.getElementById('refreshPick').value, 10);
  if (refreshTimer) clearInterval(refreshTimer);
  refreshTimer = setInterval(poll, ms);
}
async function showHistory() {
  try {
    const r = await fetch('/commands', {cache: 'no-store'});
    const j = await r.json();
    const cmds = j.commands || [];
    document.getElementById('hist').textContent =
      cmds.length ? cmds.slice().reverse().join('\\n') : '(no commands yet)';
  } catch (e) { document.getElementById('hist').textContent = '(unreachable)'; }
}
async function sendOverride() {
  const prog = parseInt(document.getElementById('ovProg').value, 10);
  await fetch('/', {method: 'POST', headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({override: {
      status: document.getElementById('ovStatus').value,
      currentTask: document.getElementById('ovTask').value.trim(),
      message: document.getElementById('ovMsg').value.trim(),
      progress: isNaN(prog) ? 0 : Math.max(0, Math.min(100, prog))}})});
  poll();
}
drawPet(); poll(); refreshTimer = setInterval(poll, 3000);
</script>
</body>
</html>"""


class Handler(BaseHTTPRequestHandler):

    # ── mini corner pet: real lottie art, pocket-sized, no controls ──
    MINI_PAGE = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>PixelPal</title>
<script src="https://cdnjs.cloudflare.com/ajax/libs/lottie-web/5.12.2/lottie.min.js"></script>
 <style>
  * { box-sizing: border-box; }
  html,body { margin:0; padding:0; width:100%; height:100%; background:#14141f; overflow:hidden; }
  html { background: transparent; }
  body { background:#14141f; overflow:hidden; border-radius: 12px;
         font-family: system-ui, sans-serif; line-height: 1;
         display:flex; flex-direction:column; align-items:center; justify-content:flex-start;
         padding: 10px 10px 8px; }
  #quit { position:fixed; top:2px; right:6px; color:#666; font-size:13px;
          cursor:pointer; display:none; z-index:10; }
  body:hover #quit { display:block; }
  #quit:hover { color:#fff; }
  #art { width: 80px; height: 80px; flex:none; display:block; overflow:hidden; }
  #art svg { width:100% !important; height:100% !important; display:block; }
  #row { display:flex; align-items:center; justify-content:center; gap:6px; padding: 0; width:100%; max-width: 88px; margin-top: 8px; overflow:hidden; }
 #dot { width: 7px; height: 7px; border-radius:50%; background:#555; flex:none;
         box-shadow: none; }
 #task { color:#f5f0e8; font-size:8px; line-height: 1.2; white-space: nowrap; overflow:hidden; text-overflow: ellipsis; text-align: center; flex:1; min-width:0;
        text-shadow: none; }
</style>
</head>
<body>
  <div id="quit" title="Close" onclick="if(window.pywebview){pywebview.api.close()}">×</div>
  <div id="art"></div>
  <div id="row"><div id="dot"></div><div id="task">…</div></div>
<script>
const q = new URLSearchParams(location.search);
const species = q.get('species') || 'cat';
const stateFor = s => (s === 'WORKING' ? 'excited' : s === 'ERROR' ? 'sad'
                        : s === 'OFFLINE' ? 'sleep' : 'idle');
let anim = null, loaded = '';
function loadArt(state) {
  if (typeof lottie === 'undefined') return;
  const key = species + '/' + state;
  if (key === loaded) return;
  loaded = key;
  if (anim) { anim.destroy(); anim = null; }
  fetch('/art/pet_' + species + '_' + state + '.json')
    .then(r => { if (!r.ok) throw 0; return r.json(); })
    .then(data => { anim = lottie.loadAnimation({container: document.getElementById('art'),
      renderer: 'svg', loop: true, autoplay: true, animationData: data, rendererSettings: {preserveAspectRatio: 'xMidYMid meet'}}); })
    .catch(() => { loaded = ''; });
}
function fitWindow() {
  // disabled: widget now uses window-sized flex layout (no triangular overflow)
}
async function poll() {
  try {
    const s = await (await fetch('/', {cache: 'no-store'})).json();
    const st = (s.status || 'IDLE').toUpperCase();
    document.getElementById('dot').style.background =
      st === 'WORKING' ? '#4caf50' : st === 'ERROR' ? '#e53935'
      : st === 'OFFLINE' ? '#555' : '#42a5f5';
    const t = s.currentTask || s.message || 'idle';
    document.getElementById('task').textContent = t.length > 26 ? t.slice(0, 25) + '…' : t;
    document.title = 'PixelPal — ' + st;
    loadArt(stateFor(st));
  } catch (e) {}
}
poll(); setInterval(poll, 3000);
window.addEventListener('load', () => setTimeout(fitWindow, 400));
setTimeout(fitWindow, 800);
setTimeout(fitWindow, 1600);
</script>
</body>
</html>"""
    def _reply(self, payload: dict, code: int = 200):
        data = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(data)

    def _reply_html(self, html: str, code: int = 200):
        data = html.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        path = self.path.split('?', 1)[0]
        if path == '/pet':
            self._reply_html(PET_PAGE)
            return
        if path == '/mini':
            self._reply_html(Handler.MINI_PAGE)
            return
        if path == '/commands':
            with lock:
                try:
                    lines = COMMANDS_LOG.read_text(encoding='utf-8').strip().split('\n')
                except Exception:
                    lines = []
            self._reply({'commands': lines[-20:]})
            return
        if path.startswith('/art/'):
            name = path[len('/art/'):]
            # Strict allow-list: pet_<species>_<state>.json only, no traversal.
            if not (name.startswith('pet_') and name.endswith('.json')
                    and '..' not in name and '/' not in name):
                self._reply({'ok': False, 'error': 'bad art name'}, 400)
                return
            art = ART_DIR / name
            if not art.exists():
                self._reply({'ok': False, 'error': 'no such art (copy pet_*.json into pet_art/)'}, 404)
                return
            data = art.read_bytes()
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(len(data)))
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(data)
            return
        with lock:
            self._reply(load_status())

    def do_POST(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            raw = self.rfile.read(length).decode("utf-8") if length else "{}"
            body = json.loads(raw)
            command = str(body.get("command", "")).strip()
            override = body.get("override") if isinstance(body, dict) else None
        except Exception:
            self._reply({"ok": False, "error": "bad json"}, 400)
            return

        # Dashboard "set status" control: rewrite the envelope directly.
        if isinstance(override, dict):
            with lock:
                status = load_status()
                for key in ("status", "currentTask", "progress", "message",
                            "pendingApproval"):
                    if key in override:
                        status[key] = override[key]
                save_status(status)
            print(f"[agent-endpoint] status override: {override}")
            self._reply({"ok": True, "status": status})
            return

        # Approval gate answer: clear a matching advertised approval.
        if isinstance(body, dict) and body.get("approvalId") and body.get("decision") in ("approve", "deny"):
            approval_id = str(body["approvalId"])
            decision = str(body["decision"])
            with lock:
                status = load_status()
                pending = status.get("pendingApproval") or {}
                matched = isinstance(pending, dict) and pending.get("id") == approval_id
                if matched:
                    status.pop("pendingApproval", None)
                    save_status(status)
                with COMMANDS_LOG.open("a", encoding="utf-8") as log:
                    log.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')}\tdecision {decision} on {approval_id}\n")
            print(f"[agent-endpoint] approval {approval_id} -> {decision} (matched={matched})")
            self._reply({"ok": True, "decision": decision, "matched": matched})
            return

        if not command:
            self._reply({"ok": False, "error": "empty command"}, 400)
            return

        with lock:
            status = load_status()
            status["status"] = "WORKING"
            status["currentTask"] = command[:60]
            status["progress"] = 10
            status["message"] = "Command received — working on it."
            save_status(status)
            with COMMANDS_LOG.open("a", encoding="utf-8") as log:
                log.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')}\t{command}\n")

        print(f"[agent-endpoint] command received: {command}")
        self._reply({"ok": True, "accepted": command})

    def log_message(self, fmt, *args):
        pass  # keep the console quiet; commands are printed explicitly


if __name__ == "__main__":
    if not STATUS_FILE.exists():
        STATUS_FILE.write_text(json.dumps(DEFAULT_STATUS, indent=2), encoding="utf-8")
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"[agent-endpoint] serving GET/POST on port {PORT} — payload from {STATUS_FILE}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
