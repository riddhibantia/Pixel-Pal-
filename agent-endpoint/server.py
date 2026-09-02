"""PixelPal agent status + command endpoint.

GET  any path  -> JSON status envelope the app polls:
                   {"status": "WORKING", "currentTask": "...", "progress": 65, "message": "..."}
POST any path  -> body {"command": "..."} is accepted as a command from the
                   app; status.json is rewritten so the next poll reflects it
                   (status WORKING, currentTask = the command), and the command
                   is appended to commands.log.

The payload is read fresh from status.json on every request, so you can also
edit it by hand while the server runs.

Run:  python server.py            (port defaults to 8765)
      python server.py 9000
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


class Handler(BaseHTTPRequestHandler):
    def _reply(self, payload: dict, code: int = 200):
        data = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        with lock:
            self._reply(load_status())

    def do_POST(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            raw = self.rfile.read(length).decode("utf-8") if length else "{}"
            command = str(json.loads(raw).get("command", "")).strip()
        except Exception:
            self._reply({"ok": False, "error": "bad json"}, 400)
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
