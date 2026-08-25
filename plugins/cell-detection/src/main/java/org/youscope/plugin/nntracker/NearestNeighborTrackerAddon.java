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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.youscope.addon.celldetection.CellDetectionResult;
import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.addon.celldetection.TrackingAddon;
import org.youscope.addon.celldetection.TrackingException;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.resource.ResourceException;
import org.youscope.common.table.ColumnDefinition;
import org.youscope.common.table.ColumnView;
import org.youscope.common.table.Table;
import org.youscope.common.table.TableDefinition;
import org.youscope.common.table.TableException;

/**
 * Minimal online tracker: frame-by-frame nearest-neighbour linking with
 * distance gating (global-greedy assignment). Runs in-process (no IPC) since it
 * is cheap and sits on the feedback critical path. Adds a {@code cell.track.id}
 * column; unmatched cells start new tracks. Reference implementation — just
 * linking, flat track ids, no lineage/division.
 */
class NearestNeighborTrackerAddon extends ResourceAdapter<NearestNeighborTrackerConfiguration>
		implements TrackingAddon {
	private static final long serialVersionUID = 90112233445501L;
	static final String TRACK_ID_COLUMN = "cell.track.id";
	private static final String TABLE_ID = "YouScope.CellDetection.Tracked";

	private double[] prevX = new double[0];
	private double[] prevY = new double[0];
	private int[] prevTrack = new int[0];
	private int nextTrackId = 1;

	NearestNeighborTrackerAddon(PositionInformation positionInformation,
			NearestNeighborTrackerConfiguration configuration) throws ConfigurationException, RemoteException {
		super(positionInformation, configuration, NearestNeighborTrackerConfiguration.TYPE_IDENTIFIER,
				NearestNeighborTrackerConfiguration.class, "Nearest-neighbour online tracker.");
	}

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		super.initialize(measurementContext);
		resetTracks();
	}

	@Override
	public synchronized void resetTracks() {
		prevX = new double[0];
		prevY = new double[0];
		prevTrack = new int[0];
		nextTrackId = 1;
	}

	@Override
	public synchronized CellDetectionResult track(CellDetectionResult detectionResult)
			throws TrackingException, RemoteException {
		if (!isInitialized())
			throw new TrackingException("Tracker not initialized.");
		Table source = detectionResult.getCellTable();
		if (source == null)
			return detectionResult;

		int n = source.getNumRows();
		double[] x = new double[n];
		double[] y = new double[n];
		try {
			ColumnView xc = source.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX.getColumnName());
			ColumnView yc = source.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX.getColumnName());
			for (int r = 0; r < n; r++) {
				x[r] = toDouble(xc.getValue(r));
				y[r] = toDouble(yc.getValue(r));
			}
		} catch (TableException e) {
			throw new TrackingException("Cell table lacks centre columns needed for tracking.", e);
		}

		int[] trackIds = assign(x, y);

		Table enriched = appendTrackColumn(source, trackIds);

		// update state to the current frame
		prevX = x;
		prevY = y;
		prevTrack = trackIds;

		return new CellDetectionResult(enriched, detectionResult.getLabelImage());
	}

	/** Global-greedy nearest-neighbour assignment within the gating distance. */
	private int[] assign(double[] x, double[] y) {
		int n = x.length;
		int[] track = new int[n];
		for (int i = 0; i < n; i++)
			track[i] = 0; // 0 = unassigned

		double maxD = getConfiguration().getMaxLinkingDistancePx();
		double maxD2 = maxD * maxD;
		int m = prevX.length;
		if (m > 0 && n > 0) {
			List<double[]> pairs = new ArrayList<double[]>(); // {dist2, i, j}
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < m; j++) {
					double dx = x[i] - prevX[j];
					double dy = y[i] - prevY[j];
					double d2 = dx * dx + dy * dy;
					if (d2 <= maxD2)
						pairs.add(new double[] { d2, i, j });
				}
			}
			Collections.sort(pairs, new Comparator<double[]>() {
				@Override
				public int compare(double[] a, double[] b) {
					return Double.compare(a[0], b[0]);
				}
			});
			boolean[] usedCur = new boolean[n];
			boolean[] usedPrev = new boolean[m];
			for (double[] p : pairs) {
				int i = (int) p[1];
				int j = (int) p[2];
				if (usedCur[i] || usedPrev[j])
					continue;
				usedCur[i] = true;
				usedPrev[j] = true;
				track[i] = prevTrack[j];
			}
		}
		for (int i = 0; i < n; i++) {
			if (track[i] == 0)
				track[i] = nextTrackId++;
		}
		return track;
	}

	private Table appendTrackColumn(Table source, int[] trackIds) throws TrackingException {
		TableDefinition srcDef = source.getTableDefinition();
		ColumnDefinition<?>[] srcCols = srcDef.getColumnDefinitions();
		ColumnDefinition<?>[] cols = new ColumnDefinition<?>[srcCols.length + 1];
		System.arraycopy(srcCols, 0, cols, 0, srcCols.length);
		cols[srcCols.length] = ColumnDefinition.createIntegerColumnDefinition(TRACK_ID_COLUMN,
				"Persistent track id assigned by the online tracker.", false);
		TableDefinition def = new TableDefinition(TABLE_ID, "Segmented + quantified + tracked cells.", cols);

		Table out = new Table(def, source.getCreationRuntime(), source.getPositionInformation(),
				source.getExecutionInformation());
		int n = source.getNumRows();
		int nCols = srcCols.length;
		try {
			ColumnView[] views = new ColumnView[nCols];
			for (int c = 0; c < nCols; c++)
				views[c] = source.getColumnView(c);
			for (int r = 0; r < n; r++) {
				Serializable[] row = new Serializable[nCols + 1];
				for (int c = 0; c < nCols; c++)
					row[c] = views[c].getValue(r);
				row[nCols] = Integer.valueOf(trackIds[r]);
				out.addRow(row);
			}
		} catch (TableException e) {
			throw new TrackingException("Could not add track column: " + e.getMessage(), e);
		}
		return out;
	}

	private static double toDouble(Serializable v) {
		if (v instanceof Number)
			return ((Number) v).doubleValue();
		return Double.NaN;
	}
}
