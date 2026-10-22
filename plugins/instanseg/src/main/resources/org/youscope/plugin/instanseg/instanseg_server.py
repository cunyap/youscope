# /*******************************************************************************
#  * Copyright © 2026 ETH Zürich
#  *
#  * This program and the accompanying materials are made available under the
#  * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
#  * License, Version 2.1 (or at your option any later version).
#  *
#  * You may choose which license you wish to use when copying, modifying,
#  * or distributing this software.
#  *
#  * Licenses are available at:
#  * - Apache 2.0: http://apache.org
#  * - LGPL 2.1:   https://gnu.org
#  *
#  * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
#  *
#  * Contributors:
#  *     Andreas P. Cuny - initial API and implementation
#  ******************************************************************************/
#
# Persistent InstanSeg worker for YouScope (own standalone backend).
#
# Loading an InstanSeg TorchScript model (and importing torch) takes seconds;
# However, the first use of a model additionally downloads its weights. This server keeps
# torch and the model(s) warm across frames and whole measurements.
#
# Wire protocol (same as every YouScope analysis backend):
#     [4-byte big-endian uint32 length][UTF-8 JSON]   in both directions.
# Input images are handed over as raw LITTLE-endian temp files (path and geometry in
# the JSON, see PythonBackendSupport.imageMetaJson). Results come back
# zlib+base64 inside the response JSON.
#
# Ops:  ping | shutdown | load | segment
#
#
# Usage: python -u instanseg_server.py --port 59718 [--host 127.0.0.1]
#                                      [--idle-timeout 7200]

SCRIPT_VERSION = "v0.1.0"

import os
import sys
import json
import zlib
import base64
import struct
import argparse
import threading
import socketserver
import time

import numpy as np

_last_activity = time.time()
_lock = threading.Lock()

# (model_name, device) -> InstanSeg instance
_models = {}
_env_text = ""      # environment snapshot (built once, lazily)
_req_text = ""      # pip freeze (built once, lazily)

# Images above this many pixels are tiled automatically in "auto" mode
# (mirrors InstanSeg's own small_image_threshold).
SMALL_IMAGE_PIXELS = 3 * 1500 * 1500


def _log(msg):
    sys.stdout.write("[instanseg_server %s] %s\n" % (SCRIPT_VERSION, msg))
    sys.stdout.flush()


def _pack(arr_or_bytes):
    """ndarray / bytes -> zlib+base64 string for embedding in JSON."""
    if isinstance(arr_or_bytes, (bytes, bytearray)):
        raw = bytes(arr_or_bytes)
    else:
        raw = np.ascontiguousarray(arr_or_bytes).tobytes()
    return base64.b64encode(zlib.compress(raw, 6)).decode("ascii")


# Model handling (lazy heavy imports)

def _resolve_device(device):
    """'auto'/'' -> None (InstanSeg picks cuda > mps > cpu); otherwise as given."""
    d = (device or "cpu").strip().lower()
    return None if d in ("", "auto") else d


def get_model(model_name, device):
    """Returns a cached InstanSeg instance, loading (and downloading) it on first use."""
    key = (model_name, device or "auto")
    if key in _models:
        return _models[key], 0.0
    t0 = time.time()
    _log("importing torch / instanseg ...")
    try:
        import torch  # noqa: F401
        from instanseg import InstanSeg
    except Exception as exc:
        raise RuntimeError(
            "Could not import InstanSeg in this Python environment (%s: %s). "
            "Install it with:  pip install instanseg-torch" % (type(exc).__name__, exc))
    _log("loading model '%s' (device=%s) - the first use of a model downloads its "
         "weights ..." % (model_name, device or "auto"))
    inst = InstanSeg(model_name, device=_resolve_device(device), verbosity=0)
    _models[key] = inst
    dt = time.time() - t0
    _log("model ready in %.1fs (device=%s, cells_and_nuclei=%s, model pixel size=%s)"
         % (dt, getattr(inst, "inference_device", "?"),
            getattr(inst.instanseg, "cells_and_nuclei", "?"),
            getattr(inst.instanseg, "pixel_size", "?")))
    return inst, dt


def _model_info(inst):
    info = {"device": str(getattr(inst, "inference_device", "?"))}
    try:
        info["cells_and_nuclei"] = bool(inst.instanseg.cells_and_nuclei)
    except Exception:
        info["cells_and_nuclei"] = False
    try:
        info["model_pixel_size"] = float(inst.instanseg.pixel_size)
    except Exception:
        info["model_pixel_size"] = 0.0
    return info


