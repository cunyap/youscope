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

/**
 * Encodes and decodes a {@link FairExportEngine.Options} to and from an
 * automatic-post-processor
 * identifier string.
 * <p>
 * The measurement configuration stores automatic post-processors as identifier
 * strings only, with no
 * place for parameters. To let the full export definition live in the
 * measurement protocol
 * (configuration.csb) and be chosen up front with the rest of the measurement
 * instead of than tweaking in
 * a later manual re-run, the options are encoded into the identifier itself as
 * a query string:
 * </p>
 * 
 * <pre>
 * YouScope.FairExport.Configured?tiff=1&amp;zarr=1&amp;rocrate=1&amp;xml=1&amp;split=combined&amp;chunk=1024&amp;shard=0
 * </pre>
 * <p>
 * This keeps the whole feature inside the plugin: no core change, and the
 * export options are documented
 * in the saved configuration alongside the protocol. Unknown or malformed
 * values fall back to
 * defaults, so a hand-edited or older identifier never crashes the export.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
final class FairExportOptionsCodec {
	private FairExportOptionsCodec() {
		// utility
	}

	/**
	 * Encodes the options into a configured identifier string.
	 * 
	 * @param options Options to encode.
	 * @return The identifier, e.g.
	 *         {@code YouScope.FairExport.Configured?tiff=1&zarr=0&...}.
	 */
	static String encode(FairExportEngine.Options options) {
		StringBuilder builder = new StringBuilder(FairExportFactory.TYPE_IDENTIFIER_CONFIGURED_PREFIX);
		builder.append('?');
		builder.append("tiff=").append(options.writeOmeTiff ? '1' : '0');
		builder.append("&zarr=").append(options.writeOmeZarr ? '1' : '0');
		builder.append("&rocrate=").append(options.writeRoCrate ? '1' : '0');
		builder.append("&useq=").append(options.writeUseq ? '1' : '0');
		builder.append("&crateroot=").append(options.rootOnlyStoreCrate ? '1' : '0');
		builder.append("&xml=").append(options.writeOmeXml ? '1' : '0');
		builder.append("&split=").append(options.tiffSplit == FairExportEngine.TiffSplit.PER_CHANNEL
				? "perchannel"
				: "combined");
		builder.append("&chunk=").append(options.zarrChunkSize);
		builder.append("&shard=").append(options.zarrSharding ? '1' : '0');
		return builder.toString();
	}

	/**
	 * Decodes a configured identifier into options. Any missing or unparseable
	 * field keeps its default.
	 * 
	 * @param typeIdentifier The configured identifier.
	 * @return The decoded options (never null).
	 */
	static FairExportEngine.Options decode(String typeIdentifier) {
		FairExportEngine.Options options = new FairExportEngine.Options();
		if (typeIdentifier == null)
			return options;
		int queryStart = typeIdentifier.indexOf('?');
		if (queryStart < 0 || queryStart + 1 >= typeIdentifier.length())
			return options;
		String query = typeIdentifier.substring(queryStart + 1);
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int equals = pair.indexOf('=');
			if (equals < 0)
				continue;
			String key = pair.substring(0, equals);
			String value = pair.substring(equals + 1);
			if ("tiff".equals(key))
				options.writeOmeTiff = "1".equals(value);
			else if ("zarr".equals(key))
				options.writeOmeZarr = "1".equals(value);
			else if ("rocrate".equals(key))
				options.writeRoCrate = "1".equals(value);
			else if ("useq".equals(key))
				options.writeUseq = "1".equals(value);
			else if ("crateroot".equals(key))
				options.rootOnlyStoreCrate = "1".equals(value);
			else if ("xml".equals(key))
				options.writeOmeXml = "1".equals(value);
			else if ("split".equals(key))
				options.tiffSplit = "perchannel".equals(value)
						? FairExportEngine.TiffSplit.PER_CHANNEL
						: FairExportEngine.TiffSplit.COMBINED;
			else if ("chunk".equals(key)) {
				try {
					options.zarrChunkSize = Integer.parseInt(value);
				} catch (@SuppressWarnings("unused") NumberFormatException e) {
					// keep default
				}
			} else if ("shard".equals(key))
				options.zarrSharding = "1".equals(value);
		}
		return options;
	}

	/**
	 * Returns a short human-readable summary of a configured identifier for display
	 * in the wizard, e.g.
	 * "OME-TIFF (per channel), OME-Zarr (chunk 1024)".
	 * 
	 * @param options Options to summarise.
	 * @return A summary string.
	 */
	static String summary(FairExportEngine.Options options) {
		StringBuilder builder = new StringBuilder();
		if (options.writeOmeTiff) {
			builder.append("OME-TIFF");
			if (options.tiffSplit == FairExportEngine.TiffSplit.PER_CHANNEL)
				builder.append(" (per channel)");
		}
		if (options.writeOmeZarr) {
			if (builder.length() > 0)
				builder.append(", ");
			builder.append("OME-Zarr (chunk ").append(options.zarrChunkSize);
			if (options.zarrSharding)
				builder.append(", sharded");
			builder.append(")");
		}
		if (options.writeRoCrate) {
			if (builder.length() > 0)
				builder.append(", ");
			builder.append("RO-Crate");
		}
		if (options.writeUseq) {
			if (builder.length() > 0)
				builder.append(", ");
			builder.append("useq plan (feedback only)");
		}
		if (builder.length() == 0)
			builder.append("nothing selected");
		return builder.toString();
	}
}
