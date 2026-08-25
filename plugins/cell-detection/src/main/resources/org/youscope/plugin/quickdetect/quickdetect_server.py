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
# Persistent analysis backend for YouScope.
#
# Implements the YouScope Analysis-Backend contract
# over a local TCP socket, so the numpy/scipy/scikit-image import (~few secs) is paid
# once and reused for every detection / visualization for the life of the server
# (across whole measurements), instead of per frame.
#
# Framing: each message is
#     [4-byte big-endian uint32 length][UTF-8 JSON].
# Large input images are handed over via a temp FILE whose path is in the request
# JSON (local, fast, robust). Result label / rgb images are returned compressed
# (zlib + base64) INSIDE the response JSON, so the client needs no binary payload
# channel and no JSON-array parsing.
#
# Ops:  ping | shutdown | detect | visualize
#
# Usage: python -u quickdetect_server.py --port 59713 [--host 127.0.0.1]
#                                        [--idle-timeout 1800]

SCRIPT_VERSION = "v0.7.4"

import os
import sys
import json
import zlib
import base64
import struct
import socket
import argparse
import threading
import socketserver
import time

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cell_detection_cpython as cd

# Quantification helpers live inside cell_detection_cpython (cd). Prefer cd; if an
# older cd without them is somehow on disk, fall back to a standalone
# quantify_cpython module, and if neither provides them, detect/visualize still
# work and only 'quantify' fails with a clear message.
if hasattr(cd, "geometry") and hasattr(cd, "quantify"):
    qz = cd
    _QZ_IMPORT_ERROR = None
else:
    try:
        import quantify_cpython as qz
        _QZ_IMPORT_ERROR = None
    except Exception as _exc:  # pragma: no cover - defensive
        qz = None
        _QZ_IMPORT_ERROR = "cell_detection_cpython lacks geometry/quantify and %r" % (_exc,)

_last_activity = time.time()
_lock = threading.Lock()


def _log(msg):
    sys.stdout.write("[quickdetect_server %s] %s\n" % (SCRIPT_VERSION, msg))
    sys.stdout.flush()


def _load_image(meta):
    with open(meta["path"], "rb") as f:
        raw = f.read()
    return cd.decode_image(raw, meta["width"], meta["height"], meta["bpp"],
                           meta["bitdepth"], meta["tx"], meta["ty"], meta["sw"])


def _pack(arr):
    """uint8/any ndarray -> zlib+base64 string for embedding in JSON."""
    raw = np.ascontiguousarray(arr).tobytes()
    return base64.b64encode(zlib.compress(raw, 6)).decode("ascii")


def handle_detect(req):
    t0 = time.time()
    detection = _load_image(req["detection"])
    quant = [_load_image(m) for m in req.get("quant", [])]
    p = req.get("params", {})
    _log("detect: %dx%d, %d quant" % (detection.shape[1], detection.shape[0], len(quant)))

    def log(m):
        _log(m)

    pos, label, rows = cd.detect(
        detection, quant,
        threshold=p["threshold"],
        internal_binning=p["internal_binning"],
        max_cell_diameter=p["max_cell_diameter"],
        min_cell_diameter=p["min_cell_diameter"],
        log=log)
    dt = (time.time() - t0) * 1000.0

    # rows to packed binary records: <iidddd> (cellId, quantId(-1=null), x, y, area, fluo(NaN=null))
    buf = bytearray()
    for r in rows:
        cid, qid, x, y, area, fluo = r
        buf += struct.pack("<iidddd", int(cid), -1 if qid is None else int(qid),
                           float(x), float(y), float(area),
                           float("nan") if fluo is None else float(fluo))

    resp = {"ok": True, "version": SCRIPT_VERSION, "op": "detect",
            "n_cells": int(pos.shape[0]), "timing_ms": round(dt, 1),
            "rows_count": len(rows), "rows_b64": _pack(bytes(buf)), "has_label": False}
    if p.get("generate_label", True):
        h, w = label.shape
        resp["has_label"] = True
        resp["label_width"] = int(w)
        resp["label_height"] = int(h)
        resp["label_b64"] = _pack(label.astype(np.uint8))
    _log("detect done: %d cells in %.0f ms" % (pos.shape[0], dt))
    return resp


def handle_visualize(req):
    t0 = time.time()
    image = _load_image(req["image"])
    cells = req.get("cells", [])
    pos = np.zeros((len(cells), 3), dtype=np.float64)
    for i, c in enumerate(cells):
        pos[i, 0] = c["x"]
        pos[i, 1] = c["y"]
        pos[i, 2] = c.get("area", 0.0)
    rgb = cd.glow(image, pos, float(req.get("params", {}).get("glow_strength", 2.0)))
    h, w, _ = rgb.shape
    dt = (time.time() - t0) * 1000.0
    _log("visualize done in %.0f ms" % dt)
    return {"ok": True, "version": SCRIPT_VERSION, "op": "visualize",
            "rgb_width": int(w), "rgb_height": int(h), "rgb_b64": _pack(rgb),
            "timing_ms": round(dt, 1)}


