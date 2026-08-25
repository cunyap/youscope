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
# YouScope Cellpose CPython bridge (vectorized, fast)
# Called by CellposeAddon.java via ProcessBuilder.
# Reads YOUSCOPE_ARGS_FILE (JSON file path), runs Cellpose 4, writes result JSON.

SCRIPT_VERSION = "v0.4.12"

import os
# CPU thread count (must precede numpy/torch import). From CELLPOSE_NUM_THREADS
# (the "CPU cores" UI spinner) if set, else all cores.
try:
    _nt = int(os.environ.get("CELLPOSE_NUM_THREADS", "").strip())
except ValueError:
    _nt = 0
_ncpu = str(_nt) if _nt and _nt > 0 else str(os.cpu_count() or 1)
os.environ["OMP_NUM_THREADS"] = _ncpu
os.environ["MKL_NUM_THREADS"] = _ncpu
os.environ["OPENBLAS_NUM_THREADS"] = _ncpu
os.environ["NUMEXPR_NUM_THREADS"] = _ncpu

import sys, json, struct, time
import numpy as np


def masks_to_blob(masks):
    """[w:int32][h:int32][w*h * uint16] big-endian."""
    h, w = int(masks.shape[0]), int(masks.shape[1])
    return struct.pack(">II", w, h) + np.ascontiguousarray(masks, dtype=">u2").tobytes()


def write_masks_bin(path, masks):
    """Writes the label image as a compact big-endian uint16 sidecar:
    [w:int32][h:int32][w*h * uint16]  (matches YouScope 16-bit byte order)."""
    tmp = path + ".tmp"
    with open(tmp, "wb") as f:
        f.write(masks_to_blob(masks))
    os.replace(tmp, path)


def read_pixels(pixel_data, w, h, bpp):
    """Reads YouScope's big-endian pixel buffer as the correct dtype
    (1->uint8, 2->big-endian uint16, 4->big-endian uint32)."""
    dt = {1: np.uint8, 2: np.dtype(">u2"), 4: np.dtype(">u4")}.get(bpp, np.dtype(">u2"))
    return np.frombuffer(pixel_data, dtype=dt).reshape(h, w)


def preprocess(raw, invert, downscale):
    """Robust percentile contrast normalization to [0,1], optional invert,
    optional downscale for speed.

    Invert note: Cellpose (incl. cpsam) expects cells LIGHT on a DARK background
    (like fluorescence). Brightfield cells are usually dark/haloed on a bright
    field, so invert typically makes brightfield cells detectable.
    Returns (float32 image, scale_used).
    """
    img = raw.astype(np.float32)
    lo = float(np.percentile(img, 1.0))
    hi = float(np.percentile(img, 99.0))
    if hi <= lo:
        hi = lo + 1.0
    img = np.clip((img - lo) / (hi - lo), 0.0, 1.0)
    if invert:
        img = 1.0 - img
    scale = 1.0
    if 0.0 < downscale < 0.999:
        try:
            from scipy.ndimage import zoom
            img = zoom(img, downscale, order=1)
            scale = downscale
        except Exception as e:
            print("downscale skipped: %s" % e)
    return img, scale


def upscale_labels(masks, full_h, full_w):
    if masks.shape[0] == full_h and masks.shape[1] == full_w:
        return masks
    try:
        from scipy.ndimage import zoom
        return zoom(masks.astype(np.int32),
                    (full_h / masks.shape[0], full_w / masks.shape[1]),
                    order=0).astype(np.int32)
    except Exception:
        return masks


def save_debug_input(img, raw, debug_dir=None):
    """Writes the exact image handed to Cellpose to cellpose_last_input.tif in the
    working directory (known-writable; the Java addon relocates it to the results
    folder). Returns (abspath_or_empty, error_message_or_empty)."""
    try:
        import tifffile
        out = np.clip(np.asarray(img, dtype=np.float64), 0.0, 1.0)
        out = (out * 255.0).astype(np.uint8)
        if out.ndim > 2:
            out = np.squeeze(out)
        path = os.path.abspath("cellpose_last_input.tif")
        tifffile.imwrite(path, out)
        return path, ""
    except Exception as e:
        return "", "%s: %s" % (type(e).__name__, e)


