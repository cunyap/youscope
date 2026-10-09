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
package org.youscope.addon.celldetection;

import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.resource.ResourceConfiguration;
import org.youscope.common.table.ColumnDefinition;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Base configuration for a quantification addon.
 *
 * <p>
 * Subclasses should override {@link #getProducedColumns(String[])} to declare
 * the feature columns they add, so the composed detection table (geometry +
 * features [+ track id]) is known at configuration time and its columns can be
 * selected for plotting/statistics before the first image is evaluated. The
 * channel suffixes passed in are produced by
 * {@link #channelSuffix(String, int)} and are the same tokens the running
 * quantifier appends to its column names, so the declared header matches the
 * produced table exactly.
 */
@XStreamAlias("quantification-configuration")
public abstract class QuantificationConfiguration extends ResourceConfiguration {
	private static final long serialVersionUID = 7712512978102625713L;

	@Override
	public void checkConfiguration() throws ConfigurationException {
		/* nothing */ }

	/**
	 * Column-name suffix for a quantification channel. Uses the channel name
	 * (sanitised to an identifier-safe token) when available, else {@code chN}.
	 * Runtime and configuration-time both call this, guaranteeing identical
	 * column identifiers.
	 *
	 * @param channel the channel name (may be null/blank).
	 * @param index   zero-based channel index (fallback when the name is blank).
	 */
	public static String channelSuffix(String channel, int index) {
		if (channel != null) {
			StringBuilder sb = new StringBuilder(channel.length());
			boolean lastUnderscore = false;
			for (int i = 0; i < channel.length(); i++) {
				char c = channel.charAt(i);
				if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
					sb.append(c);
					lastUnderscore = false;
				} else if (!lastUnderscore) {
					sb.append('_');
					lastUnderscore = true;
				}
			}
			// trim leading/trailing underscores
			int start = 0, end = sb.length();
			while (start < end && sb.charAt(start) == '_')
				start++;
			while (end > start && sb.charAt(end - 1) == '_')
				end--;
			String token = sb.substring(start, end);
			if (!token.isEmpty())
				return token;
		}
		return "ch" + index;
	}

	/**
	 * Declares the columns this quantifier's table will contain, for the given
	 * per-channel suffixes. The default is the shared geometry a quantifier's
	 * backend always prepends ({@code cell.index}, {@code cell.center.x},
	 * {@code cell.center.y}, {@code cell.area}); subclasses append their features.
	 *
	 * @param channelSuffixes one suffix per configured quantification channel
	 *                        (from {@link #channelSuffix(String, int)}).
	 */
	public ColumnDefinition<?>[] getProducedColumns(String[] channelSuffixes) {
		return new ColumnDefinition<?>[] {
				CellDetectionTableColumns.TABLE_COLUMN_CELL_ID,
				CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX,
				CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX,
				CellDetectionTableColumns.TABLE_COLUMN_AREA_PX
		};
	}
}
