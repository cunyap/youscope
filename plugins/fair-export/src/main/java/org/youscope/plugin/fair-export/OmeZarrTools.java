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

import java.awt.Desktop;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Helpers to point browser-based OME-Zarr tools at a locally-served export:
 * build
 * the tool URL with a {@code ?source=<dataUrl>} parameter and open it in the
 * system browser.
 *
 * The OME-NGFF validator (and the viewers it links to) take the dataset
 * location
 * as {@code ...?source=<url>}; we serve the export folder with
 * {@link LocalDataServer} and hand the validator the resulting
 * {@code http://127.0.0.1:PORT/<store>.ome.zarr} URL.
 */
public final class OmeZarrTools {
	/**
	 * Default (GitHub-hosted) OME-NGFF validator. Configurable via
	 * {@code fairexport_validator_base}.
	 */
	public static final String DEFAULT_VALIDATOR_BASE = "https://ome.github.io/ome-ngff-validator/";

	/**
	 * Reads {@code fairexport_open_launcher} from {@code graalpy_config.txt}: when
	 * set to a truthy value
	 * ({@code 1}/{@code true}/{@code yes}), the "Validate / view" action opens the
	 * self-hosted launcher
	 * page (which offers napari-on-click via the local server) instead of sending
	 * the browser straight
	 * to the GitHub-hosted validator. Defaults to false (GitHub-hosted), matching
	 * the historical
	 * behaviour.
	 * 
	 * @param youscopeDir YouScope working directory, or null for {@code user.dir}.
	 * @return true if the self-hosted launcher should be opened.
	 */
	public static boolean isOpenLauncherPreferred(String youscopeDir) {
		String dir = youscopeDir != null ? youscopeDir : System.getProperty("user.dir", ".");
		File cfg = new File(dir, "graalpy_config.txt");
		if (!cfg.exists())
			return false;
		try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(
				java.nio.file.Files.newInputStream(cfg.toPath()), java.nio.charset.Charset.forName("UTF-8")))) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				int eq = line.indexOf('=');
				if (eq > 0 && line.substring(0, eq).trim().equals("fairexport_open_launcher")) {
					String value = line.substring(eq + 1).trim().toLowerCase();
					return value.equals("1") || value.equals("true") || value.equals("yes") || value.equals("on");
				}
			}
		} catch (@SuppressWarnings("unused") java.io.IOException ignored) {
			/* default false */ }
		return false;
	}

	private OmeZarrTools() {
	}

	/**
	 * Lists the {@code *.ome.zarr} stores directly inside {@code outputFolder}
	 * (each is a directory whose name ends in {@code .ome.zarr}).
	 */
	public static List<File> findOmeZarrStores(File outputFolder) {
		List<File> stores = new ArrayList<File>();
		if (outputFolder == null || !outputFolder.isDirectory())
			return stores;
		File[] children = outputFolder.listFiles();
		if (children == null)
			return stores;
		for (File child : children) {
			if (child.isDirectory() && child.getName().toLowerCase().endsWith(".ome.zarr"))
				stores.add(child);
		}
		return stores;
	}

	/**
	 * Builds the data URL for a store served from {@code serverBaseUrl} (which ends
	 * in {@code /}). The store's folder name is URL-encoded per path segment.
	 */
	public static String buildDataUrl(String serverBaseUrl, File store) {
		String base = serverBaseUrl.endsWith("/") ? serverBaseUrl : serverBaseUrl + "/";
		return base + encodePathSegment(store.getName());
	}

	/**
	 * Builds a validator URL: {@code <validatorBase>?source=<dataUrl>}.
	 * 
	 * @param validatorBase base URL of the validator (or null for the default).
	 * @param dataUrl       the http URL of the served {@code *.ome.zarr} store.
	 */
	public static String buildValidatorUrl(String validatorBase, String dataUrl) {
		String base = (validatorBase == null || validatorBase.trim().isEmpty())
				? DEFAULT_VALIDATOR_BASE
				: validatorBase.trim();
		String sep = base.contains("?") ? "&" : "?";
		return base + sep + "source=" + urlEncode(dataUrl);
	}

	/** Opens the given URL in the system browser, if supported. */
	public static void openInBrowser(String url) throws Exception {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
			throw new UnsupportedOperationException("Opening a browser is not supported on this system. "
					+ "Open this URL manually:\n" + url);
		Desktop.getDesktop().browse(new URI(url));
	}

	private static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (@SuppressWarnings("unused") UnsupportedEncodingException e) {
			return s;
		}
	}

	/**
	 * URL-encodes a single path segment, keeping it a valid path element (space ->
	 * %20, not +).
	 */
	private static String encodePathSegment(String segment) {
		String enc = urlEncode(segment);
		// URLEncoder encodes spaces as '+', which is wrong inside a path; fix it.
		return enc.replace("+", "%20");
	}
}
