#/*******************************************************************************
# * Copyright (c) 2026 Andreas P. Cuny
# * All rights reserved. This program and the accompanying materials
# * are made available under the terms of the GNU Public License v2.0
# * which accompanies this distribution, and is available at
# * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
# *
# * Contributors:
# *     Andreas P. Cuny - initial API and implementation
# ******************************************************************************/

# YouScope GraalPy compatibility preamble.
# Loaded once per Context by GraalPyScriptEngine before the first eval().
# Provides: YouScope bindings refresh, CPython bridge, openBIS spool helper.

# Polyglot binding refresh
# The _refresh_bindings() function is called at the start of each eval()
# via GraalPyScriptEngine to pull Java objects into Python globals.


_YOUSCOPE_BINDINGS = [
    # Always-available bindings (every script console / job context):
    "youscopeServer",       # YouScopeServer RMI interface -- server.getMicroscope() etc.
    "youscopeClient",       # YouScopeClient -- client-side GUI/addon interactions
    # Measurement / job context bindings (only set when running inside a job):
    "microscope",
    "measurementContext",
    "jobs",
    "evaluationNumber",
    "well",
    "position",
    "executionInformation",
    "scriptInterface",
]

def _refresh_bindings():
    import builtins as _b
    _g = globals()
    for _name in _YOUSCOPE_BINDINGS:
        try:
            import polyglot as _p
            _val = _p.import_value(_name)
            if _val is not None:
                _g[_name] = _val
                setattr(_b, _name, _val)
        except BaseException:
            pass

# Note: called by GraalPyScriptEngine.eval() before each user script.
# Not called here at module load time (polyglotBindings null at that point).


# CPython bridge
# Calls a Python script in the system CPython/conda environment.
# This enables cellpose, torch, OpenCV, scikit-image from GraalPy scripts.
def _get_cpython_exe():
    """
    Find the CPython executable to use for the bridge.
    Checks (in order):
      1. graalpy_config.txt in YouScope dir (written by setup_graalpy_venv.ps1)
      2. 'python' on system PATH (conda base or system Python)
      3. 'python3' on system PATH
    """
    import os, java as _java
    youscope_dir = _java.type("java.lang.System").getProperty("user.dir", ".")
    config_path = os.path.join(youscope_dir, "graalpy_config.txt")

    if os.path.exists(config_path):
        with open(config_path) as f:
            for line in f:
                if line.startswith("cpython_executable="):
                    exe = line.strip().split("=", 1)[1]
                    if os.path.exists(exe):
                        return exe
    # Fallback to PATH
    import subprocess
    for candidate in ["python", "python3"]:
        try:
            r = subprocess.run([candidate, "-c", "import sys; print(sys.executable)"],
                               capture_output=True, text=True, timeout=5)
            if r.returncode == 0:
                return candidate
        except Exception:
            pass
    return None


def run_in_cpython(script_path, timeout=300, **kwargs):
    """
    Run a Python script in the CPython environment and return its JSON output.

    The script must write its result as JSON to stdout on the last line.

    Example usage in a YouScope script:
        result = run_in_cpython(
            "C:/YouScope/scripts/run_cellpose.py",
            image_path="C:/data/img.tif",
            model="cpsam",
            diameter=0
        )
        n_cells = result["n_cells"]

    The script receives kwargs as JSON via the environment variable
    YOUSCOPE_ARGS, and should print a JSON result to stdout.
    """
    import subprocess, json, os

    cpython = _get_cpython_exe()
    if cpython is None:
        raise RuntimeError(
            "No CPython executable found. "
            "Install Miniconda and run setup_graalpy_venv.ps1, "
            "or ensure 'python' is on PATH."
        )

    env = os.environ.copy()
    env["YOUSCOPE_ARGS"] = json.dumps(kwargs)

    r = subprocess.run(
        [cpython, script_path],
        capture_output=True, text=True,
        timeout=timeout, env=env
    )

    if r.returncode != 0:
        raise RuntimeError(
            f"CPython script failed (exit {r.returncode}):\n"
            f"STDERR: {r.stderr[-2000:] if r.stderr else '(none)'}"
        )

    # Parse last non-empty line as JSON result
    lines = [l.strip() for l in r.stdout.split("\n") if l.strip()]
    if not lines:
        return {}
    try:
        return json.loads(lines[-1])
    except json.JSONDecodeError:
        # Return all stdout as string if not JSON
        return {"stdout": r.stdout, "stderr": r.stderr}


