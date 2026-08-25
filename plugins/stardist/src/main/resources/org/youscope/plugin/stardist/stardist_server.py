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
# stardist_server.py - Persistent StarDist worker for YouScope
#
# WHY:
# Loading StarDist2D(pretrained_model='2D_versatile_he') takes several seconds
# (downloading/reading the model weights from disk, moving to
# device). Calling this fresh in a one-shot subprocess on every single
# detectCells() call makes live-cell / feedback microscopy with fast
# imaging cycles infeasible.
#
# This script keeps the model loaded in memory and listens on a local
# TCP socket. StarDistAddon.java starts this once (or reuses a running
# instance) and sends each image as a lightweight request instead of
# spawning a new Python process + reloading the model every time.
#
# USAGE (manual):
#   python stardist_server.py --port 59712 --model 2D_versatile_he --gpu false
#
# REMOTE GPU SERVER (optional for example when the acquisition PC has no GPU):
# StarDist on CPU can take long per full frame. If you have a separate
# machine with an NVIDIA GPU, run the server THERE and point YouScope at it:
#
#   1. On the GPU machine, in a CPython env with stardist and CUDA-enabled torch:
#        python stardist_server.py --host 0.0.0.0 --port 59712 \
#               --model 2D_versatile_he --gpu true --preload
#      (--host 0.0.0.0 accepts remote clients; open the port in the firewall.)
#   2. In YouScope's StarDist job config set "Server host" to that machine's
#      IP/hostname and a matching "Server port". A non-local host is never
#      auto-started and YouScope simply connects to it.
#   Inference then runs on the remote GPU; everything else is unchanged.
#
# YouScope starts/manages a LOCAL server automatically via StarDistAddon's
# initialize() when "Use persistent server" is enabled in the
# StarDist job configuration (recommended for measurements with
# repeated cell detection, e.g. every imaging cycle).


SCRIPT_VERSION = "v0.5.1"

import os
# CPU thread count for inference. Must be set before numpy/torch are imported to
# take effect, so it comes from an environment variable the Java addon sets from
# the "CPU cores" UI spinner (argparse would be too late). 0/empty = all cores.
_req_threads = os.environ.get("STARDIST_NUM_THREADS", "").strip()
try:
    _nt = int(_req_threads)
except ValueError:
    _nt = 0
_ncpu = str(_nt) if _nt and _nt > 0 else str(os.cpu_count() or 1)
os.environ["OMP_NUM_THREADS"] = _ncpu
os.environ["MKL_NUM_THREADS"] = _ncpu
os.environ["OPENBLAS_NUM_THREADS"] = _ncpu
os.environ["NUMEXPR_NUM_THREADS"] = _ncpu


import sys, json, struct, socket, socketserver, threading, argparse, time, base64, zlib
import numpy as np

_model = None
_stardist_version = "unknown"
_model_lock = threading.Lock()
_model_name = None
_model_gpu = False


def get_model(model_name, gpu):
    global _model, _model_name, _model_gpu, _stardist_version
    with _model_lock:
        if _model is None or _model_name != model_name or _model_gpu != gpu:
            import stardist
            from stardist.models import StarDist2D
            _stardist_version = getattr(stardist, "__version__", getattr(stardist,"version","unknown"))
            # Pin torch to all cores for CPU inference.
            try:
                import torch
                torch.set_num_threads(int(_ncpu))
                dev = "cuda" if (gpu and torch.cuda.is_available()) else "cpu"
                print("[stardist_server] stardist=%s torch=%s device=%s threads=%d"
                      % (_stardist_version, getattr(torch, "__version__", "?"),
                         dev, torch.get_num_threads()), flush=True)
            except Exception as _te:
                print("[stardist_server] torch thread setup skipped: %s" % _te, flush=True)
            print("[stardist_server] Loading model '%s' (gpu=%s)..." % (model_name, gpu),
                  flush=True)
            t0 = time.time()
            if os.path.isdir(model_name):
                model_dir=os.path.abspath(model_name)
                _model = StarDist2D(None, name=os.path.basename(model_dir), basedir=os.path.dirname(model_dir))
            else:
                _model = StarDist2D.from_pretrained(model_name)
            _model_name = model_name
            _model_gpu = gpu
            print("[stardist_server] Model loaded in %.1fs" % (time.time() - t0), flush=True)
        return _model