def main():
    t0 = time.time()
    args_file = os.environ.get("YOUSCOPE_ARGS_FILE")
    if not args_file or not os.path.exists(args_file):
        die("YOUSCOPE_ARGS_FILE not set or not found: " + str(args_file))

    with open(args_file) as f:
        args = json.load(f)

    image_bin       = args.get("image_bin")
    result_json     = args.get("result_json")
    model_name      = args.get("model",               "cpsam")
    diameter        = float(args.get("diameter",      0))
    flow_threshold  = float(args.get("flow_threshold",   0.4))
    cellprob_thresh = float(args.get("cellprob_threshold", 0.0))
    gpu             = bool(args.get("gpu",            False))
    channels        = args.get("channels",            [0, 0])
    generate_label  = bool(args.get("generate_label", True))
    fast_mode       = bool(args.get("fast_mode",      False))
    masks_bin_out   = args.get("masks_bin_out",       None)
    invert          = bool(args.get("invert",          False))
    downscale       = float(args.get("downscale",      1.0))
    debug_dir       = args.get("debug_dir",            None)

    if not image_bin or not os.path.exists(image_bin):
        die("image_bin not found: " + str(image_bin), result_json)

    with open(image_bin, "rb") as f:
        header = f.read(12)
        w, h, bpp = struct.unpack(">III", header)
        pixel_data = f.read()

    # YouScope serialises multi-byte pixels big-endian; match it (32-bit in scope).
    raw = read_pixels(pixel_data, w, h, bpp)
    img, scale = preprocess(raw, invert, downscale)
    debug_image_path, debug_error = save_debug_input(img, raw, debug_dir)

    t_load_start = time.time()
    try:
        import cellpose
        from cellpose import models
        import inspect
        cellpose_version = getattr(cellpose, "version", "unknown")
        try:
            import torch
            torch.set_num_threads(os.cpu_count() or 1)
        except Exception:
            pass
        model = models.CellposeModel(gpu=gpu, pretrained_model=model_name)
        t_model_loaded = time.time()

        params = inspect.signature(model.eval).parameters
        eval_kwargs = dict(
            diameter=diameter if diameter > 0 else None,
            flow_threshold=flow_threshold,
            cellprob_threshold=cellprob_thresh,
        )
        # Cellpose-SAM (v4) removed the `channels` argument (SAM is channel-order
        # invariant); older 4.x builds accept but ignore it. Only pass it when
        # the installed version's eval() actually declares it, so this works on
        # both without raising TypeError.
        if "channels" in params:
            eval_kwargs["channels"] = channels
        # Fast mode: skip resampling for lower latency (slightly coarser masks).
        if fast_mode and "resample" in params:
            eval_kwargs["resample"] = False

        try:
            import torch
            _ctx = torch.inference_mode()
        except Exception:
            import contextlib
            _ctx = contextlib.nullcontext()
        with _ctx:
            eval_out = model.eval(img, **eval_kwargs)
        # Single-image eval returns (masks, flows, styles); some versions return
        # a 4th element (diams). Index defensively rather than fixed-arity unpack.
        masks = eval_out[0]
        if scale != 1.0:
            masks = upscale_labels(masks, h, w)
        t_inference_done = time.time()

        n_cells = int(masks.max())
        cell_props = compute_cell_props_fast(masks, raw, n_cells)
        t_props_done = time.time()

        result = {
            "n_cells":    n_cells,
            "height":     int(masks.shape[0]),
            "width":      int(masks.shape[1]),
            "model":      model_name,
            "cellpose_version": cellpose_version,
            "cell_props": cell_props,
            "error":      "",
            "debug_image": debug_image_path,
            "debug_error": debug_error,
            "timing": {
                "model_load_s":  round(t_model_loaded - t_load_start, 2),
                "inference_s":   round(t_inference_done - t_model_loaded, 2),
                "props_s":       round(t_props_done - t_inference_done, 2),
                "total_s":       round(time.time() - t0, 2),
            }
        }
        if generate_label:
            try:
                import base64, zlib
                result["masks_b64"] = base64.b64encode(
                    zlib.compress(masks_to_blob(masks), 6)).decode("ascii")
            except Exception as e:
                result["masks_error"] = "%s: %s" % (type(e).__name__, e)
        if generate_label and masks_bin_out:
            try:
                write_masks_bin(masks_bin_out, masks)
                result["masks_bin"] = masks_bin_out
            except Exception as e:
                result["masks_bin_error"] = "%s: %s" % (type(e).__name__, e)
        # Reproducibility text so env/requirements reach the results folder even on
        # this (slow) one-shot path, matching the persistent server.
        try:
            import base64, zlib, platform
            env_text = "# Cellpose CPython environment snapshot (one-shot)\n" \
                + "python=%s\nexecutable=%s\n" % (platform.python_version(), sys.executable)
            for mod in ("numpy", "scipy", "torch", "cellpose"):
                try:
                    m = __import__(mod)
                    env_text += "%s=%s\n" % (mod, getattr(m, "__version__", "?"))
                except Exception:
                    pass
            result["env_b64"] = base64.b64encode(
                zlib.compress(env_text.encode("utf-8"), 6)).decode("ascii")
            import subprocess
            fr = subprocess.run([sys.executable, "-m", "pip", "freeze"],
                                capture_output=True, text=True, timeout=60)
            if fr.returncode == 0:
                result["req_b64"] = base64.b64encode(
                    zlib.compress(fr.stdout.encode("utf-8"), 6)).decode("ascii")
        except Exception:
            pass

        write_result(result_json, result)
        print("CELLPOSE OK: %d cells, load=%.1fs infer=%.1fs props=%.1fs total=%.1fs" % (
            n_cells, result["timing"]["model_load_s"], result["timing"]["inference_s"],
            result["timing"]["props_s"], result["timing"]["total_s"]))
        sys.exit(0)

    except Exception as e:
        import traceback
        die(str(e) + "\n" + traceback.format_exc()[-1000:], result_json)


