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
package org.youscope.plugin.trackpytracker;

import org.youscope.addon.celldetection.TrackingConfiguration;
import org.youscope.common.configuration.YSConfigAlias;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration for the trackpy online tracker. Frame-to-frame linking only
 * (current frame linked to the previous one), like the CellX / nearest-neighbour
 * trackers. Adds the {@code cell.track.id} column.
 */
@YSConfigAlias("Trackpy Tracker")
@XStreamAlias("trackpy-tracker-configuration")
public class TrackpyTrackerConfiguration extends TrackingConfiguration
{
	private static final long serialVersionUID = 71000048100092001L;

	/** Type identifier of this tracker. */
	public static final String TYPE_IDENTIFIER = "YouScope.tracking.Trackpy";

	@YSConfigAlias("search range (px)")
	@XStreamAlias("search-range-px")
	private double searchRange = 15.0;

	@YSConfigAlias("memory (frames, 0 = frame-to-frame)")
	@XStreamAlias("memory")
	private int memory = 0;

	@Override
	public String getTypeIdentifier()
	{
		return TYPE_IDENTIFIER;
	}

	/** @return maximum distance (px) a cell may move between frames (trackpy search_range). */
	public double getSearchRange()
	{
		return searchRange;
	}

	public void setSearchRange(double searchRange)
	{
		this.searchRange = searchRange <= 0 ? 1.0 : searchRange;
	}

	/**
	 * @return trackpy memory (frames a cell may vanish and still be reconnected). 0
	 *         keeps this a pure frame-to-frame linker.
	 */
	public int getMemory()
	{
		return memory;
	}

	public void setMemory(int memory)
	{
		this.memory = memory < 0 ? 0 : memory;
	}
}
