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

package org.youscope.plugin.argolight;

import java.io.Serializable;
import java.util.ArrayList;

import org.youscope.common.Well;

/**
 * Immutable description of a single Argolight test pattern (one selectable
 * position in the calibration measurement).
 *
 * <p>
 * Coordinates are given relative to the central cross, with +Y pointing up
 * (datasheet convention). The conversion to YouScope's +Y-down stage convention
 * is done in {@link ArgolightPlateDefinition#createMicroplateLayout()}.
 * </p>
 *
 * <p>
 * The z-stack can be described either symmetrically (a step plus a number of
 * slices below/above focus) or as an explicit range (start..end at a given
 * step). Patterns can additionally request field scanning: when a scan field is
 * larger than the camera field of view, the imaging job is wrapped in a
 * plate-scanning job that tiles the whole field.
 * </p>
 *
 * @author Andreas P. Cuny
 */
public class ArgolightPattern implements Serializable {
	private static final long serialVersionUID = 2L;

	/** Z-stack description modes. */
	public enum ZMode {
		/** Symmetric around focus: step, slicesBelow, slicesAbove. */
		SYMMETRIC,
		/** Explicit range: zStart..zEnd at zStep. */
		RANGE
	}

	private final String id;
	private final String name;
	private final String iconType;
	private final int wellRow;
	private final int wellCol;
	private final double relativeXUm;
	private final double relativeYUm;
	private final double fovWidthUm;
	private final double fovHeightUm;
	private final int groupId;

	private final boolean zStack;
	private final ZMode zMode;
	private final double zStepUm;
	private final int zSlicesBelow;
	private final int zSlicesAbove;
	private final double zStartUm;
	private final double zEndUm;

	private final boolean scan;
	private final double scanFieldWidthUm;
	private final double scanFieldHeightUm;
	private final boolean overview;

	private final double iconScale;
	private final double iconDetail;
	private final double markerScale;

	/**
	 * Full constructor.
	 */
	public ArgolightPattern(String id, String name, String iconType, int wellRow, int wellCol,
			double relativeXUm, double relativeYUm, double fovWidthUm, double fovHeightUm, int groupId,
			boolean zStack, ZMode zMode, double zStepUm, int zSlicesBelow, int zSlicesAbove, double zStartUm,
			double zEndUm,
			boolean scan, double scanFieldWidthUm, double scanFieldHeightUm, boolean overview,
			double iconScale, double iconDetail, double markerScale) {
		this.id = id;
		this.name = name;
		this.iconType = iconType == null ? "generic" : iconType;
		this.wellRow = wellRow;
		this.wellCol = wellCol;
		this.relativeXUm = relativeXUm;
		this.relativeYUm = relativeYUm;
		this.fovWidthUm = fovWidthUm <= 0 ? 100 : fovWidthUm;
		this.fovHeightUm = fovHeightUm <= 0 ? 100 : fovHeightUm;
		this.groupId = groupId;
		this.zStack = zStack;
		this.zMode = zMode == null ? ZMode.SYMMETRIC : zMode;
		this.zStepUm = zStepUm;
		this.zSlicesBelow = zSlicesBelow;
		this.zSlicesAbove = zSlicesAbove;
		this.zStartUm = zStartUm;
		this.zEndUm = zEndUm;
		this.scan = scan;
		this.scanFieldWidthUm = scanFieldWidthUm;
		this.scanFieldHeightUm = scanFieldHeightUm;
		this.overview = overview;
		this.iconScale = iconScale <= 0 ? 1.0 : iconScale;
		this.iconDetail = iconDetail;
		this.markerScale = markerScale <= 0 ? 1.0 : markerScale;
	}

	/** @return stable machine identifier (also default image save name). */
	public String getId() {
		return id;
	}

	/** @return human readable name. */
	public String getName() {
		return name;
	}

	/** @return icon glyph key. */
	public String getIconType() {
		return iconType;
	}

	/** @return 0-based row identifier (label only). */
	public int getWellRow() {
		return wellRow;
	}

	/** @return 0-based column identifier (label only). */
	public int getWellCol() {
		return wellCol;
	}

	/** @return the {@link Well} label for this pattern. */
	public Well getWell() {
		return new Well(wellRow, wellCol);
	}

	/** @return x relative to central cross (+X right), um. */
	public double getRelativeXUm() {
		return relativeXUm;
	}

