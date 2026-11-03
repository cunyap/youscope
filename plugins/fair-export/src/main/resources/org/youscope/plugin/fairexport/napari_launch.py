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

#!/usr/bin/env python3
"""Open a local OME-Zarr store in napari via napari-ome-zarr.

Launched fire-and-forget by the FAIR-export plugin. napari reads the local path
directly (no HTTP server needed). Requires the `napari` and `napari-ome-zarr` in the
env pointed at by `fairexport_cpython_executable` (falling back to the shared
cell-detection / cpython env).

Usage: napari_launch.py <path-to-store.ome.zarr>
"""
import sys


def main():
    if len(sys.argv) < 2:
        sys.stderr.write("usage: napari_launch.py <path-to-.ome.zarr>\n")
        sys.exit(2)
    path = sys.argv[1]
    try:
        import napari
    except Exception as exc:  # pragma: no cover - depends on the env
        sys.stderr.write(
            "napari is not available in this environment (%r).\n"
            "Install it with:  pip install napari napari-ome-zarr\n" % (exc,))
        sys.exit(3)

    viewer = napari.Viewer()
    opened = False
    # Prefer the napari-ome-zarr reader so multiscale + OME metadata are honoured.
    try:
        viewer.open(path, plugin="napari-ome-zarr")
        opened = True
    except Exception as exc:
        sys.stderr.write("napari-ome-zarr could not open the store (%r); "
                         "falling back to napari's default reader.\n" % (exc,))
    if not opened:
        try:
            viewer.open(path)
        except Exception as exc:
            sys.stderr.write("napari could not open %s (%r)\n" % (path, exc))
            sys.exit(4)
    napari.run()


if __name__ == "__main__":
    main()