def run_cellpose(image_array, model="cpsam", diameter=0, gpu=False,
                 channels=None, script=None, timeout=120):
    """
    High-level helper: segment cells using Cellpose via CPython bridge.

    Args:
        image_array: numpy array (H x W) or (H x W x C) from YouScope image
        model: Cellpose 4 model name ('cpsam', 'cpsam_v2', 'cpdino', etc.)
        diameter: expected cell diameter in pixels (0 = auto)
        gpu: use GPU if available
        channels: [cytoplasm_ch, nucleus_ch] (default [0, 0] = grayscale)
        script: path to custom cellpose script (uses built-in if None)
        timeout: seconds to wait for segmentation

    Returns:
        dict with keys: n_cells, masks_flat, height, width

    Example:
        import numpy as np
        img = np.array(...)  # your microscope image
        result = run_cellpose(img, model='cpsam')
        print(f"Found {result['n_cells']} cells")
        masks = np.array(result['masks_flat']).reshape(result['height'], result['width'])
    """
    import os, json, tempfile, java as _java

    # Write image to temp file as numpy .npy
    try:
        import numpy as np
        tmp = tempfile.mktemp(suffix=".npy")
        np.save(tmp, image_array)
        image_path = tmp
        has_numpy = True
    except ImportError:
        # Fallback: pass image as list (slow for large images)
        image_path = None
        has_numpy = False

    youscope_dir = _java.type("java.lang.System").getProperty("user.dir", ".")
    if script is None:
        script = os.path.join(youscope_dir, "scripts", "run_cellpose.py")

        # Auto-create the script if it doesn't exist
        os.makedirs(os.path.dirname(script), exist_ok=True)
        if not os.path.exists(script):
            _write_cellpose_bridge_script(script)

    kwargs = dict(model=model, diameter=diameter, gpu=gpu,
                  channels=channels or [0, 0])
    if image_path:
        kwargs["image_npy"] = image_path

    result = run_in_cpython(script, timeout=timeout, **kwargs)

    if image_path and has_numpy and os.path.exists(image_path):
        try:
            os.remove(image_path)
        except Exception:
            pass

    return result


def _write_cellpose_bridge_script(path):
    """Write the default cellpose bridge script to YouScope/scripts/."""
    script = '''#!/usr/bin/env python3
# run_cellpose.py
# Called by YouScope GraalPy via run_cellpose() / run_in_cpython().
# Reads YOUSCOPE_ARGS from environment, runs Cellpose, prints JSON result.

import os, sys, json
import numpy as np

args = json.loads(os.environ.get("YOUSCOPE_ARGS", "{}"))

model_name = args.get("model", "cpsam")
diameter   = args.get("diameter", 0)
gpu        = args.get("gpu", False)
channels   = args.get("channels", [0, 0])
image_npy  = args.get("image_npy")

if image_npy and os.path.exists(image_npy):
    img = np.load(image_npy)
else:
    print(json.dumps({"error": "No image provided", "n_cells": 0}))
    sys.exit(1)

try:
    from cellpose import models
    model = models.CellposeModel(gpu=gpu, pretrained_model=model_name)
    masks, flows, styles = model.eval(img, diameter=diameter,
                                      channels=channels)
    n_cells = int(masks.max())
    # Output result as JSON (last line of stdout)
    result = {
        "n_cells":    n_cells,
        "masks_flat": masks.flatten().tolist(),
        "height":     masks.shape[0],
        "width":      masks.shape[1],
        "model":      model_name,
    }
    print(json.dumps(result))
    sys.exit(0)
except Exception as e:
    print(json.dumps({"error": str(e), "n_cells": 0}))
    sys.exit(1)
'''
    with open(path, "w") as f:
        f.write(script)



