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
/**
 * 
 */
package org.youscope.addon.analysis;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.ResourceException;

/**
 * Boilerplate shared by every Python-backed cell-detection addon (segmenter,
 * quantifier, tracker, ...). A new Python-backed plugin can call them.
 *
 * These cover the plumbing that is genuinely identical across plugins:
 * <ul>
 * <li>reading {@code graalpy_config.txt} keys (with your own prefix + a
 * fallback),</li>
 * <li>resolving a writable work directory and extracting the bundled server
 * script,</li>
 * <li>serialising an {@link ImageEvent} to a little-endian raw temp file and
 * describing it as JSON for the wire protocol,</li>
 * <li>decoding the little-endian int/double arrays the server returns.</li>
 * </ul>
 *
 * Everything that is plugin <i>identity</i> (type identifier, default port,
 * config
 * prefix, server script name, backend version) intentionally stays in the
 * plugin,
 * where it belongs; this class only removes duplicated <i>code</i>, not
 * per-plugin
 * parameters. Pair it with {@link AnalysisBackendClient} for the request
 * lifecycle.
 */
public final class PythonBackendSupport {
	private PythonBackendSupport() {
	}

	// graalpy_config.txt

	/**
	 * Reads a single key from {@code <ys>/graalpy_config.txt}.
	 * 
	 * @param ys  YouScope working directory (usually
	 *            {@code System.getProperty("user.dir")}).
	 * @param key the exact key to read.
	 * @return the trimmed value, or {@code null} if absent.
	 */
	public static String readConfig(String ys, String key) {
		File cfg = new File(ys, "graalpy_config.txt");
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

	/**
	 * As {@link #readConfig}, parsed as an int, returning {@code def} when
	 * absent/invalid.
	 */
	public static int readConfigInt(String ys, String key, int def) {
		String v = readConfig(ys, key);
		if (v == null)
			return def;
		try {
			return Integer.parseInt(v.trim());
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return def;
		}
	}

	/**
	 * Resolves a config value from a list of keys in priority order (e.g. your
	 * plugin-specific key first, then the shared {@code celldetection_*} fallback).
	 * 
	 * @return the first non-empty value, or {@code null}.
	 */
	public static String readConfigWithFallback(String ys, String... keys) {
		for (String key : keys) {
			String v = readConfig(ys, key);
			if (v != null && !v.trim().isEmpty())
				return v;
		}
		return null;
	}

	/** As {@link #readConfigWithFallback}, parsed as an int with a default. */
	public static int readConfigIntWithFallback(String ys, int def, String... keys) {
		for (String key : keys) {
			String v = readConfig(ys, key);
			if (v != null && !v.trim().isEmpty()) {
				try {
					return Integer.parseInt(v.trim());
				} catch (@SuppressWarnings("unused") NumberFormatException e) {
					/* try next */ }
			}
		}
		return def;
	}

	/** @return the first non-null, non-blank argument, or {@code null}. */
	public static String firstNonNull(String... vals) {
		for (String v : vals)
			if (v != null && !v.trim().isEmpty())
				return v;
		return null;
	}

	// Work dir and script extraction

	/**
	 * Picks the first writable directory among {@code preferred}, {@code fallback}
	 * and a temp-dir default, creating it if needed.
	 */
	public static String resolveWritableDir(String preferred, String fallback) {
		String[] cands = { preferred, fallback,
				System.getProperty("java.io.tmpdir") + File.separator + "youscope-celldetection" };
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

	/**
	 * Copies a server script bundled in the plugin JAR to {@code targetPath}.
	 * 
	 * @param owner        a class in the plugin whose class loader can see the
	 *                     resource
	 *                     (pass {@code YourAddon.class}).
	 * @param resourceName absolute resource path, e.g.
	 *                     {@code "/org/youscope/plugin/xxx/xxx_server.py"}.
	 */
	public static void extractScript(Class<?> owner, String resourceName, String targetPath) throws ResourceException {
		File f = new File(targetPath);
		try {
			if (f.getParentFile() != null)
				f.getParentFile().mkdirs();
			InputStream is = owner.getResourceAsStream(resourceName);
			if (is == null)
				throw new ResourceException(resourceName + " not found in plugin JAR.");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int nrd;
			try (BufferedInputStream bis = new BufferedInputStream(is)) {
				while ((nrd = bis.read(buf)) != -1)
					baos.write(buf, 0, nrd);
			}
			Files.write(f.toPath(), baos.toByteArray());
		} catch (IOException e) {
			throw new ResourceException("Could not write script to " + targetPath, e);
		}
	}

	// Image serialisation

	/**
	 * Serialises an image's pixels to little-endian bytes (8/16/32-bit).
	 * 
	 * @throws IllegalArgumentException for unsupported pixel array types.
	 */
	public static byte[] serializeImageLE(ImageEvent<?> image) {
		int w = image.getWidth(), h = image.getHeight(), bpp = image.getBytesPerPixel();
		Object raw = image.getImageData();
		byte[] data = new byte[w * h * bpp];
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		if (raw instanceof byte[])
			System.arraycopy(raw, 0, data, 0, Math.min(((byte[]) raw).length, data.length));
		else if (raw instanceof short[]) {
			short[] s = (short[]) raw;
			int nn = Math.min(s.length, w * h);
			for (int i = 0; i < nn; i++)
				buf.putShort(s[i]);
		} else if (raw instanceof int[]) {
			int[] s = (int[]) raw;
			int nn = Math.min(s.length, w * h);
			for (int i = 0; i < nn; i++)
				buf.putInt(s[i]);
		} else
			throw new IllegalArgumentException("Unsupported image data type.");
		return data;
	}

	/**
	 * Serialises {@code image} to a fresh little-endian raw temp file, registering
	 * the created path in {@code temps} so the caller can delete it afterwards.
	 * 
	 * @param prefix temp-file prefix, e.g. {@code "ys_trackpy_"}.
	 */
	public static Path writeImageTemp(ImageEvent<?> image, String prefix, List<Path> temps) throws IOException {
		byte[] le = serializeImageLE(image);
		Path p = Files.createTempFile(prefix, ".raw");
		temps.add(p);
		Files.write(p, le);
		return p;
	}

	/** JSON metadata describing a raw image file for the wire protocol. */
	public static String imageMetaJson(ImageEvent<?> image, Path file) {
		return imageMetaJson(image, file, null);
	}

	/**
	 * JSON metadata describing a raw image file, optionally tagging it with a
	 * channel {@code suffix} (used by quantifiers to name per-channel columns).
	 */
	public static String imageMetaJson(ImageEvent<?> image, Path file, String suffix) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"path\":\"").append(AnalysisBackendClient.escapeJson(file.toString())).append("\",");
		sb.append("\"width\":").append(image.getWidth()).append(",");
		sb.append("\"height\":").append(image.getHeight()).append(",");
		sb.append("\"bpp\":").append(image.getBytesPerPixel()).append(",");
		sb.append("\"bitdepth\":").append(image.getBitDepth()).append(",");
		sb.append("\"tx\":").append(image.isTransposeX()).append(",");
		sb.append("\"ty\":").append(image.isTransposeY()).append(",");
		sb.append("\"sw\":").append(image.isSwitchXY());
		if (suffix != null)
			sb.append(",\"suffix\":\"").append(AnalysisBackendClient.escapeJson(suffix)).append("\"");
		sb.append("}");
		return sb.toString();
	}

	// Result decoding

	/**
	 * Decodes a little-endian int32 array (typically from
	 * {@code AnalysisBackendClient.inflateBase64}).
	 */
	public static int[] toIntArray(byte[] b) {
		ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
		int[] out = new int[b.length / 4];
		for (int i = 0; i < out.length; i++)
			out[i] = bb.getInt();
		return out;
	}

	/** Decodes a little-endian float64 array. */
	public static double[] toDoubleArray(byte[] b) {
		ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
		double[] out = new double[b.length / 8];
		for (int i = 0; i < out.length; i++)
			out[i] = bb.getDouble();
		return out;
	}
}
