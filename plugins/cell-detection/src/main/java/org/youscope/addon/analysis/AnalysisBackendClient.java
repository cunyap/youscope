/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.addon.analysis;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Minimal, language-agnostic client for a YouScope analysis backend.
 *
 * <p>
 * Wire format (mirrors the Cellpose server, so one client can eventually talk
 * to
 * all backends): each message is
 * {@code [4-byte big-endian uint32 length][UTF-8 JSON]}.
 * Large input images are handed over via temp files (paths in the request
 * JSON);
 * result images come back zlib+base64 inside the response JSON. This class owns
 * only transport + lifecycle; callers build the request JSON and read the
 * response with the small scalar/base64 helpers here.
 *
 * <p>
 * A local server (host 127.0.0.1 / localhost) is auto-started on demand and
 * stopped on JVM shutdown; a non-local host is only connected to, never
 * started.
 */
public final class AnalysisBackendClient {
	private final String host;
	private final int port;
	private final String pythonExe; // launcher for a local server (may be null for remote)
	private final String serverScript; // absolute path to the *_server.py
	private final String logFile; // where to redirect the server's stdout
	private final String[] serverArgs; // full server arg list (after the script path)

	private static final List<int[]> STARTED_LOCAL = new ArrayList<int[]>(); // ports we launched
	private static final Object HOOK_LOCK = new Object();
	private static volatile boolean hookRegistered = false;

	/**
	 * General constructor: the caller supplies the exact server arguments (after
	 * {@code python -u <serverScript>}), e.g.
	 * {@code {"--host","127.0.0.1","--port",
	 * "59712","--model","cpsam","--gpu","false"}}. This lets one client drive any
	 * backend (QuickDetect, Cellpose, StarDist, CellX, ...).
	 */
	public AnalysisBackendClient(String host, int port, String pythonExe, String serverScript,
			String logFile, String[] serverArgs) {
		this.host = host;
		this.port = port;
		this.pythonExe = pythonExe;
		this.serverScript = serverScript;
		this.logFile = logFile;
		this.serverArgs = serverArgs == null ? new String[0] : serverArgs;
	}

	/**
	 * Convenience constructor for the default server args
	 * (--host/--port[/--idle-timeout]).
	 */
	public AnalysisBackendClient(String host, int port, String pythonExe, String serverScript,
			String logFile, int idleTimeoutSeconds) {
		this(host, port, pythonExe, serverScript, logFile,
				idleTimeoutSeconds > 0
						? new String[] { "--host", host == null ? "127.0.0.1" : host,
								"--port", Integer.toString(port),
								"--idle-timeout", Integer.toString(idleTimeoutSeconds) }
						: new String[] { "--host", host == null ? "127.0.0.1" : host,
								"--port", Integer.toString(port) });
	}

	private boolean isLocal() {
		return host == null || host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost");
	}

	/**
	 * Ensures a backend of the expected version is reachable, (re)starting a local
	 * one if necessary. Returns the version the backend reports.
	 *
	 * @throws IOException if no compatible backend can be reached/started.
	 */
	public String ensureUp(String expectedVersion, int startTimeoutMs) throws IOException {
		String v = ping();
		if (v != null) {
			if (expectedVersion == null || expectedVersion.equals(v) || !isLocal())
				return v;
			// Local but stale then shut it down and start the bundled version.
			shutdownQuietly();
			sleep(300);
		}
		if (!isLocal())
			throw new IOException("No analysis backend reachable at " + host + ":" + port
					+ " (remote host is never auto-started).");

		launchServer();
		registerShutdownHook();
		long deadline = System.currentTimeMillis() + startTimeoutMs;
		while (System.currentTimeMillis() < deadline) {
			v = ping();
			if (v != null && (expectedVersion == null || expectedVersion.equals(v)))
				return v;
			sleep(200);
		}
		throw new IOException("Analysis backend on port " + port + " did not become ready; see " + logFile);
	}

	/** Sends one request JSON, returns the response JSON. */
	public String request(String requestJson, int timeoutMs) throws IOException {
		try (Socket s = new Socket()) {
			s.connect(new InetSocketAddress(host, port), Math.min(timeoutMs, 5000));
			s.setSoTimeout(timeoutMs);
			byte[] req = requestJson.getBytes(StandardCharsets.UTF_8);
			OutputStream out = s.getOutputStream();
			out.write(intToBytes(req.length));
			out.write(req);
			out.flush();
			DataInputStream in = new DataInputStream(s.getInputStream());
			int len = in.readInt();
			if (len < 0 || len > (256 << 20))
				throw new IOException("Unreasonable response length: " + len);
			byte[] resp = new byte[len];
			in.readFully(resp);
			return new String(resp, StandardCharsets.UTF_8);
		}
	}

	/**
	 * {@code ping}: reported version, or null if unreachable. Sends both
	 * {@code op} and {@code cmd} and reads {@code version} or
	 * {@code script_version},
	 * so it works with every backend's server (QuickDetect, Cellpose, ...).
	 */
	public String ping() {
		try {
			String r = request("{\"op\":\"ping\",\"cmd\":\"ping\"}", 4000);
			String v = extractString(r, "version");
			return v != null ? v : extractString(r, "script_version");
		} catch (@SuppressWarnings("unused") IOException e) {
			return null;
		}
	}

