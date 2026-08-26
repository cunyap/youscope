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

"""
Cell detection plus glow for the YouScope cell-detection plugin, using scipy /
scikit-image library functions.
Runs in the YouScope CPython environment; the GraalPy side only marshals bytes.

Improvements form original CelLX based implementation:

MATLAB call:             library analogue
-------------------------------------------------------------------------------
medfilt2:               scipy.ndimage.median_filter(size=3, mode='constant')
edge(I,'sobel',thr):    scipy.ndimage.correlate w/ fspecial('sobel')/8, b>thr^2
imdilate([se90 se0]):   scipy.ndimage.binary_dilation (3x3 square)
imfill('holes'):        scipy.ndimage.binary_fill_holes
bwlabeln:               skimage.measure.label (8-connectivity)
regionprops(Area):      numpy.bincount on the label image (O(pixels))
regionprops(Centroid):  scipy.ndimage.center_of_mass on the kept labels only
imresize:               skimage.transform.rescale / resize
proximity de-dup:       scipy.spatial.cKDTree.query_pairs  (O(N log N), was O(N^2))
"""

import numpy as np
from scipy import ndimage as ndi
from scipy.spatial import cKDTree
from skimage import measure, transform

SCRIPT_VERSION = "v0.7.4"


_SOBEL = np.array([[1.0, 2.0, 1.0],
                   [0.0, 0.0, 0.0],
                   [-1.0, -2.0, -1.0]]) / 8.0


def decode_image(raw_le, width, height, bytes_per_pixel, bit_depth,
                 transpose_x=False, transpose_y=False, switch_xy=False):
    """Little-endian pixel bytes to oriented uint8 (H, W) image (MATLAB-faithful)."""
    if bytes_per_pixel == 1:
        dtype = np.uint8
    elif bytes_per_pixel == 2:
        dtype = np.uint16
    elif bytes_per_pixel == 4:
        dtype = np.uint32
    else:
        raise ValueError("Only 8/16/32-bit grayscale supported (bpp=%r)." % bytes_per_pixel)

    flat = np.frombuffer(raw_le, dtype=np.dtype(dtype).newbyteorder('<'))
    if flat.size != width * height:
        raise ValueError("Pixel buffer %d != width*height %d" % (flat.size, width * height))

    img = flat.reshape(height, width).astype(dtype, copy=True)
    if transpose_y:
        img = np.flipud(img)
    if transpose_x:
        img = np.fliplr(img)
    if switch_xy:
        img = img.T
    return _to_uint8(img, bit_depth)


def _to_uint8(img, bit_depth):
    if img.dtype == np.uint8:
        return img
    max_value = float((1 << bit_depth) - 1) if bit_depth > 0 else 255.0
    if img.dtype in (np.uint16, np.uint32):
        scaled = img.astype(np.float64) / (max_value / 255.0)
        return np.clip(np.rint(scaled), 0, 255).astype(np.uint8)
    raise ValueError("Only 8/16/32-bit grayscale supported.")


def detect(image_u8, quant_images_u8, threshold, internal_binning,
           max_cell_diameter, min_cell_diameter, log=None):
    """Detect cells (+ optional fluorescence). Returns (pos Nx3 [x,y,area] 0-based,
    label_image uint8, rows). ``log`` is an optional callable(str) for tracing."""
    def _l(msg):
        if log:
            log(msg)

    H, W = image_u8.shape
    binning = max(1, int(internal_binning))
    resize_scale = 1.0 / binning
    lower = (min_cell_diameter ** 2) * (resize_scale ** 2)
    upper = (max_cell_diameter ** 2) * (resize_scale ** 2)

    if binning != 1:
        work = transform.rescale(image_u8, resize_scale, anti_aliasing=True,
                                 preserve_range=True)
        work = np.clip(np.rint(work), 0, 255).astype(np.uint8)
    else:
        work = image_u8

    _l("median_filter %dx%d" % (work.shape[1], work.shape[0]))
    work = ndi.median_filter(work, size=3, mode='constant', cval=0)

    _l("sobel edges")
    a = work.astype(np.float64) / 255.0
    bx = ndi.correlate(a, _SOBEL.T, mode='nearest')
    by = ndi.correlate(a, _SOBEL, mode='nearest')
    edges = (bx * bx + by * by) > (float(threshold) ** 2)

    _l("dilate + fill")
    dil = ndi.binary_dilation(edges, structure=np.ones((3, 3), dtype=bool))
    filled = ndi.binary_fill_holes(dil)

    _l("label")
    labels = measure.label(filled, connectivity=2)
    counts = np.bincount(labels.ravel())          # counts[k] = area of label k
    lab_ids = np.arange(counts.size)
    keep = lab_ids[(lab_ids > 0) & (counts > lower) & (counts < upper)]
    _l("%d regions -> %d pass size gate (%.1f, %.1f)"
       % (max(counts.size - 1, 0), keep.size, lower, upper))

    if keep.size == 0:
        return (np.zeros((0, 3), dtype=np.float64),
                np.zeros((H, W), dtype=np.uint8), [])

    # centroids only for kept labels (row, col), 0-based
    centroids = ndi.center_of_mass(filled, labels, list(keep))
    pos = np.empty((keep.size, 3), dtype=np.float64)
    for i, (cy, cx) in enumerate(centroids):
        pos[i, 0] = (cx + 1.0) / resize_scale     # MATLAB 1-based -> /scale
        pos[i, 1] = (cy + 1.0) / resize_scale
        pos[i, 2] = counts[keep[i]] / (resize_scale ** 2)
    old_labels = list(keep)

    # proximity de-dup: remove the higher-indexed member of any too-close pair
    _l("dedup %d candidates" % pos.shape[0])
    remove = set()
    if pos.shape[0] > 1:
        radius = (upper * 2.5) ** 0.5
        tree = cKDTree(pos[:, 0:2])
        for i, j in tree.query_pairs(radius):
            remove.add(j if j > i else i)
    survivor_mask = np.ones(pos.shape[0], dtype=bool)
    for idx in remove:
        survivor_mask[idx] = False
    survivors = [old_labels[i] for i in range(pos.shape[0]) if survivor_mask[i]]
    pos = pos[survivor_mask]
    _l("%d after dedup" % pos.shape[0])

    # relabel so label==c corresponds to table row c
    remap = np.zeros(int(labels.max()) + 1, dtype=np.int32)
    for new_id, old_id in enumerate(survivors, start=1):
        remap[old_id] = new_id
    label_work = remap[labels]

    if binning != 1:
        label_full = transform.resize(label_work, (H, W), order=0,
                                      preserve_range=True,
                                      anti_aliasing=False).astype(np.uint8)
    else:
        label_full = label_work.astype(np.uint8)

    pos[:, 0:2] -= 1.0                              # 0-based for Java

    rows = []
    m = pos.shape[0]
    if not quant_images_u8:
        for c in range(1, m + 1):
            rows.append((c, None, float(pos[c - 1, 0]), float(pos[c - 1, 1]),
                         float(pos[c - 1, 2]), None))
    else:
        for c in range(1, m + 1):
            cell_mask = (label_full == c)
            for img_idx, qimg in enumerate(quant_images_u8, start=0):
                vals = qimg[cell_mask]
                fluo = float(vals.mean()) / 255.0 if vals.size else 0.0
                rows.append((c, img_idx, float(pos[c - 1, 0]), float(pos[c - 1, 1]),
                             float(pos[c - 1, 2]), fluo))
    _l("done: %d cells, %d rows" % (m, len(rows)))
    return pos, label_full, rows


