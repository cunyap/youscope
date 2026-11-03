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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Starts and manages the bundled {@code cors_server.py}, a local CORS-enabled
 * static HTTP server rooted at the FAIR-export output folder, so browser tools
 * (the OME-NGFF validator, vizarr, neuroglancer, vol-e) can fetch the exported
 * {@code *.ome.zarr} store by URL.
 * <p>
 * Self-contained on purpose: the FAIR-export plugin does not depend on
 * cell-detection, so this re-reads {@code graalpy_config.txt} itself (the same
 * file and key conventions) rather than importing shared helpers. The Python
 * interpreter is resolved from {@code fairexport_cpython_executable}, falling
 * back
 * to {@code celldetection_cpython_executable}, {@code cpython_executable}, then
 * {@code python} on the PATH.
 * </p>
 * <p>
 * The server self-shuts after an idle timeout; this class also stops it
 * explicitly
 * (call {@link #stop()} when the window closes).
 * </p>
 */
public class LocalDataServer {
	/** Resource path of the bundled server script. */
	private static final String SCRIPT_RESOURCE = "/org/youscope/plugin/fairexport/cors_server.py";
	/**
	 * Resource path of the bundled napari launcher, wired into the server's
	 * /__napari endpoint.
	 */
	private static final String NAPARI_SCRIPT_RESOURCE = "/org/youscope/plugin/fairexport/napari_launch.py";
	private static final Pattern URL_LINE = Pattern.compile("listening on (http://\\S+)");

	private final String youscopeDir;
	private Process process = null;
	private String baseUrl = null;
	/**
	 * Canonical path of the folder currently being served, so a change of root
	 * triggers a restart.
	 */
	private String servedRoot = null;
	/**
	 * Signature of the config the running server was started with (port, idle,
	 * validator/vizarr, base).
	 */
	private String servedConfigSignature = null;

	/**
	 * @param youscopeDir YouScope working directory containing
	 *                    {@code graalpy_config.txt}
	 *                    (usually {@code System.getProperty("user.dir")}).
	 */
	public LocalDataServer(String youscopeDir) {
		this.youscopeDir = youscopeDir == null ? "." : youscopeDir;
	}

	/** @return true while the server subprocess is alive. */
	public synchronized boolean isRunning() {
		return process != null && process.isAlive();
	}

	/**
	 * @return the base URL (e.g. {@code http://127.0.0.1:39621/}) once started,
	 *         else null.
	 */
	public synchronized String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * @return the URL of the self-hosted viewer/launcher page
	 *         ({@code <base>__viewer}) once started,
	 *         else null. This page lists the stores with an "Open in napari" button
	 *         (which launches napari via
	 *         the server's {@code /__napari} endpoint) and onward links to the
	 *         validator and web viewers.
	 */
	public synchronized String getViewerUrl() {
		return baseUrl == null ? null : baseUrl + "__viewer";
	}

	/**
	 * Starts the server rooted at {@code root} and blocks until it has bound a port
	 * and printed its URL,
	 * or the timeout elapses.
	 * <p>
	 * If a server is already running but rooted at a <em>different</em> folder, or
	 * the relevant
	 * {@code graalpy_config.txt} settings (port, idle timeout, validator/vizarr
	 * paths, validator base)
	 * have changed since it started, it is stopped and restarted so the new
	 * root/settings take effect.
	 * This is important because the same {@code LocalDataServer} instance is reused
	 * across clicks: without
	 * the restart, the first root/port would be frozen for the whole session, so a
	 * second measurement (or
	 * a changed port) would be served from the wrong folder and cause a "File
	 * not found" and
	 * "port not updated".
	 * </p>
	 * 
	 * @param root           the folder to serve (must directly contain the
	 *                       {@code *.ome.zarr} store(s)).
	 * @param startTimeoutMs how long to wait for the server to come up.
	 * @return the base URL.
	 * @throws IOException if the interpreter/script cannot be launched or the
	 *                     server
	 *                     does not report a URL in time.
	 */
	public synchronized String start(File root, int startTimeoutMs) throws IOException {
		if (root == null || !root.isDirectory())
			throw new IOException("Folder to serve does not exist: " + root);

		String requestedRoot = canonical(root);
		String requestedSignature = configSignature();

		// Reuse only if the running server serves exactly this root with the same
		// settings.
		if (isRunning() && baseUrl != null
				&& requestedRoot.equals(servedRoot)
				&& requestedSignature.equals(servedConfigSignature))
			return baseUrl;

		// Otherwise (not running, different root, or changed settings) start fresh.
		if (isRunning())
			stop();

		String python = FairExportPython.resolvePython(youscopeDir);
		String workDir = FairExportPython.resolveWorkDir(youscopeDir);
		String script = workDir + File.separator + "cors_server.py";
		FairExportPython.extractScript(SCRIPT_RESOURCE, script);

		// Extract the napari launcher too and wire it into the server, so the
		// self-hosted viewer page's
		// "Open in napari" button launches napari for real (a browser page cannot spawn
		// a desktop app,
		// but this local server can). Best-effort: if extraction fails, the /__napari
		// endpoint simply
		// reports that the launcher is unavailable.
		String napariScript = workDir + File.separator + "napari_launch.py";
		boolean napariReady = true;
		try {
			FairExportPython.extractScript(NAPARI_SCRIPT_RESOURCE, napariScript);
		} catch (@SuppressWarnings("unused") IOException ignored) {
			napariReady = false;
		}

		int port = FairExportPython.readConfigInt(youscopeDir, "fairexport_viz_port", 0);
		int idle = FairExportPython.readConfigInt(youscopeDir, "fairexport_viz_idle_timeout_seconds", 1800);

		java.util.List<String> command = new java.util.ArrayList<String>();
		command.add(python);
		command.add(script);
		command.add("--root");
		command.add(root.getAbsolutePath());
		command.add("--host");
		command.add("127.0.0.1");
		command.add("--port");
		command.add(Integer.toString(port));
		command.add("--idle-timeout");
		command.add(Integer.toString(idle));
		if (napariReady) {
			command.add("--napari-script");
			command.add(napariScript);
		}
		// Optional self-hosted validator build (offline validation), and a configurable
		// validator base
		// used by the self-hosted viewer page links. Both default to unset
		// (GitHub-hosted).
		String validatorDir = FairExportPython.readConfig(youscopeDir, "fairexport_self_hosted_validator");
		if (validatorDir != null && !validatorDir.trim().isEmpty()) {
			command.add("--validator-dir");
			command.add(validatorDir.trim());
		}
		String vizarrDir = FairExportPython.readConfig(youscopeDir, "fairexport_self_hosted_vizarr");
		if (vizarrDir != null && !vizarrDir.trim().isEmpty()) {
			command.add("--vizarr-dir");
			command.add(vizarrDir.trim());
		}
		String validatorBase = FairExportPython.readConfig(youscopeDir, "fairexport_validator_base");
		if (validatorBase != null && !validatorBase.trim().isEmpty()) {
			command.add("--validator-base");
			command.add(validatorBase.trim());
		}

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		this.process = p;

		// read stdout until the URL line appears or we time out / the process dies
		long deadline = System.currentTimeMillis() + Math.max(2000, startTimeoutMs);
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("UTF-8")));
		String url = null;
		while (System.currentTimeMillis() < deadline) {
			String line = reader.readLine();
			if (line == null)
				break; // process closed its output (likely exited)
			Matcher m = URL_LINE.matcher(line);
			if (m.find()) {
				url = m.group(1);
				break;
			}
		}
		if (url == null) {
			stop();
			throw new IOException("Local data server did not start (no URL reported). "
					+ "Check that '" + python + "' exists and can run cors_server.py"
					+ (port != 0
							? ", and that port " + port + " is free (set fairexport_viz_port=0 for an automatic port)."
							: "."));
		}
		if (!url.endsWith("/"))
			url = url + "/";
		this.baseUrl = url;
		this.servedRoot = requestedRoot;
		this.servedConfigSignature = requestedSignature;

		// drain the rest of the output in the background so the pipe never blocks
		final BufferedReader drain = reader;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (drain.readLine() != null) {
						/* discard */ }
				} catch (@SuppressWarnings("unused") IOException ignored) {
					/* server stopped */ }
			}
		}, "fair-viz-server-drain");
		t.setDaemon(true);
		t.start();

		return baseUrl;
	}

	/**
	 * Canonical path (falls back to absolute) so root comparisons are stable across
	 * separators/case.
	 */
	private static String canonical(File file) {
		try {
			return file.getCanonicalPath();
		} catch (@SuppressWarnings("unused") IOException e) {
			return file.getAbsolutePath();
		}
	}

	/**
	 * Signature of the config keys the server is launched with, to detect changes
	 * needing a restart.
	 */
	private String configSignature() {
		return "port=" + FairExportPython.readConfig(youscopeDir, "fairexport_viz_port")
				+ ";idle=" + FairExportPython.readConfig(youscopeDir, "fairexport_viz_idle_timeout_seconds")
				+ ";vdir=" + FairExportPython.readConfig(youscopeDir, "fairexport_self_hosted_validator")
				+ ";vizdir=" + FairExportPython.readConfig(youscopeDir, "fairexport_self_hosted_vizarr")
				+ ";vbase=" + FairExportPython.readConfig(youscopeDir, "fairexport_validator_base");
	}

	/** Stops the server subprocess if running. */
	public synchronized void stop() {
		if (process != null) {
			try {
				process.destroy();
				if (process.isAlive()) {
					try {
						process.waitFor();
					} catch (@SuppressWarnings("unused") InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			} catch (@SuppressWarnings("unused") Exception ignored) {
				/* best effort */ } finally {
				process.destroyForcibly();
				process = null;
				baseUrl = null;
				servedRoot = null;
				servedConfigSignature = null;
			}
		}
	}

}