# Transparent import system: GraalPy venv first, CPython fallback
# Users can write "import numpy as np" and it works transparently.
# If the module is not in the GraalPy venv, it is proxied from CPython.

import importlib
import importlib.abc
import importlib.machinery
import sys as _sys

# Modules that cannot work in GraalPy (C extensions) go straight to CPython
# Without this list, GraalPy would attempt them and produce confusing errors
_CPYTHON_ONLY_MODULES = set({
    # Pre-known C-extension packages: go straight to CPython
    # New packages added dynamically when ImportError is caught
    "numpy", "np",
    "matplotlib", "matplotlib.pyplot", "plt",
    "scipy", "sklearn", "skimage",
    "cv2", "PIL", "imageio",
    "torch", "torchvision",
    "cellpose",
    "zarr", "ome_zarr",
})

# Track which modules have been proxied to avoid infinite recursion
_cpython_proxied = {}


class _CPythonFallbackFinder(importlib.abc.MetaPathFinder):
    """
    Import hook: catches failed GraalPy imports and routes to CPython.

    This finder is registered last on sys.meta_path so it only runs when
    all normal GraalPy import mechanisms have already failed. This means:
    - Pure-Python packages in graalpy-venv import normally (no proxy overhead)
    - C-extension packages (numpy, matplotlib etc.) fall through to this proxy
    - Newly pip-installed CPython packages are immediately available
    - No restart required after installing new packages with uv pip install
    """

    def find_module(self, fullname, path=None):
        # Only intercept known-CPython modules and modules already proxied.
        # For unknown modules, let GraalPy fail naturally first.
        # The _CPYTHON_ONLY_MODULES set is just an optimisation to skip
        # GraalPy's slow failure path for known C-extension packages.
        base = fullname.split(".")[0]
        if (base in _CPYTHON_ONLY_MODULES
                or fullname in _CPYTHON_ONLY_MODULES
                or fullname in _cpython_proxied):
            return self
        return None

    def load_module(self, fullname):
        if fullname in _cpython_proxied:
            return _cpython_proxied[fullname]
        proxy = _CPythonModuleProxy(fullname)
        _cpython_proxied[fullname] = proxy
        _sys.modules[fullname] = proxy
        return proxy

    @classmethod
    def proxy_on_import_error(cls, fullname):
        """Call this when an ImportError occurs for fullname."""
        if fullname not in _cpython_proxied:
            _CPYTHON_ONLY_MODULES.add(fullname.split(".")[0])
            proxy = _CPythonModuleProxy(fullname)
            _cpython_proxied[fullname] = proxy
            _sys.modules[fullname] = proxy
        return _cpython_proxied[fullname]


class _CPythonModuleProxy:
    """
    Proxy for a CPython module. Attribute access and calls are
    transparently executed in the CPython environment.
    Supports: np.zeros(), plt.imshow(), skimage.filters.gaussian() etc.
    """

    def __init__(self, module_name):
        object.__setattr__(self, "_mod", module_name)
        object.__setattr__(self, "_cache", {})

    def __repr__(self):
        return "<CPythonProxy[%s]>" % object.__getattribute__(self, "_mod")

    def __getattr__(self, attr):
        mod = object.__getattribute__(self, "_mod")
        cache = object.__getattribute__(self, "_cache")
        key = "%s.%s" % (mod, attr)

        # Return sub-module proxy for dotted access (matplotlib.pyplot)
        # We can't know if it's a submodule or function without asking CPython
        # Use a callable proxy that handles both cases
        if key not in cache:
            cache[key] = _CPythonAttrProxy(mod, attr)
        return cache[key]