def glow(image_u8, cell_positions, glow_strength):
    """Soft radial glow around each detected cell; returns (H, W, 3) uint8."""
    H, W = image_u8.shape
    base = image_u8.astype(np.float64) / (1.0 + glow_strength)
    img_red = base.copy()
    img_green = base.copy()
    img_blue = base.copy()

    if cell_positions.shape[0] == 0:
        rgb = np.stack([img_red, img_green, img_blue], axis=2)
        return np.clip(np.rint(rgb), 0, 255).astype(np.uint8)

    pos = np.rint(cell_positions).astype(np.int64)
    mean_area = np.nanmean(cell_positions[:, 2])
    glow_width = int(round(np.sqrt(mean_area) * 3)) if mean_area > 0 else 1
    if glow_width % 2 == 0:
        glow_width += 1
    if glow_width < 1:
        glow_width = 1
    half = (glow_width - 1) // 2

    if half > 0:
        yy, xx = np.meshgrid(np.arange(-half, half + 1),
                             np.arange(-half, half + 1), indexing='ij')
        glow_kernel = glow_strength * (1.0 - np.sqrt(xx * xx + yy * yy) / half)
        glow_kernel[glow_kernel < 0] = 0.0
    else:
        glow_kernel = np.array([[glow_strength]], dtype=np.float64)

    for i in range(pos.shape[0]):
        if np.isnan(cell_positions[i, 0]):
            continue
        x, y = pos[i, 0], pos[i, 1]
        if x - half < 0:
            x = half
        elif x + half > W - 1:
            x = W - 1 - half
        if y - half < 0:
            y = half
        elif y + half > H - 1:
            y = H - 1 - half
        if x - half < 0 or y - half < 0 or x + half > W - 1 or y + half > H - 1:
            continue
        region = img_blue[y - half:y + half + 1, x - half:x + half + 1]
        img_blue[y - half:y + half + 1, x - half:x + half + 1] = region * (1.0 + glow_kernel)

    rgb = np.stack([img_red, img_green, img_blue], axis=2)
    return np.clip(np.rint(rgb), 0, 255).astype(np.uint8)


# Quantification (moved in-module on purpose)
# -----------------------------------------------------------------------------
# The quantifier lives here, inside cell_detection_cpython.py, rather than in a
# separate quantify_cpython.py. This module is already extracted from the plugin
# JAR on every startup, so bundling the quantifier with it removes the need to
# package/extract an ADDITIONAL resource file. quickdetect_server.py calls these
# via `cd.geometry(...)` / `cd.quantify(...)`.
#
# geometry(label)                         -> (columns, ids, values)
# quantify(name, label, channels, params) -> (columns, ids, values)
#
# Both enumerate labels in the same order (shared _label_ids helper) so the
# server can concatenate geometry + feature values column-wise. Geometry column
# names match org.youscope.addon.celldetection.CellDetectionTableColumns so the
# nearest-neighbour tracker finds cell.center.x / cell.center.y downstream.


# Geometry column names that MUST match CellDetectionTableColumns on the Java side.
COL_CENTER_X = "cell.center.x"
COL_CENTER_Y = "cell.center.y"
COL_AREA = "cell.area"


def _q_col(name, dtype="double"):
    return {"name": name, "dtype": dtype}


def _label_ids(label):
    """Sorted unique positive label ids (background 0 dropped). Single source of
    truth for cell ordering, shared by geometry() and quantify()."""
    label = np.ascontiguousarray(label)
    if label.size == 0:
        return np.zeros((0,), dtype=np.int64)
    counts = np.bincount(label.ravel().astype(np.int64, copy=False))
    ids = np.nonzero(counts)[0]
    ids = ids[ids > 0]
    return ids.astype(np.int64, copy=False)


def geometry(label, matlab_centroid=False):
    """Core per-cell geometry: centroid (x, y) and area. Returns (columns, ids,
    values) with values[:, 0]=x (col), [:, 1]=y (row), [:, 2]=area; 0-based.
    When matlab_centroid is set, the centroid is reported in MATLAB's convention
    (1-based and rounded), to match native CellX."""
    label = np.asarray(label, dtype=np.int64)
    ids = _label_ids(label)
    columns = [_q_col(COL_CENTER_X), _q_col(COL_CENTER_Y), _q_col(COL_AREA)]

    n = ids.shape[0]
    values = np.zeros((n, 3), dtype=np.float64)
    if n == 0:
        return columns, ids.tolist(), values

    counts = np.bincount(label.ravel())
    values[:, 2] = counts[ids].astype(np.float64)

    # A list index (even length 1) always yields a list of (row, col) tuples.
    centroids = ndi.center_of_mass(np.ones_like(label, dtype=np.float64), label, list(ids))
    for i, (cy, cx) in enumerate(centroids):
        if matlab_centroid:
            values[i, 0] = _mround(cx + 1.0)   # 1-based, rounded (MATLAB)
            values[i, 1] = _mround(cy + 1.0)
        else:
            values[i, 0] = float(cx)   # x = column
            values[i, 1] = float(cy)   # y = row
    return columns, ids.tolist(), values