	public void shutdownQuietly() {
		try {
			request("{\"op\":\"shutdown\",\"cmd\":\"shutdown\"}", 3000);
		} catch (@SuppressWarnings("unused") IOException e) {
			// ignore
		}
	}

	// Server lifecycle

	private void launchServer() throws IOException {
		if (pythonExe == null || !new File(pythonExe).exists())
			throw new IOException("python executable not found: " + pythonExe);
		if (serverScript == null || !new File(serverScript).exists())
			throw new IOException("server script not found: " + serverScript);
		List<String> cmd = new ArrayList<String>();
		cmd.add(pythonExe);
		cmd.add("-u");
		cmd.add(serverScript);
		for (String a : serverArgs)
			cmd.add(a);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.environment().put("PYTHONUNBUFFERED", "1");
		pb.redirectErrorStream(true);
		if (logFile != null) {
			File lf = new File(logFile);
			if (lf.getParentFile() != null)
				lf.getParentFile().mkdirs();
			pb.redirectOutput(ProcessBuilder.Redirect.appendTo(lf));
		}
		pb.start(); // detached; discovered again via ping()
		synchronized (STARTED_LOCAL) {
			STARTED_LOCAL.add(new int[] { port });
		}
	}

	private void registerShutdownHook() {
		synchronized (HOOK_LOCK) {
			if (hookRegistered)
				return;
			hookRegistered = true;
		}
		final String h = host;
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (STARTED_LOCAL) {
					for (int[] p : STARTED_LOCAL) {
						try (Socket s = new Socket()) {
							s.connect(new InetSocketAddress(h, p[0]), 800);
							s.setSoTimeout(1200);
							byte[] req = "{\"op\":\"shutdown\",\"cmd\":\"shutdown\"}".getBytes(StandardCharsets.UTF_8);
							s.getOutputStream().write(intToBytes(req.length));
							s.getOutputStream().write(req);
							s.getOutputStream().flush();
						} catch (@SuppressWarnings("unused") IOException ignored) {
							// best effort
						}
					}
				}
			}
		}, "analysis-backend-shutdown"));
	}

	// Helpers: JSON scalars + zlib/base64

	public static String extractString(String json, String key) {
		String pat = "\"" + key + "\"";
		int i = json.indexOf(pat);
		if (i < 0)
			return null;
		int c = json.indexOf(':', i + pat.length());
		if (c < 0)
			return null;
		int q = json.indexOf('"', c + 1);
		if (q < 0)
			return null;
		StringBuilder sb = new StringBuilder();
		for (int k = q + 1; k < json.length(); k++) {
			char ch = json.charAt(k);
			if (ch == '\\' && k + 1 < json.length()) {
				sb.append(json.charAt(++k));
				continue;
			}
			if (ch == '"')
				break;
			sb.append(ch);
		}
		return sb.toString();
	}

	public static long extractLong(String json, String key, long def) {
		String pat = "\"" + key + "\"";
		int i = json.indexOf(pat);
		if (i < 0)
			return def;
		int c = json.indexOf(':', i + pat.length());
		if (c < 0)
			return def;
		int k = c + 1;
		while (k < json.length() && Character.isWhitespace(json.charAt(k)))
			k++;
		int start = k;
		while (k < json.length() && (Character.isDigit(json.charAt(k)) || json.charAt(k) == '-'))
			k++;
		try {
			return Long.parseLong(json.substring(start, k));
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return def;
		}
	}

	public static boolean extractBool(String json, String key, boolean def) {
		String pat = "\"" + key + "\"";
		int i = json.indexOf(pat);
		if (i < 0)
			return def;
		int c = json.indexOf(':', i + pat.length());
		if (c < 0)
			return def;
		String rest = json.substring(c + 1).trim();
		if (rest.startsWith("true"))
			return true;
		if (rest.startsWith("false"))
			return false;
		return def;
	}

	/** Decodes a zlib+base64 field into raw bytes. */
	public static byte[] inflateBase64(String base64Text) throws IOException {
		byte[] compressed = Base64.getDecoder().decode(base64Text);
		Inflater inflater = new Inflater();
		inflater.setInput(compressed);
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(compressed.length * 3);
		byte[] buf = new byte[8192];
		try {
			while (!inflater.finished()) {
				int n = inflater.inflate(buf);
				if (n == 0) {
					if (inflater.finished() || inflater.needsDictionary())
						break;
					if (inflater.needsInput())
						break;
				}
				out.write(buf, 0, n);
			}
		} catch (DataFormatException e) {
			throw new IOException("Corrupt zlib data from analysis backend: " + e.getMessage(), e);
		} finally {
			inflater.end();
		}
		return out.toByteArray();
	}

	public static String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static byte[] intToBytes(int v) {
		return new byte[] { (byte) ((v >>> 24) & 0xFF), (byte) ((v >>> 16) & 0xFF),
				(byte) ((v >>> 8) & 0xFF), (byte) (v & 0xFF) };
	}

	private static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