def collect_env(model_name, device):
    """Reproducible environment snapshot and pip freeze, built once per server."""
    global _env_text, _req_text
    if _env_text:
        return
    env = {"python": sys.version.split()[0], "executable": sys.executable,
           "cpu_count": os.cpu_count(), "server_version": SCRIPT_VERSION,
           "model": model_name, "device_requested": device}
    for mod, attr in (("numpy", "__version__"), ("scipy", "__version__"),
                      ("skimage", "__version__"), ("torch", "__version__"),
                      ("instanseg", "__version__")):
        try:
            m = __import__(mod)
            env[mod + "_version"] = getattr(m, attr, "?")
        except Exception:
            env[mod + "_version"] = "not-installed"
    try:
        import torch
        env["torch_cuda_available"] = bool(torch.cuda.is_available())
        env["torch_threads"] = int(torch.get_num_threads())
    except Exception:
        pass
    _env_text = "# InstanSeg CPython environment snapshot\n" + \
        "".join("%s=%s\n" % (k, v) for k, v in env.items())
    try:
        import subprocess
        fr = subprocess.run([sys.executable, "-m", "pip", "freeze"],
                            capture_output=True, text=True, timeout=60)
        if fr.returncode == 0:
            _req_text = fr.stdout
    except Exception as exc:
        _log("pip freeze skipped: %s" % exc)


# Image / label helpers

def load_image(meta):
    """Raw little-endian file -> oriented float32 (H, W), native intensity values.
    Orientation flags are applied exactly like the other YouScope backends
    (flipud for ty, fliplr for tx, transpose for sw), so the returned label image
    lives in the same oriented space as every other segmenter's output."""
    with open(meta["path"], "rb") as f:
        raw = f.read()
    bpp = int(meta["bpp"])
    dt = {1: "<u1", 2: "<u2", 4: "<u4"}.get(bpp)
    if dt is None:
        raise ValueError("Only 8/16/32-bit grayscale images are supported (bpp=%r)." % bpp)
    w, h = int(meta["width"]), int(meta["height"])
    flat = np.frombuffer(raw, dtype=np.dtype(dt))
    if flat.size != w * h:
        raise ValueError("Pixel buffer holds %d values but width*height = %d." % (flat.size, w * h))
    img = flat.reshape(h, w).astype(np.float32)
    if meta.get("ty"):
        img = np.flipud(img)
    if meta.get("tx"):
        img = np.fliplr(img)
    if meta.get("sw"):
        img = img.T
    return np.ascontiguousarray(img)


def relabel_sequential(lab):
    """Compacts label ids to 1..N (cell.index must equal the id in the mask).
    Returns (int32 labels, N)."""
    lab = np.rint(lab).astype(np.int64)
    ids = np.unique(lab)
    ids = ids[ids > 0]
    n = int(ids.size)
    if n == 0:
        return np.zeros(lab.shape, dtype=np.int32), 0
    lut = np.zeros(int(ids.max()) + 1, dtype=np.int32)
    lut[ids] = np.arange(1, n + 1, dtype=np.int32)
    return lut[np.clip(lab, 0, None)], n


def core_geometry(lab, n):
    """Per-cell core geometry for labels 1..n.
    Returns (ids int32[n], vals float64[n,4]) with columns
    center.x (col), center.y (row), area, perimeter (all pixel units, 0-based
    centroid). Perimeter is NaN if unavailable."""
    ids = np.arange(1, n + 1, dtype=np.int32)
    vals = np.full((n, 4), np.nan, dtype=np.float64)
    if n == 0:
        return ids, vals
    try:
        from skimage import measure
        t = measure.regionprops_table(
            lab, properties=("label", "area", "centroid", "perimeter"))
        order = np.argsort(t["label"])
        vals[:, 0] = np.asarray(t["centroid-1"])[order]      # x = column
        vals[:, 1] = np.asarray(t["centroid-0"])[order]      # y = row
        vals[:, 2] = np.asarray(t["area"])[order]
        vals[:, 3] = np.asarray(t["perimeter"])[order]
        return np.asarray(t["label"])[order].astype(np.int32), vals
    except Exception as exc:
        _log("skimage regionprops unavailable (%s) - using numpy fallback "
             "(perimeter = boundary pixel count)" % exc)
    flat = lab.ravel()
    area = np.bincount(flat, minlength=n + 1)[1:n + 1].astype(np.float64)
    yy, xx = np.indices(lab.shape)
    sx = np.bincount(flat, weights=xx.ravel(), minlength=n + 1)[1:n + 1]
    sy = np.bincount(flat, weights=yy.ravel(), minlength=n + 1)[1:n + 1]
    with np.errstate(invalid="ignore", divide="ignore"):
        vals[:, 0] = sx / area
        vals[:, 1] = sy / area
    vals[:, 2] = area
    pad = np.pad(lab, 1)
    inner = ((pad[1:-1, 1:-1] == pad[:-2, 1:-1]) & (pad[1:-1, 1:-1] == pad[2:, 1:-1]) &
             (pad[1:-1, 1:-1] == pad[1:-1, :-2]) & (pad[1:-1, 1:-1] == pad[1:-1, 2:]))
    edge = lab * (~inner)
    vals[:, 3] = np.bincount(edge.ravel(), minlength=n + 1)[1:n + 1]
    return ids, vals


