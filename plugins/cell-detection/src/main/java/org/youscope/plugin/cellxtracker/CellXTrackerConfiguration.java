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
package org.youscope.plugin.cellxtracker;

import org.youscope.addon.celldetection.TrackingConfiguration;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.configuration.YSConfigAlias;
import org.youscope.common.configuration.YSConfigDescription;
import org.youscope.common.configuration.YSConfigIcon;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration for the CellX online tracker: frame-to-frame weighted-graph
 * matching (mask overlap + centroid distance + shape cost, solved with the
 * Hungarian algorithm plus terminal costs). Ported from CellXTrackerYouScope.
 *
 * @author YouScope
 */
@YSConfigAlias("CellX online tracker")
@YSConfigDescription("Frame-to-frame CellX linking: combines mask overlap, centroid distance and shape (major axis + orientation) into a weighted-graph matching solved by the Hungarian algorithm. Works on any segmenter's mask.")
@XStreamAlias("cellx-tracker-configuration")
@YSConfigIcon("icons/cellx-icon.png")
public class CellXTrackerConfiguration extends TrackingConfiguration {
	private static final long serialVersionUID = 74100049100012001L;

	/** Type identifier of this tracker. */
	public static final String TYPE_IDENTIFIER = "YouScope.tracking.CellX";

	@YSConfigAlias("spatial motion factor (px)")
	@XStreamAlias("spatial-motion-factor")
	private double spatialMotionFactor = 50.0;

	@YSConfigAlias("max gap connection distance (frames, 0=off)")
	@XStreamAlias("max-gap-connection-distance")
	private int maxGapConnectionDistance = 0;

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	/** @return the distance normaliser (maximum expected motion in px). */
	public double getSpatialMotionFactor() {
		return spatialMotionFactor;
	}

	public void setSpatialMotionFactor(double spatialMotionFactor) {
		this.spatialMotionFactor = spatialMotionFactor <= 0 ? 1.0 : spatialMotionFactor;
	}

	/**
	 * @return how many frames a lost track may be reconnected across (CellX
	 *         maxGapConnectionDistance). 0 disables gap closing (frame-to-frame
	 *         linking only); CellX's own default is 3.
	 */
	public int getMaxGapConnectionDistance() {
		return maxGapConnectionDistance;
	}

	public void setMaxGapConnectionDistance(int maxGapConnectionDistance) {
		this.maxGapConnectionDistance = maxGapConnectionDistance < 0 ? 0 : maxGapConnectionDistance;
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		super.checkConfiguration();
	}
}