class _CPythonAttrProxy:
    """Proxy for a module attribute. Handles both function calls and sub-attributes."""

    def __init__(self, module, attr):
        self._module = module
        self._attr   = attr
        self._cache  = {}

    def __call__(self, *args, **kwargs):
        return _cpython_call_attr(self._module, self._attr, args, kwargs)

    def __getattr__(self, attr):
        if attr.startswith("_"):
            raise AttributeError(attr)
        key = attr
        if key not in self._cache:
            self._cache[key] = _CPythonAttrProxy(
                "%s.%s" % (self._module, self._attr), attr)
        return self._cache[key]

    def __repr__(self):
        return "<CPythonAttr[%s.%s]>" % (self._module, self._attr)


def _cpython_call_attr(module, attr, args=(), kwargs=None):
    """Execute module.attr(*args, **kwargs) in CPython, return JSON result."""
    import os, json, tempfile as _tf
    import java as _java
    youscope_dir = _java.type("java.lang.System").getProperty("user.dir", ".")
    kwargs = kwargs or {}

    # Serialise arguments: lists/numbers pass through, numpy arrays use .tolist()
    def _serialise(v):
        if hasattr(v, "tolist"):   return v.tolist()
        if hasattr(v, "_mod"):     return None  # proxy: skip
        return v

    safe_args   = [_serialise(a) for a in args]
    safe_kwargs = {k: _serialise(v) for k, v in kwargs.items()}

    script = os.path.join(youscope_dir, "scripts", "_cpython_attr_call.py")
    if not os.path.exists(script):
        os.makedirs(os.path.dirname(script), exist_ok=True)
        with open(script, "w") as f:
            f.write("""import os, json, importlib

data = json.loads(os.environ.get("YOUSCOPE_ARGS", "{}"))
module_name = data["module"]
attr_name   = data["attr"]
args        = data.get("args", [])
kwargs      = data.get("kwargs", {})
result_path = data.get("result_path")

parts = module_name.split(".")
mod = importlib.import_module(parts[0])
for p in parts[1:]:
    mod = getattr(mod, p)
fn = getattr(mod, attr_name)
result = fn(*args, **kwargs)

# Serialise result
if hasattr(result, "tolist"):  result = result.tolist()
elif hasattr(result, "item"):  result = result.item()
elif hasattr(result, "__iter__") and not isinstance(result, (str, dict)):
    try: result = list(result)
    except Exception: result = str(result)

out = json.dumps({"ok": True, "result": result})
if result_path:
    with open(result_path, "w") as f: f.write(out)
else:
    print(out)
""")

    result_path = _tf.mktemp(suffix=".json")
    run_in_cpython(
        script,
        module=module, attr=attr,
        args=safe_args, kwargs=safe_kwargs,
        result_path=result_path)

    if os.path.exists(result_path):
        with open(result_path) as f:
            data = json.load(f)
        try: os.remove(result_path)
        except Exception: pass
        return data.get("result")
    return None


# Register the import fallback hook
# Only add it once (idempotent)
_hook_class = _CPythonFallbackFinder
if not any(isinstance(h, _hook_class) for h in _sys.meta_path):
    _sys.meta_path.append(_CPythonFallbackFinder())

# Also pre-proxy the most common scientific modules
# so "import numpy as np" works immediately
def _preproxy_module(name):
    proxy = _CPythonModuleProxy(name)
    _cpython_proxied[name] = proxy
    _sys.modules[name] = proxy
    return proxy

# Pre-create proxies for common modules users expect to work
numpy      = _preproxy_module("numpy")
matplotlib = _preproxy_module("matplotlib")
skimage    = _preproxy_module("skimage")
imageio    = _preproxy_module("imageio")
cv2        = _preproxy_module("cv2")
scipy      = _preproxy_module("scipy")

# matplotlib.pyplot shortcut (users always do "import matplotlib.pyplot as plt")
_plt_proxy = _CPythonModuleProxy("matplotlib.pyplot")
_sys.modules["matplotlib.pyplot"] = _plt_proxy


