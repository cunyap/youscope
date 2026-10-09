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
package org.youscope.plugin.cellxquantifier;

import java.util.ArrayList;
import java.util.List;

import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.addon.celldetection.QuantificationConfiguration;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.configuration.YSConfigAlias;
import org.youscope.common.configuration.YSConfigDescription;
import org.youscope.common.configuration.YSConfigIcon;
import org.youscope.common.table.ColumnDefinition;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration for the CellXMaskInterface quantifier: measures the CellX
 * per-cell geometry and fluorescence feature set (whole-cell / membrane /
 * nucleus
 * /
 * bright-area totals and configurable quantiles, plus a mixture-model
 * background)
 * from any label mask and channels. Ported from the MATLAB CellXMaskInterface;
 * parameter names mirror the CellX parameter file.
 *
 * @author YouScope
 */
@YSConfigAlias("CellX (CellXMaskInterface) quantifier")
@YSConfigDescription("Measures the CellX feature set (geometry plus cell/membrane/nucleus/bright-area intensity totals plus configurable quantiles, GMM background) from a label mask and channels. Works with any segmenter's mask.")
@YSConfigIcon("icons/cellx-mask-icon.png")
@XStreamAlias("cellx-quantifier-configuration")
public class CellXMaskQuantifierConfiguration extends QuantificationConfiguration {
	private static final long serialVersionUID = 74100048100012001L;

	/** The identifier for this configuration. */
	public static final String TYPE_IDENTIFIER = "YouScope.quantification.CellX";

	@YSConfigAlias("membrane width (px)")
	@XStreamAlias("membrane-width-px")
	private int membraneWidth = 2;

	@YSConfigAlias("nuclear volume fraction")
	@XStreamAlias("nuclear-volume-fraction")
	private double nuclearVolumeFraction = 0.07;

	@YSConfigAlias("bright-area percentage (0-1)")
	@XStreamAlias("bright-area-percentage")
	private double brightAreaPercentage = 0.3;

	@YSConfigAlias("background GMM components")
	@XStreamAlias("intensity-classes-count")
	private int intensityClassesCount = 3;

	@YSConfigAlias("smoothing size (px)")
	@XStreamAlias("smoothing-size-px")
	private int smoothingSize = 3;

	@YSConfigAlias("smooth before bright area (Wiener/median)")
	@XStreamAlias("use-wiener-filter")
	private boolean useWienerFilter = true;

	@YSConfigAlias("fluo quantiles (%, comma-separated)")
	@XStreamAlias("fluo-quantiles")
	private String fluoQuantiles = "25,50,75";

	@YSConfigAlias("scale intensities to 16-bit (match CellX)")
	@XStreamAlias("scale-to-16bit")
	private boolean scaleTo16Bit = true;

	@YSConfigAlias("fluo align pixel move (0=off)")
	@XStreamAlias("fluo-align-pixel-move")
	private int fluoAlignPixelMove = 0;

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	public int getMembraneWidth() {
		return membraneWidth;
	}

	public void setMembraneWidth(int membraneWidth) {
		this.membraneWidth = membraneWidth < 1 ? 1 : membraneWidth;
	}

	public double getNuclearVolumeFraction() {
		return nuclearVolumeFraction;
	}

	public void setNuclearVolumeFraction(double nuclearVolumeFraction) {
		this.nuclearVolumeFraction = nuclearVolumeFraction;
	}

	public double getBrightAreaPercentage() {
		return brightAreaPercentage;
	}

	public void setBrightAreaPercentage(double brightAreaPercentage) {
		if (brightAreaPercentage < 0)
			brightAreaPercentage = 0;
		if (brightAreaPercentage > 1)
			brightAreaPercentage = 1;
		this.brightAreaPercentage = brightAreaPercentage;
	}

	public int getIntensityClassesCount() {
		return intensityClassesCount;
	}

	public void setIntensityClassesCount(int intensityClassesCount) {
		this.intensityClassesCount = intensityClassesCount < 1 ? 1 : intensityClassesCount;
	}

	public int getSmoothingSize() {
		return smoothingSize;
	}

	public void setSmoothingSize(int smoothingSize) {
		this.smoothingSize = smoothingSize < 1 ? 1 : smoothingSize;
	}

	public boolean isUseWienerFilter() {
		return useWienerFilter;
	}

	public void setUseWienerFilter(boolean useWienerFilter) {
		this.useWienerFilter = useWienerFilter;
	}