def core_geometry(label):
    """Segmenter core columns: cell.center.x, cell.center.y, cell.area,
    cell.perimeter (0-based centroid; MATLAB regionprops perimeter). Single source
    of truth shared with the mask quantifiers, used by the FastER segmenter."""
    label = np.asarray(label, dtype=np.int64)
    ids = _label_ids(label)
    cols = [COL_CENTER_X, COL_CENTER_Y, COL_AREA, "cell.perimeter"]
    n = ids.shape[0]
    values = np.zeros((n, 4), dtype=np.float64)
    if n == 0:
        return cols, ids.tolist(), values
    counts = np.bincount(label.ravel())
    values[:, 2] = counts[ids].astype(np.float64)
    centroids = ndi.center_of_mass(np.ones_like(label, dtype=np.float64), label, list(ids))
    objs = ndi.find_objects(label)
    for i, cid in enumerate(ids):
        cy, cx = centroids[i]
        values[i, 0] = float(cx)
        values[i, 1] = float(cy)
        sl = objs[int(cid) - 1]
        if sl is not None:
            values[i, 3] = _matlab_perimeter(label[sl] == cid)
    return cols, ids.tolist(), values


def _read_label_image(path):
    """Read a FastER output label image regardless of the on-disk format.

    FastER's headless export (v1.4.7+) writes a 16-bit grayscale PNG even though
    the output path ends in .tif, so a TIFF-only reader fails with
    'not a TIFF file: header=b"\\x89PNG"'. Sniff the magic bytes and use tifffile
    for real TIFFs, otherwise try PNG-capable readers (imageio / PIL), falling back
    to tifffile last."""
    import numpy as _np
    with open(path, "rb") as f:
        head = f.read(8)
    is_tiff = head[:2] in (b"II", b"MM")
    is_png = head[:8].startswith(b"\x89PNG")

    if is_tiff and not is_png:
        import tifffile
        return _np.asarray(tifffile.imread(path))

    errors = []
    # imageio first, then PIL, then tifffile as a last resort.
    try:
        try:
            import imageio.v2 as _iio
        except ImportError:
            import imageio as _iio
        return _np.asarray(_iio.imread(path))
    except Exception as exc:
        errors.append("imageio: %s" % (exc,))
    try:
        from PIL import Image as _Image
        return _np.asarray(_Image.open(path))
    except Exception as exc:
        errors.append("PIL: %s" % (exc,))
    try:
        import tifffile
        return _np.asarray(tifffile.imread(path))
    except Exception as exc:
        errors.append("tifffile: %s" % (exc,))
    raise RuntimeError(
        "Could not read FastER output %r (header=%r) with any available reader. "
        "Install 'imageio' or 'Pillow' in the backend env. Tried: %s"
        % (path, head[:8], "; ".join(errors)))


_FASTER_STAGE_CACHE = {}


def _faster_resolve_model(model, svm, wd):
    """FastER derives the SVM path as `<model_arg>_svm`. Ensure that sibling exists
    next to the model path we pass. If the model's own sibling is present, use the
    model in place. Otherwise stage the model plus the (separately chosen) SVM together
    into the work dir with matching basenames so FastER finds them. Returns the
    model path to pass on the command line."""
    import os
    import shutil

    sib = model + "_svm"
    if os.path.isfile(sib):
        return model  # sibling already present -> run in place

    if not svm or not os.path.isfile(svm):
        raise RuntimeError(
            "FastER needs the SVM model file alongside the .fastER model "
            "(expected '%s'). Exporting a headless model writes both '<name>.fastER' "
            "and '<name>.fastER_svm'; select the .fastER_svm file in the FastER "
            "configuration (it is auto-detected when it sits next to the .fastER file)."
            % sib)

    key = (model, svm)
    cached = _FASTER_STAGE_CACHE.get(key)
    if cached and os.path.isfile(cached) and os.path.isfile(cached + "_svm"):
        return cached

    staged_model = os.path.join(wd, os.path.basename(model))
    staged_svm = staged_model + "_svm"
    shutil.copyfile(model, staged_model)
    shutil.copyfile(svm, staged_svm)
    _FASTER_STAGE_CACHE[key] = staged_model
    return staged_model


def segment_faster(image, binary, model, svm=None, work_dir=None):
    """Run the headless FastER binary on one frame and return its label image.

    Writes the frame to input.tif, runs
    `"<binary>" -headless "<model>" "<input.tif>" "<output.tif>"`, and reads the
    16-bit label output. FastER already emits a labelled image, so it is used
    directly (background 0, one integer id per cell). FastER also requires the
    SVM sidecar `<model>_svm`; `_faster_resolve_model` guarantees it is co-located
    with the model path passed on the command line."""
    import os
    import tempfile
    import subprocess
    import tifffile

    if not binary or not os.path.isfile(binary):
        raise RuntimeError("FastER binary not found: %r" % (binary,))
    if not model or not os.path.isfile(model):
        raise RuntimeError("FastER model (.fastER) not found: %r" % (model,))
    wd = work_dir or tempfile.gettempdir()
    try:
        os.makedirs(wd, exist_ok=True)
    except OSError:
        wd = tempfile.gettempdir()
    model_arg = _faster_resolve_model(model, svm, wd)
    inp = os.path.join(wd, "faster_input.tif")
    outp = os.path.join(wd, "faster_output.tif")
    if os.path.exists(outp):
        try:
            os.remove(outp)
        except OSError:
            pass
    tifffile.imwrite(inp, np.asarray(image))
    cmd = [binary, "-headless", model_arg, inp, outp]
    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if proc.returncode != 0 or not os.path.exists(outp):
        msg = (proc.stderr or proc.stdout or b"").decode("utf-8", "replace")[:500]
        raise RuntimeError("FastER failed (exit %s): %s" % (proc.returncode, msg))
    lab = _read_label_image(outp)
    lab = np.asarray(lab)
    if lab.ndim > 2:  # collapse an accidental channel/page axis (e.g. RGB PNG)
        lab = lab[..., 0] if lab.shape[-1] <= 4 else lab[0]
    return lab.astype(np.int64)


def quantify(name, label, channels, params):
    """Dispatch to the named quantifier. Ships 'intensity' (minimal) and 'cellx'
    (CellXMaskInterface features). Unknown names raise (server to clean error)."""
    name = (name or "intensity").lower()
    if name == "intensity":
        return _quantify_intensity(label, channels, params or {})
    if name in ("cellx", "cellxmask"):
        return quantify_cellx(label, channels, params or {})
    raise ValueError("Unknown quantifier %r (this backend provides: intensity, cellx)." % (name,))


def _size_mismatch_msg(c, ch_shape, mask_shape):
    return ("quantification channel %d is %dx%d but the segmentation mask is %dx%d. "
            "The segmentation image and the quantification image(s) must have the same "
            "dimensions (check that they use the same binning / camera ROI / pixel size)."
            % (c, ch_shape[1], ch_shape[0], mask_shape[1], mask_shape[0]))