def compute_cell_props_fast(masks, raw_image, n_cells):
    if n_cells == 0:
        return []
    from scipy import ndimage
    labels = np.arange(1, n_cells + 1)
    areas = np.bincount(masks.ravel(), minlength=n_cells + 1)[1:n_cells + 1]
    centroids = ndimage.center_of_mass(masks, labels=masks, index=labels)
    bboxes = ndimage.find_objects(masks)
    raw_f = raw_image.astype(np.float64)
    mean_intensities = ndimage.mean(raw_f, labels=masks, index=labels)
    sum_intensities  = ndimage.sum(raw_f, labels=masks, index=labels)

    cell_props = []
    for i, label in enumerate(labels):
        area = int(areas[i])
        if area == 0:
            continue
        cy, cx = centroids[i]
        bbox = bboxes[label - 1]
        if bbox is None:
            continue
        y_slice, x_slice = bbox
        min_y, max_y = y_slice.start, y_slice.stop - 1
        min_x, max_x = x_slice.start, x_slice.stop - 1
        perim = 2 * 3.14159265 * (area / 3.14159265) ** 0.5
        circ = (4 * 3.14159265 * area / (perim ** 2)) if perim > 0 else 0.0
        cell_props.append({
            "id": int(label), "area": area,
            "center_x": float(cx), "center_y": float(cy),
            "min_x": int(min_x), "max_x": int(max_x),
            "min_y": int(min_y), "max_y": int(max_y),
            "perimeter": float(perim), "circularity": float(circ),
            "mean_intensity": float(mean_intensities[i]),
            "total_intensity": float(sum_intensities[i]),
        })
    return cell_props


def read_pixels(pixel_data, w, h, bpp):
    """Reads YouScope's big-endian pixel buffer as the correct dtype.
    bpp 1 -> uint8, 2 -> big-endian uint16, 4 -> big-endian uint32."""
    if bpp == 1:
        dt = np.uint8
    elif bpp == 2:
        dt = np.dtype(">u2")
    elif bpp == 4:
        dt = np.dtype(">u4")
    else:
        dt = np.dtype(">u2")
    return np.frombuffer(pixel_data, dtype=dt).reshape(h, w)


def preprocess(raw, invert, downscale, pmin, pmax):
    """StarDist's standard percentile intensity normalization, plus optional
    inversion and optional downscale for CPU speed.

    Normalization uses csbdeep's ``normalize(img, pmin, pmax)`` (the same
    routine the StarDist notebooks use), so the image the model sees matches
    how the pretrained / custom model was trained. Result is clipped to [0,1].

    Invert note: StarDist (incl. 2D_versatile_he) expects cells to be LIGHT on a
    DARK background, like fluorescence. Brightfield cells are usually the
    opposite (dark / haloed on a bright field), so enabling invert typically
    makes brightfield cells detectable. Returns (float32 image, scale_used).
    """
    from csbdeep.utils import normalize
    img = raw.astype(np.float32)
    img = normalize(img, pmin, pmax)
    img = np.clip(img, 0.0, 1.0)
    if invert:
        img = 1.0 - img
    scale = 1.0
    if 0.0 < downscale < 0.999:
        try:
            from scipy.ndimage import zoom
            img = zoom(img, downscale, order=1)
            scale = downscale
        except Exception as e:
            print("[stardist_server] downscale skipped: %s" % e, flush=True)
    return img.astype(np.float32), scale


def upscale_labels(masks, full_h, full_w):
    """Nearest-neighbour upscale of a label image back to full resolution."""
    if masks.shape[0] == full_h and masks.shape[1] == full_w:
        return masks
    try:
        from scipy.ndimage import zoom
        zy = full_h / masks.shape[0]
        zx = full_w / masks.shape[1]
        return zoom(masks.astype(np.int32), (zy, zx), order=0).astype(np.int32)
    except Exception as e:
        print("[stardist_server] mask upscale skipped: %s" % e, flush=True)
        return masks


