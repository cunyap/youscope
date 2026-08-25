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
package org.youscope.plugin.trackmatetracker;

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
import org.youscope.addon.celldetection.TrackingConfiguration;
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
 * trackmate online tracker. Links the current frame's cells to the previous frame's
 * cells via a dedicated trackmate backend (own server, own Python env), then maps
 * previous label ids to persistent track ids and appends {@code cell.track.id}.
 * Frame-to-frame only (no gap closing), like the CellX / nearest-neighbour trackers.
 */
class TrackmateTrackerAddon extends ResourceAdapter<TrackmateTrackerConfiguration> implements TrackingAddon
{
	private static final String BACKEND_VERSION = "v0.1.2";
	private static final int DEFAULT_PORT = 59717;
	private static final String TABLE_ID = "YouScope.tracking.Trackmate.Results";

	private AnalysisBackendClient client = null;
	private int timeoutMs = 300000;

	private ImageEvent<?> prevLabel = null;
	private Map<Integer, Integer> prevIdToTrack = new HashMap<Integer, Integer>();
	private int nextTrackId = 1;

	TrackmateTrackerAddon(PositionInformation positionInformation, TrackmateTrackerConfiguration configuration)
			throws ConfigurationException, RemoteException
	{
		super(positionInformation, configuration, TrackmateTrackerConfiguration.TYPE_IDENTIFIER,
				TrackmateTrackerConfiguration.class, "TrackMate tracker.");
	}

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException
	{
		if(isInitialized())
			return;
		super.initialize(measurementContext);
		resetTracks();

		String ys = System.getProperty("user.dir", ".");
		// Dedicated trackmate_* env, falling back to the shared celldetection_* keys.
		String cpython = firstNonNull(readConfig(ys, "trackmate_cpython_executable"),
				readConfig(ys, "celldetection_cpython_executable"), readConfig(ys, "cpython_executable"));
		if(cpython == null || !new File(cpython).exists())
			cpython = "python";
		timeoutMs = readConfigInt(ys, "trackmate_timeout_seconds",
				readConfigInt(ys, "celldetection_timeout_seconds", 300)) * 1000;
		int port = readConfigInt(ys, "trackmate_backend_port", DEFAULT_PORT);
		int idle = readConfigInt(ys, "trackmate_idle_timeout_seconds",
				readConfigInt(ys, "celldetection_idle_timeout_seconds", 1800));
		String host = firstNonNull(readConfig(ys, "trackmate_backend_host"),
				readConfig(ys, "celldetection_backend_host"));
		if(host == null)
			host = "127.0.0.1";
		String workdir = resolveWritableDir(firstNonNull(readConfig(ys, "trackmate_work_dir"),
				readConfig(ys, "celldetection_work_dir")), ys + File.separator + "scripts");
		String serverScript = workdir + File.separator + "trackmate_server.py";
		String logFile = workdir + File.separator + "trackmate_server.log";

		try
		{
			extractScript("/org/youscope/plugin/trackmatetracker/trackmate_server.py", serverScript);
		}
		catch(ResourceException e)
		{
			sendMessage("Trackmate tracker: could not extract backend script (" + e.getMessage() + ").");
		}

		client = new AnalysisBackendClient(host, port, cpython, serverScript, logFile, idle);
		try
		{
			client.ensureUp(BACKEND_VERSION, readConfigInt(ys, "trackmate_server_start_seconds",
					readConfigInt(ys, "celldetection_server_start_seconds", 60)) * 1000);
		}
		catch(IOException e)
		{
			sendMessage("Trackmate tracker: backend not reachable (" + e.getMessage() + ").");
		}
	}

	@Override
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException
	{
		client = null;
		resetTracks();
		super.uninitialize(measurementContext);
	}

	@Override
	public synchronized void resetTracks() throws RemoteException
	{
		prevLabel = null;
		prevIdToTrack = new HashMap<Integer, Integer>();
		nextTrackId = 1;
	}

