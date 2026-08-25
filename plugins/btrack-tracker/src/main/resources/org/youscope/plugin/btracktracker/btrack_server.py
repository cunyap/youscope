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
"""Persistent btrack tracking backend for YouScope.

A long-running TCP server (same wire protocol as the cell-detection backend) so
the btrack import is paid once. Frame-to-frame linking only: each 'track' request
runs the btrack Bayesian (Kalman) assignment on the previous + current frame's
cell centroids and returns, per current cell, the previous LABEL id it links to
(0 = new). No global optimisation / hypothesis step (that is btrack's batch,
whole-movie mode); this is the minimal online adaptation. The Java addon maps
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
_LOG_PATH = os.environ.get("BTRACK_SERVER_LOG", "")

# Minimal constant-velocity motion model (position + velocity in x/y/z). Enough
# for frame-to-frame nearest-state assignment; no hypothesis/optimisation model.
_MOTION = {"motion_model": {
    "name": "cell_motion", "dt": 1.0, "measurements": 3, "states": 6,
    "accuracy": 7.5, "prob_not_assign": 0.1, "max_lost": 5,
    "A": {"matrix": [1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1,
                     0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1]},
    "H": {"matrix": [1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0]},
    "P": {"sigma": 150.0, "matrix": [0.1, 0, 0, 0, 0, 0, 0, 0.1, 0, 0, 0, 0,
                                     0, 0, 0.1, 0, 0, 0, 0, 0, 0, 1, 0, 0,
                                     0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1]},
    "G": {"sigma": 15.0, "matrix": [0.5, 0.5, 0.5, 1, 1, 1]},
    "R": {"sigma": 5.0, "matrix": [1, 0, 0, 0, 1, 0, 0, 0, 1]}}}


def _log(msg):
    line = "[btrack_server %s] %s" % (SCRIPT_VERSION, msg)
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


def track_btrack(prev_label, cur_label, params):
    """Link current cells to previous cells with btrack (two-frame Bayesian
    assignment). Returns (cur_ids, linked_prev_ids)."""
    import logging
    import btrack
    from btrack import btypes
    logging.getLogger("btrack").setLevel(logging.CRITICAL)

    max_search = float(params.get("max_search_radius", 50.0))

    cids, ccen = _centroids(cur_label)
    if not cids:
        return [], []
    pids, pcen = _centroids(prev_label)
    if not pids:
        return cids, [0] * len(cids)

    npv = len(pids)
    h, w = prev_label.shape

    def mk(cen, t):
        return [btypes.PyTrackObject.from_dict(
            {"ID": int(t * 1000000 + i), "x": float(cen[i, 0]), "y": float(cen[i, 1]),
             "z": 0.0, "t": int(t)}) for i in range(len(cen))]

    objs = mk(pcen, 0) + mk(ccen, 1)
    cur_to_prev = {cid: 0 for cid in cids}
    with btrack.BayesianTracker() as bt:
        cfg = dict(_MOTION)
        cfg["motion_model"] = dict(_MOTION["motion_model"])
        cfg["motion_model"]["max_lost"] = int(params.get("max_lost", 5))
        bt.configure(cfg)
        bt.max_search_radius = max_search
        bt.append(objs)
        bt.volume = ((0, int(w) + 1), (0, int(h) + 1))
        bt.track()
        # refs are positional indices into the appended list: [0..npv) = prev,
        # [npv..) = cur. A tracklet spanning both frames links a prev to a cur cell.
        for tr in bt.tracks:
            p = c = None
            refs = list(tr.refs)
            dummy = list(tr.dummy) if hasattr(tr, "dummy") else [False] * len(refs)
            for ref, dum in zip(refs, dummy):
                if dum or ref < 0:
                    continue
                if ref < npv:
                    p = pids[ref]
                else:
                    ci = ref - npv
                    if 0 <= ci < len(cids):
                        c = cids[ci]
            if p is not None and c is not None:
                cur_to_prev[c] = p
    return cids, [cur_to_prev[cid] for cid in cids]


def handle_track(req):
    t0 = time.time()
    prev = _load_mask(req["prev_mask"])
    cur = _load_mask(req["cur_mask"])
    cids, linked = track_btrack(prev, cur, req.get("params", {}) or {})
    dtm = (time.time() - t0) * 1000.0
    nprev = len(np.unique(prev)) - (1 if 0 in prev else 0)
    _log("track: %d prev, %d cur cells in %.0f ms" % (nprev, len(cids), dtm))
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
            self._send({"ok": True, "version": SCRIPT_VERSION, "trackers": ["btrack"], "op": "ping"})
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
    allow_reuse_address = (os.name != "nt")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=59715)
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
