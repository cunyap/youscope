/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
package org.youscope.plugin.cellxtracker;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.youscope.addon.analysis.AnalysisBackendClient;
import org.youscope.addon.celldetection.CellDetectionResult;
import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.addon.celldetection.TrackingAddon;
import org.youscope.addon.celldetection.TrackingException;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.resource.ResourceException;
import org.youscope.common.table.ColumnDefinition;
import org.youscope.common.table.ColumnView;
import org.youscope.common.table.Table;
import org.youscope.common.table.TableDefinition;
import org.youscope.common.table.TableException;

/**
 * CellX online tracker: frame-to-frame weighted-graph matching (overlap +
 * distance + shape), ported from CellXTrackerYouScope. The heavy numeric work
 * (regionprops, cost matrix, Hungarian assignment) runs in the shared quant
 * backend; this addon holds the previous frame's label mask and the
 * label-id to track-id map, and appends a flat {@code cell.track.id} column.
 *
 * @author YouScope
 */
class CellXTrackerAddon extends ResourceAdapter<CellXTrackerConfiguration> implements TrackingAddon {
	private static final long serialVersionUID = 74100049100012002L;
	private static final String BACKEND_VERSION = "v0.6.9";
	static final String TRACK_ID_COLUMN = "cell.track.id";
	private static final String TABLE_ID = "YouScope.CellDetection.TrackedCellX";

	private AnalysisBackendClient client = null;
	private int timeoutMs = 300000;

	// previous-frame state (held by the addon, backend is stateless)
	private ImageEvent<?> prevLabel = null;
	private Map<Integer, Integer> prevIdToTrack = new HashMap<Integer, Integer>();
	private int nextTrackId = 1;
	private int frameIndex = 0;
	// geometry {cx,cy,major,orient} of each currently-active track (last frame)
	private Map<Integer, double[]> trackGeom = new HashMap<Integer, double[]>();
	// older lost tracks eligible for gap reconnection: geometry + last-seen frame
	private Map<Integer, double[]> lostGeom = new HashMap<Integer, double[]>();
	private Map<Integer, Integer> lostLastSeen = new HashMap<Integer, Integer>();

	CellXTrackerAddon(PositionInformation positionInformation, CellXTrackerConfiguration configuration)
			throws ConfigurationException, RemoteException {
		super(positionInformation, configuration, CellXTrackerConfiguration.TYPE_IDENTIFIER,
				CellXTrackerConfiguration.class, "CellX online tracker.");
	}

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		if (isInitialized())
			return;
		super.initialize(measurementContext);
		resetTracks();

