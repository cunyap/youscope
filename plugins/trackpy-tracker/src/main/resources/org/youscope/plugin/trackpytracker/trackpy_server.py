# /*******************************************************************************
#  * Copyright (c) 2026 Andreas P. Cuny.
#  * All rights reserved. This program and the accompanying materials
#  * are made available under the terms of the GNU Public License v2.0
#  * which accompanies this distribution, and is available at
#  * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
#  * 
#  * Contributors:
#  *     Andreas P. Cuny - initial API and implementation
#  ******************************************************************************/

#!/usr/bin/env python3
"""Persistent trackpy tracking backend for YouScope.

A long-running TCP server (same wire protocol as the cell-detection backend:
length-prefixed JSON, images passed as raw little-endian temp files) so the
trackpy/pandas import is paid once. Frame-to-frame linking only: each 'track'
request links the current frame's cells to the previous frame's cells and returns,
per current cell, the previous LABEL id it links to (0 = new). The Java addon maps
label ids to persistent track ids.
"""
SCRIPT_VERSION = "v0.1.0"

import os
import sys
import json
import zlib
import base64
import struct
import time
import argparse
import threading
import socketserver

import numpy as np

_last_activity = time.time()
_lock = threading.Lock()
_LOG_PATH = os.environ.get("TRACKPY_SERVER_LOG", "")


def _log(msg):
    line = "[trackpy_server %s] %s" % (SCRIPT_VERSION, msg)
    try:
        print(line, flush=True)
    except Exception:
        pass
    if _LOG_PATH:
        try:
            with open(_LOG_PATH, "a") as f:
                f.write(line + "\n")
        except Exception:
            pass


def _pack(arr):
    return base64.b64encode(zlib.compress(np.ascontiguousarray(arr).tobytes(), 6)).decode("ascii")


def _load_mask(meta):
    dt = {1: "<u1", 2: "<u2", 4: "<u4"}[meta["bpp"]]
    with open(meta["path"], "rb") as f:
        raw = f.read()
    img = np.frombuffer(raw, dtype=dt).reshape(meta["height"], meta["width"]).astype(np.int64)
    if meta.get("ty"):
        img = np.flipud(img)
    if meta.get("tx"):
        img = np.fliplr(img)
    if meta.get("sw"):
        img = img.T
    return np.ascontiguousarray(img)


def _centroids(label):
    from scipy import ndimage as ndi
    ids = [int(i) for i in np.unique(label) if i != 0]
    if not ids:
        return [], np.zeros((0, 2))
    cen = ndi.center_of_mass(np.ones_like(label, dtype=np.float64), label, ids)
    xy = np.array([(c[1], c[0]) for c in cen], dtype=np.float64)  # x=col, y=row
    return ids, xy


def track_trackpy(prev_label, cur_label, params):
    """Link current cells to previous cells with trackpy (two-frame linking).
    Returns (cur_ids, linked_prev_ids)."""
    import pandas as pd
    import trackpy
    try:
        trackpy.quiet()
    except Exception:
        pass

    search_range = float(params.get("search_range", 15.0))
    memory = int(params.get("memory", 0))
    adaptive_stop = params.get("adaptive_stop", None)

    cids, ccen = _centroids(cur_label)
    if not cids:
        return [], []
    pids, pcen = _centroids(prev_label)
    if not pids:
        return cids, [0] * len(cids)

    rows = [{"frame": 0, "x": float(pcen[i, 0]), "y": float(pcen[i, 1]), "orig": pids[i]}
            for i in range(len(pids))]
    rows += [{"frame": 1, "x": float(ccen[i, 0]), "y": float(ccen[i, 1]), "orig": cids[i]}
             for i in range(len(cids))]
    df = pd.DataFrame(rows)

    kwargs = {"search_range": search_range, "memory": memory}
    if adaptive_stop is not None:
        kwargs["adaptive_stop"] = float(adaptive_stop)
        kwargs["adaptive_step"] = float(params.get("adaptive_step", 0.95))
    linked = trackpy.link(df, **kwargs)

    prev_particle = {int(r.orig): int(r.particle)
                     for _, r in linked[linked.frame == 0].iterrows()}
    particle_prev = {v: k for k, v in prev_particle.items()}
    cur_map = {int(r.orig): particle_prev.get(int(r.particle), 0)
               for _, r in linked[linked.frame == 1].iterrows()}
    return cids, [cur_map.get(c, 0) for c in cids]