# Ops

def handle_load(req):
    """Preloads (and downloads, if needed) a model so the first frame is fast."""
    p = req.get("params", {}) or {}
    model_name = p.get("model", "fluorescence_nuclei_and_cells")
    device = p.get("device", "cpu")
    t0 = time.time()
    inst, load_s = get_model(model_name, device)
    info = _model_info(inst)
    return {"ok": True, "version": SCRIPT_VERSION, "op": "load",
            "model": model_name, "device": info["device"],
            "cells_and_nuclei": info["cells_and_nuclei"],
            "model_pixel_size": info["model_pixel_size"],
            "init_ms": round(load_s * 1000.0, 1),
            "timing_ms": round((time.time() - t0) * 1000.0, 1)}


def handle_segment(req):
    import torch

    t_start = time.time()
    p = req.get("params", {}) or {}
    model_name = p.get("model", "fluorescence_nuclei_and_cells")
    device = p.get("device", "cpu")
    target = p.get("target", "nuclei")
    pixel_size = float(p.get("pixel_size", 0.0) or 0.0)
    processing = p.get("processing", "auto")
    tile_size = int(p.get("tile_size", 512))
    normalise = bool(p.get("normalise", True))
    invert = bool(p.get("invert", False))
    in_channels = int(p.get("input_channels", 0) or 0)
    generate_label = bool(p.get("generate_label", True))
    threads = int(p.get("threads", 0) or 0)
    warnings = []

    if threads > 0:
        try:
            torch.set_num_threads(threads)
        except Exception:
            pass

    inst, init_s = get_model(model_name, device)
    info = _model_info(inst)
    if p.get("want_env"):
        collect_env(model_name, device)

    img = load_image(req["image"])
    h, w = img.shape
    _log("segment: %dx%d, model=%s, target=%s, processing=%s" % (w, h, model_name, target, processing))

    if invert:
        img = (float(img.max()) + float(img.min())) - img

    # Channel handling. Brightfield (H&E-style) models expect 3 channels; the
    # fluorescence / single-channel models take a single grayscale channel.
    if in_channels <= 0:
        in_channels = 3 if "brightfield" in model_name.lower() else 1
    chw = np.repeat(img[None, :, :], in_channels, axis=0) if in_channels > 1 else img[None, :, :]
    tensor = torch.from_numpy(np.ascontiguousarray(chw, dtype=np.float32))

    # Target: a nuclei-only model has no cell channel.
    if target not in ("nuclei", "cells"):
        target = "nuclei"
    if target == "cells" and not info["cells_and_nuclei"]:
        warnings.append("Model '%s' only segments nuclei; 'cells' target ignored." % model_name)
        target = "nuclei"

    kw = {}
    for key in ("seed_threshold", "overlap_threshold", "mask_threshold", "mean_threshold",
                "fg_threshold"):
        v = p.get(key)
        if v is not None and float(v) >= 0:
            kw[key] = float(v)
    for key in ("min_size", "peak_distance"):
        v = p.get(key)
        if v is not None and int(v) >= 0:
            kw[key] = int(v)
    for key in ("cleanup_fragments", "resolve_cell_and_nucleus"):
        if key in p and p[key] is not None:
            kw[key] = bool(p[key])

    ps = pixel_size if pixel_size > 0 else None
    tiled = (processing == "tiled") or (processing == "auto" and h * w > SMALL_IMAGE_PIXELS)

    t_inf = time.time()
    with torch.no_grad():
        if tiled:
            out = inst.eval_medium_image(tensor, pixel_size=ps, normalise=normalise,
                                         tile_size=tile_size, batch_size=1,
                                         return_image_tensor=False, target=target, **kw)
        else:
            out = inst.eval_small_image(tensor, pixel_size=ps, normalise=normalise,
                                        return_image_tensor=False, target=target, **kw)
    out = out.detach().cpu().numpy() if hasattr(out, "detach") else np.asarray(out)
    infer_s = time.time() - t_inf

    # out: [B, C, H, W] (or [C, H, W]); pick the requested channel.
    while out.ndim > 3:
        out = out[0]
    if out.shape[0] > 1:
        lab = out[0 if target == "nuclei" else 1]
    else:
        lab = out[0]
    if lab.shape != (h, w):
        # Defensive: output must match the (oriented) input. Nearest-neighbour resize.
        from scipy.ndimage import zoom
        lab = zoom(lab, (h / lab.shape[0], w / lab.shape[1]), order=0)
        warnings.append("Label output resized to input size.")

    lab, n = relabel_sequential(lab)
    if n > 65535:
        raise RuntimeError("InstanSeg found %d cells; more than 65535 labels are not supported." % n)

    ids, vals = core_geometry(lab, n)
    dt_ms = (time.time() - t_start - init_s) * 1000.0     # steady-state: excludes model init

    resp = {"ok": True, "version": SCRIPT_VERSION, "op": "segment",
            "n_cells": n, "width": int(w), "height": int(h),
            "target": target, "tiled": bool(tiled), "device": info["device"],
            "timing_ms": round(dt_ms, 1), "inference_ms": round(infer_s * 1000.0, 1),
            "init_ms": round(init_s * 1000.0, 1),
            "ids_b64": _pack(ids.astype("<i4")),
            "values_b64": _pack(vals.astype("<f8")),
            "has_label": False}
    if warnings:
        resp["warning"] = " ".join(warnings)
    if generate_label:
        resp["has_label"] = True
        resp["label_b64"] = _pack(lab.astype("<u2"))       # row-major (H, W)
    if p.get("want_env") and _env_text:
        resp["env_b64"] = _pack(_env_text.encode("utf-8"))
        if _req_text:
            resp["req_b64"] = _pack(_req_text.encode("utf-8"))
    _log("segment done: %d cells in %.0f ms (inference %.0f ms%s)"
         % (n, dt_ms, infer_s * 1000.0, ", model init %.1fs" % init_s if init_s else ""))
    return resp


