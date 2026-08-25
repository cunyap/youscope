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
"""Persistent CellProfiler (Mode A) quantification backend for YouScope.

Mode A = fixed measurement modules. Given a label mask and one or more channel
images, this runs a small, fixed set of CellProfiler measurement modules
(MeasureObjectSizeShape, MeasureObjectIntensity, MeasureTexture, optionally
MeasureGranularity) on the *already-segmented* objects and returns per-cell
feature columns. No pipeline file, no object identification but the objects come
from our mask.

Wire protocol matches the cell-detection backend (length-prefixed JSON; images as
raw little-endian temp files; results zlib+base64).

"""
SCRIPT_VERSION = "v0.1.3"

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
_LOG_PATH = os.environ.get("CELLPROFILER_SERVER_LOG", "")

# object + module names used inside the transient pipeline
_OBJECT_NAME = "Cells"


def _log(msg):
    line = "[cellprofiler_server %s] %s" % (SCRIPT_VERSION, msg)
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
    img = np.frombuffer(raw, dtype=dt).reshape(meta["height"], meta["width"]).astype(np.int32)
    return _orient(img, meta)


def _load_channel(meta):
    dt = {1: "<u1", 2: "<u2", 4: "<u4"}[meta["bpp"]]
    with open(meta["path"], "rb") as f:
        raw = f.read()
    img = np.frombuffer(raw, dtype=dt).reshape(meta["height"], meta["width"]).astype(np.float64)
    return _orient(img, meta)


def _orient(img, meta):
    if meta.get("ty"):
        img = np.flipud(img)
    if meta.get("tx"):
        img = np.fliplr(img)
    if meta.get("sw"):
        img = img.T
    return np.ascontiguousarray(img)


def _normalize01(img):
    """CellProfiler expects float images in [0, 1]. Scale by the channel's bit
    depth so intensity features are comparable frame-to-frame."""
    m = float(img.max()) if img.size else 0.0
    if m <= 0:
        return img.astype(np.float32)
    # nearest power-of-two-ish max for the bit depth; fall back to observed max
    for depth in (255.0, 4095.0, 65535.0):
        if m <= depth:
            return (img / depth).astype(np.float32)
    return (img / m).astype(np.float32)


# CellProfiler is imported LAZILY, on the first quantify request and not at module
# load. Importing cellprofiler_core is heavy and, in some installs, starts a JVM
# (javabridge) that can hang or hard-crash the interpreter; doing that at import
# time would prevent the server from ever binding its port, which surfaces to the
# Java side as an opaque "connection refused". Deferring it means the server binds
# immediately (ping works) and any CellProfiler import problem is reported cleanly
# over the wire on the first quantify instead.
_CP = {"loaded": False, "ok": False, "error": None}


def _ensure_cp():
    if _CP["loaded"]:
        return _CP
    _CP["loaded"] = True
    try:
        import cellprofiler_core.object as _cpo
        import cellprofiler_core.image as _cpi
        import cellprofiler_core.measurement as _cpm
        import cellprofiler_core.workspace as _cpw
        import cellprofiler_core.pipeline as _cpp
        from cellprofiler_core.image import ImageSetList as _ImageSetList
        _CP.update({"ok": True, "cpo": _cpo, "cpi": _cpi, "cpm": _cpm,
                    "cpw": _cpw, "cpp": _cpp, "ImageSetList": _ImageSetList})
        _log("CellProfiler imported successfully")
    except BaseException as exc:  # catch SystemExit etc. too, keep the server alive
        import traceback
        _CP.update({"ok": False, "error": traceback.format_exc()})
        _log("CellProfiler import FAILED: %r" % (exc,))
    return _CP


def _make_module(kind, channels):
    """Instantiate + configure one measurement module for our object set."""
    if kind == "size_shape":
        from cellprofiler.modules.measureobjectsizeshape import MeasureObjectSizeShape
        m = MeasureObjectSizeShape()
        # objects to measure
        try:
            m.object_groups[0].name.value = _OBJECT_NAME
        except Exception:
            m.objects_list.value = _OBJECT_NAME
        return m
    if kind == "intensity":
        from cellprofiler.modules.measureobjectintensity import MeasureObjectIntensity
        m = MeasureObjectIntensity()
        m.images_list.value = ",".join(channels)
        try:
            m.objects_list.value = _OBJECT_NAME
        except Exception:
            m.object_groups[0].name.value = _OBJECT_NAME
        return m
    if kind == "texture":
        from cellprofiler.modules.measuretexture import MeasureTexture
        m = MeasureTexture()
        m.images_list.value = ",".join(channels)
        m.objects_list.value = _OBJECT_NAME
        try:
            m.scale_groups[0].scale.value = 3
        except Exception:
            pass
        return m
    if kind == "granularity":
        from cellprofiler.modules.measuregranularity import MeasureGranularity
        m = MeasureGranularity()
        m.images_list.value = ",".join(channels)
        try:
            m.objects_list.value = _OBJECT_NAME
        except Exception:
            pass
        return m
    raise ValueError("unknown module kind %r" % (kind,))