def _orient_native(raw, meta):
    """Reshape raw bytes to native dtype and apply orientation flags, without the
    uint8 rescale (quantification needs the original intensity values)."""
    dt = {1: "<u1", 2: "<u2", 4: "<u4"}[meta["bpp"]]
    img = np.frombuffer(raw, dtype=dt).reshape(meta["height"], meta["width"])
    img = img.astype(np.float64)
    if meta.get("ty"):
        img = np.flipud(img)
    if meta.get("tx"):
        img = np.fliplr(img)
    if meta.get("sw"):
        img = img.T
    return img


def handle_quantify(req):
    if qz is None:
        raise RuntimeError("quantify_cpython module unavailable (%s); "
                           "cannot quantify." % _QZ_IMPORT_ERROR)
    t0 = time.time()
    m = req["mask"]
    with open(m["path"], "rb") as f:
        raw = f.read()
    label = _orient_native(raw, m).astype(np.int32)

    channels = []
    channel_names = []
    channel_bitdepths = []
    for ch in req.get("channels", []):
        with open(ch["path"], "rb") as f:
            channels.append(_orient_native(f.read(), ch))
        channel_names.append(ch.get("name", ""))
        channel_bitdepths.append(ch.get("bitdepth", 16))

    quantifier = req.get("quantifier", "intensity")
    params = dict(req.get("params", {}) or {})
    params.setdefault("channel_names", channel_names)
    params.setdefault("channel_bitdepths", channel_bitdepths)
    geo_cols, ids, geo_vals = qz.geometry(label, matlab_centroid=(quantifier == "cellx"))
    feat_cols, feat_ids, feat_vals = qz.quantify(quantifier, label, channels, params)
    # geometry and features are computed over the same label ids, in the same order
    columns = geo_cols + feat_cols
    if feat_vals.shape[1] > 0:
        values = np.concatenate([geo_vals, feat_vals], axis=1)
    else:
        values = geo_vals
    dtm = (time.time() - t0) * 1000.0
    _log("quantify(%s): %d cells, %d columns in %.0f ms" % (quantifier, len(ids), len(columns), dtm))
    return {
        "ok": True, "version": SCRIPT_VERSION, "op": "quantify", "quantifier": quantifier,
        "columns": columns,
        "columns_names": "|".join(c["name"] for c in columns),
        "columns_dtypes": "|".join(c["dtype"] for c in columns),
        "cell_count": len(ids), "cols_count": len(columns),
        "timing_ms": round(dtm, 1),
        "ids_b64": _pack(np.asarray(ids, dtype="<i4")),
        "values_b64": _pack(np.ascontiguousarray(values, dtype="<f8")),
    }


def _mask_sidecar_b64(label):
    """Pack a label image as Cellpose-style sidecar bytes (w,h big-endian uint32
    header + row-major big-endian uint16 labels), zlib+base64 for JSON."""
    label = np.asarray(label)
    h, w = label.shape[0], label.shape[1]
    header = struct.pack(">II", w, h)
    body = np.ascontiguousarray(label.astype(">u2")).tobytes()
    return base64.b64encode(zlib.compress(header + body, 6)).decode("ascii")


def handle_faster(req):
    t0 = time.time()
    image = _load_image(req["image"])
    params = req.get("params", {}) or {}
    label = cd.segment_faster(image, params.get("binary"), params.get("model"),
                              params.get("svm"), params.get("work_dir"))
    cols, ids, vals = cd.core_geometry(label)
    dtm = (time.time() - t0) * 1000.0
    _log("faster: %d cells in %.0f ms" % (len(ids), dtm))
    return {
        "ok": True, "version": SCRIPT_VERSION, "op": "faster",
        "cell_count": len(ids), "timing_ms": round(dtm, 1),
        "mask_b64": _mask_sidecar_b64(label),
        "columns_names": "|".join(cols),
        "ids_b64": _pack(np.asarray(ids, dtype="<i4")),
        "values_b64": _pack(np.asarray(vals, dtype="<f8")),
    }


