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
package org.youscope.plugin.intensityquantifier;

import java.util.ArrayList;
import java.util.List;
import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.addon.celldetection.QuantificationConfiguration;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.common.configuration.YSConfigAlias;
import org.youscope.common.configuration.YSConfigIcon;
import org.youscope.common.table.ColumnDefinition;
import org.youscope.plugin.quickdetect.QuickDetectConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/** Configuration of the minimal (intensity) quantifier. */
@YSConfigAlias("Minimal Intensity Quantifier")
@XStreamAlias("intensity-quantifier-configuration")
@YSConfigIcon("icons/minimal-int-quant-icon.png")
public class IntensityQuantifierConfiguration extends QuantificationConfiguration {
	private static final long serialVersionUID = 4455012399011253L;
	/** Type identifier of this quantifier. */
	public static final String TYPE_IDENTIFIER = "YouScope.quantification.Intensity";

	@XStreamAlias("subtract-background")
	private boolean subtractBackground = false;
	@XStreamAlias("background-percentile")
	private double backgroundPercentile = 5.0;

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	public boolean isSubtractBackground() {
		return subtractBackground;
	}

	public void setSubtractBackground(boolean v) {
		this.subtractBackground = v;
	}

	public double getBackgroundPercentile() {
		return backgroundPercentile;
	}

	public void setBackgroundPercentile(double v) {
		this.backgroundPercentile = v;
	}

	@Override
	public ColumnDefinition<?>[] getProducedColumns(String[] channelSuffixes) {
		List<ColumnDefinition<?>> cols = new ArrayList<ColumnDefinition<?>>();
		cols.add(CellDetectionTableColumns.TABLE_COLUMN_CELL_ID);
		cols.add(CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX);
		cols.add(CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX);
		cols.add(CellDetectionTableColumns.TABLE_COLUMN_AREA_PX);
		if (channelSuffixes != null) {
			for (String s : channelSuffixes) {
				cols.add(ColumnDefinition.createDoubleColumnDefinition("cell.intensity.mean." + s,
						"Mean intensity of channel '" + s + "' over the cell pixels.", true));
				cols.add(ColumnDefinition.createDoubleColumnDefinition("cell.intensity.integrated." + s,
						"Summed intensity of channel '" + s + "' over the cell pixels.", true));
			}
		}
		return cols.toArray(new ColumnDefinition<?>[cols.size()]);
	}
}