def _channel_suffixes(params, n_channels):
    """Column suffix per channel. Java passes the resolved channel names in
    params['channel_names'] (already sanitised to identifier-safe tokens); when a
    name is missing/blank we fall back to 'chN' so columns are always valid and
    stable between the configured header and the produced table."""
    names = params.get("channel_names") or []
    out = []
    for c in range(n_channels):
        nm = names[c] if c < len(names) else None
        nm = ("%s" % nm).strip() if nm is not None else ""
        out.append(nm if nm else ("ch%d" % c))
    return out


def _quantify_intensity(label, channels, params):
    """Per-channel mean plus integrated (optionally background-subtracted) intensity.

        mean.chC       = mean(pixels of cell)          [- bg  if subtract]
        integrated.chC = sum(pixels of cell)           [- bg*area  if subtract]
    with bg = percentile(whole channel, background_percentile).
    params: {"subtract_background": bool, "background_percentile": float}."""
    label = np.asarray(label, dtype=np.int64)
    ids = _label_ids(label)
    n = ids.shape[0]

    subtract = _q_as_bool(params.get("subtract_background", False))
    percentile = _q_as_float(params.get("background_percentile", 5.0), 5.0)

    columns = []
    sfx = _channel_suffixes(params, len(channels))
    for c in range(len(channels)):
        columns.append(_q_col("cell.intensity.mean.%s" % sfx[c]))
        columns.append(_q_col("cell.intensity.integrated.%s" % sfx[c]))

    values = np.zeros((n, len(columns)), dtype=np.float64)
    if n == 0 or len(channels) == 0:
        return columns, ids.tolist(), values

    id_list = list(ids)
    counts = np.bincount(label.ravel())
    areas = counts[ids].astype(np.float64)

    for c, ch in enumerate(channels):
        ch = np.asarray(ch, dtype=np.float64)
        if ch.shape != label.shape:
            raise ValueError(_size_mismatch_msg(c, ch.shape, label.shape))
        bg = float(np.percentile(ch, percentile)) if subtract else 0.0
        means = np.asarray(ndi.mean(ch, label, id_list), dtype=np.float64).reshape(-1)
        sums = np.asarray(ndi.sum_labels(ch, label, id_list), dtype=np.float64).reshape(-1)
        values[:, 2 * c] = means - bg
        values[:, 2 * c + 1] = sums - bg * areas

    return columns, ids.tolist(), values


def _q_as_bool(v):
    if isinstance(v, bool):
        return v
    if isinstance(v, str):
        return v.strip().lower() in ("1", "true", "yes", "on")
    return bool(v)


def _q_as_float(v, default):
    try:
        return float(v)
    except (TypeError, ValueError):
        return float(default)



# CellX: CellXMaskInterface quantifier plus CellX online tracker (ported from MATLAB)

from scipy.optimize import linear_sum_assignment as _linear_sum_assignment

try:
    from sklearn.mixture import GaussianMixture
    _HAVE_SKLEARN = True
except Exception:  # pragma: no cover
    _HAVE_SKLEARN = False


# Small helpers

def _label_ids(label):
    label = np.ascontiguousarray(label)
    if label.size == 0:
        return np.zeros((0,), dtype=np.int64)
    counts = np.bincount(label.ravel().astype(np.int64, copy=False))
    ids = np.nonzero(counts)[0]
    return ids[ids > 0].astype(np.int64, copy=False)


def _disk(radius):
    """Binary disk structuring element matching MATLAB strel('disk', r) (approx,
    chessboard-free). For r>=1 uses a euclidean disk; r<1 -> single pixel."""
    r = int(max(0, round(radius)))
    if r < 1:
        return np.ones((1, 1), dtype=bool)
    L = np.arange(-r, r + 1)
    X, Y = np.meshgrid(L, L)
    return (X * X + Y * Y) <= (r * r + 1e-9)


def _p_major(p):
    if hasattr(p, "axis_major_length"):
        return float(p.axis_major_length)
    return float(p.major_axis_length)


def _p_minor(p):
    if hasattr(p, "axis_minor_length"):
        return float(p.axis_minor_length)
    return float(p.minor_axis_length)


def _matlab_quantile(x, p):
    """MATLAB quantile(): linear interpolation on the (0.5:1:N-0.5)/N grid.
    x: 1-D array; p: scalar or array of probabilities. Returns same shape as p."""
    x = np.asarray(x, dtype=np.float64).ravel()
    n = x.size
    p = np.atleast_1d(np.asarray(p, dtype=np.float64))
    if n == 0:
        return np.full(p.shape, np.nan)
    if n == 1:
        return np.full(p.shape, x[0])
    xs = np.sort(x)
    grid = (np.arange(1, n + 1) - 0.5) / n          # positions of sorted samples
    out = np.interp(p, grid, xs)                    # clamps to xs[0]/xs[-1] outside
    return out


def _quartiles(vals):
    """(q25, median, q75) MATLAB-compatible; NaNs for empty input."""
    if vals.size == 0:
        return (np.nan, np.nan, np.nan)
    q = _matlab_quantile(vals, [0.25, 0.5, 0.75])
    return (float(q[0]), float(q[1]), float(q[2]))



# Background: Gaussian mixture on non-cell pixels (CellXMaskMixtureModel)

def _background_gmm(channel, bg_mask, n_components=3, rng_seed=0):
    """Returns (mean, std) of the dominant background Gaussian, mirroring
    CellXMaskMixtureModel.computeMixtureModel: subsample, trim histogram tails,
    fit a k-component GMM, take the component with the largest mixing weight.
    Falls back to median/std when the fit is unavailable or degenerate."""
    vals = channel[bg_mask].astype(np.float64).ravel()
    if vals.size == 0:
        return (0.0, 0.0)

    # subsample large populations (MATLAB: > 5e4 -> 5e4 random samples)
    rng = np.random.default_rng(rng_seed)
    if vals.size > 50000:
        vals = vals[rng.integers(0, vals.size, 50000)]

    # histogram-tail trimming for a cleaner fit
    nbins = 100 if vals.size > 5000 else max(1, int(round(vals.size / 50)))
    counts, edges = np.histogram(vals, bins=nbins)
    centers = 0.5 * (edges[:-1] + edges[1:])
    min_count = 0.1 * vals.size / nbins
    keep = counts > min_count
    if np.any(keep):
        vals = np.repeat(centers[keep], counts[keep].astype(int))

    uniq = np.unique(vals)
    if not _HAVE_SKLEARN or uniq.size < n_components or vals.size < n_components:
        return (float(np.mean(vals)), float(np.std(vals)))

    try:
        gm = GaussianMixture(n_components=n_components, n_init=3,
                             max_iter=500, tol=1e-6, random_state=rng_seed)
        gm.fit(vals.reshape(-1, 1))
        idx = int(np.argmax(gm.weights_))
        mean = float(gm.means_[idx, 0])
        std = float(np.sqrt(gm.covariances_[idx, 0, 0]))
        return (mean, std)
    except Exception:
        return (float(np.mean(vals)), float(np.std(vals)))


