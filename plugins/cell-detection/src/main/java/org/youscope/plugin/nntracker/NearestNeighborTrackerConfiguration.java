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
package org.youscope.plugin.nntracker;

import org.youscope.addon.celldetection.TrackingConfiguration;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.common.configuration.YSConfigAlias;
import org.youscope.common.configuration.YSConfigIcon;
import org.youscope.plugin.quickdetect.QuickDetectConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/** Configuration of the minimal nearest-neighbour online linker. */
@YSConfigAlias("Nearest Neighborhood Tracker")
@XStreamAlias("nn-tracker-configuration")
@YSConfigIcon("icons/nn-track-icon.png")
public class NearestNeighborTrackerConfiguration extends TrackingConfiguration {
	private static final long serialVersionUID = 55123991102532L;
	/** Type identifier of this tracker. */
	public static final String TYPE_IDENTIFIER = "YouScope.tracking.NearestNeighbor";

	@XStreamAlias("max-linking-distance-px")
	private double maxLinkingDistancePx = 30.0;

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	public double getMaxLinkingDistancePx() {
		return maxLinkingDistancePx;
	}

	public void setMaxLinkingDistancePx(double v) {
		this.maxLinkingDistancePx = v;
	}
}