def build_debug_blob(img):
    """Returns ([w:int32][h:int32][w*h uint8] of the preprocessed image, error).
    The Java side writes this as stardist_last_input.png in the results folder.
    Request-time file writes from this process were unreliable on some Windows
    installs, so we hand the pixels back instead of writing a file here."""
    try:
        out = np.clip(np.asarray(img, dtype=np.float64), 0.0, 1.0)
        out = (out * 255.0).astype(np.uint8)
        if out.ndim > 2:
            out = np.squeeze(out)
        h, w = int(out.shape[0]), int(out.shape[1])
        return struct.pack(">II", w, h) + out.tobytes(), ""
    except Exception as e:
        return b"", "%s: %s" % (type(e).__name__, e)


def handle_request(args):
    t0 = time.time()
    stages = []
    def stage(msg):
        stages.append("+%.1fs %s" % (time.time() - t0, msg))
    image_bin       = args["image_bin"]
    model_name      = args.get("model",               "2D_versatile_fluo")
    prob_thresh     = float(args.get("prob_thresh",   -1.0))
    nms_thresh      = float(args.get("nms_thresh",    -1.0))
    n_tiles         = int(args.get("n_tiles",          1))
    percentile_low  = float(args.get("percentile_low",  1.0))
    percentile_high = float(args.get("percentile_high", 99.8))
    gpu             = bool(args.get("gpu",            False))
    generate_label  = bool(args.get("generate_label", True))
    masks_bin_out   = args.get("masks_bin_out",       None)
    invert          = bool(args.get("invert",          False))
    downscale       = float(args.get("downscale",      1.0))
    debug_dir       = args.get("debug_dir",            None)

    try:
        with open(image_bin, "rb") as f:
            header = f.read(12)
            w, h, bpp = struct.unpack(">III", header)
            pixel_data = f.read()
    except OSError as e:
        raise RuntimeError("could not read the input image handed over by YouScope "
                           "(%s): %s" % (image_bin, e))

    # YouScope serialises multi-byte pixels BIG-ENDIAN (its 16-bit ImageEvent
    # byte order). Reading them little-endian byte-swaps every pixel into noise
    # -> 0 cells. Use the matching big-endian dtype. 32-bit cams are in scope.
    raw = read_pixels(pixel_data, w, h, bpp)
    stage("read image %dx%d (%d bpp)" % (w, h, bpp))

    img, scale = preprocess(raw, invert, downscale, percentile_low, percentile_high)
    debug_blob, debug_error = build_debug_blob(img)
    stage("preprocessed (downscale=%.3f)" % scale)

    t_ready = time.time()
    model = get_model(model_name, gpu)
    t_model = time.time()
    stage("model ready (%.1fs)" % (t_model - t_ready))

    masks = run_eval(model, img, prob_thresh, nms_thresh, n_tiles,
                     img.shape[1], img.shape[0])
    stage("inference done")
    # Upscale label mask back to full resolution (nearest-neighbour keeps IDs).
    if scale != 1.0:
        masks = upscale_labels(masks, h, w)
    t_infer = time.time()
    print("[stardist_server] Inference done in %.1fs" % (t_infer - t_model),
          flush=True)

    n_cells = int(masks.max())
    cell_props = compute_cell_props_fast(masks, raw, n_cells)
    stage("computed props for %d cells" % n_cells)

    result = {
        "n_cells": n_cells, "height": int(masks.shape[0]), "width": int(masks.shape[1]),
        "model": model_name, "stardist_version": _stardist_version,
        "cell_props": cell_props, "error": "",
        "debug_error": debug_error,
        "preprocess": {"invert": invert, "downscale": scale,
                       "raw_min": int(raw.min()), "raw_max": int(raw.max()),
                       "raw_std": round(float(raw.std()), 3), "bpp": bpp},
        "timing": {
            "model_get_s": round(t_model - t_ready, 3),
            "inference_s": round(t_infer - t_model, 3),
            "total_s": round(time.time() - t0, 3),
        }
    }
    # Compact, escape-safe transport: zlib-compress each binary payload then
    # base64 it into the JSON. A label image is mostly background, so the mask
    # compresses from megabytes to tens of KB. This keeps the JSON small (the
    # large uncompressed base64 was being truncated in transit on the target).
    def _pack(b):
        return base64.b64encode(zlib.compress(b, 6)).decode("ascii")

    if generate_label:
        try:
            blob = masks_to_blob(masks)
            result["masks_b64"] = _pack(blob)
            result["masks_inline_bytes"] = len(blob)       # uncompressed
            result["masks_packed_bytes"] = len(result["masks_b64"])  # on the wire
            stage("mask packed: %d bytes -> %d chars base64"
                  % (len(blob), len(result["masks_b64"])))
        except Exception as e:
            result["masks_error"] = "%s: %s" % (type(e).__name__, e)
            stage("mask pack FAILED: %s" % e)
    # Best-effort file sidecar too (one-shot path / harmless if it fails).
    if generate_label and masks_bin_out:
        try:
            write_masks_bin(masks_bin_out, masks)
            result["masks_bin"] = masks_bin_out
        except Exception as e:
            result["masks_bin_error"] = "%s: %s" % (type(e).__name__, e)
    # Reproducibility text (env + pip freeze), packed the same way. Kept BEFORE the
    # larger debug image so the important small fields never get crowded out.
    try:
        if _env_text:
            result["env_b64"] = _pack(_env_text.encode("utf-8"))
        if _req_text:
            result["req_b64"] = _pack(_req_text.encode("utf-8"))
    except Exception:
        pass
    # Diagnostic input image LAST (largest, least important).
    if debug_blob:
        try:
            result["debug_b64"] = _pack(debug_blob)
        except Exception as e:
            result["debug_error"] = (debug_error + "; " if debug_error else "") \
                + "%s: %s" % (type(e).__name__, e)
    # Request-time debug dump to the working dir (which IS writable here, the env is
    # written there). Confirms the mask is a non-empty LABEL image (0=bg, 1..N per
    # cell) and records payload sizes, per the request to save it somewhere.
    try:
        with open("stardist_last_detection.txt", "w") as f:
            uniq = np.unique(masks)
            f.write("script_version=%s\n" % SCRIPT_VERSION)
            f.write("generate_label=%s n_cells=%s\n" % (generate_label, n_cells))
            f.write("masks_shape=%s dtype=%s max=%s nonzero_px=%s n_labels=%s\n"
                    % (masks.shape, masks.dtype, int(masks.max()),
                       int((masks > 0).sum()), int(uniq.size)))
            f.write("label_ids_sample=%s\n" % (uniq[:20].tolist(),))
            f.write("masks_b64_len=%s env_text_len=%s req_text_len=%s\n"
                    % (len(result.get("masks_b64", "")), len(_env_text), len(_req_text)))
        # Also drop a viewable 8-bit PNG of the mask (scaled) for eyeballing.
        try:
            import tifffile
            vis = (masks.astype(np.float64) / max(1, int(masks.max())) * 255.0
                   ).astype(np.uint8)
            tifffile.imwrite("stardist_last_masks.tif", vis)
        except Exception:
            pass
    except Exception as e:
        print("[stardist_server] detection debug dump skipped: %s" % e, flush=True)
    print("[stardist_server] detection done: %d cells in %.1fs, mask %dx%d packed to %d bytes"
          % (n_cells, time.time() - t0, masks.shape[1], masks.shape[0],
             len(result.get("masks_b64", ""))), flush=True)
    stage("response ready (%d cells)" % n_cells)
    result["server_stages"] = stages
    result["script_version"] = SCRIPT_VERSION
    return result