# Per-cell geometry regions (SeedsImporter and CellXMaskSeed)

def _matlab_perimeter(mask):
    """MATLAB regionprops Perimeter: 8-connected boundary trace plus the
    Vossepoel-Smeulders weighting used by regionprops' computePerimeterFromBoundary
    (even step 0.980, odd/diagonal step 1.406, minus 0.091 per corner)."""
    m = np.asarray(mask, dtype=bool)
    if m.sum() <= 1:
        return 0.0
    m = np.pad(m, 1)  # clean background border
    H, W = m.shape
    fg = np.argwhere(m)
    start = (int(fg[0][0]), int(fg[0][1]))  # topmost-leftmost
    off = [(-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1)]  # clockwise

    def fgp(p):
        r, c = p
        return 0 <= r < H and 0 <= c < W and m[r, c]

    b = (start[0], start[1] - 1)  # entered start from the west
    boundary = [start]
    cur = start
    second = None
    guard = 0
    limit = 8 * int(m.sum()) + 16
    while guard < limit:
        guard += 1
        d = (b[0] - cur[0], b[1] - cur[1])
        k = off.index(d)
        found = None
        for i in range(1, 9):
            nk = (k + i) % 8
            p = (cur[0] + off[nk][0], cur[1] + off[nk][1])
            if fgp(p):
                found = p
                b = (cur[0] + off[(nk - 1) % 8][0], cur[1] + off[(nk - 1) % 8][1])
                break
        if found is None:
            break
        cur = found
        if len(boundary) == 1:
            second = cur
        boundary.append(cur)
        if cur == start and second is not None and len(boundary) > 2 and boundary[1] == second:
            boundary.pop()
            break

    # B is the closed boundary (first point repeated).
    B = np.array(boundary + [boundary[0]], dtype=np.float64)
    delta = np.diff(B, axis=0) ** 2  # squared coordinate steps between consecutive points
    if delta.shape[0] <= 1:
        return 0.0
    is_corner = np.any(np.diff(np.vstack([delta, delta[0]]), axis=0) != 0, axis=1)
    is_even = np.any(delta == 0, axis=1)  # orthogonal step (one coordinate unchanged)
    perim = np.sum(is_even) * 0.980 + np.sum(~is_even) * 1.406 - np.sum(is_corner) * 0.091
    return float(perim)


def _matlab_ellipse(pr):
    """MATLAB regionprops MajorAxisLength / MinorAxisLength / Orientation.

    Adds the +1/12 pixel-variance correction that skimage omits (so the axis
    lengths, and therefore floor(minorAxis/2) that drives the cellVolume erosion
    layer count, match MATLAB exactly) and uses MATLAB's y-flipped orientation
    convention. Returns (major, minor, orientation_degrees)."""
    mc = pr.moments_central
    area = mc[0, 0]
    if area <= 0:
        return 0.0, 0.0, 0.0
    uxx = mc[0, 2] / area + 1.0 / 12.0  # column (x) variance
    uyy = mc[2, 0] / area + 1.0 / 12.0  # row (y) variance
    uxy = -mc[1, 1] / area              # y flipped -> negate
    common = np.sqrt((uxx - uyy) ** 2 + 4.0 * uxy ** 2)
    major = 2.0 * np.sqrt(2.0) * np.sqrt(uxx + uyy + common)
    minor = 2.0 * np.sqrt(2.0) * np.sqrt(max(uxx + uyy - common, 0.0))
    if uyy > uxx:
        num = uyy - uxx + np.sqrt((uyy - uxx) ** 2 + 4.0 * uxy ** 2)
        den = 2.0 * uxy
    else:
        num = 2.0 * uxy
        den = uxx - uyy + np.sqrt((uxx - uyy) ** 2 + 4.0 * uxy ** 2)
    orient = 0.0 if (num == 0.0 and den == 0.0) else (180.0 / np.pi) * np.arctan(num / den)
    return float(major), float(minor), float(orient)


def _mround(x):
    """MATLAB round() for non-negative values (round half away from zero)."""
    return float(np.floor(x + 0.5))


class _CellGeom:
    __slots__ = ("cell_mask", "cyto_mask", "mem_mask", "bbox", "area",
                 "minor_axis", "major_axis", "orientation", "perimeter",
                 "cell_volume", "cyto_volume", "nuc_radius")


def _cell_geometry(label, ids, membrane_width, nuclear_volume_fraction):
    """Per-cell region masks + geometry (major/minor axis, orientation, perimeter),
    cellVolume / cytosolVolume and nucleus radius, in a bounding-box crop for speed.
    Returns dict id to _CellGeom (crop-local masks + bbox)."""
    objects = ndi.find_objects(label)
    mem_shape = _disk(membrane_width)
    disk1 = _disk(1)
    props = {int(p.label): p for p in measure.regionprops(label)}
    out = {}
    for cid in ids:
        cid = int(cid)
        sl = objects[cid - 1]
        if sl is None:
            continue
        crop = (label[sl] == cid)
        g = _CellGeom()
        g.bbox = sl
        g.cell_mask = crop
        g.area = int(crop.sum())
        # cytosol = erode(cell, disk(membraneWidth)); membrane = cell XOR cytosol.
        # Native computes these with imerode on p(i).Image; matched to native's
        # mem.area at border_value=0 (out-of-image treated as background).
        cyto = ndi.binary_erosion(crop, structure=mem_shape, border_value=0)
        g.cyto_mask = cyto
        g.mem_mask = crop & (~cyto)
        # shape descriptors: MATLAB-exact major/minor/orientation so that
        # floor(minorAxis/2) (the cellVolume erosion layer count) matches native.
        pr = props.get(cid)
        if pr is not None:
            g.major_axis, g.minor_axis, g.orientation = _matlab_ellipse(pr)
            g.perimeter = _matlab_perimeter(crop)
        else:
            g.major_axis = g.minor_axis = g.orientation = g.perimeter = 0.0
        # pseudo-3D cellVolume + cytosolVolume: iterative erosion (SeedsImporter)
        half = int(np.floor(g.minor_axis / 2.0))
        erosion_mask = crop.copy()
        cyto_mask2 = cyto.copy()
        total_voxels = 0
        cyto_voxels = 0
        for j in range(1, half + 1):
            if j % 2 == 1:
                erosion_mask = ndi.binary_erosion(erosion_mask, structure=disk1, border_value=0)
                cyto_mask2 = ndi.binary_erosion(cyto_mask2, structure=disk1, border_value=0)
            total_voxels += int(erosion_mask.sum())
            cyto_voxels += int(cyto_mask2.sum())
        g.cell_volume = 2 * total_voxels + g.area
        g.cyto_volume = 2 * cyto_voxels + int(cyto.sum())
        nuc_vol = nuclear_volume_fraction * g.cell_volume
        g.nuc_radius = (3.0 * nuc_vol / (4.0 * np.pi)) ** (1.0 / 3.0) if nuc_vol > 0 else 0.0
        out[cid] = g
    return out


