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
# YouScope GlowVisualizer CPython bridge.
# Launched by GlowVisualizerAddon.java via ProcessBuilder (not GraalPy):
#   python -u glow_bridge.py     with env YOUSCOPE_ARGS_FILE=<json>
# Reads the args JSON, renders the glow overlay, writes the RGB result to a raw
# sidecar file, and prints progress to stdout (Java forwards it to the YouScope log).

SCRIPT_VERSION = "v0.5.0"

import os
import sys
import json
import time


def _emit(msg):
    sys.stdout.write("[glow_bridge %s] %s\n" % (SCRIPT_VERSION, msg))
    sys.stdout.flush()


def _write_error(result_path, tb):
    try:
        with open(result_path, "w") as f:
            f.write("version %s\nok 0\nerror %s\n" % (SCRIPT_VERSION, tb.replace("\n", " | ")))
    except Exception:
        pass


def main():
    _emit("start (pid %d)" % os.getpid())
    args_path = os.environ.get("YOUSCOPE_ARGS_FILE", "")
    if not args_path or not os.path.exists(args_path):
        _emit("FATAL: YOUSCOPE_ARGS_FILE not set or missing: %r" % args_path)
        sys.exit(2)
    with open(args_path, "r") as f:
        args = json.load(f)
    result_path = args["result_file"]

    try:
        import numpy as np
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        import cell_detection_cpython as cd
        _emit("libraries imported; algorithm module %s" % cd.SCRIPT_VERSION)
    except Exception:
        import traceback
        tb = traceback.format_exc()
        _emit("FATAL import error:\n" + tb)
        _write_error(result_path, tb)
        sys.exit(3)

    try:
        m = args["image"]
        with open(m["path"], "rb") as fh:
            raw = fh.read()
        image = cd.decode_image(raw, m["width"], m["height"], m["bpp"], m["bitdepth"],
                                m["tx"], m["ty"], m["sw"])

        n = int(args["cell_count"])
        if n > 0:
            with open(args["positions_path"], "rb") as fh:
                pos = np.frombuffer(fh.read(), dtype="<f8").reshape(n, 3).copy()
        else:
            pos = np.zeros((0, 3), dtype=np.float64)
        _emit("glow: %dx%d, %d cell(s)" % (image.shape[1], image.shape[0], n))

        t0 = time.time()
        rgb = cd.glow(image, pos, float(args["glow_strength"]))
        dt = (time.time() - t0) * 1000.0

        h, w, _ = rgb.shape
        with open(args["rgb_path"], "wb") as fh:
            fh.write(np.ascontiguousarray(rgb, dtype=np.uint8).tobytes())

        with open(result_path, "w") as f:
            f.write("version %s\n" % SCRIPT_VERSION)
            f.write("algo_version %s\n" % cd.SCRIPT_VERSION)
            f.write("ok 1\n")
            f.write("timing_ms %.1f\n" % dt)
            f.write("rgb %d %d\n" % (w, h))
        _emit("done in %.0f ms" % dt)
    except Exception:
        import traceback
        tb = traceback.format_exc()
        _emit("ERROR:\n" + tb)
        _write_error(result_path, tb)
        sys.exit(1)


if __name__ == "__main__":
    main()
