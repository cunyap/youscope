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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import org.youscope.addon.celldetection.CellDetectionResult;
import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.addon.celldetection.CellVisualizationAddon;
import org.youscope.addon.celldetection.CellVisualizationException;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.resource.ResourceException;
import org.youscope.common.table.ColumnView;
import org.youscope.common.table.Table;
import org.youscope.common.util.ImageConvertException;
import org.youscope.common.util.ImageTools;

/**
 * Draws each cell's persistent track id and an optional short motion trail.
 *
 * <p>
 * Reads centre coordinates ({@code cell.center.x/.y}) and the track id column
 * added by a {@code TrackingAddon} (preferring {@code cell.track.id}, falling
 * back to the standard {@code track.index}, then to the row index) from the
 * cell table, so it composes with any segmenter + quantifier + tracker.
 *
 * <p>
 * The trail is inherently stateful across frames: the addon remembers each
 * track's recent centroids (bounded by the configured trail length) and prunes
 * tracks once their trail has fully aged out. Runs in-process (pure Java, no
 * IPC), like the label visualizer, so it is safe on the feedback path.
 *
 * @author Andreas P. Cuny
 */
class TrackVisualizerAddon extends ResourceAdapter<TrackVisualizerConfiguration> implements CellVisualizationAddon {
	private static final long serialVersionUID = 74007731210012002L;
	private static final float FONT_SIZE = 11f;
	private static final int TEXT_DX = 10;

	/** Distinct, stable per-track colours (cycled by track id). */
	private static final Color[] TRACK_COLORS = {
			new Color(255, 80, 80), new Color(80, 255, 80), new Color(90, 160, 255),
			new Color(255, 220, 60), new Color(255, 120, 255), new Color(80, 255, 255),
			new Color(255, 160, 60), new Color(200, 200, 255)
	};

	/**
	 * trackId to recent centroids (oldest first), capped at trailLength+1 points.
	 */
	private final Map<Integer, ArrayDeque<int[]>> trails = new HashMap<Integer, ArrayDeque<int[]>>();
	/** trackId -> frame index when last updated (for aging/pruning). */
	private final Map<Integer, Integer> lastSeen = new HashMap<Integer, Integer>();
	private int frame = 0;