def compute_cell_props_fast(masks, raw_image, n_cells):
    """
    Vectorized per-cell property computation using scipy.ndimage.
    This replaces the O(n_cells * image_size) Python loop with
    O(image_size) scipy calls which is critical for images with thousands
    of cells (e.g. confluent monolayers of yeast and bacteria).
    """
    if n_cells == 0:
        return []

    from scipy import ndimage

    labels = np.arange(1, n_cells + 1)

    # Areas: count of pixels per label: vectorized via bincount
    areas = np.bincount(masks.ravel(), minlength=n_cells + 1)[1:n_cells + 1]

    # Centroids: one scipy call for all labels at once
    centroids = ndimage.center_of_mass(masks, labels=masks, index=labels)
    # centroids is list of (row, col) tuples = (y, x)

    # Bounding boxes: one scipy call for all labels at once
    bboxes = ndimage.find_objects(masks)
    # bboxes[i] corresponds to label i+1; may be None if label absent

    # Mean / sum intensity per label using raw image
    raw_f = raw_image.astype(np.float64)
    mean_intensities = ndimage.mean(raw_f, labels=masks, index=labels)
    sum_intensities  = ndimage.sum(raw_f,  labels=masks, index=labels)

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

        # Perimeter approximation from area (fast; avoids per-cell skimage calls)
        # For exact perimeter, use skimage.measure.regionprops in a separate
        # optional pass but this adds significant time for large cell counts.
        perim = 2 * 3.14159265 * (area / 3.14159265) ** 0.5
        circ = (4 * 3.14159265 * area / (perim ** 2)) if perim > 0 else 0.0

        cell_props.append({
            "id":              int(label),
            "area":            area,
            "center_x":        float(cx),
            "center_y":        float(cy),
            "min_x":           int(min_x),
            "max_x":           int(max_x),
            "min_y":           int(min_y),
            "max_y":           int(max_y),
            "perimeter":       float(perim),
            "circularity":     float(circ),
            "mean_intensity":  float(mean_intensities[i]),
            "total_intensity": float(sum_intensities[i]),
        })

    return cell_props


def die(msg, result_path=None):
    write_result(result_path, {"error": msg, "n_cells": 0, "cell_props": []})
    print("CELLPOSE ERROR:", msg[:300], file=sys.stderr)
    sys.exit(1)


def write_result(path, data):
    if path:
        # Write atomically: write to temp then rename, so a partial write
        # (e.g. if the process is killed mid-write) never leaves a
        # half-written file that Java would parse as n_cells=0.
        tmp_path = path + ".tmp"
        with open(tmp_path, "w") as f:
            json.dump(data, f, separators=(",", ":"))
        os.replace(tmp_path, path)


if __name__ == "__main__":
    main()