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
