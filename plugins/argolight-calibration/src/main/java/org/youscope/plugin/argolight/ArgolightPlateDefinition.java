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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.youscope.common.Well;
import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.microplate.WellLayout;

/**
 * A full Argolight plate definition: a named set of {@link ArgolightPattern}s
 * plus the imaging defaults used when pre-filling jobs.
 *
 * <p>
 * The definition also acts as the {@link MicroplateLayout} source: patterns
 * are converted into {@link WellLayout}s, translating the datasheet's +Y-up
 * relative coordinates into YouScope's +Y-down absolute layout coordinates
 * (origin at the top-left of the bounding box, as expected by the well
 * selection UI and the microplate runtime).
 * </p>
 *
 * @author Andreas P. Cuny
 */
public class ArgolightPlateDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	private String name = "Argolight";
	private String channelGroup = "Channels";
	private String channel = "";
	private String camera = "";
	private double exposureMs = 5.0;
	private boolean saveImages = true;

	// Acquisition geometry used to size field-scanning (plate-scanning) jobs.
	private int cameraPixelsX = 2048;
	private int cameraPixelsY = 2048;
	private double pixelSizeUm = 0.1625;
	private double tileOverlap = 0.1;

	// Stage orientation applied ONLY to the generated microplate layout (i.e. the
	// physical stage positions), never to the pattern picker or the well labels.
	// This lets the stage motion match a camera whose X/Y have been mirrored so the
	// Argolight logo reads correctly, without changing how patterns are displayed
	// or labelled. Defaults reproduce the previous behaviour (no change).
	private boolean stageMirrorX = false;
	private boolean stageMirrorY = false;
	private boolean stageTranspose = false;

	/**
	 * @return whether stage X is mirrored relative to the datasheet coordinates.
	 */
	public boolean isStageMirrorX() {
		return stageMirrorX;
	}

	/** @param v whether to mirror stage X. */
	public void setStageMirrorX(boolean v) {
		stageMirrorX = v;
	}

	/**
	 * @return whether stage Y is mirrored relative to the datasheet coordinates.
	 */
	public boolean isStageMirrorY() {
		return stageMirrorY;
	}

	/** @param v whether to mirror stage Y. */
	public void setStageMirrorY(boolean v) {
		stageMirrorY = v;
	}

	/** @return whether stage X and Y axes are swapped. */
	public boolean isStageTranspose() {
		return stageTranspose;
	}

	/** @param v whether to swap stage X and Y axes. */
	public void setStageTranspose(boolean v) {
		stageTranspose = v;
	}

	private final ArrayList<ArgolightPattern> patterns = new ArrayList<>();

	/** @return the display name of this plate definition. */
	public String getName() {
		return name;
	}

	/** @param name display name. */
	public void setName(String name) {
		this.name = name;
	}

	/** @return default channel group used to pre-fill imaging jobs. */
	public String getChannelGroup() {
		return channelGroup;
	}

	/** @param channelGroup default channel group. */
	public void setChannelGroup(String channelGroup) {
		this.channelGroup = channelGroup;
	}

	/** @return default channel used to pre-fill imaging jobs. */
	public String getChannel() {
		return channel;
	}

	/** @param channel default channel. */
	public void setChannel(String channel) {
		this.channel = channel;
	}

	/** @return default camera device used to pre-fill imaging jobs. */
	public String getCamera() {
		return camera;
	}

	/** @param camera default camera device. */
	public void setCamera(String camera) {
		this.camera = camera;
	}

	/** @return default exposure (ms) used to pre-fill imaging jobs. */
	public double getExposureMs() {
		return exposureMs;
	}

	/** @param exposureMs default exposure (ms). */
	public void setExposureMs(double exposureMs) {
		this.exposureMs = exposureMs;
	}

	/** @return whether pre-filled imaging jobs save images by default. */
	public boolean isSaveImages() {
		return saveImages;
	}

	/** @param saveImages default save-images flag. */
	public void setSaveImages(boolean saveImages) {
		this.saveImages = saveImages;
	}

	/** @return camera width in pixels. */
	public int getCameraPixelsX() {
		return cameraPixelsX;
	}

	/** @param cameraPixelsX camera width in pixels. */
	public void setCameraPixelsX(int cameraPixelsX) {
		if (cameraPixelsX > 0)
			this.cameraPixelsX = cameraPixelsX;
	}

	/** @return camera height in pixels. */
	public int getCameraPixelsY() {
		return cameraPixelsY;
	}

	/** @param cameraPixelsY camera height in pixels. */
	public void setCameraPixelsY(int cameraPixelsY) {
		if (cameraPixelsY > 0)
			this.cameraPixelsY = cameraPixelsY;
	}

	/**
	 * @return pixel size at the sample, um/pixel (camera pixel / objective
	 *         magnification).
	 */
	public double getPixelSizeUm() {
		return pixelSizeUm;
	}

	/** @param pixelSizeUm pixel size at the sample, um/pixel. */
	public void setPixelSizeUm(double pixelSizeUm) {
		if (pixelSizeUm > 0)
			this.pixelSizeUm = pixelSizeUm;
	}

	/** @return fractional overlap between neighbouring scan tiles (0..1). */
	public double getTileOverlap() {
		return tileOverlap;
	}

	/** @param tileOverlap fractional overlap between tiles (0..1). */
	public void setTileOverlap(double tileOverlap) {
		if (tileOverlap >= 0 && tileOverlap < 0.95)
			this.tileOverlap = tileOverlap;
	}

	/** @return camera field of view width at the sample, um. */
	public double getCameraFovWidthUm() {
		return cameraPixelsX * pixelSizeUm;
	}

	/** @return camera field of view height at the sample, um. */
	public double getCameraFovHeightUm() {
		return cameraPixelsY * pixelSizeUm;
	}

	/** @param pattern pattern to append. */
	public void addPattern(ArgolightPattern pattern) {
		patterns.add(pattern);
	}

	/**
	 * Reassigns well labels to follow standard well-plate conventions: rows are
	 * letters A, B, C... from top to bottom (highest +Y first), columns are
	 * numbers 1, 2, 3... from left to right (lowest X first). Coordinates are
	 * bucketed to the nearest micrometre so patterns that share an X or a Y line
	 * up in the same column or row.
	 */
	public void reindexWellsByPosition() {
		if (patterns.isEmpty())
			return;
		java.util.TreeSet<Long> xs = new java.util.TreeSet<>();
		java.util.TreeSet<Long> ys = new java.util.TreeSet<>();
		for (ArgolightPattern p : patterns) {
			xs.add(Math.round(p.getRelativeXUm()));
			ys.add(Math.round(p.getRelativeYUm()));
		}
		// columns: ascending X -> 0,1,2,...
		java.util.HashMap<Long, Integer> colOf = new java.util.HashMap<>();
		int c = 0;
		for (Long x : xs)
			colOf.put(x, c++);
		// rows: descending Y (top first) -> 0,1,2,...
		java.util.HashMap<Long, Integer> rowOf = new java.util.HashMap<>();
		int r = 0;
		for (Long y : ys.descendingSet())
			rowOf.put(y, r++);

		ArrayList<ArgolightPattern> reindexed = new ArrayList<>(patterns.size());
		for (ArgolightPattern p : patterns) {
			int row = rowOf.get(Math.round(p.getRelativeYUm()));
			int col = colOf.get(Math.round(p.getRelativeXUm()));
			reindexed.add(p.withWell(row, col));
		}
		patterns.clear();
		patterns.addAll(reindexed);
	}

	/** @return an unmodifiable snapshot of all patterns, in definition order. */
	public List<ArgolightPattern> getPatterns() {
		return Collections.unmodifiableList(new ArrayList<>(patterns));
	}

	/** @return number of patterns. */
	public int getNumPatterns() {
		return patterns.size();
	}

	/**
	 * Finds the pattern whose {@link ArgolightPattern#getWell()} equals the given
	 * well.
	 * 
	 * @param well well label to look up.
	 * @return the pattern, or null if none.
	 */
	public ArgolightPattern getPatternForWell(Well well) {
		if (well == null)
			return null;
		for (ArgolightPattern p : patterns) {
			if (p.getWell().equals(well))
				return p;
		}
		return null;
	}

	/**
	 * Builds a {@link MicroplateLayout} from the patterns. The datasheet uses
	 * +Y-up; YouScope stage/layout coordinates are +Y-down, so Y is inverted.
	 * The bounding box is shifted so all layout coordinates are non-negative,
	 * matching how rectangular layouts are expressed (top-left origin), while
	 * preserving the relative geometry between patterns.
	 * 
	 * @return a microplate layout with one well per pattern.
	 */
	public MicroplateLayout createMicroplateLayout() {
		// Determine bounding box in "screen" space (x right, y down = -relativeY),
		// after applying the configured stage orientation.
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (ArgolightPattern p : patterns) {
			double[] s = stageXY(p);
			double left = s[0] - stageFovW(p) / 2.0;
			double top = (-s[1]) - stageFovH(p) / 2.0;
			if (left < minX)
				minX = left;
			if (top < minY)
				minY = top;
		}
		if (patterns.isEmpty()) {
			minX = 0;
			minY = 0;
		}

		final ArrayList<WellLayout> wellLayouts = new ArrayList<>(patterns.size());
		for (ArgolightPattern p : patterns) {
			double[] s = stageXY(p);
			double left = (s[0] - stageFovW(p) / 2.0) - minX;
			double top = ((-s[1]) - stageFovH(p) / 2.0) - minY;
			wellLayouts.add(new WellLayout(left, top, stageFovW(p), stageFovH(p), p.getWell()));
		}
		return new ArgolightMicroplateLayout(wellLayouts);
	}

	/**
	 * Applies the configured stage orientation (transpose, then mirror X / mirror
	 * Y)
	 * to a pattern's datasheet coordinates, returning the effective (x, y) used for
	 * the physical stage layout only.
	 * 
	 * @param p pattern.
	 * @return effective [x, y] in micrometres.
	 */
	private double[] stageXY(ArgolightPattern p) {
		double ex = p.getRelativeXUm();
		double ey = p.getRelativeYUm();
		if (stageTranspose) {
			double t = ex;
			ex = ey;
			ey = t;
		}
		if (stageMirrorX)
			ex = -ex;
		if (stageMirrorY)
			ey = -ey;
		return new double[] { ex, ey };
	}

	private double stageFovW(ArgolightPattern p) {
		return stageTranspose ? p.getFovHeightUm() : p.getFovWidthUm();
	}

	private double stageFovH(ArgolightPattern p) {
		return stageTranspose ? p.getFovWidthUm() : p.getFovHeightUm();
	}

	/**
	 * Minimal {@link MicroplateLayout} backed by a fixed list of wells.
	 */
	private static final class ArgolightMicroplateLayout implements MicroplateLayout {
		private static final long serialVersionUID = 1L;
		private final ArrayList<WellLayout> wellLayouts;

		ArgolightMicroplateLayout(ArrayList<WellLayout> wellLayouts) {
			this.wellLayouts = wellLayouts;
		}

		@Override
		public Iterator<WellLayout> iterator() {
			return wellLayouts.iterator();
		}

		@Override
		public int getNumWells() {
			return wellLayouts.size();
		}

		@Override
		public WellLayout getWell(int index) {
			if (index < 0 || index >= wellLayouts.size())
				return null;
			return wellLayouts.get(index);
		}

		@Override
		public WellLayout getWell(Well well) {
			for (WellLayout wellLayout : wellLayouts) {
				if (wellLayout.getWell().equals(well))
					return wellLayout;
			}
			return null;
		}
	}
}