	@Override
	public synchronized CellDetectionResult track(CellDetectionResult detectionResult)
			throws TrackingException, RemoteException
	{
		if(!isInitialized() || client == null)
			throw new TrackingException("Trackmate tracker not initialized.");
		Table source = detectionResult.getCellTable();
		ImageEvent<?> curLabel = detectionResult.getLabelImage();
		if(source == null || curLabel == null)
			return detectionResult;

		Map<Integer, Integer> curIdToTrack = new HashMap<Integer, Integer>();
		if(prevLabel == null)
		{
			// first frame: every cell starts a new track
			int[] ids = readCellIds(source);
			for(int id : ids)
				curIdToTrack.put(Integer.valueOf(id), Integer.valueOf(nextTrackId++));
		}
		else
		{
			int[][] linked = requestLinking(prevLabel, curLabel);
			int[] curIds = linked[0];
			int[] linkedPrev = linked[1];
			for(int k = 0; k < curIds.length; k++)
			{
				int labelId = curIds[k];
				int prevId = k < linkedPrev.length ? linkedPrev[k] : 0;
				Integer track = null;
				if(prevId > 0)
					track = prevIdToTrack.get(Integer.valueOf(prevId));
				if(track == null)
					track = Integer.valueOf(nextTrackId++);
				curIdToTrack.put(Integer.valueOf(labelId), track);
			}
		}

		Table enriched = appendTrackColumn(source, curIdToTrack);
		prevLabel = curLabel;
		prevIdToTrack = curIdToTrack;
		return new CellDetectionResult(enriched, curLabel);
	}

	// Backend call