	/** @return the raw quantile string, e.g. "25,50,75" (percentages). */
	public String getFluoQuantiles() {
		return fluoQuantiles;
	}

	public void setFluoQuantiles(String fluoQuantiles) {
		this.fluoQuantiles = (fluoQuantiles == null || fluoQuantiles.trim().isEmpty()) ? "25,50,75"
				: fluoQuantiles.trim();
	}

	public boolean isScaleTo16Bit() {
		return scaleTo16Bit;
	}

	public void setScaleTo16Bit(boolean scaleTo16Bit) {
		this.scaleTo16Bit = scaleTo16Bit;
	}

	/** @return the CellX fluoAlignPixelMove window (0 = no alignment). */
	public int getFluoAlignPixelMove() {
		return fluoAlignPixelMove;
	}

	public void setFluoAlignPixelMove(int fluoAlignPixelMove) {
		this.fluoAlignPixelMove = fluoAlignPixelMove < 0 ? 0 : fluoAlignPixelMove;
	}

	/**
	 * Parses {@link #getFluoQuantiles()} into an ordered list of percentages
	 * (0-100). Invalid tokens are skipped; if nothing valid remains, defaults to
	 * {25, 50, 75}.
	 */
	public double[] getQuantilePercents() {
		List<Double> out = new ArrayList<Double>();
		for (String tok : fluoQuantiles.split(",")) {
			tok = tok.trim();
			if (tok.isEmpty())
				continue;
			try {
				double v = Double.parseDouble(tok);
				if (v >= 0 && v <= 100)
					out.add(Double.valueOf(v));
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				// skip invalid token
			}
		}
		if (out.isEmpty())
			return new double[] { 25.0, 50.0, 75.0 };
		double[] arr = new double[out.size()];
		for (int i = 0; i < arr.length; i++)
			arr[i] = out.get(i).doubleValue();
		return arr;
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		super.checkConfiguration();
	}

	/** Quantile column stem, matching the backend _q_label: 50 -> "median". */
	static String quantileLabel(double pct) {
		if (Math.abs(pct - 50.0) < 1e-9)
			return "median";
		if (Math.abs(pct - Math.rint(pct)) < 1e-9)
			return "q" + (int) Math.rint(pct);
		return "q" + String.valueOf(pct).replace('.', 'p');
	}

	/**
	 * Channel-independent CellX geometry column identifiers (match native CellX).
	 */
	private static final String[] CELLX_GEOMETRY = {
			"cell.majoraxis", "cell.minoraxis", "cell.orientation", "cell.volume",
			"cell.perimeter", "cell.mem.area", "cell.mem.volume", "cell.nuc.radius", "cell.nuc.area"
	};

	private static final String[] CELLX_REGIONS = { "cell", "mem", "nuc", "bright" };

	@Override
	public ColumnDefinition<?>[] getProducedColumns(String[] channelSuffixes) {
		double[] quantiles = getQuantilePercents();
		List<ColumnDefinition<?>> cols = new ArrayList<ColumnDefinition<?>>();
		// prepended by the backend geometry(): id + centroid + area
		cols.add(CellDetectionTableColumns.TABLE_COLUMN_CELL_ID);
		cols.add(CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX);
		cols.add(CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX);
		cols.add(CellDetectionTableColumns.TABLE_COLUMN_AREA_PX);
		// CellX geometry block (channel-independent)
		for (String g : CELLX_GEOMETRY)
			cols.add(ColumnDefinition.createDoubleColumnDefinition(g, "CellX geometry " + g + ".", true));
		// per-channel fluo block
		if (channelSuffixes != null) {
			for (String s : channelSuffixes) {
				cols.add(dbl("fluo.background.mean." + s, s));
				cols.add(dbl("fluo.background.std." + s, s));
				for (String region : CELLX_REGIONS) {
					cols.add(dbl("fluo." + region + ".total." + s, s));
					for (double q : quantiles)
						cols.add(dbl("fluo." + region + "." + quantileLabel(q) + "." + s, s));
				}
				cols.add(dbl("fluo.bright.euler." + s, s));
			}
		}
		return cols.toArray(new ColumnDefinition<?>[cols.size()]);
	}

	private static ColumnDefinition<?> dbl(String name, String channel) {
		return ColumnDefinition.createDoubleColumnDefinition(name, "CellX feature for channel '" + channel + "'.",
				true);
	}
}
