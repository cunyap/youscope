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
"""Persistent TrackMate tracking backend for YouScope.

Frame-to-frame linking only: each 'track' request builds a two-frame TrackMate
SpotCollection from the previous plus current cell centroids and runs TrackMate's
SparseLAPFrameToFrameTracker (the frame-to-frame LAP linker) to link them. Returns,
per current cell, the previous LABEL id it links to (0 = new). The Java addon maps
label ids to persistent track ids.

Wire protocol matches the cell-detection backend (length-prefixed JSON; masks as
raw little-endian temp files; results zlib+base64).
"""
SCRIPT_VERSION = "v0.1.2"

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
_LOG_PATH = os.environ.get("TRACKMATE_SERVER_LOG", "")

# pyimagej endpoint; overridable via env for a pinned/offline Fiji.
_IJ_ENDPOINT = os.environ.get("TRACKMATE_FIJI_ENDPOINT", "sc.fiji:fiji")


def _log(msg):
    line = "[trackmate_server %s] %s" % (SCRIPT_VERSION, msg)
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


# ImageJ and TrackMate classes are resolved LAZILY on first use and NOT at import 
# so the server binds its port before the (slow, JVM-starting) init runs.
_TM = {"loaded": False, "ok": False, "error": None}


def _ensure_trackmate():
    if _TM["loaded"]:
        return _TM
    _TM["loaded"] = True
    try:
        import imagej
        from scyjava import jimport
        _log("initialising ImageJ/Fiji (%s) - first run may download Fiji..." % _IJ_ENDPOINT)
        ij = imagej.init(_IJ_ENDPOINT, mode="headless")

        # TrackMate 7+ package layout. If your TrackMate is older, the tracker is
        # in fiji.plugin.trackmate.tracking.sparselap instead of ...tracking.jaqaman.
        Spot = jimport("fiji.plugin.trackmate.Spot")
        SpotCollection = jimport("fiji.plugin.trackmate.SpotCollection")
        try:
            SparseLAP = jimport("fiji.plugin.trackmate.tracking.jaqaman.SparseLAPFrameToFrameTracker")
            keys_pkg = "fiji.plugin.trackmate.tracking.jaqaman.LAPUtils"
        except Exception:
            SparseLAP = jimport("fiji.plugin.trackmate.tracking.sparselap.SparseLAPFrameToFrameTracker")
            keys_pkg = "fiji.plugin.trackmate.tracking.LAPUtils"
        TrackerKeys = jimport("fiji.plugin.trackmate.tracking.TrackerKeys")
        JHashMap = jimport("java.util.HashMap")
        JDouble = jimport("java.lang.Double")
        JInteger = jimport("java.lang.Integer")

        _TM.update({"ok": True, "ij": ij, "Spot": Spot, "SpotCollection": SpotCollection,
                    "SparseLAP": SparseLAP, "TrackerKeys": TrackerKeys,
                    "HashMap": JHashMap, "Double": JDouble, "Integer": JInteger,
                    "keys_pkg": keys_pkg})
        _log("ImageJ/TrackMate ready")
    except BaseException as exc:  # keep the server alive whatever happens
        import traceback
        _TM.update({"ok": False, "error": traceback.format_exc()})
        _log("TrackMate init FAILED: %r" % (exc,))
    return _TM


def _run_trackmate(prev_ids, prev_cen, cur_ids, cur_cen, linking_max_distance):
    """Link current spots to previous spots with TrackMate's frame-to-frame LAP
    tracker. Returns a dict {cur_label_id: prev_label_id_or_0}."""
    tm = _ensure_trackmate()
    if not tm["ok"]:
        raise RuntimeError("TrackMate/Fiji unavailable:\n%s" % tm["error"])
    Spot = tm["Spot"]; SpotCollection = tm["SpotCollection"]
    SparseLAP = tm["SparseLAP"]; TrackerKeys = tm["TrackerKeys"]
    HashMap = tm["HashMap"]; Double = tm["Double"]; Integer = tm["Integer"]

    spots = SpotCollection()
    spot_to_label = {}   # TrackMate spot name -> ('p'/'c', label id)
    RADIUS = 5.0
    QUALITY = 1.0

    def add(cen, ids, frame, tag):
        jframe = Integer(int(frame))
        for i in range(len(ids)):
            s = Spot(float(cen[i, 0]), float(cen[i, 1]), 0.0, RADIUS, QUALITY)
            s.putFeature("FRAME", Double(float(frame)))
            spots.add(s, jframe)   # SpotCollection.add(Spot, java.lang.Integer)
            spot_to_label[s.ID()] = (tag, ids[i])

    add(prev_cen, prev_ids, 0, "p")
    add(cur_cen, cur_ids, 1, "c")

    settings = HashMap()
    settings.put(TrackerKeys.KEY_LINKING_MAX_DISTANCE, Double(float(linking_max_distance)))
    settings.put(TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double(1.05))

    tracker = SparseLAP(spots, settings)
    if not tracker.checkInput() or not tracker.process():
        raise RuntimeError("TrackMate tracker failed: %s" % tracker.getErrorMessage())
    graph = tracker.getResult()

    cur_to_prev = {cid: 0 for cid in cur_ids}
    for edge in graph.edgeSet():
        s1 = graph.getEdgeSource(edge)
        s2 = graph.getEdgeTarget(edge)
        a = spot_to_label.get(s1.ID())
        b = spot_to_label.get(s2.ID())
        if a is None or b is None:
            continue
        # one endpoint is a previous spot, the other a current spot
        if a[0] == "p" and b[0] == "c":
            cur_to_prev[b[1]] = a[1]
        elif a[0] == "c" and b[0] == "p":
            cur_to_prev[a[1]] = b[1]
    return cur_to_prev


def handle_track(req):
    prev = _load_mask(req["prev_mask"])
    cur = _load_mask(req["cur_mask"])
    params = req.get("params", {}) or {}
    linking = float(params.get("linking_max_distance", 15.0))

    cids, ccen = _centroids(cur)
    if not cids:
        return _result([], [], time.time())
    pids, pcen = _centroids(prev)
    if not pids:
        return _result(cids, [0] * len(cids), time.time())

    # Warm up Fiji/TrackMate before timing so the reported time is the per-frame
    # linking cost, not the one-time JVM+Fiji init (which is logged separately).
    tm = _ensure_trackmate()
    if not tm["ok"]:
        raise RuntimeError("TrackMate/Fiji unavailable:\n%s" % tm["error"])

    t0 = time.time()
    cur_to_prev = _run_trackmate(pids, pcen, cids, ccen, linking)
    linked = [cur_to_prev.get(cid, 0) for cid in cids]
    return _result(cids, linked, t0)


def _result(cids, linked, t0):
    dtm = (time.time() - t0) * 1000.0
    _log("track: %d cur cells in %.0f ms" % (len(cids), dtm))
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
            self._send({"ok": True, "version": SCRIPT_VERSION, "trackers": ["trackmate"],
                        "trackmate_loaded": _TM["loaded"], "trackmate_ok": _TM["ok"],
                        "note": "Fiji/TrackMate is initialised on first track", "op": "ping"})
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
    parser.add_argument("--port", type=int, default=59717)
    parser.add_argument("--idle-timeout", type=int, default=0)
    args = parser.parse_args()

    _log("SCRIPT_VERSION %s - starting up (Fiji/TrackMate initialised lazily on first request)" % SCRIPT_VERSION)
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