	/** Sends prev plus cur masks to the trackmate backend; returns {curIds, linkedPrevIds}. */
	private int[][] requestLinking(ImageEvent<?> prev, ImageEvent<?> cur) throws TrackingException
	{
		List<Path> temps = new ArrayList<Path>();
		try
		{
			Path pf = writeImage(prev, temps);
			Path cf = writeImage(cur, temps);
			TrackmateTrackerConfiguration cfg = getConfiguration();
			StringBuilder req = new StringBuilder();
			req.append("{\"op\":\"track\",\"tracker\":\"trackmate\",");
			req.append("\"prev_mask\":").append(imageMetaJson(prev, pf)).append(",");
			req.append("\"cur_mask\":").append(imageMetaJson(cur, cf)).append(",");
			req.append("\"params\":{\"linking_max_distance\":").append(cfg.getLinkingMaxDistance());
			req.append("}}");

			String resp = client.request(req.toString(), timeoutMs);
			if(!AnalysisBackendClient.extractBool(resp, "ok", false))
				throw new TrackingException("Backend error: " + AnalysisBackendClient.extractString(resp, "error"));
			int[] curIds = toIntArray(AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "cur_ids_b64")));
			int[] linked = toIntArray(AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "linked_prev_ids_b64")));
			return new int[][] { curIds, linked };
		}
		catch(TrackingException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new TrackingException("Trackmate tracking failed: " + e.getMessage(), e);
		}
		finally
		{
			for(Path p : temps)
			{
				try { Files.deleteIfExists(p); } catch(@SuppressWarnings("unused") IOException ignored) { /* best effort */ }
			}
		}
	}

	// Table assembly

	private int[] readCellIds(Table source) throws TrackingException
	{
		int n = source.getNumRows();
		int[] ids = new int[n];
		try
		{
			ColumnView idView = source.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_CELL_ID.getColumnName());
			for(int r = 0; r < n; r++)
			{
				Serializable v = idView.getValue(r);
				ids[r] = (v instanceof Number) ? ((Number) v).intValue() : (r + 1);
			}
		}
		catch(TableException e)
		{
			for(int r = 0; r < n; r++)
				ids[r] = r + 1;
		}
		return ids;
	}

	private Table appendTrackColumn(Table source, Map<Integer, Integer> idToTrack) throws TrackingException
	{
		TableDefinition srcDef = source.getTableDefinition();
		ColumnDefinition<?>[] srcCols = srcDef.getColumnDefinitions();
		ColumnDefinition<?>[] cols = new ColumnDefinition<?>[srcCols.length + 1];
		System.arraycopy(srcCols, 0, cols, 0, srcCols.length);
		cols[srcCols.length] = ColumnDefinition.createIntegerColumnDefinition(
				TrackingConfiguration.TRACK_ID_COLUMN, "Persistent track id assigned by the TrackMate tracker.", false);
		TableDefinition def = new TableDefinition(TABLE_ID, "Segmented + quantified + trackmate-tracked cells.", cols);

		Table out = new Table(def, source.getCreationRuntime(), source.getPositionInformation(), source.getExecutionInformation());
		int n = source.getNumRows();
		int nCols = srcCols.length;
		try
		{
			ColumnView[] views = new ColumnView[nCols];
			for(int c = 0; c < nCols; c++)
				views[c] = source.getColumnView(c);
			ColumnView idView = source.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_CELL_ID.getColumnName());
			for(int r = 0; r < n; r++)
			{
				Serializable[] row = new Serializable[nCols + 1];
				for(int c = 0; c < nCols; c++)
					row[c] = views[c].getValue(r);
				Serializable idv = idView.getValue(r);
				Integer cellId = (idv instanceof Number) ? Integer.valueOf(((Number) idv).intValue()) : Integer.valueOf(r + 1);
				Integer track = idToTrack.get(cellId);
				row[nCols] = Integer.valueOf(track == null ? 0 : track.intValue());
				out.addRow(row);
			}
		}
		catch(TableException e)
		{
			throw new TrackingException("Could not add trackmate track column: " + e.getMessage(), e);
		}
		return out;
	}

	// Image serialisation plus helpers (@ToDo: migrate to shared new PythonBackendSupport) 

	private Path writeImage(ImageEvent<?> image, List<Path> temps) throws IOException
	{
		byte[] le = serializeImageLE(image);
		Path p = Files.createTempFile("ys_trackmate_", ".raw");
		temps.add(p);
		Files.write(p, le);
		return p;
	}

	private static byte[] serializeImageLE(ImageEvent<?> image)
	{
		int w = image.getWidth(), h = image.getHeight(), bpp = image.getBytesPerPixel();
		Object raw = image.getImageData();
		byte[] data = new byte[w * h * bpp];
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		if(raw instanceof byte[])
			System.arraycopy(raw, 0, data, 0, Math.min(((byte[]) raw).length, data.length));
		else if(raw instanceof short[])
		{
			short[] s = (short[]) raw;
			int nn = Math.min(s.length, w * h);
			for(int i = 0; i < nn; i++)
				buf.putShort(s[i]);
		}
		else if(raw instanceof int[])
		{
			int[] s = (int[]) raw;
			int nn = Math.min(s.length, w * h);
			for(int i = 0; i < nn; i++)
				buf.putInt(s[i]);
		}
		else
			throw new IllegalArgumentException("Unsupported image data type.");
		return data;
	}

	private static String imageMetaJson(ImageEvent<?> image, Path file)
	{
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

	private static int[] toIntArray(byte[] b)
	{
		ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
		int[] out = new int[b.length / 4];
		for(int i = 0; i < out.length; i++)
			out[i] = bb.getInt();
		return out;
	}

	private static String firstNonNull(String... vals)
	{
		for(String v : vals)
			if(v != null && !v.trim().isEmpty())
				return v;
		return null;
	}

	private static String readConfig(String ys, String key)
	{
		File cfg = new File(ys, "graalpy_config.txt");
		if(!cfg.exists())
			return null;
		try(BufferedReader r = new BufferedReader(new InputStreamReader(Files.newInputStream(cfg.toPath()), Charset.forName("UTF-8"))))
		{
			String line;
			while((line = r.readLine()) != null)
			{
				line = line.trim();
				if(line.startsWith("#") || line.isEmpty())
					continue;
				int eq = line.indexOf('=');
				if(eq > 0 && line.substring(0, eq).trim().equals(key))
					return line.substring(eq + 1).trim();
			}
		}
		catch(@SuppressWarnings("unused") IOException ignored) { /* fall through */ }
		return null;
	}

	private static int readConfigInt(String ys, String key, int def)
	{
		String v = readConfig(ys, key);
		if(v == null)
			return def;
		try { return Integer.parseInt(v.trim()); }
		catch(@SuppressWarnings("unused") NumberFormatException e) { return def; }
	}

	private void extractScript(String resourceName, String targetPath) throws ResourceException
	{
		File f = new File(targetPath);
		try
		{
			if(f.getParentFile() != null)
				f.getParentFile().mkdirs();
			InputStream is = TrackmateTrackerAddon.class.getResourceAsStream(resourceName);
			if(is == null)
				throw new ResourceException(resourceName + " not found in plugin JAR.");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int nrd;
			try(BufferedInputStream bis = new BufferedInputStream(is))
			{
				while((nrd = bis.read(buf)) != -1)
					baos.write(buf, 0, nrd);
			}
			Files.write(f.toPath(), baos.toByteArray());
		}
		catch(IOException e)
		{
			throw new ResourceException("Could not write script to " + targetPath, e);
		}
	}

	private static String resolveWritableDir(String preferred, String fallback)
	{
		String[] cands = { preferred, fallback, System.getProperty("java.io.tmpdir") + File.separator + "youscope-celldetection" };
		for(String c : cands)
		{
			if(c == null || c.trim().isEmpty())
				continue;
			File d = new File(c.trim());
			try { if(!d.exists()) d.mkdirs(); if(d.isDirectory() && d.canWrite()) return d.getAbsolutePath(); }
			catch(@SuppressWarnings("unused") Exception ignored) { /* next */ }
		}
		return new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
	}
}
