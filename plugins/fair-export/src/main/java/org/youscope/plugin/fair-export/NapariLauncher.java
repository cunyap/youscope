/*******************************************************************************
 * Copyright © 2026 ETH Zürich
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
 * License, Version 2.1 (or at your option any later version).
 *
 * You may choose which license you wish to use when copying, modifying,
 * or distributing this software.
 *
 * Licenses are available at:
 * - Apache 2.0: http://apache.org
 * - LGPL 2.1:   https://gnu.org
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
 *
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/

package org.youscope.plugin.fairexport;

import java.io.File;
import java.io.IOException;

/**
 * Opens a local {@code *.ome.zarr} store in napari, fire-and-forget, using the
 * bundled {@code napari_launch.py} in the configured Python env. napari reads
 * the
 * local path directly, so no HTTP server is involved.
 */
public final class NapariLauncher {
	private static final String SCRIPT_RESOURCE = "/org/youscope/plugin/fairexport/napari_launch.py";

	private NapariLauncher() {
	}

	/**
	 * Launches napari on {@code store}. Returns immediately; napari runs in its own
	 * process (its stdout/stderr go to {@code napari_launch.log} in the work dir).
	 * 
	 * @throws IOException if the interpreter/script cannot be launched.
	 */
	public static void open(String youscopeDir, File store) throws IOException {
		if (store == null || !store.exists())
			throw new IOException("OME-Zarr store not found: " + store);
		String python = FairExportPython.resolvePython(youscopeDir);
		String workDir = FairExportPython.resolveWorkDir(youscopeDir);
		String script = workDir + File.separator + "napari_launch.py";
		FairExportPython.extractScript(SCRIPT_RESOURCE, script);

		ProcessBuilder pb = new ProcessBuilder(python, script, store.getAbsolutePath());
		pb.redirectErrorStream(true);
		pb.redirectOutput(new File(workDir, "napari_launch.log"));
		pb.start(); // fire-and-forget; napari manages its own lifecycle
	}
}
