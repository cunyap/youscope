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
package org.youscope.plugin.trackmatetracker;

import org.youscope.addon.celldetection.TrackingConfiguration;
import org.youscope.common.configuration.YSConfigAlias;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration for the trackmate online tracker. Frame-to-frame linking only
 * (current frame linked to the previous one), like the CellX / nearest-neighbour
 * trackers. Adds the {@code cell.track.id} column.
 */
@YSConfigAlias("Trackmate Tracker")
@XStreamAlias("trackmate-tracker-configuration")
public class TrackmateTrackerConfiguration extends TrackingConfiguration
{
	private static final long serialVersionUID = 71000048100092001L;

	/** Type identifier of this tracker. */
	public static final String TYPE_IDENTIFIER = "YouScope.tracking.Trackmate";

	@YSConfigAlias("linking max distance (px)")
	@XStreamAlias("linking-max-distance-px")
	private double linkingMaxDistance = 15.0;

	@Override
	public String getTypeIdentifier()
	{
		return TYPE_IDENTIFIER;
	}

	/**
	 * @return TrackMate frame-to-frame LAP linking max distance (px): the largest
	 *         allowed displacement of a cell between consecutive frames.
	 */
	public double getLinkingMaxDistance()
	{
		return linkingMaxDistance;
	}

	public void setLinkingMaxDistance(double linkingMaxDistance)
	{
		this.linkingMaxDistance = linkingMaxDistance <= 0 ? 1.0 : linkingMaxDistance;
	}
}
