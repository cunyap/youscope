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
package org.youscope.plugin.trackvisualizer;

import org.youscope.addon.celldetection.CellVisualizationConfiguration;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.configuration.YSConfigAlias;
import org.youscope.common.configuration.YSConfigClassification;
import org.youscope.common.configuration.YSConfigDescription;
import org.youscope.common.configuration.YSConfigIcon;
import org.youscope.plugin.glowvisualizer.GlowVisualizerConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration for the Track Visualizer: draws each cell's persistent track id
 * and (optionally) a short motion trail connecting its recent positions. Pairs
 * with any {@code TrackingAddon} (e.g. the nearest-neighbour online linker),
 * reading the {@code cell.track.id} column it adds.
 *
 * @author YouScope
 */
@YSConfigAlias("Track Visualizer")
@YSConfigClassification("cell visualization")
@YSConfigDescription("Draws the persistent track id next to each cell and, optionally, a short trail connecting each track's recent positions across frames. Use together with a tracking algorithm.")
@XStreamAlias("track-visualizer-configuration")
@YSConfigIcon("icons/track-viz-icon.png")
public class TrackVisualizerConfiguration extends CellVisualizationConfiguration {
	private static final long serialVersionUID = 74007731210012001L;

	/** The identifier for this configuration. */
	public static final String CONFIGURATION_ID = "YouScope.TrackVisualizer";

	@YSConfigAlias("draw on top of original image")
	@XStreamAlias("draw-into-org-image")
	private boolean drawIntoOrgImage = true;

	@YSConfigAlias("label cells with their track id")
	@XStreamAlias("draw-track-ids")
	private boolean drawTrackIds = true;

	@YSConfigAlias("trail length (frames; 0 = no trail)")
	@XStreamAlias("trail-length")
	private int trailLength = 10;

	/** Constructor. */
	public TrackVisualizerConfiguration() {
		// do nothing.
	}

	@Override
	public String getTypeIdentifier() {
		return CONFIGURATION_ID;
	}

	/**
	 * @return true to draw over the original microscope image, false for a black
	 *         canvas.
	 */
	public boolean isDrawIntoOrgImage() {
		return drawIntoOrgImage;
	}

	/**
	 * @param drawIntoOrgImage true to draw over the original image, false for a
	 *                         black canvas.
	 */
	public void setDrawIntoOrgImage(boolean drawIntoOrgImage) {
		this.drawIntoOrgImage = drawIntoOrgImage;
	}

	/** @return true to draw each cell's track id as text. */
	public boolean isDrawTrackIds() {
		return drawTrackIds;
	}

	/** @param drawTrackIds true to draw each cell's track id as text. */
	public void setDrawTrackIds(boolean drawTrackIds) {
		this.drawTrackIds = drawTrackIds;
	}

	/**
	 * @return number of recent positions connected as a trail (0 disables the
	 *         trail).
	 */
	public int getTrailLength() {
		return trailLength;
	}

	/**
	 * @param trailLength number of recent positions to connect (clamped to &gt;=
	 *                    0).
	 */
	public void setTrailLength(int trailLength) {
		this.trailLength = trailLength < 0 ? 0 : trailLength;
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		super.checkConfiguration();
	}

	@Override
	public String[] getImageSaveNames() {
		return new String[0];
	}

	@Override
	public int getNumberOfImages() {
		return 1;
	}
}
