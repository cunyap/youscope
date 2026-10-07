/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
package org.youscope.plugin.btracktracker;

import org.youscope.addon.celldetection.TrackingConfiguration;
import org.youscope.common.configuration.YSConfigAlias;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration for the btrack online tracker. Frame-to-frame linking only
 * (current frame linked to the previous one), like the CellX / nearest-neighbour
 * trackers. Adds the {@code cell.track.id} column.
 */
@YSConfigAlias("Btrack Tracker")
@XStreamAlias("btrack-tracker-configuration")
public class BtrackTrackerConfiguration extends TrackingConfiguration
{
	private static final long serialVersionUID = 71000048100092001L;

	/** Type identifier of this tracker. */
	public static final String TYPE_IDENTIFIER = "YouScope.tracking.Btrack";

	@YSConfigAlias("max search radius (px)")
	@XStreamAlias("max-search-radius-px")
	private double maxSearchRadius = 50.0;

	@YSConfigAlias("max lost (frames)")
	@XStreamAlias("max-lost")
	private int maxLost = 5;

	@Override
	public String getTypeIdentifier()
	{
		return TYPE_IDENTIFIER;
	}

	/** @return maximum distance (px) a cell may move between frames (btrack max_search_radius). */
	public double getMaxSearchRadius()
	{
		return maxSearchRadius;
	}

	public void setMaxSearchRadius(double maxSearchRadius)
	{
		this.maxSearchRadius = maxSearchRadius <= 0 ? 1.0 : maxSearchRadius;
	}

	/**
	 * @return btrack motion-model max_lost (how many frames a track may be lost
	 *         before termination). Kept small for frame-to-frame linking.
	 */
	public int getMaxLost()
	{
		return maxLost;
	}

	public void setMaxLost(int maxLost)
	{
		this.maxLost = maxLost < 0 ? 0 : maxLost;
	}
}