		String ys = System.getProperty("user.dir", ".");
		String cpython = firstNonNull(readConfig(ys, "quant_cpython_executable"), readConfig(ys, "cpython_executable"));
		if (cpython == null || !new File(cpython).exists())
			cpython = "python";
		timeoutMs = readConfigInt(ys, "celldetection_timeout_seconds", 300) * 1000;
		int port = readConfigInt(ys, "quant_backend_port", readConfigInt(ys, "celldetection_backend_port", 59713));
		int idle = readConfigInt(ys, "celldetection_idle_timeout_seconds", 1800);
		String host = firstNonNull(readConfig(ys, "quant_backend_host"), readConfig(ys, "celldetection_backend_host"));
		if (host == null)
			host = "127.0.0.1";
		String workdir = resolveWritableDir(
				firstNonNull(readConfig(ys, "quant_work_dir"), readConfig(ys, "celldetection_work_dir")),
				ys + File.separator + "scripts");
		String serverScript = workdir + File.separator + "quickdetect_server.py";
		String logFile = workdir + File.separator + "quickdetect_server.log";
		try {
			extractScript("/org/youscope/plugin/quickdetect/quickdetect_server.py", serverScript);
			extractScript("/org/youscope/addon/celldetection/utils/cell_detection_cpython.py",
					workdir + File.separator + "cell_detection_cpython.py");
		} catch (ResourceException e) {
			sendMessage("CellX tracker: could not extract backend scripts (" + e.getMessage() + ").");
		}
		client = new AnalysisBackendClient(host, port, cpython, serverScript, logFile, idle);
		try {
			client.ensureUp(BACKEND_VERSION, readConfigInt(ys, "celldetection_server_start_seconds", 60) * 1000);
		} catch (IOException e) {
			sendMessage("CellX tracker: backend not reachable (" + e.getMessage() + ").");
		}
	}

	@Override
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		client = null;
		resetTracks();
		super.uninitialize(measurementContext);
	}

	@Override
	public synchronized void resetTracks() throws RemoteException {
		prevLabel = null;
		prevIdToTrack = new HashMap<Integer, Integer>();
		nextTrackId = 1;
		frameIndex = 0;
		trackGeom = new HashMap<Integer, double[]>();
		lostGeom = new HashMap<Integer, double[]>();
		lostLastSeen = new HashMap<Integer, Integer>();
	}

	@Override
	public synchronized CellDetectionResult track(CellDetectionResult detectionResult)
			throws TrackingException, RemoteException {
		if (!isInitialized() || client == null)
			throw new TrackingException("Tracker not initialized.");
		Table source = detectionResult.getCellTable();
		ImageEvent<?> curLabel = detectionResult.getLabelImage();
		if (source == null || curLabel == null)
			return detectionResult; // nothing to track without a mask

		frameIndex++;
		int maxGap = getConfiguration().getMaxGapConnectionDistance();

		// backend links prev to cur (with gap-1 folded in) and reconnects older lost
		// tracks (gap 2..maxGap); it also returns each current cell's geometry.
		LinkResult lr = requestLinking(prevLabel, curLabel, maxGap);

		Map<Integer, Integer> curIdToTrack = new HashMap<Integer, Integer>();
		Map<Integer, double[]> curGeom = new HashMap<Integer, double[]>();
		for (int k = 0; k < lr.curIds.length; k++) {
			int labelId = lr.curIds[k];
			int linkedPrev = k < lr.linkedPrev.length ? lr.linkedPrev[k] : 0;
			int gapTrack = k < lr.gapTrack.length ? lr.gapTrack[k] : 0;
			Integer track = null;
			if (linkedPrev > 0)
				track = prevIdToTrack.get(Integer.valueOf(linkedPrev));
			if (track == null && gapTrack > 0) {
				track = Integer.valueOf(gapTrack); // reconnected older lost track
				lostGeom.remove(track);
				lostLastSeen.remove(track);
			}
			if (track == null)
				track = Integer.valueOf(nextTrackId++);
			curIdToTrack.put(Integer.valueOf(labelId), track);
			curGeom.put(track, new double[] { lr.cx[k], lr.cy[k], lr.major[k], lr.orient[k] });
		}

		// maintain the older lost-track pool
		if (maxGap > 0) {
			// age out lost tracks beyond the gap window
			java.util.Iterator<Map.Entry<Integer, Integer>> it = lostLastSeen.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, Integer> e = it.next();
				if (frameIndex - e.getValue().intValue() > maxGap) {
					lostGeom.remove(e.getKey());
					it.remove();
				}
			}
			// previous-frame tracks not continued this frame become newly lost
			java.util.Set<Integer> curTracks = new java.util.HashSet<Integer>(curIdToTrack.values());
			for (Map.Entry<Integer, Integer> e : prevIdToTrack.entrySet()) {
				Integer t = e.getValue();
				if (!curTracks.contains(t) && !lostGeom.containsKey(t)) {
					double[] g = trackGeom.get(t); // geometry from the previous frame
					if (g != null) {
						lostGeom.put(t, g);
						lostLastSeen.put(t, Integer.valueOf(frameIndex - 1));
					}
				}
			}
		}

		Table enriched = appendTrackColumn(source, curIdToTrack);

		// advance state to the current frame
		prevLabel = curLabel;
		prevIdToTrack = curIdToTrack;
		trackGeom = curGeom;

		return new CellDetectionResult(enriched, curLabel);
	}

	/** Parsed backend response for one tracking step. */
	private static final class LinkResult {
		int[] curIds = new int[0];
		int[] linkedPrev = new int[0];
		int[] gapTrack = new int[0];
		double[] cx = new double[0];
		double[] cy = new double[0];
		double[] major = new double[0];
		double[] orient = new double[0];
	}

	// Backend call

	/**
	 * Sends prev and cur masks (and the older lost-track pool) to the backend
	 * 'track'
	 * op. When {@code prev} is null (first frame) an all-zero previous mask is
	 * sent,
	 * so every current cell starts a new track while still returning geometry.
	 */
	private LinkResult requestLinking(ImageEvent<?> prev, ImageEvent<?> cur, int maxGap) throws TrackingException {
		List<Path> temps = new ArrayList<Path>();
		try {
			Path cf = writeImage(cur, temps);
			String prevMeta;
			if (prev != null)
				prevMeta = imageMetaJson(prev, writeImage(prev, temps));
			else
				prevMeta = imageMetaJson(cur, writeZerosLike(cur, temps)); // empty previous frame

			CellXTrackerConfiguration cfg = getConfiguration();
			StringBuilder req = new StringBuilder();
			req.append("{\"op\":\"track\",\"tracker\":\"cellx\",");
			req.append("\"prev_mask\":").append(prevMeta).append(",");
			req.append("\"cur_mask\":").append(imageMetaJson(cur, cf)).append(",");
			if (maxGap > 0)
				req.append("\"lost\":").append(buildLostJson()).append(",");
			req.append("\"params\":{\"spatial_motion_factor\":").append(cfg.getSpatialMotionFactor());
			req.append(",\"max_gap_connection_distance\":").append(maxGap).append("}}");

			String resp = client.request(req.toString(), timeoutMs);
			if (!AnalysisBackendClient.extractBool(resp, "ok", false))
				throw new TrackingException("Backend error: " + AnalysisBackendClient.extractString(resp, "error"));
			LinkResult lr = new LinkResult();
			lr.curIds = toIntArray(
					AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "cur_ids_b64")));
			lr.linkedPrev = toIntArray(AnalysisBackendClient
					.inflateBase64(AnalysisBackendClient.extractString(resp, "linked_prev_ids_b64")));
			lr.gapTrack = toIntArray(AnalysisBackendClient
					.inflateBase64(AnalysisBackendClient.extractString(resp, "gap_track_ids_b64")));
			lr.cx = toDoubleArray(
					AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "cur_cx_b64")));
			lr.cy = toDoubleArray(
					AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "cur_cy_b64")));
			lr.major = toDoubleArray(
					AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "cur_major_b64")));
			lr.orient = toDoubleArray(
					AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "cur_orient_b64")));
			return lr;
		} catch (TrackingException e) {
			throw e;
		} catch (Exception e) {
			throw new TrackingException("CellX tracking failed: " + e.getMessage(), e);
		} finally {
			for (Path p : temps) {
				try {
					Files.deleteIfExists(p);
				} catch (@SuppressWarnings("unused") IOException ignored) {
					/* not fatal */ }
			}
		}
	}

	// Table assembly

	private Table appendTrackColumn(Table source, Map<Integer, Integer> idToTrack) throws TrackingException {
		TableDefinition srcDef = source.getTableDefinition();
		ColumnDefinition<?>[] srcCols = srcDef.getColumnDefinitions();
		ColumnDefinition<?>[] cols = new ColumnDefinition<?>[srcCols.length + 1];
		System.arraycopy(srcCols, 0, cols, 0, srcCols.length);
		cols[srcCols.length] = ColumnDefinition.createIntegerColumnDefinition(TRACK_ID_COLUMN,
				"Persistent track id assigned by the CellX online tracker.", false);
		TableDefinition def = new TableDefinition(TABLE_ID, "Segmented + quantified + CellX-tracked cells.", cols);

		Table out = new Table(def, source.getCreationRuntime(), source.getPositionInformation(),
				source.getExecutionInformation());
		int n = source.getNumRows();
		int nCols = srcCols.length;
		try {
			ColumnView[] views = new ColumnView[nCols];
			for (int c = 0; c < nCols; c++)
				views[c] = source.getColumnView(c);
			ColumnView idView = source.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_CELL_ID.getColumnName());
			for (int r = 0; r < n; r++) {
				Serializable[] row = new Serializable[nCols + 1];
				for (int c = 0; c < nCols; c++)
					row[c] = views[c].getValue(r);
				Serializable idv = idView.getValue(r);
				Integer cellId = (idv instanceof Number) ? Integer.valueOf(((Number) idv).intValue())
						: Integer.valueOf(r + 1);
				Integer track = idToTrack.get(cellId);
				row[nCols] = Integer.valueOf(track == null ? 0 : track.intValue());
				out.addRow(row);
			}
		} catch (TableException e) {
			throw new TrackingException("Could not add CellX track column: " + e.getMessage(), e);
		}
		return out;
	}

	// Helpers (shared with the quantifier machinery)

	private static int[] toIntArray(byte[] b) {
		ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
		int[] out = new int[b.length / 4];
		for (int i = 0; i < out.length; i++)
			out[i] = bb.getInt();
		return out;
	}

	private static double[] toDoubleArray(byte[] b) {
		ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
		double[] out = new double[b.length / 8];
		for (int i = 0; i < out.length; i++)
			out[i] = bb.getDouble();
		return out;
	}

	/**
	 * JSON array of the older lost-track pool: {track, gap, cx, cy, major, orient}.
	 */
	private String buildLostJson() {
		StringBuilder sb = new StringBuilder("[");
		boolean first = true;
		for (Map.Entry<Integer, double[]> e : lostGeom.entrySet()) {
			Integer tid = e.getKey();
			double[] g = e.getValue();
			Integer lastSeen = lostLastSeen.get(tid);
			if (lastSeen == null)
				continue;
			int gap = frameIndex - lastSeen.intValue();
			if (!first)
				sb.append(",");
			first = false;
			sb.append("{\"track\":").append(tid.intValue());
			sb.append(",\"gap\":").append(gap);
			sb.append(",\"cx\":").append(g[0]);
			sb.append(",\"cy\":").append(g[1]);
			sb.append(",\"major\":").append(g[2]);
			sb.append(",\"orient\":").append(g[3]).append("}");
		}
		sb.append("]");
		return sb.toString();
	}

	/** Writes an all-zero label buffer with the same dimensions as {@code like}. */
	private Path writeZerosLike(ImageEvent<?> like, List<Path> temps) throws IOException {
		int bytes = like.getWidth() * like.getHeight() * like.getBytesPerPixel();
		Path p = Files.createTempFile("ys_z_", ".raw");
		temps.add(p);
		Files.write(p, new byte[bytes]);
		return p;
	}

	private Path writeImage(ImageEvent<?> image, List<Path> temps) throws IOException {
		byte[] le = serializeImageLE(image);
		Path p = Files.createTempFile("ys_t_", ".raw");
		temps.add(p);
		Files.write(p, le);
		return p;
	}

	private static byte[] serializeImageLE(ImageEvent<?> image) {
		int w = image.getWidth(), h = image.getHeight(), bpp = image.getBytesPerPixel();
		Object raw = image.getImageData();
		byte[] data = new byte[w * h * bpp];
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		if (raw instanceof byte[])
			System.arraycopy(raw, 0, data, 0, Math.min(((byte[]) raw).length, data.length));
		else if (raw instanceof short[]) {
			short[] s = (short[]) raw;
			int n = Math.min(s.length, w * h);
			for (int i = 0; i < n; i++)
				buf.putShort(s[i]);
		} else if (raw instanceof int[]) {
			int[] s = (int[]) raw;
			int n = Math.min(s.length, w * h);
			for (int i = 0; i < n; i++)
				buf.putInt(s[i]);
		} else
			throw new IllegalArgumentException("Unsupported image data type.");
		return data;
	}

	private static String imageMetaJson(ImageEvent<?> image, Path file) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"path\":\"").append(AnalysisBackendClient.escapeJson(file.toString())).append("\",");
		sb.append("\"width\":").append(image.getWidth()).append(",");
		sb.append("\"height\":").append(image.getHeight()).append(",");
		sb.append("\"bpp\":").append(image.getBytesPerPixel()).append(",");
		sb.append("\"bitdepth\":").append(image.getBitDepth()).append(",");
		sb.append("\"tx\":").append(image.isTransposeX()).append(",");
		sb.append("\"ty\":").append(image.isTransposeY()).append(",");
		sb.append("\"sw\":").append(image.isSwitchXY()).append("}");
		return sb.toString();
	}

	private static String firstNonNull(String a, String b) {
		return a != null ? a : b;
	}

	private void extractScript(String resourceName, String targetPath) throws ResourceException {
		File f = new File(targetPath);
		try {
			if (f.getParentFile() != null)
				f.getParentFile().mkdirs();
			InputStream is = CellXTrackerAddon.class.getResourceAsStream(resourceName);
			if (is == null)
				throw new ResourceException(resourceName + " not found in plugin JAR.");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int n;
			try (BufferedInputStream bis = new BufferedInputStream(is)) {
				while ((n = bis.read(buf)) != -1)
					baos.write(buf, 0, n);
			}
			Files.write(f.toPath(), baos.toByteArray());
		} catch (IOException e) {
			throw new ResourceException("Could not write script to " + targetPath, e);
		}
	}

	private static String readConfig(String ys, String key) {
		File cfg = new File(ys, "graalpy_config.txt");
		if (!cfg.exists())
			return null;
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(Files.newInputStream(cfg.toPath()), Charset.forName("UTF-8")))) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#") || line.isEmpty())
					continue;
				int eq = line.indexOf('=');
				if (eq > 0 && line.substring(0, eq).trim().equals(key))
					return line.substring(eq + 1).trim();
			}
		} catch (@SuppressWarnings("unused") IOException ignored) {
			/* fall through */ }
		return null;
	}

	private static int readConfigInt(String ys, String key, int def) {
		String v = readConfig(ys, key);
		if (v == null)
			return def;
		try {
			return Integer.parseInt(v.trim());
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return def;
		}
	}

	private static String resolveWritableDir(String preferred, String fallback) {
		String[] cands = { preferred, fallback,
				System.getProperty("java.io.tmpdir") + File.separator + "youscope-celldetection" };
		for (String c : cands) {
			if (c == null || c.trim().isEmpty())
				continue;
			File d = new File(c.trim());
			try {
				if (!d.exists())
					d.mkdirs();
				if (d.isDirectory() && d.canWrite())
					return d.getAbsolutePath();
			} catch (@SuppressWarnings("unused") Exception ignored) {
				/* next */ }
		}
		return new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
	}
}
