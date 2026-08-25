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
# YouScope QuickDetect CPython bridge.
# Called by QuickDetectAddon.java via ProcessBuilder (not GraalPy):
#   python -u quick_detect_bridge.py     with env YOUSCOPE_ARGS_FILE=<json>
# Reads the args JSON, runs the scipy/scikit-image detection, writes a small
# line-based result file + a raw uint8 label sidecar, and prints progress to
# stdout (Java captures it and forwards to the YouScope message log).

SCRIPT_VERSION = "v0.5.0"

import os
import sys
import json
import time


def _emit(msg):
    # Goes to stdout -> captured by Java (redirectErrorStream) -> YouScope log.
    sys.stdout.write("[quick_detect_bridge %s] %s\n" % (SCRIPT_VERSION, msg))
    sys.stdout.flush()


def main():
    _emit("start (pid %d)" % os.getpid())
    args_path = os.environ.get("YOUSCOPE_ARGS_FILE", "")
    if not args_path or not os.path.exists(args_path):
        _emit("FATAL: YOUSCOPE_ARGS_FILE not set or missing: %r" % args_path)
        sys.exit(2)

    with open(args_path, "r") as f:
        args = json.load(f)
    result_path = args["result_file"]

    # Import heavy libs AFTER we know we can run, so import errors are reported.
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

    steps = []

    def log(m):
        steps.append(m)
        _emit(m)

    def load(meta):
        with open(meta["path"], "rb") as fh:
            raw = fh.read()
        return cd.decode_image(raw, meta["width"], meta["height"], meta["bpp"],
                               meta["bitdepth"], meta["tx"], meta["ty"], meta["sw"])

    try:
        detection = load(args["detection"])
        quant = [load(m) for m in args.get("quant", [])]
        _emit("detect: %dx%d, %d quant image(s)"
              % (detection.shape[1], detection.shape[0], len(quant)))

        t0 = time.time()
        pos, label, rows = cd.detect(
            detection, quant,
            threshold=args["threshold"],
            internal_binning=args["internal_binning"],
            max_cell_diameter=args["max_cell_diameter"],
            min_cell_diameter=args["min_cell_diameter"],
            log=log)
        dt = (time.time() - t0) * 1000.0

        with open(result_path, "w") as f:
            f.write("version %s\n" % SCRIPT_VERSION)
            f.write("algo_version %s\n" % cd.SCRIPT_VERSION)
            f.write("ok 1\n")
            f.write("timing_ms %.1f\n" % dt)
            f.write("ncells %d\n" % pos.shape[0])
            if args.get("generate_label"):
                h, w = label.shape
                with open(args["label_path"], "wb") as lf:
                    lf.write(np.ascontiguousarray(label, dtype=np.uint8).tobytes())
                f.write("label %d %d %s\n" % (w, h, args["label_path"]))
            for r in rows:
                cid, qid, x, y, area, fluo = r
                f.write("cell %d %s %.4f %.4f %.4f %s\n" % (
                    cid,
                    "-" if qid is None else str(int(qid)),
                    x, y, area,
                    "-" if fluo is None else ("%.6f" % fluo)))
        _emit("done: %d cells in %.0f ms" % (pos.shape[0], dt))
    except Exception:
        import traceback
        tb = traceback.format_exc()
        _emit("ERROR:\n" + tb)
        _write_error(result_path, tb)
        sys.exit(1)


def _write_error(result_path, tb):
    try:
        with open(result_path, "w") as f:
            f.write("version %s\n" % SCRIPT_VERSION)
            f.write("ok 0\n")
            f.write("error %s\n" % tb.replace("\n", " | "))
    except Exception:
        pass


if __name__ == "__main__":
    main()