def _nucleus_pixels(channel, g, nuc_radius):
    """Nucleus = disk of radius nuc_radius centred where a disk-convolution of the
    masked cell image is maximal (CellXMaskIntensityExtractor.computeNuclearPixelList).
    Returns a boolean mask over the FULL image."""
    H, W = channel.shape
    sl = g.bbox
    crop = channel[sl].astype(np.float64) * g.cell_mask
    disk = _disk(nuc_radius).astype(np.float64)
    conv = ndi.correlate(crop, disk, mode="constant", cval=0.0)
    cy, cx = np.unravel_index(int(np.argmax(conv)), conv.shape)
    # to full-image coords
    y0 = sl[0].start
    x0 = sl[1].start
    cy_full = y0 + cy
    cx_full = x0 + cx
    r = nuc_radius
    ry0 = max(0, int(np.floor(cy_full - r)))
    ry1 = min(H, int(np.ceil(cy_full + r)) + 1)
    rx0 = max(0, int(np.floor(cx_full - r)))
    rx1 = min(W, int(np.ceil(cx_full + r)) + 1)
    mask = np.zeros((H, W), dtype=bool)
    if ry1 > ry0 and rx1 > rx0:
        yy, xx = np.mgrid[ry0:ry1, rx0:rx1]
        mask[ry0:ry1, rx0:rx1] = (xx - cx_full) ** 2 + (yy - cy_full) ** 2 <= r * r
    return mask


def _bright_area(channel_smoothed, g, bright_pct):
    """Top `bright_pct` brightest cell pixels: (values, euler number).
    Euler = # connected components (8-conn) of the bright set in the cell crop."""
    sl = g.bbox
    crop_vals = channel_smoothed[sl][g.cell_mask]
    n = crop_vals.size
    k = int(round(bright_pct * n))
    if k <= 0:
        return np.zeros((0,), dtype=np.float64), 0
    order = np.argsort(crop_vals)[::-1][:k]
    bright_vals = crop_vals[order]
    # place bright pixels back into the crop to count components
    crop_mask = np.zeros(g.cell_mask.shape, dtype=bool)
    ys, xs = np.nonzero(g.cell_mask)
    crop_mask[ys[order], xs[order]] = True
    euler = int(measure.label(crop_mask, connectivity=2).max())
    return bright_vals, euler



# CellX quantifier

_CELLX_REGIONS = ["cell", "mem", "nuc", "bright"]

# Channel-independent geometry columns (names match native CellX's table).
_CELLX_GEOMETRY = ["cell.majoraxis", "cell.minoraxis", "cell.orientation",
                   "cell.volume", "cell.perimeter", "cell.mem.area",
                   "cell.mem.volume", "cell.nuc.radius", "cell.nuc.area"]


def _q_label(pct):
    """Quantile column stem for a percentage: 50 -> 'median', else 'q<pct>'
    (integer percentages -> q25/q75; fractional -> q12p5, dot-free for identifiers)."""
    if abs(pct - 50.0) < 1e-9:
        return "median"
    if abs(pct - round(pct)) < 1e-9:
        return "q%d" % int(round(pct))
    return "q" + ("%g" % pct).replace(".", "p")


def _quantiles_at(vals, fracs):
    """Quantile values (MATLAB convention) at the given fractions; NaN if empty."""
    if vals.size == 0:
        return [np.nan] * len(fracs)
    q = _matlab_quantile(vals, fracs)
    return [float(x) for x in np.atleast_1d(q)]


def cellx_feature_names(quantiles_pct):
    """Per-channel fluo feature stems (order matches quantify_cellx output), for a
    given list of quantile percentages. Used to keep Java/Python headers in sync."""
    qlabels = [_q_label(float(p)) for p in quantiles_pct]
    feats = ["background.mean", "background.std"]
    for region in _CELLX_REGIONS:
        feats.append("%s.total" % region)
        for ql in qlabels:
            feats.append("%s.%s" % (region, ql))
    feats.append("bright.euler")
    return feats


def _align_fluo(image, cell_binary, pad, bg_value):
    """CellX fluoAlignPixelMove: pad the image by `pad` with the background value,
    then choose the integer shift in [-pad,pad]^2 that maximises total intensity
    under the cell mask (ties to smallest |shift|); return the aligned image
    (CellXMaskIntensityExtractor.findOptimalPixelMoveAndAlignedImage)."""
    if pad <= 0:
        return image
    H, W = image.shape
    ext = np.pad(image, pad, mode="constant", constant_values=bg_value)
    best = None  # (score, move_magnitude, shifted)
    for dy in range(-pad, pad + 1):
        for dx in range(-pad, pad + 1):
            shifted = ext[dy + pad:dy + pad + H, dx + pad:dx + pad + W]
            score = float(shifted[cell_binary].sum())
            mag = abs(dx) + abs(dy)
            if best is None or score > best[0] + 1e-9 or (abs(score - best[0]) < 1e-9 and mag < best[1]):
                best = (score, mag, shifted)
    return best[2]