def prepare_for_model(model, img):
    """Adapt a 2D grayscale image to the number of input channels the loaded
    model expects, returning (image, axes).

    Single-channel models (``2D_versatile_fluo``, ``2D_paper_dsb2018`` and most
    custom fluorescence models) take a 2D ``YX`` image directly. RGB models such
    as ``2D_versatile_he`` expect a 3-channel ``YXC`` image; a grayscale frame is
    replicated across those channels so the model runs instead of raising
    "axes (YXC) must be of length 2".
    """
    try:
        n_ch = int(getattr(model.config, "n_channel_in", 1))
    except Exception:
        n_ch = 1
    if img.ndim == 2:
        if n_ch <= 1:
            return img, "YX"
        return np.repeat(img[..., np.newaxis], n_ch, axis=-1), "YXC"
    return img, "YXC"


def run_eval(model, img, prob_thresh, nms_thresh, n_tiles, w, h):
    """Runs StarDist instance prediction.

    ``img`` is already percentile-normalized by preprocess(), so it is passed
    straight to predict_instances. A threshold < 0 means "use the model's own
    optimized value" (predict_instances treats None that way), which is the
    recommended setting for both pretrained and custom models. ``n_tiles`` > 1
    splits large frames into tiles to bound memory.
    """
    img_in, axes = prepare_for_model(model, img)
    kwargs = {"axes": axes}
    if prob_thresh is not None and prob_thresh >= 0:
        kwargs["prob_thresh"] = prob_thresh
    if nms_thresh is not None and nms_thresh >= 0:
        kwargs["nms_thresh"] = nms_thresh
    if n_tiles and n_tiles > 1:
        # n_tiles must have one entry per image axis (channel axis gets 1).
        kwargs["n_tiles"] = (int(n_tiles), int(n_tiles), 1) if axes == "YXC" \
            else (int(n_tiles), int(n_tiles))
    print("[stardist_server] Running inference on %dx%d image (axes=%s, %s)..."
          % (w, h, axes, kwargs if kwargs else "model defaults"), flush=True)
    labels, _ = model.predict_instances(img_in, **kwargs)
    return labels


