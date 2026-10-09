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
package org.youscope.plugin.cellprofilerquantifier;

import java.util.List;

import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.addon.celldetection.QuantificationConfiguration;
import org.youscope.common.configuration.YSConfigAlias;
import org.youscope.common.table.ColumnDefinition;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration for the CellProfiler (Mode A) quantifier. Mode A runs a fixed set
 * of CellProfiler measurement modules (size/shape, intensity, texture) on the
 * segmenter's label mask and the quantification channels. Which module families run
 * is toggled here; the produced columns follow directly from the toggles and
 * channels via {@link CellProfilerFeatures}.
 */
@YSConfigAlias("CellProfiler Quantifier")
@XStreamAlias("cellprofiler-quantifier-configuration")
public class CellProfilerQuantifierConfiguration extends QuantificationConfiguration
{
	private static final long serialVersionUID = 71000048100094001L;

	/** Type identifier of this quantifier. */
	public static final String TYPE_IDENTIFIER = "YouScope.quantification.CellProfiler";

	@YSConfigAlias("measure size & shape")
	@XStreamAlias("measure-shape")
	private boolean measureShape = true;

	@YSConfigAlias("measure intensity")
	@XStreamAlias("measure-intensity")
	private boolean measureIntensity = true;

	@YSConfigAlias("measure texture")
	@XStreamAlias("measure-texture")
	private boolean measureTexture = false;

	@Override
	public String getTypeIdentifier()
	{
		return TYPE_IDENTIFIER;
	}

	public boolean isMeasureShape()
	{
		return measureShape;
	}

	public void setMeasureShape(boolean measureShape)
	{
		this.measureShape = measureShape;
	}

	public boolean isMeasureIntensity()
	{
		return measureIntensity;
	}

	public void setMeasureIntensity(boolean measureIntensity)
	{
		this.measureIntensity = measureIntensity;
	}

	public boolean isMeasureTexture()
	{
		return measureTexture;
	}

	public void setMeasureTexture(boolean measureTexture)
	{
		this.measureTexture = measureTexture;
	}

	@Override
	public ColumnDefinition<?>[] getProducedColumns(String[] channelSuffixes)
	{
		List<String> feats = CellProfilerFeatures.requestedFeatures(
				measureShape, measureIntensity, measureTexture, channelSuffixes);
		// Core geometry columns are carried through from the segmenter so the
		// Label/Track visualizers find cell.center.x/y etc.; then the CellProfiler
		// features are appended.
		ColumnDefinition<?>[] core = new ColumnDefinition<?>[]
		{
			CellDetectionTableColumns.TABLE_COLUMN_CELL_ID,
			CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX,
			CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX,
			CellDetectionTableColumns.TABLE_COLUMN_AREA_PX,
			ColumnDefinition.createDoubleColumnDefinition("cell.perimeter", "Cell perimeter (px).", true)
		};
		ColumnDefinition<?>[] cols = new ColumnDefinition<?>[core.length + feats.size()];
		System.arraycopy(core, 0, cols, 0, core.length);
		for(int i = 0; i < feats.size(); i++)
		{
			String name = CellProfilerFeatures.PREFIX + feats.get(i);
			cols[core.length + i] = ColumnDefinition.createDoubleColumnDefinition(name, name, true);
		}
		return cols;
	}
}