def handle_track(req):
    t0 = time.time()
    prev = _load_mask(req["prev_mask"])
    cur = _load_mask(req["cur_mask"])
    cids, linked = track_trackpy(prev, cur, req.get("params", {}) or {})
    dtm = (time.time() - t0) * 1000.0
    _log("track: %d prev, %d cur cells in %.0f ms" %
         (len(np.unique(prev)) - (1 if 0 in prev else 0), len(cids), dtm))
    return {
        "ok": True, "version": SCRIPT_VERSION, "op": "track",
        "cur_count": len(cids), "timing_ms": round(dtm, 1),
        "cur_ids_b64": _pack(np.asarray(cids, dtype="<i4")),
        "linked_prev_ids_b64": _pack(np.asarray(linked, dtype="<i4")),
    }


class Handler(socketserver.BaseRequestHandler):
    def handle(self):
        global _last_activity
        try:
            length = struct.unpack(">I", self._recv_exact(4))[0]
            req = json.loads(self._recv_exact(length).decode("utf-8"))
        except Exception as exc:
            self._send({"ok": False, "error": "bad request: %r" % (exc,), "version": SCRIPT_VERSION})
            return

        op = req.get("op", req.get("cmd"))
        _last_activity = time.time()

        if op == "ping":
            self._send({"ok": True, "version": SCRIPT_VERSION, "trackers": ["trackpy"], "op": "ping"})
            return
        if op == "shutdown":
            self._send({"ok": True, "version": SCRIPT_VERSION, "op": "shutdown"})
            _log("shutdown requested - exiting")
            threading.Thread(target=self.server.shutdown, daemon=True).start()
            return
        try:
            with _lock:
                if op == "track":
                    self._send(handle_track(req))
                else:
                    self._send({"ok": False, "error": "unknown op %r" % (op,), "version": SCRIPT_VERSION})
        except Exception:
            import traceback
            tb = traceback.format_exc()
            _log("ERROR:\n" + tb)
            self._send({"ok": False, "error": tb, "version": SCRIPT_VERSION})

    def _recv_exact(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.request.recv(n - len(buf))
            if not chunk:
                raise EOFError("client closed")
            buf += chunk
        return buf

    def _send(self, obj):
        data = json.dumps(obj, separators=(",", ":")).encode("utf-8")
        self.request.sendall(struct.pack(">I", len(data)) + data)


class ThreadingTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    daemon_threads = True
    # MUST be False on Windows: SO_REUSEADDR there lets a second server bind the
    # same live port, which would split requests across two servers.
    allow_reuse_address = (os.name != "nt")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=59714)
    parser.add_argument("--idle-timeout", type=int, default=0)
    args = parser.parse_args()

    _log("SCRIPT_VERSION %s - starting up" % SCRIPT_VERSION)
    server = None
    for _attempt in range(6):
        try:
            server = ThreadingTCPServer((args.host, args.port), Handler)
            break
        except OSError as exc:
            _log("bind %s:%d failed (%s); retry %d/6" % (args.host, args.port, exc, _attempt + 1))
            time.sleep(0.5)
    if server is None:
        _log("could not bind %s:%d - another server is probably already there; exiting." % (args.host, args.port))
        sys.exit(0)

    if args.idle_timeout > 0:
        def _watch():
            while True:
                time.sleep(15)
                if time.time() - _last_activity > args.idle_timeout:
                    _log("idle for >%ds - exiting" % args.idle_timeout)
                    server.shutdown()
                    return
        threading.Thread(target=_watch, daemon=True).start()

    _log("listening on %s:%d" % (args.host, args.port))
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    _log("stopped")


if __name__ == "__main__":
    main()