def masks_to_blob(masks):
    """Serialises a label mask to the wire/sidecar format:
    [w:int32][h:int32][w*h * uint16]  big-endian (matches YouScope 16-bit order)."""
    h, w = int(masks.shape[0]), int(masks.shape[1])
    return struct.pack(">II", w, h) + np.ascontiguousarray(masks, dtype=">u2").tobytes()


def write_masks_bin(path, masks):
    """Writes the label mask sidecar file (one-shot path). Same bytes as
    masks_to_blob."""
    tmp = path + ".tmp"
    with open(tmp, "wb") as f:
        f.write(masks_to_blob(masks))
    os.replace(tmp, path)


class StarDistRequestHandler(socketserver.BaseRequestHandler):
    def handle(self):
        try:
            # Read 4-byte length prefix then JSON payload
            length_bytes = self._recv_exact(4)
            length = struct.unpack(">I", length_bytes)[0]
            payload = self._recv_exact(length)
            args = json.loads(payload.decode("utf-8"))

            # Lightweight control commands (used by the Java addon to verify the
            # server version and to replace a stale one).
            cmd = args.get("cmd")
            if cmd == "ping":
                resp = json.dumps({"script_version": SCRIPT_VERSION},
                                  separators=(",", ":")).encode("utf-8")
                self.request.sendall(struct.pack(">I", len(resp)) + resp)
                return
            if cmd == "shutdown":
                resp = json.dumps({"script_version": SCRIPT_VERSION, "shutdown": True},
                                  separators=(",", ":")).encode("utf-8")
                try:
                    self.request.sendall(struct.pack(">I", len(resp)) + resp)
                except Exception:
                    pass
                print("[stardist_server] shutdown requested - exiting", flush=True)
                threading.Thread(target=lambda: (time.sleep(0.2), os._exit(0)),
                                 daemon=True).start()
                return

            try:
                result = handle_request(args)
            except Exception as e:
                import traceback
                result = {"error": str(e) + "\n" + traceback.format_exc()[-1000:],
                          "n_cells": 0, "cell_props": []}
            # Always stamp the version so the client can detect a stale server.
            result["script_version"] = SCRIPT_VERSION

            # Single length-prefixed JSON frame. The label mask and diagnostic
            # image ride inside it as base64 (masks_b64 / debug_b64).
            response = json.dumps(result, separators=(",", ":")).encode("utf-8")
            self.request.sendall(struct.pack(">I", len(response)) + response)
        except Exception as e:
            try:
                err = json.dumps({"error": "server error: " + str(e), "n_cells": 0}, separators=(",", ":")).encode()
                self.request.sendall(struct.pack(">I", len(err)) + err)
            except Exception:
                pass

    def _recv_exact(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.request.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("Connection closed while reading")
            buf += chunk
        return buf


class ThreadingTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    # allow_reuse_address must be False on Windows: with it True, SO_REUSEADDR
    # lets a second server bind the same port while an old one is still there, and
    # connections then hit a random one of them. False makes a second bind fail cleanly so the Java addon steps
    # to the next port instead.
    allow_reuse_address = False
    daemon_threads = True


def collect_env(model_name, gpu):
    """Gathers a reproducible environment snapshot."""
    env = {"python": sys.version.split()[0], "executable": sys.executable,
           "cpu_count": os.cpu_count(), "stardist": _stardist_version,
           "model": model_name, "gpu_requested": gpu}
    for mod in ("numpy", "scipy", "torch", "stardist"):
        try:
            m = __import__(mod)
            env[mod + "_version"] = getattr(m, "__version__",
                                            getattr(m, "version", "?"))
        except Exception:
            env[mod + "_version"] = "not-installed"
    try:
        import torch
        env["torch_cuda_available"] = bool(torch.cuda.is_available())
        env["torch_threads"] = int(torch.get_num_threads())
        env["device"] = "cuda" if (gpu and torch.cuda.is_available()) else "cpu"
    except Exception:
        env["device"] = "cpu"
    return env


_env_text = ""
_req_text = ""


def write_env_snapshot(env):
    """Writes stardist_environment.txt + stardist_requirements.txt to CWD, and
    stores their text in module globals so the Java addon can also write them
    straight into the measurement results folder (file-copy from CWD was not
    reliable on all installs)."""
    global _env_text, _req_text
    try:
        _env_text = "# StarDist CPython environment snapshot\n" \
            + "".join("%s=%s\n" % (k, v) for k, v in env.items())
        with open("stardist_environment.txt", "w") as f:
            f.write(_env_text)
        print("[stardist_server] ENV " + json.dumps(env), flush=True)
    except Exception as e:
        print("[stardist_server] env snapshot skipped: %s" % e, flush=True)
    try:
        import subprocess
        freeze = subprocess.run([sys.executable, "-m", "pip", "freeze"],
                                capture_output=True, text=True, timeout=60)
        if freeze.returncode == 0:
            _req_text = freeze.stdout
            with open("stardist_requirements.txt", "w") as f:
                f.write(_req_text)
    except Exception as e:
        print("[stardist_server] pip freeze skipped: %s" % e, flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1",
                        help="Bind address. Use 0.0.0.0 to accept remote GPU clients.")
    parser.add_argument("--port", type=int, default=59712)
    parser.add_argument("--model", default="2D_versatile_fluo")
    parser.add_argument("--gpu", default="false")
    parser.add_argument("--preload", action="store_true",
                        help="Load the model immediately on startup instead of on first request")
    args = parser.parse_args()

    print("[stardist_server] SCRIPT_VERSION %s - starting up" % SCRIPT_VERSION,
          flush=True)
    gpu = args.gpu.lower() in ("true", "1", "yes")

    # Bind the port first, before the (slow) model load. This lets YouScope see
    # the server immediately, so it never times out and starts a duplicate server
    # or falls back to the slow one-shot path while this one is still loading.
    # Requests that arrive during the load wait in the listen backlog until
    # serve_forever() starts (which is only after READY is printed).
    try:
        server = ThreadingTCPServer((args.host, args.port), StarDistRequestHandler)
    except OSError as e:
        # Port already in use (another server is there). Exit cleanly with a
        # distinct code so the Java addon moves to the next port instead of
        # waiting for a READY that will never come.
        print("[stardist_server] could not bind %s:%d (%s). Another server is "
              "probably already using this port - exiting so the client can pick "
              "another port." % (args.host, args.port, e), flush=True)
        sys.exit(3)
    print("[stardist_server] listening on %s:%d - loading model next"
          % (args.host, args.port), flush=True)

    if args.preload:
        get_model(args.model, gpu)      # loads the model while the port is bound
    write_env_snapshot(collect_env(args.model, gpu))

    print("[stardist_server] READY - model loaded, accepting detections", flush=True)

    # After READY, stop writing to the stdout pipe we inherited from YouScope. If
    # the Java reader goes away (e.g. YouScope restarts but this server keeps
    # running), that pipe fills and the NEXT print() raises [Errno 22] on Windows,
    # which would abort a detection. Send further output to a local file instead;
    # the per-detection timeline is returned to Java inside the response
    # (server_stages), so nothing useful is lost.
    try:
        _logf = open("stardist_server_stdout.log", "a", buffering=1)
        sys.stdout = _logf
        sys.stderr = _logf
    except Exception:
        pass

    server.serve_forever()


if __name__ == "__main__":
    main()