def quantify_cellx(label, channels, params):
    """CellXMaskInterface features. Returns (columns, ids, values).

    Columns: the CellX geometry block (channel-independent) then, per channel, the
    fluo block (background + per-region total & configurable quantiles plus bright
    euler). Quantiles come from params['quantiles'] (percentages, default
    [25,50,75]). To match native CellX's 16-bit intensity convention, 8/12-bit
    images are optionally upscaled to 16-bit (params['scale_to_16bit'])."""
    label = np.asarray(label, dtype=np.int64)
    ids = _label_ids(label)
    n = ids.shape[0]

    membrane_width = int(params.get("membrane_width", 2))
    nvf = float(params.get("nuclear_volume_fraction", 0.07))
    bright_pct = float(params.get("bright_area_percentage", 0.3))
    n_gauss = int(params.get("intensity_classes_count", 3))
    wiener = int(params.get("smoothing_size", 3))
    use_wiener = bool(params.get("use_wiener", True))
    quantiles_pct = [float(p) for p in (params.get("quantiles") or [25.0, 50.0, 75.0])]
    qfracs = [p / 100.0 for p in quantiles_pct]
    scale16 = bool(params.get("scale_to_16bit", True))
    bitdepths = params.get("channel_bitdepths") or []
    align_move = int(params.get("fluo_align_pixel_move", 0))

    feat_stems = cellx_feature_names(quantiles_pct)
    sfx = _channel_suffixes(params, len(channels))

    columns = [{"name": g, "dtype": "double"} for g in _CELLX_GEOMETRY]
    for c in range(len(channels)):
        for f in feat_stems:
            columns.append({"name": "fluo.%s.%s" % (f, sfx[c]), "dtype": "double"})

    values = np.zeros((n, len(columns)), dtype=np.float64)
    if n == 0:
        return columns, ids.tolist(), values

    geom = _cell_geometry(label, ids, membrane_width, nvf)
    cell_binary = label > 0
    n_geo = len(_CELLX_GEOMETRY)
    n_feat = len(feat_stems)

    # geometry block (channel-independent)
    for r, cid in enumerate(ids):
        g = geom.get(int(cid))
        if g is None:
            continue
        mem_area = int(g.mem_mask.sum())
        mem_volume = g.cell_volume - g.cyto_volume
        nuc_area = float(np.pi * g.nuc_radius * g.nuc_radius)
        # native rounds every geometry column except orientation (round half away
        # from zero); cellVolume/mem.area/mem.volume are already integer-valued.
        values[r, 0:n_geo] = [_mround(g.major_axis), _mround(g.minor_axis), g.orientation,
                              _mround(g.cell_volume), _mround(g.perimeter), _mround(mem_area),
                              _mround(mem_volume), _mround(g.nuc_radius), _mround(nuc_area)]

    for c, ch in enumerate(channels):
        ch = np.asarray(ch, dtype=np.float64)
        if ch.shape != label.shape:
            raise ValueError(_size_mismatch_msg(c, ch.shape, label.shape))
        # match native CellX, which works in 16-bit: upscale lower-bit images.
        if scale16 and c < len(bitdepths):
            bd = int(bitdepths[c]) if bitdepths[c] else 16
            if 0 < bd < 16:
                ch = ch * (65535.0 / (2 ** bd - 1))
        bg_mean, bg_std = _background_gmm(ch, ~cell_binary, n_gauss)
        # CellX fluoAlignPixelMove: shift the fluo image to best register it with
        # the mask before measuring. Background is computed pre-alignment (and used
        # to pad); region measurements use the aligned image.
        if align_move > 0:
            ch = _align_fluo(ch, cell_binary, align_move, bg_mean)
        smoothed = ndi.median_filter(ch, size=wiener, mode="nearest") if use_wiener else ch
        base = n_geo + c * n_feat
        for r, cid in enumerate(ids):
            g = geom.get(int(cid))
            if g is None:
                continue
            sl = g.bbox
            cell_vals = ch[sl][g.cell_mask]
            mem_vals = ch[sl][g.mem_mask]
            nuc_vals = ch[_nucleus_pixels(ch, g, g.nuc_radius)]
            bright_vals, euler = _bright_area(smoothed, g, bright_pct)

            row = [bg_mean, bg_std]
            for region_vals in (cell_vals, mem_vals, nuc_vals, bright_vals):
                row.append(float(region_vals.sum()))
                row.extend(_quantiles_at(region_vals, qfracs))
            row.append(float(euler))
            values[r, base:base + n_feat] = row

    return columns, ids.tolist(), values


# CellX online tracker (CellXTrackerYouScope)

def _frame_props(label, ids):
    """centroid(x,y), area, majorAxisLength, orientation(deg) per id."""
    props = {int(p.label): p for p in measure.regionprops(label)}
    cent = np.zeros((len(ids), 2), dtype=np.float64)
    major = np.zeros(len(ids), dtype=np.float64)
    orient = np.zeros(len(ids), dtype=np.float64)
    area = np.zeros(len(ids), dtype=np.float64)
    for i, cid in enumerate(ids):
        p = props.get(int(cid))
        if p is None:
            continue
        cent[i, 0] = p.centroid[1]  # x = col
        cent[i, 1] = p.centroid[0]  # y = row
        major[i] = _p_major(p)
        # skimage orientation is radians ccw from row-axis; CellX uses MATLAB
        # regionprops Orientation in degrees. Convert to degrees.
        orient[i] = np.degrees(p.orientation)
        area[i] = p.area
    return cent, area, major, orient


def _overlap_costs(prevL, curL, prev_ids, cur_ids):
    """1 - overlap fraction, both directions. prevL/curL are relabelled to
    1..n by ascending id order matching prev_ids/cur_ids."""
    npv, ncur = len(prev_ids), len(cur_ids)
    o12 = np.ones((npv, ncur))
    o21 = np.ones((ncur, npv))
    prev_index = {int(v): i for i, v in enumerate(prev_ids)}
    cur_index = {int(v): i for i, v in enumerate(cur_ids)}
    prev_area = {int(v): int((prevL == int(v)).sum()) for v in prev_ids}
    cur_area = {int(v): int((curL == int(v)).sum()) for v in cur_ids}
    both = (prevL > 0) & (curL > 0)
    pv = prevL[both]
    cv = curL[both]
    if pv.size:
        pairs, counts = np.unique(np.stack([pv, cv], axis=1), axis=0, return_counts=True)
        for (a, b), cnt in zip(pairs, counts):
            i = prev_index.get(int(a)); j = cur_index.get(int(b))
            if i is None or j is None:
                continue
            o12[i, j] = 1.0 - cnt / max(1, prev_area[int(a)])
            o21[j, i] = 1.0 - cnt / max(1, cur_area[int(b)])
    return o12, o21


def _shape_cost_pair(maj1, ori1, maj2, ori2):
    """CellX shape cost (symmetric average of the two normalisations)."""
    maxc = np.sqrt(0.25 ** 2 + 0.25 ** 2 + 0.5 ** 2)
    ck = np.array([maj1, maj1, round(ori1)])
    cl = np.array([maj2, maj2, round(ori2)])
    refk = np.array([ck[0], ck[1], 180.0]); refk[refk == 0] = 1e-9
    refl = np.array([cl[0], cl[1], 180.0]); refl[refl == 0] = 1e-9
    s12 = np.sqrt((((ck - cl) / refk) ** 2).sum()) / maxc
    s21 = np.sqrt((((ck - cl) / refl) ** 2).sum()) / maxc
    return 0.5 * (s12 + s21)