	/** @return y relative to central cross (+Y up), um. */
	public double getRelativeYUm() {
		return relativeYUm;
	}

	/** @return nominal FOV / marker width, um. */
	public double getFovWidthUm() {
		return fovWidthUm;
	}

	/** @return nominal FOV / marker height, um. */
	public double getFovHeightUm() {
		return fovHeightUm;
	}

	/** @return well group id 0..6. */
	public int getGroupId() {
		return groupId;
	}

	/** @return true if a z-stack should be pre-filled. */
	public boolean isZStack() {
		return zStack;
	}

	/** @return the z-stack description mode. */
	public ZMode getZMode() {
		return zMode;
	}

	/** @return z step size, um. */
	public double getZStepUm() {
		return zStepUm;
	}

	/** @return number of slices below focus (symmetric mode). */
	public int getZSlicesBelow() {
		return zSlicesBelow;
	}

	/** @return number of slices above focus (symmetric mode). */
	public int getZSlicesAbove() {
		return zSlicesAbove;
	}

	/** @return z-range start, um (range mode). */
	public double getZStartUm() {
		return zStartUm;
	}

	/** @return z-range end, um (range mode). */
	public double getZEndUm() {
		return zEndUm;
	}

	/** @return true if this pattern should be field-scanned (tiled). */
	public boolean isScan() {
		return scan;
	}

	/** @return the scan field width, um. */
	public double getScanFieldWidthUm() {
		return scanFieldWidthUm;
	}

	/** @return the scan field height, um. */
	public double getScanFieldHeightUm() {
		return scanFieldHeightUm;
	}

	/**
	 * @return true if this position should additionally acquire a leading
	 *         single-frame overview image.
	 */
	public boolean isOverview() {
		return overview;
	}

	/** @return relative glyph size factor within the marker (1.0 = default). */
	public double getIconScale() {
		return iconScale;
	}

	/** @return glyph-specific detail parameter (meaning depends on icon). */
	public double getIconDetail() {
		return iconDetail;
	}

	/** @return relative marker size factor (1.0 = default). */
	public double getMarkerScale() {
		return markerScale;
	}

	/**
	 * Computes the list of relative focus positions (um) to pre-fill a z-slides
	 * job. Symmetric mode: step=1, below=3, above=3 -> [-3..3]. Range mode:
	 * start=0, end=3.5, step=0.1 -> [0, 0.1, ..., 3.5].
	 * 
	 * @return relative focus positions in um (empty if not a z-stack).
	 */
	public double[] computeRelativeZPositions() {
		if (!zStack)
			return new double[0];
		if (zMode == ZMode.RANGE) {
			double step = Math.abs(zStepUm);
			if (step <= 0)
				return new double[] { zStartUm };
			ArrayList<Double> positions = new ArrayList<>();
			if (zEndUm >= zStartUm) {
				// include end within a small epsilon.
				for (double z = zStartUm; z <= zEndUm + step / 1000.0; z += step)
					positions.add(round(z));
			} else {
				for (double z = zStartUm; z >= zEndUm - step / 1000.0; z -= step)
					positions.add(round(z));
			}
			double[] result = new double[positions.size()];
			for (int i = 0; i < result.length; i++)
				result[i] = positions.get(i);
			return result;
		}
		int n = zSlicesBelow + zSlicesAbove + 1;
		if (n <= 0)
			return new double[0];
		double[] z = new double[n];
		int idx = 0;
		for (int i = -zSlicesBelow; i <= zSlicesAbove; i++)
			z[idx++] = round(i * zStepUm);
		return z;
	}

	private static double round(double v) {
		return Math.round(v * 1e6) / 1e6;
	}

	/**
	 * Returns a copy of this pattern with a different well label (row/column).
	 * Used to reassign labels according to well-plate conventions.
	 * 
	 * @param newWellRow new 0-based row (letter).
	 * @param newWellCol new 0-based column (number-1).
	 * @return a copy with the new well label.
	 */
	public ArgolightPattern withWell(int newWellRow, int newWellCol) {
		return new ArgolightPattern(id, name, iconType, newWellRow, newWellCol, relativeXUm, relativeYUm,
				fovWidthUm, fovHeightUm, groupId, zStack, zMode, zStepUm, zSlicesBelow, zSlicesAbove, zStartUm, zEndUm,
				scan, scanFieldWidthUm, scanFieldHeightUm, overview, iconScale, iconDetail, markerScale);
	}
}