# Server plumbing

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
            self._send({"ok": True, "version": SCRIPT_VERSION, "op": "ping",
                        "algorithms": ["instanseg"], "stages": ["segment"]})
            return
        if op == "shutdown":
            self._send({"ok": True, "version": SCRIPT_VERSION, "op": "shutdown"})
            _log("shutdown requested - exiting")
            threading.Thread(target=self.server.shutdown, daemon=True).start()
            return
        try:
            with _lock:                       # serialise heavy work
                if op == "segment":
                    self._send(handle_segment(req))
                elif op == "load":
                    self._send(handle_load(req))
                else:
                    self._send({"ok": False, "error": "unknown op %r" % (op,),
                                "version": SCRIPT_VERSION})
        except Exception as exc:
            import traceback
            tb = traceback.format_exc()
            _log("ERROR:\n" + tb)
            # One-line, human-readable "error" (the Java client shows it verbatim and
            # its JSON helper flattens newlines); the full traceback is in this
            # server's log and in the separate "traceback" field.
            msg = ("%s: %s" % (type(exc).__name__, exc)).replace("\r", " ").replace("\n", " | ")
            self._send({"ok": False, "error": msg, "traceback": tb[-1500:],
                        "version": SCRIPT_VERSION})
        finally:
            _last_activity = time.time()

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
    # False on Windows: SO_REUSEADDR there lets a second server bind a live port.
    allow_reuse_address = (os.name != "nt")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=59718)
    parser.add_argument("--idle-timeout", type=int, default=0,
                        help="exit after this many seconds without a request (0 = never)")
    args = parser.parse_args()

    _log("SCRIPT_VERSION %s - starting up" % SCRIPT_VERSION)
    # Bind first (before any heavy import) so the Java side sees the server at once.
    server = None
    for attempt in range(6):
        try:
            server = ThreadingTCPServer((args.host, args.port), Handler)
            break
        except OSError as exc:
            _log("bind %s:%d failed (%s); retry %d/6" % (args.host, args.port, exc, attempt + 1))
            time.sleep(0.5)
    if server is None:
        _log("could not bind %s:%d - another server is probably already there; exiting."
             % (args.host, args.port))
        sys.exit(0)

    if args.idle_timeout > 0:
        def _watch():
            while True:
                time.sleep(15)
                if time.time() - _last_activity > args.idle_timeout and not _lock.locked():
                    _log("idle for >%ds - exiting" % args.idle_timeout)
                    server.shutdown()
                    return
        threading.Thread(target=_watch, daemon=True).start()

    _log("READY - listening on %s:%d (torch/instanseg are imported on first use)"
         % (args.host, args.port))
    try:
        server.serve_forever()
    finally:
        server.server_close()
        _log("stopped")


if __name__ == "__main__":
    main()