# CPython transparency layer
# Lets users write normal Python in the GraalPy console and use packages
# (numpy, matplotlib, scikit-image, imageio) that only exist in the CPython env.
#
# Three ways to use CPython packages from GraalPy scripts:
#
# 1. Magic imports (most transparent):
#       np = cpython_import("numpy")
#       img = np.zeros((512, 512), dtype="uint16")
#
# 2. @cpython decorator:
#       @cpython
#       def segment_cells(image_array, model="cpsam"):
#           from cellpose import models
#           m = models.CellposeModel(pretrained_model=model)
#           masks, _, _ = m.eval(image_array, diameter=0)
#           return masks.tolist()
#
# 3. exec_in_cpython() for ad-hoc blocks:
#       result = exec_in_cpython("""
#           import numpy as np
#           img = np.random.randint(0, 255, (512, 512), dtype=np.uint8)
#           n_cells = 42  # run cellpose here
#       """, returns=["n_cells", "img"])

import os as _os, json as _json, tempfile as _tempfile

def cpython_import(module_name, as_name=None):
    """
    Import a Python module from the CPython environment and return a proxy.

    The proxy intercepts attribute access and method calls, executing them
    in CPython via the bridge and returning the results to GraalPy.

    Usage:
        np = cpython_import("numpy")
        arr = np.zeros((100, 100))          # runs in CPython, returns list
        arr_sum = np.sum(arr)               # returns float
        img = np.frombuffer(raw, dtype=np.uint8).reshape(h, w)

    Limitations:
        - Return values are JSON-serialised (numbers, lists, strings, dicts)
        - For large arrays, use run_cellpose() or exec_in_cpython() instead
        - Cannot pass GraalPy objects (Java proxy objects) directly to CPython
    """
    return _CPythonModuleProxy(module_name, as_name or module_name)


class _CPythonModuleProxy:
    """Proxy for a CPython module. Attribute access runs code in CPython."""

    def __init__(self, module_name, display_name=None):
        object.__setattr__(self, "_module_name", module_name)
        object.__setattr__(self, "_display_name", display_name or module_name)
        object.__setattr__(self, "_attr_cache", {})

    def __repr__(self):
        return "<CPythonProxy: %s>" % object.__getattribute__(self, "_display_name")

    def __getattr__(self, attr):
        mod = object.__getattribute__(self, "_module_name")
        cache = object.__getattribute__(self, "_attr_cache")
        if attr in cache:
            return cache[attr]

        def _proxy_call(*args, **kwargs):
            return _cpython_call(mod, attr, args, kwargs)

        cache[attr] = _proxy_call
        return _proxy_call


def _cpython_call(module, func, args=(), kwargs=None):
    """
    Call module.func(*args, **kwargs) in CPython and return the JSON result.
    """
    import java as _j
    youscope_dir = _j.type("java.lang.System").getProperty("user.dir", ".")
    kwargs = kwargs or {}

    script = _os.path.join(youscope_dir, "scripts", "cpython_call.py")
    if not _os.path.exists(script):
        _os.makedirs(_os.path.dirname(script), exist_ok=True)
        with open(script, "w") as f:
            f.write("""import os, sys, json

args_file = os.environ.get("YOUSCOPE_ARGS_FILE")
if args_file:
    with open(args_file) as f:
        args = json.load(f)
else:
    args = json.loads(os.environ.get("YOUSCOPE_ARGS", "{}"))

module_name = args["module"]
func_name   = args["func"]
call_args   = args.get("args", [])
call_kwargs = args.get("kwargs", {})
result_file = args.get("result_file")

import importlib
mod  = importlib.import_module(module_name)
func = getattr(mod, func_name)
result = func(*call_args, **call_kwargs)

# Serialize result
if hasattr(result, "tolist"):   result = result.tolist()
elif hasattr(result, "item"):   result = result.item()

out = json.dumps({"result": result, "error": None})
if result_file:
    with open(result_file, "w") as f: f.write(out)
else:
    print(out)
""")

    result_file = _tempfile.mktemp(suffix=".json")
    result = run_in_cpython(
        script,
        module=module, func=func,
        args=list(args), kwargs=kwargs,
        result_file=result_file)

    if result_file and _os.path.exists(result_file):
        with open(result_file) as f:
            data = _json.load(f)
        try: _os.remove(result_file)
        except Exception: pass
        if data.get("error"):
            raise RuntimeError("CPython call %s.%s failed: %s" % (module, func, data["error"]))
        return data.get("result")
    return result.get("result")


