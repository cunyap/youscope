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
 * Base configuration for a tracking addon.
 *
 * <p>
 * {@link #getProducedColumns()} declares the column(s) a tracker adds so the
 * composed detection table header is known at configuration time. All bundled
 * trackers add a single flat {@code cell.track.id}.
 */
@XStreamAlias("tracking-configuration")
public abstract class TrackingConfiguration extends ResourceConfiguration {
	private static final long serialVersionUID = 7712512978102625714L;

	/**
	 * Column name for the persistent per-cell track id added by online trackers.
	 */
	public static final String TRACK_ID_COLUMN = "cell.track.id";

	@Override
	public void checkConfiguration() throws ConfigurationException {
		/* nothing */ }

	/** Columns this tracker adds to the table (default: {@code cell.track.id}). */
	public ColumnDefinition<?>[] getProducedColumns() {
		return new ColumnDefinition<?>[] {
				ColumnDefinition.createIntegerColumnDefinition(TRACK_ID_COLUMN,
						"Persistent track id assigned by the online tracker.", false)
		};
	}
}