	TrackVisualizerAddon(PositionInformation positionInformation, TrackVisualizerConfiguration configuration)
			throws ConfigurationException, RemoteException {
		super(positionInformation, configuration, TrackVisualizerConfiguration.CONFIGURATION_ID,
				TrackVisualizerConfiguration.class, "Track Visualizer");
	}

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		super.initialize(measurementContext);
		reset();
	}

	@Override
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		reset();
		super.uninitialize(measurementContext);
	}

	private synchronized void reset() {
		trails.clear();
		lastSeen.clear();
		frame = 0;
	}

	@Override
	public synchronized ImageEvent<?> visualizeCells(ImageEvent<?> orgImage, CellDetectionResult detectionResult)
			throws CellVisualizationException, RemoteException {
		if (!isInitialized())
			throw new CellVisualizationException("Addon not yet initialized.");
		if (orgImage == null)
			throw new CellVisualizationException("Original microscope image is null.");
		if (detectionResult == null || detectionResult.getCellTable() == null)
			throw new CellVisualizationException("Cell detection result (table) is null.");

		TrackVisualizerConfiguration cfg = getConfiguration();
		int trailLength = cfg.getTrailLength();

		BufferedImage image;
		try {
			BufferedImage src = ImageTools.getMicroscopeImage(orgImage);
			image = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g0 = image.createGraphics();
			if (cfg.isDrawIntoOrgImage())
				g0.drawImage(src, 0, 0, null);
			else {
				g0.setColor(Color.BLACK);
				g0.fillRect(0, 0, image.getWidth(), image.getHeight());
			}
			g0.dispose();
		} catch (ImageConvertException e) {
			throw new CellVisualizationException("Could not create image object out of original image data.", e);
		}

		Table table = detectionResult.getCellTable();
		ColumnView xView = getColumn(table, CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX.getColumnName());
		ColumnView yView = getColumn(table, CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX.getColumnName());
		if (xView == null || yView == null)
			return finish(image, orgImage); // no centres therefore nothing meaningful to draw

		// Track id column: tracker's "cell.track.id" first, then standard
		// "track.index".
		ColumnView trackView = getColumn(table, "cell.track.id");
		if (trackView == null)
			trackView = getColumn(table, CellDetectionTableColumns.TABLE_COLUMN_CELL_TRACKING_ID.getColumnName());

		frame++;
		int n = table.getNumRows();

		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(g.getFont().deriveFont(FONT_SIZE));
		int ascent = g.getFontMetrics().getAscent();

		for (int row = 0; row < n; row++) {
			Double xRaw = toDouble(xView.getValue(row));
			Double yRaw = toDouble(yView.getValue(row));
			if (xRaw == null || yRaw == null)
				continue;
			int x = (int) Math.round(xRaw.doubleValue());
			int y = (int) Math.round(yRaw.doubleValue());

			Integer trackId = trackView == null ? Integer.valueOf(row + 1) : toInt(trackView.getValue(row));
			if (trackId == null)
				trackId = Integer.valueOf(row + 1);

			// update this track's trail
			ArrayDeque<int[]> hist = trails.get(trackId);
			if (hist == null) {
				hist = new ArrayDeque<int[]>();
				trails.put(trackId, hist);
			}
			hist.addLast(new int[] { x, y });
			while (hist.size() > trailLength + 1)
				hist.removeFirst();
			lastSeen.put(trackId, Integer.valueOf(frame));

			Color color = TRACK_COLORS[Math.floorMod(trackId.intValue(), TRACK_COLORS.length)];

			// trail
			if (trailLength > 0 && hist.size() > 1) {
				g.setColor(color);
				int[] prev = null;
				for (int[] p : hist) {
					if (prev != null)
						g.drawLine(prev[0], prev[1], p[0], p[1]);
					prev = p;
				}
			}

			// current-position marker
			g.setColor(color);
			g.fillOval(x - 2, y - 2, 5, 5);

			// id text
			if (cfg.isDrawTrackIds()) {
				g.setColor(Color.WHITE);
				g.drawString(Integer.toString(trackId.intValue()), x + TEXT_DX, y + ascent / 2);
			}
		}

		// draw aging trails of tracks not present this frame, then prune fully-aged
		// tracks
		if (trailLength > 0) {
			for (Map.Entry<Integer, ArrayDeque<int[]>> e : trails.entrySet()) {
				Integer id = e.getKey();
				Integer seen = lastSeen.get(id);
				if (seen != null && seen.intValue() == frame)
					continue; // already drawn above
				ArrayDeque<int[]> hist = e.getValue();
				if (hist.size() < 2)
					continue;
				g.setColor(TRACK_COLORS[Math.floorMod(id.intValue(), TRACK_COLORS.length)]);
				int[] prev = null;
				for (int[] p : hist) {
					if (prev != null)
						g.drawLine(prev[0], prev[1], p[0], p[1]);
					prev = p;
				}
			}
		}
		g.dispose();

		prune(trailLength);
		return finish(image, orgImage);
	}

	private void prune(int trailLength) {
		java.util.Iterator<Map.Entry<Integer, Integer>> it = lastSeen.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Integer> e = it.next();
			if (frame - e.getValue().intValue() > trailLength) {
				trails.remove(e.getKey());
				it.remove();
			}
		}
	}

	private ImageEvent<?> finish(BufferedImage image, ImageEvent<?> orgImage) throws CellVisualizationException {
		try {
			ImageEvent<?> out = ImageTools.toYouScopeImage(image);
			out.setPositionInformation(orgImage.getPositionInformation());
			return out;
		} catch (ImageConvertException e) {
			throw new CellVisualizationException("Could not convert visualization image to YouScope image format.", e);
		}
	}

	private static ColumnView getColumn(Table table, String name) {
		try {
			return table.getColumnView(name);
		} catch (@SuppressWarnings("unused") Exception e) {
			return null;
		}
	}

	private static Double toDouble(Serializable v) {
		if (v instanceof Number)
			return Double.valueOf(((Number) v).doubleValue());
		return null;
	}

	private static Integer toInt(Serializable v) {
		if (v instanceof Number)
			return Integer.valueOf(((Number) v).intValue());
		return null;
	}
}