def track_cellx(prev_label, cur_label, params):
    """Frame-to-frame CellX linking with online gap closing.

    Returns (assign, prev_ids, cur_ids, cur_geom, gap_assign):
      assign[j]     = index into prev_ids that current cell j links to, or -1.
      cur_geom      = dict of per-current-cell arrays (cx, cy, major, orient, area)
                      so the caller can cache geometry for the lost-track pool.
      gap_assign[j] = a reconnected lost TRACK id for current cell j (0 = none),
                      only for cells not linked to the previous frame. Gap closing
                      reconnects a track lost <= maxGapConnectionDistance frames ago
                      to a new cell using the CellX distance + shape cost
                      (CellXTrackerStartEndGapConnector, online adaptation)."""
    prev_label = np.asarray(prev_label, dtype=np.int64)
    cur_label = np.asarray(cur_label, dtype=np.int64)
    prev_ids = _label_ids(prev_label)
    cur_ids = _label_ids(cur_label)
    npv, ncur = len(prev_ids), len(cur_ids)

    cc, carea, cmaj, cori = _frame_props(cur_label, cur_ids) if ncur else (
        np.zeros((0, 2)), np.zeros(0), np.zeros(0), np.zeros(0))
    cur_geom = {"cx": cc[:, 0].tolist() if ncur else [],
                "cy": cc[:, 1].tolist() if ncur else [],
                "major": cmaj.tolist(), "orient": cori.tolist(), "area": carea.tolist()}

    assign = [-1] * ncur
    if ncur and npv:
        spatial_motion = float(params.get("spatial_motion_factor", 50.0))
        pc, _, pmaj, pori = _frame_props(prev_label, prev_ids)
        o12, o21 = _overlap_costs(prev_label, cur_label, prev_ids, cur_ids)
        d = np.sqrt(((pc[:, None, :] - cc[None, :, :]) ** 2).sum(axis=2)) / spatial_motion
        maxc = np.sqrt(0.25 ** 2 + 0.25 ** 2 + 0.5 ** 2)
        # shape cost, vectorised: the descriptor is [major, major, round(orient)]
        # normalised per-cell by [major, major, 180]. s12 normalises by the previous
        # cell's major, s21 by the current cell's; both share the same difference.
        rpo = np.round(pori); rco = np.round(cori)
        dmaj = pmaj[:, None] - cmaj[None, :]
        dori = (rpo[:, None] - rco[None, :]) / 180.0
        refk0 = np.where(pmaj[:, None] == 0, 1e-9, pmaj[:, None])
        refl0 = np.where(cmaj[None, :] == 0, 1e-9, cmaj[None, :])
        s12 = np.sqrt(2.0 * (dmaj / refk0) ** 2 + dori ** 2) / maxc          # (npv, ncur)
        s21T = np.sqrt(2.0 * (dmaj / refl0) ** 2 + dori ** 2) / maxc         # (npv, ncur) == s21.T
        joint = 1.0 * (o12 + o21.T) + 0.5 * (d + d.T.T) + 0.5 * (s12 + s21T)
        terminal = 2.0 * (1.0 + 0.5 + 0.5)
        term = np.full((npv, npv), np.inf); np.fill_diagonal(term, terminal)
        cost = np.concatenate([joint, term], axis=1)
        row_ind, col_ind = _linear_sum_assignment(np.where(np.isinf(cost), 1e9, cost))
        for i, jcol in zip(row_ind, col_ind):
            if jcol < ncur and cost[i, jcol] < terminal:
                assign[int(jcol)] = int(i)

    # Online gap closing (CellXTrackerStartEndGapConnector, online form)
    # Reconnect current cells the frame-to-frame linker left unmatched, using the
    # CellX distance+shape cost with a permissive threshold. Two sources of
    # candidates: (a) previous-frame cells that were themselves unmatched -> a
    # gap-1 reconnection, folded into `assign` (returns a prev label id); and
    # (b) an older lost-track pool (gap 2..maxGap) passed by the caller ->
    # `gap_assign` (returns a track id).
    gap_assign = [0] * ncur
    max_gap = int(params.get("max_gap_connection_distance", 0))
    if max_gap > 0 and ncur:
        spatial_motion = float(params.get("spatial_motion_factor", 50.0))
        gap_terminal = 2.0 * (0.5 + 0.5)  # distance plus shape portion of the terminal cost
        unmatched_cur = [j for j in range(ncur) if assign[j] == -1]

        # (a) gap-1: unmatched current vs unmatched previous cells
        if npv and unmatched_cur:
            matched_prev = set(i for i in assign if i >= 0)
            unmatched_prev = [i for i in range(npv) if i not in matched_prev]
            cands = []
            for j in unmatched_cur:
                for i in unmatched_prev:
                    dist = np.hypot(pc[i, 0] - cc[j, 0], pc[i, 1] - cc[j, 1]) / spatial_motion
                    shape = _shape_cost_pair(pmaj[i], pori[i], cmaj[j], cori[j])
                    c = dist + shape
                    if c < gap_terminal:
                        cands.append((c, j, i))
            cands.sort(key=lambda t: t[0])
            used_j, used_i = set(), set()
            for c, j, i in cands:
                if j in used_j or i in used_i:
                    continue
                assign[j] = i  # fold into the normal prev linkage
                used_j.add(j); used_i.add(i)
            unmatched_cur = [j for j in unmatched_cur if assign[j] == -1]

        # (b) gap 2..maxGap: unmatched current vs the older lost-track pool
        lost = params.get("lost") or []
        if lost and unmatched_cur:
            cands = []
            for j in unmatched_cur:
                for lo in lost:
                    if int(lo.get("gap", 2)) > max_gap:
                        continue
                    dist = np.hypot(cc[j, 0] - float(lo["cx"]), cc[j, 1] - float(lo["cy"])) / spatial_motion
                    shape = _shape_cost_pair(float(lo["major"]), float(lo["orient"]), cmaj[j], cori[j])
                    c = dist + shape
                    if c < gap_terminal:
                        cands.append((c, j, int(lo["track"])))
            cands.sort(key=lambda t: t[0])
            used_j, used_t = set(), set()
            for c, j, tid in cands:
                if j in used_j or tid in used_t:
                    continue
                gap_assign[j] = tid
                used_j.add(j); used_t.add(tid)

    return assign, prev_ids.tolist(), cur_ids.tolist(), cur_geom, gap_assign