def _run_cellprofiler(label, channel_imgs, channel_names, module_kinds, requested):
    """Run the fixed measurement modules on the given objects and channels.
    If `requested` is a non-empty list of CellProfiler feature names (without the
    'cell.cp.' prefix), the result columns are exactly those, in that order (NaN
    where a feature was not produced). This keeps the YouScope table columns
    identical at config time and run time regardless of CellProfiler version.
    Returns (feature_names, ids, values[n_cells, n_features])."""
    cp = _ensure_cp()
    _cpo = cp["cpo"]; _cpi = cp["cpi"]; _cpm = cp["cpm"]
    _cpw = cp["cpw"]; _cpp = cp["cpp"]; _ImageSetList = cp["ImageSetList"]

    pipeline = _cpp.Pipeline()
    measurements = _cpm.Measurements()
    object_set = _cpo.ObjectSet()

    objects = _cpo.Objects()
    objects.segmented = label.astype(np.int32)
    object_set.add_objects(objects, _OBJECT_NAME)

    image_set_list = _ImageSetList()
    image_set = image_set_list.get_image_set(0)
    for name, img in zip(channel_names, channel_imgs):
        image_set.add(name, _cpi.Image(_normalize01(img)))

    n_cells = int(label.max())
    measurements.add_measurement("Image", "Count_%s" % _OBJECT_NAME, n_cells)

    modules = [_make_module(k, channel_names) for k in module_kinds]
    for idx, module in enumerate(modules, start=1):
        module.module_num = idx
        pipeline.add_module(module)
    for module in modules:
        workspace = _cpw.Workspace(pipeline, module, image_set, object_set,
                                   measurements, image_set_list)
        module.run(workspace)

    produced = {f: np.asarray(measurements.get_measurement(_OBJECT_NAME, f), dtype=np.float64)
                for f in measurements.get_feature_names(_OBJECT_NAME)}

    if requested:
        feature_names = list(requested)
    else:
        feature_names = sorted(produced.keys())

    n = n_cells
    values = np.full((n, len(feature_names)), np.nan, dtype=np.float64)
    produced_keys = list(produced.keys())
    for c, feat in enumerate(feature_names):
        col = produced.get(feat)
        if col is None:
            # No exact match: try a prefix match. CellProfiler suffixes texture
            # features with scale + gray-levels (e.g. Texture_Contrast_Cit_3_256),
            # which is version-dependent, so we request the un-suffixed name and
            # bind it to whatever suffixed feature the installed version produced.
            hits = sorted(k for k in produced_keys if k.startswith(feat + "_"))
            if hits:
                col = produced[hits[0]]
        if col is None:
            continue
        m = min(len(col), n)
        values[:m, c] = col[:m]
    ids = list(range(1, n + 1))
    feature_names = ["cell.cpro." + f for f in feature_names]
    return feature_names, ids, values


def handle_quantify(req):
    cp = _ensure_cp()
    if not cp["ok"]:
        return {"ok": False, "version": SCRIPT_VERSION,
                "error": "CellProfiler could not be imported in this env:\n%s" % cp["error"]}
    t0 = time.time()
    label = _load_mask(req["mask"])
    channels = req.get("channels", []) or []
    channel_imgs = []
    channel_names = []
    for i, cm in enumerate(channels):
        channel_imgs.append(_load_channel(cm))
        channel_names.append(cm.get("suffix") or ("ch%d" % i))

    params = req.get("params", {}) or {}
    kinds = params.get("modules") or ["size_shape", "intensity", "texture"]
    if not channel_names:
        # no channels: intensity/texture have nothing to measure; shape only
        kinds = [k for k in kinds if k == "size_shape"] or ["size_shape"]
    requested = params.get("requested_features") or []

    names, ids, values = _run_cellprofiler(label, channel_imgs, channel_names, kinds, requested)
    dtm = (time.time() - t0) * 1000.0
    _log("quantify: %d cells x %d features (%s) in %.0f ms" %
         (len(ids), len(names), "+".join(kinds), dtm))
    return {
        "ok": True, "version": SCRIPT_VERSION, "op": "quantify",
        "cell_count": len(ids), "cols_count": len(names), "timing_ms": round(dtm, 1),
        "columns_names": "|".join(names),
        "ids_b64": _pack(np.asarray(ids, dtype="<i4")),
        "values_b64": _pack(np.asarray(values, dtype="<f8")),
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
            self._send({"ok": True, "version": SCRIPT_VERSION, "quantifiers": ["cellprofiler"],
                        "cellprofiler_loaded": _CP["loaded"], "cellprofiler_ok": _CP["ok"],
                        "note": "CellProfiler is imported on first quantify", "op": "ping"})
            return
        if op == "shutdown":
            self._send({"ok": True, "version": SCRIPT_VERSION, "op": "shutdown"})
            _log("shutdown requested - exiting")
            threading.Thread(target=self.server.shutdown, daemon=True).start()
            return
        try:
            with _lock:
                if op == "quantify":
                    self._send(handle_quantify(req))
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
    parser.add_argument("--port", type=int, default=59716)
    parser.add_argument("--idle-timeout", type=int, default=0)
    args = parser.parse_args()

    _log("SCRIPT_VERSION %s - starting up (CellProfiler imported lazily on first request)" % SCRIPT_VERSION)
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
