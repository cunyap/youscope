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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Small shared plumbing for the FAIR-export plugin's Python helpers (the local
 * data server and the napari launcher): reading {@code graalpy_config.txt},
 * resolving the interpreter and a work directory, and extracting a bundled
 * script.
 * <p>
 * Kept inside the fair-export plugin (not borrowed from cell-detection) so the
 * plugin stays self-contained. The interpreter is resolved from
 * {@code fairexport_cpython_executable}, falling back to
 * {@code celldetection_cpython_executable}, {@code cpython_executable}, then
 * {@code python} on the PATH.
 */
final class FairExportPython {
	private FairExportPython() {
	}

	/**
	 * Reads a single key from {@code <youscopeDir>/graalpy_config.txt}, or null.
	 */
	static String readConfig(String youscopeDir, String key) {
		File cfg = new File(youscopeDir == null ? "." : youscopeDir, "graalpy_config.txt");
		if (!cfg.exists())
			return null;
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(Files.newInputStream(cfg.toPath()), Charset.forName("UTF-8")))) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#") || line.isEmpty())
					continue;
				int eq = line.indexOf('=');
				if (eq > 0 && line.substring(0, eq).trim().equals(key))
					return line.substring(eq + 1).trim();
			}
		} catch (@SuppressWarnings("unused") IOException ignored) {
			/* fall through */ }
		return null;
	}

	static int readConfigInt(String youscopeDir, String key, int def) {
		String v = readConfig(youscopeDir, key);
		if (v == null)
			return def;
		try {
			return Integer.parseInt(v.trim());
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return def;
		}
	}

	/**
	 * Writes/updates a set of keys in {@code <youscopeDir>/graalpy_config.txt},
	 * preserving other lines,
	 * comments and ordering. A key whose value is null or empty is removed. Used by
	 * the FAIR-export
	 * settings UI so the validator/viewer options are persisted to the same
	 * config file the runtime
	 * reads. Existing keys are updated in place; new keys are appended under a
	 * managed section.
	 * 
	 * @param youscopeDir YouScope working directory (holds the config file).
	 * @param entries     Ordered key/value pairs to apply.
	 * @throws IOException If the file cannot be written.
	 */
	static void writeConfig(String youscopeDir, java.util.LinkedHashMap<String, String> entries) throws IOException {
		File cfg = new File(youscopeDir == null ? "." : youscopeDir, "graalpy_config.txt");
		java.util.List<String> lines = new java.util.ArrayList<String>();
		if (cfg.exists()) {
			try (BufferedReader r = new BufferedReader(
					new InputStreamReader(Files.newInputStream(cfg.toPath()), Charset.forName("UTF-8")))) {
				String line;
				while ((line = r.readLine()) != null)
					lines.add(line);
			}
		}
		java.util.LinkedHashMap<String, String> remaining = new java.util.LinkedHashMap<String, String>(entries);
		// Update existing keys in place.
		for (int i = 0; i < lines.size(); i++) {
			String trimmed = lines.get(i).trim();
			if (trimmed.startsWith("#") || trimmed.isEmpty())
				continue;
			int eq = trimmed.indexOf('=');
			if (eq <= 0)
				continue;
			String key = trimmed.substring(0, eq).trim();
			if (remaining.containsKey(key)) {
				String value = remaining.remove(key);
				if (value == null || value.trim().isEmpty())
					lines.set(i, "#" + key + "="); // comment out to "remove" without losing history
				else
					lines.set(i, key + "=" + value.trim());
			}
		}
		// Append any new keys.
		boolean headerWritten = false;
		for (java.util.Map.Entry<String, String> e : remaining.entrySet()) {
			if (e.getValue() == null || e.getValue().trim().isEmpty())
				continue;
			if (!headerWritten) {
				if (!lines.isEmpty() && !lines.get(lines.size() - 1).trim().isEmpty())
					lines.add("");
				lines.add("# FAIR export settings (managed by the plugin UI)");
				headerWritten = true;
			}
			lines.add(e.getKey() + "=" + e.getValue().trim());
		}
		StringBuilder out = new StringBuilder();
		for (String line : lines)
			out.append(line).append(System.lineSeparator());
		try (java.io.Writer w = new java.io.OutputStreamWriter(Files.newOutputStream(cfg.toPath()),
				Charset.forName("UTF-8"))) {
			w.write(out.toString());
		}
	}

	/**
	 * Resolves the Python interpreter (fairexport -> celldetection -> cpython ->
	 * PATH).
	 */
	static String resolvePython(String youscopeDir) {
		String python = firstNonNull(
				readConfig(youscopeDir, "fairexport_cpython_executable"),
				readConfig(youscopeDir, "celldetection_cpython_executable"),
				readConfig(youscopeDir, "cpython_executable"));
		if (python == null || !new File(python).exists())
			python = "python";
		return python;
	}

	/** Resolves a writable work directory for extracted scripts. */
	static String resolveWorkDir(String youscopeDir) {
		String[] cands = { readConfig(youscopeDir, "fairexport_work_dir"),
				(youscopeDir == null ? "." : youscopeDir) + File.separator + "scripts",
				System.getProperty("java.io.tmpdir") + File.separator + "youscope-fairexport" };
		for (String c : cands) {
			if (c == null || c.trim().isEmpty())
				continue;
			File d = new File(c.trim());
			try {
				if (!d.exists())
					d.mkdirs();
				if (d.isDirectory() && d.canWrite())
					return d.getAbsolutePath();
			} catch (@SuppressWarnings("unused") Exception ignored) {
				/* next */ }
		}
		return new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
	}

	/** Copies a bundled script resource to {@code targetPath}. */
	static void extractScript(String resourceName, String targetPath) throws IOException {
		File f = new File(targetPath);
		if (f.getParentFile() != null)
			f.getParentFile().mkdirs();
		InputStream is = FairExportPython.class.getResourceAsStream(resourceName);
		if (is == null)
			throw new IOException(resourceName + " not found in plugin JAR.");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int nrd;
		try (BufferedInputStream bis = new BufferedInputStream(is)) {
			while ((nrd = bis.read(buf)) != -1)
				baos.write(buf, 0, nrd);
		}
		Files.write(f.toPath(), baos.toByteArray());
	}

	static String firstNonNull(String... vals) {
		for (String v : vals)
			if (v != null && !v.trim().isEmpty())
				return v;
		return null;
	}
}