def cpython(func):
    """
    Decorator: run this function entirely in the CPython environment.

    Usage:
        @cpython
        def analyze(image_bytes, width, height):
            import numpy as np
            from skimage import filters
            img = np.frombuffer(image_bytes, dtype=np.uint8).reshape(height, width)
            return float(filters.threshold_otsu(img))

        threshold = analyze(raw_bytes, 1024, 1024)

    The decorated function's SOURCE CODE is sent to CPython and executed there.
    Arguments must be JSON-serialisable (numbers, strings, lists, bytes as list).
    Return value must be JSON-serialisable.
    """
    import inspect as _inspect

    def _wrapper(*args, **kwargs):
        import java as _j
        youscope_dir = _j.type("java.lang.System").getProperty("user.dir", ".")

        # Get function source and strip the @cpython decorator line
        src_lines = _inspect.getsource(func).split("
")
        src_lines = [l for l in src_lines if not l.strip().startswith("@cpython")]
        func_src = "
".join(src_lines)

        script = _os.path.join(youscope_dir, "scripts", "cpython_decorated.py")
        _os.makedirs(_os.path.dirname(script), exist_ok=True)
        with open(script, "w") as f:
            f.write("""import os, json

args_data = json.loads(os.environ.get("YOUSCOPE_ARGS", "{}"))
func_src  = args_data["func_src"]
func_name = args_data["func_name"]
call_args = args_data.get("args", [])
call_kw   = args_data.get("kwargs", {})

exec(func_src, globals())
result = globals()[func_name](*call_args, **call_kw)
if hasattr(result, "tolist"): result = result.tolist()
elif hasattr(result, "item"): result = result.item()
print(json.dumps({"result": result}))
""")

        r = run_in_cpython(
            script,
            func_src=func_src,
            func_name=func.__name__,
            args=list(args),
            kwargs=kwargs)
        return r.get("result") if isinstance(r, dict) else r

    _wrapper.__name__ = func.__name__
    _wrapper.__doc__  = (func.__doc__ or "") + " [runs in CPython]"
    return _wrapper


def exec_in_cpython(code, returns=None, **context):
    """
    Execute a block of Python code in the CPython environment.

    Args:
        code:    Python source code string to execute
        returns: list of variable names to return to GraalPy
        **context: variables to inject into the CPython execution namespace
                   (must be JSON-serialisable)

    Returns:
        dict of {name: value} for each name in `returns`, or {} if returns=None

    Example:
        result = exec_in_cpython("""
            import numpy as np
            from skimage.filters import gaussian
            img_smoothed = gaussian(img, sigma=2)
            mean_val = float(img_smoothed.mean())
        """, returns=["mean_val"], img=my_image_list)
        print("Mean:", result["mean_val"])
    """
    import java as _j
    youscope_dir = _j.type("java.lang.System").getProperty("user.dir", ".")

    script = _os.path.join(youscope_dir, "scripts", "exec_in_cpython.py")
    _os.makedirs(_os.path.dirname(script), exist_ok=True)
    with open(script, "w") as f:
        f.write("""import os, json

data     = json.loads(os.environ.get("YOUSCOPE_ARGS", "{}"))
code     = data["code"]
returns  = data.get("returns") or []
ctx      = data.get("context") or {}

ns = dict(ctx)
exec(compile(code, "<cpython_exec>", "exec"), ns)

out = {}
for k in returns:
    v = ns.get(k)
    if hasattr(v, "tolist"):  v = v.tolist()
    elif hasattr(v, "item"):  v = v.item()
    out[k] = v
print(json.dumps(out))
""")

    return run_in_cpython(
        script,
        code=code,
        returns=returns or [],
        context=context)


# Image conversion and display helpers
# These functions work within GraalPy's constraints:
# - toNumpyImage: uses array module (stdlib). No numpy needed in GraalPy venv
# - displayImage/saveImage: delegate to CPython env where matplotlib/imageio live

def toNumpyImage(imageEvent):
    """
    Convert a YouScope ImageEvent to an image array.

    In GraalPy: returns a list-of-lists (compatible with matplotlib.imshow,
    tifffile, and the CPython bridge functions).

    Usage:
        img = toNumpyImage(imageEvent)
        displayImage(img)           # shows via CPython/matplotlib
        saveImage(imageEvent, path) # saves via CPython/tifffile
        result = run_cellpose(img)  # segments via CPython/cellpose
    """
    import array as _arr

    width        = imageEvent.getWidth()
    height       = imageEvent.getHeight()
    bpp          = imageEvent.getBytesPerPixel()
    bit_depth    = imageEvent.getBitDepth()
    raw          = bytes(imageEvent.getImageData())
    fmt          = "B" if bpp == 1 else "H"
    flat         = _arr.array(fmt, raw)
    max_val      = float((1 << bit_depth) - 1) if bit_depth > 0 else 65535.0

    # Build list-of-lists normalised to 0-255 uint8 for display
    rows = []
    for r in range(height):
        row = flat[r * width:(r + 1) * width]
        if bpp == 1:
            rows.append(list(row))
        else:
            # Normalise 16-bit to 8-bit for display
            rows.append([int(v * 255.0 / max_val) for v in row])

    if imageEvent.isTransposeY():
        rows = rows[::-1]
    if imageEvent.isTransposeX():
        rows = [r[::-1] for r in rows]
    if imageEvent.isSwitchXY():
        rows = [[rows[r][c] for r in range(height)] for c in range(width)]

    return rows


def saveImage(imageEvent, filename):
    """
    Save a YouScope ImageEvent as TIFF via CPython bridge (uses tifffile + numpy).
    filename: full path to output .tif file
    """
    import os, tempfile, struct

    width  = imageEvent.getWidth()
    height = imageEvent.getHeight()
    bpp    = imageEvent.getBytesPerPixel()
    raw    = bytes(imageEvent.getImageData())

    import java as _java
    youscope_dir = _java.type("java.lang.System").getProperty("user.dir", ".")
    tmp_bin = tempfile.mktemp(suffix=".bin")
    with open(tmp_bin, "wb") as f:
        f.write(struct.pack(">III", width, height, bpp))
        f.write(raw)

    script = os.path.join(youscope_dir, "scripts", "save_image.py")
    if not os.path.exists(script):
        os.makedirs(os.path.dirname(script), exist_ok=True)
        with open(script, "w") as f:
            f.write("""import os, sys, json, struct
import numpy as np
import tifffile

args = json.loads(os.environ.get("YOUSCOPE_ARGS", "{}"))
src = args["image_bin"]
dst = args["output_path"]
with open(src, "rb") as fh:
    w, h, bpp = struct.unpack(">III", fh.read(12))
    raw = fh.read()
dtype = np.uint8 if bpp == 1 else np.uint16
img = np.frombuffer(raw, dtype=dtype).reshape(h, w)
tifffile.imwrite(dst, img, compression="lzw")
print(json.dumps({"saved": dst}))
""")

    result = run_in_cpython(script, image_bin=tmp_bin, output_path=filename)
    try: os.remove(tmp_bin)
    except Exception: pass
    if result.get("saved"):
        print("Saved:", result["saved"])
    elif result.get("error"):
        raise RuntimeError("saveImage failed: " + result["error"])


def displayImage(imageEvent_or_array, title="YouScope Image"):
    """
    Display an image using matplotlib via the CPython bridge.

    Accepts either:
      - A YouScope ImageEvent (from takeImage())
      - A list-of-lists from toNumpyImage()

    Usage:
        event = takeImage(youscopeServer, "Channel", "FITC", 20)
        displayImage(event)
        # or:
        img = toNumpyImage(event)
        displayImage(img)
    """
    import os, tempfile, struct, json

    import java as _java
    youscope_dir = _java.type("java.lang.System").getProperty("user.dir", ".")

    # Accept both ImageEvent and list-of-lists
    if hasattr(imageEvent_or_array, "getWidth"):
        # It's an ImageEvent -- extract pixels
        imageEvent = imageEvent_or_array
        width  = imageEvent.getWidth()
        height = imageEvent.getHeight()
        bpp    = imageEvent.getBytesPerPixel()
        raw    = bytes(imageEvent.getImageData())
        tmp_bin = tempfile.mktemp(suffix=".bin")
        with open(tmp_bin, "wb") as f:
            f.write(struct.pack(">III", width, height, bpp))
            f.write(raw)
        img_arg = None
    else:
        # It's a list-of-lists from toNumpyImage()
        rows = imageEvent_or_array
        height = len(rows)
        width  = len(rows[0]) if rows else 0
        tmp_bin = tempfile.mktemp(suffix=".bin")
        # Write as uint8
        import array as _arr
        flat = _arr.array("B", [v for row in rows for v in row])
        with open(tmp_bin, "wb") as f:
            f.write(struct.pack(">III", width, height, 1))
            f.write(bytes(flat))

    script = os.path.join(youscope_dir, "scripts", "display_image.py")
    if not os.path.exists(script):
        os.makedirs(os.path.dirname(script), exist_ok=True)
        with open(script, "w") as f:
            f.write("""import os, sys, json, struct
import numpy as np

try:
    import matplotlib
    matplotlib.use("TkAgg")
    import matplotlib.pyplot as plt
    HAVE_MATPLOTLIB = True
except ImportError:
    HAVE_MATPLOTLIB = False

args = json.loads(os.environ.get("YOUSCOPE_ARGS", "{}"))
src   = args["image_bin"]
title = args.get("title", "YouScope Image")

with open(src, "rb") as fh:
    w, h, bpp = struct.unpack(">III", fh.read(12))
    raw = fh.read()

dtype = np.uint8 if bpp == 1 else np.uint16
img = np.frombuffer(raw, dtype=dtype).reshape(h, w)
if bpp == 2 and img.max() > 0:
    img = (img.astype(np.float32) * 255 / img.max()).astype(np.uint8)

if HAVE_MATPLOTLIB:
    plt.figure(figsize=(8, 8))
    plt.imshow(img, cmap="gray", vmin=0, vmax=255)
    plt.title(title)
    plt.axis("off")
    plt.tight_layout()
    plt.show()
    print(json.dumps({"displayed": True, "backend": "matplotlib"}))
else:
    # Fallback: save as TIFF and report path
    import tifffile, tempfile as tf
    out = tf.mktemp(suffix=".tif")
    tifffile.imwrite(out, img)
    print(json.dumps({"displayed": False, "saved_to": out,
                      "message": "matplotlib not available, saved to " + out}))
""")

    result = run_in_cpython(script, image_bin=tmp_bin, title=title)
    try: os.remove(tmp_bin)
    except Exception: pass

    if isinstance(result, dict):
        if result.get("displayed"):
            print("Image displayed:", title)
        elif result.get("saved_to"):
            print("matplotlib unavailable -- image saved to:", result["saved_to"])



# openBIS spool helper

def spool_for_openbis_upload(measurement_folder, space, project,
                             experiment=None, metadata=None):
    """
    Queue a measurement folder for upload to openBIS.
    The openbis_uploader service picks up the manifest and uploads.
    """
    import json, os, time, java as _java

    youscope_dir = _java.type("java.lang.System").getProperty("user.dir", ".")
    spool_dir = os.path.join(youscope_dir, "openbis-spool")
    os.makedirs(spool_dir, exist_ok=True)

    manifest = {
        "measurement_folder": str(measurement_folder),
        "space":              space,
        "project":            project,
        "experiment":         experiment or project,
        "metadata":           metadata or {},
        "queued_at":          time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
    }

    fname = "upload_%s.json" % time.strftime("%Y%m%d_%H%M%S")
    with open(os.path.join(spool_dir, fname), "w") as f:
        json.dump(manifest, f, indent=2)

    print("[openBIS] Queued for upload: %s -> %s/%s" % (
        measurement_folder, space, project))