def handle_track(req):
    """CellX online tracker: link previous->current label masks. Returns, per
    current cell (in ascending label-id order), the previous LABEL id it links to
    (0 = new track), so the Java side can propagate persistent track ids."""
    if qz is None or not hasattr(qz, "track_cellx"):
        raise RuntimeError("tracker unavailable (%s)." % _QZ_IMPORT_ERROR)
    t0 = time.time()
    pm = req["prev_mask"]
    cm = req["cur_mask"]
    with open(pm["path"], "rb") as f:
        prevL = _orient_native(f.read(), pm).astype(np.int32)
    with open(cm["path"], "rb") as f:
        curL = _orient_native(f.read(), cm).astype(np.int32)
    params = dict(req.get("params", {}) or {})
    if "lost" in req:
        params["lost"] = req["lost"]
    assign, prev_ids, cur_ids, cur_geom, gap_assign = qz.track_cellx(prevL, curL, params)
    linked = []
    for j in range(len(cur_ids)):
        linked.append(prev_ids[assign[j]] if assign[j] >= 0 else 0)
    dtm = (time.time() - t0) * 1000.0
    _log("track: %d prev, %d cur cells in %.0f ms" % (len(prev_ids), len(cur_ids), dtm))
    return {
        "ok": True, "version": SCRIPT_VERSION, "op": "track",
        "cur_count": len(cur_ids), "timing_ms": round(dtm, 1),
        "cur_ids_b64": _pack(np.asarray(cur_ids, dtype="<i4")),
        "linked_prev_ids_b64": _pack(np.asarray(linked, dtype="<i4")),
        "gap_track_ids_b64": _pack(np.asarray(gap_assign, dtype="<i4")),
        "cur_cx_b64": _pack(np.asarray(cur_geom["cx"], dtype="<f8")),
        "cur_cy_b64": _pack(np.asarray(cur_geom["cy"], dtype="<f8")),
        "cur_major_b64": _pack(np.asarray(cur_geom["major"], dtype="<f8")),
        "cur_orient_b64": _pack(np.asarray(cur_geom["orient"], dtype="<f8")),
    }


class Handler(socketserver.BaseRequestHandler):
    def handle(self):
        global _last_activity
        try:
            length = struct.unpack(">I", self._recv_exact(4))[0]
            req = json.loads(self._recv_exact(length).decode("utf-8"))
        except Exception as exc:
            self._send({"ok": False, "error": "bad request: %r" % (exc,),
                        "version": SCRIPT_VERSION})
            return

        op = req.get("op", req.get("cmd"))
        _last_activity = time.time()

        if op == "ping":
            self._send({"ok": True, "version": SCRIPT_VERSION,
                        "algorithms": ["quickdetect", "glow"], "stages": ["segment", "quantify", "visualize", "track", "faster"], "quantifiers": ["intensity", "cellx"], "trackers": ["cellx"], "op": "ping"})
            return
        if op == "shutdown":
            self._send({"ok": True, "version": SCRIPT_VERSION, "op": "shutdown"})
            _log("shutdown requested - exiting")
            threading.Thread(target=self.server.shutdown, daemon=True).start()
            return
        try:
            with _lock:                              # serialize heavy numeric work
                if op == "detect":
                    self._send(handle_detect(req))
                elif op == "visualize":
                    self._send(handle_visualize(req))
                elif op == "quantify":
                    self._send(handle_quantify(req))
                elif op == "track":
                    self._send(handle_track(req))
                elif op == "faster":
                    self._send(handle_faster(req))
                else:
                    self._send({"ok": False, "error": "unknown op %r" % (op,),
                                "version": SCRIPT_VERSION})
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
    # Must be False on Windows: SO_REUSEADDR there lets a second server bind the
    # same live port, producing silent cross-talk. False makes a duplicate bind
    # fail cleanly so the Java addon reuses the existing server instead.
    allow_reuse_address = False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=59713)
    parser.add_argument("--idle-timeout", type=int, default=0,
                        help="exit after this many seconds with no request (0=never)")
    args = parser.parse_args()

    _log("SCRIPT_VERSION %s - starting up (algo %s)" % (SCRIPT_VERSION, cd.SCRIPT_VERSION))
    # Retry the bind briefly: when the Java addon restarts a stale server, the old
    # port may linger for a moment. If a different live server truly owns the port,
    # all attempts fail and we exit cleanly so the addon reuses that one.
    server = None
    for _attempt in range(6):
        try:
            server = ThreadingTCPServer((args.host, args.port), Handler)
            break
        except OSError as exc:
            _log("bind %s:%d failed (%s); retry %d/6" % (args.host, args.port, exc, _attempt + 1))
            time.sleep(0.5)
    if server is None:
        _log("could not bind %s:%d - another server is probably already there; exiting."
             % (args.host, args.port))
        sys.exit(0)

    # Optional idle watchdog so orphaned servers do not linger forever.
    if args.idle_timeout > 0:
        def _watch():
            while True:
                time.sleep(15)
                if time.time() - _last_activity > args.idle_timeout:
                    _log("idle for >%ds - exiting" % args.idle_timeout)
                    server.shutdown()
                    return
        threading.Thread(target=_watch, daemon=True).start()

    _log("READY - listening on %s:%d" % (args.host, args.port))
    try:
        server.serve_forever()
    finally:
        server.server_close()
        _log("stopped")


if __name__ == "__main__":
    main()
