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
package org.youscope.plugin.cellprofilerquantifier;

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
import java.util.List;

import org.youscope.addon.analysis.AnalysisBackendClient;
import org.youscope.addon.celldetection.CellDetectionResult;
import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.addon.celldetection.QuantificationAddon;
import org.youscope.addon.celldetection.QuantificationConfiguration;
import org.youscope.addon.celldetection.QuantificationException;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.resource.ResourceException;
import org.youscope.common.table.ColumnDefinition;
import org.youscope.common.table.Table;
import org.youscope.common.table.TableDefinition;
import org.youscope.common.table.TableException;

/**
 * CellProfiler (Mode A) quantifier addon. Sends the segmenter's label mask and the
 * quantification channels to a dedicated CellProfiler backend, which runs a fixed
 * set of measurement modules (size/shape, intensity, texture) on the pre-segmented
 * objects and returns per-cell feature columns. The requested feature list is
 * fixed by {@link CellProfilerFeatures} so the table columns match at config time
 * and run time regardless of CellProfiler version.
 */
class CellProfilerQuantifierAddon extends ResourceAdapter<CellProfilerQuantifierConfiguration>
		implements QuantificationAddon
{
	private static final String BACKEND_VERSION = "v0.1.3";
	private static final int DEFAULT_PORT = 59716;
	private static final String TABLE_ID = "YouScope.quantification.CellProfiler.Results";

	private AnalysisBackendClient client = null;
	private int timeoutMs = 600000;

	CellProfilerQuantifierAddon(PositionInformation positionInformation,
			CellProfilerQuantifierConfiguration configuration)
			throws ConfigurationException, RemoteException
	{
		super(positionInformation, configuration, CellProfilerQuantifierConfiguration.TYPE_IDENTIFIER,
				CellProfilerQuantifierConfiguration.class, "CellProfiler (Mode A) quantifier.");
	}

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException
	{
		if(isInitialized())
			return;
		super.initialize(measurementContext);
		String ys = System.getProperty("user.dir", ".");
		// Dedicated cellprofiler_* env, falling back to the shared celldetection_* keys.
		String cpython = firstNonNull(readConfig(ys, "cellprofiler_cpython_executable"),
				readConfig(ys, "celldetection_cpython_executable"), readConfig(ys, "cpython_executable"));
		if(cpython == null || !new File(cpython).exists())
			cpython = "python";
		timeoutMs = readConfigInt(ys, "cellprofiler_timeout_seconds",
				readConfigInt(ys, "celldetection_timeout_seconds", 600)) * 1000;
		int port = readConfigInt(ys, "cellprofiler_backend_port", DEFAULT_PORT);
		int idle = readConfigInt(ys, "cellprofiler_idle_timeout_seconds",
				readConfigInt(ys, "celldetection_idle_timeout_seconds", 1800));
		String host = firstNonNull(readConfig(ys, "cellprofiler_backend_host"),
				readConfig(ys, "celldetection_backend_host"));
		if(host == null)
			host = "127.0.0.1";
		String workdir = resolveWritableDir(firstNonNull(readConfig(ys, "cellprofiler_work_dir"),
				readConfig(ys, "celldetection_work_dir")), ys + File.separator + "scripts");
		String serverScript = workdir + File.separator + "cellprofiler_server.py";
		String logFile = workdir + File.separator + "cellprofiler_server.log";

		try
		{
			extractScript("/org/youscope/plugin/cellprofilerquantifier/cellprofiler_server.py", serverScript);
		}
		catch(ResourceException e)
		{
			sendMessage("CellProfiler quantifier: could not extract backend script (" + e.getMessage() + ").");
		}

		client = new AnalysisBackendClient(host, port, cpython, serverScript, logFile, idle);
		try
		{
			client.ensureUp(BACKEND_VERSION, readConfigInt(ys, "cellprofiler_server_start_seconds",
					readConfigInt(ys, "celldetection_server_start_seconds", 120)) * 1000);
		}
		catch(IOException e)
		{
			sendMessage("CellProfiler quantifier: backend not reachable (" + e.getMessage() + ").");
		}
	}

	@Override
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException
	{
		client = null;
		super.uninitialize(measurementContext);
	}

	@Override
	public CellDetectionResult quantify(CellDetectionResult detectionResult, ImageEvent<?>[] quantificationImages)
			throws QuantificationException, RemoteException
	{
		if(!isInitialized() || client == null)
			throw new QuantificationException("CellProfiler quantifier not initialized.");
		ImageEvent<?> label = detectionResult.getLabelImage();
		if(label == null)
			throw new QuantificationException("Segmenter produced no label image; cannot quantify. "
					+ "Enable label output on the segmentation algorithm.");
		if(quantificationImages == null)
			quantificationImages = new ImageEvent[0];

		CellProfilerQuantifierConfiguration cfg = getConfiguration();
		List<Path> temps = new ArrayList<Path>();
		try
		{
			String[] suffixes = new String[quantificationImages.length];
			for(int i = 0; i < quantificationImages.length; i++)
			{
				ImageEvent<?> qi = quantificationImages[i];
				if(qi != null && (qi.getWidth() != label.getWidth() || qi.getHeight() != label.getHeight()))
					throw new QuantificationException("Quantification channel " + i + " is " + qi.getWidth() + "x"
							+ qi.getHeight() + " but the segmentation mask is " + label.getWidth() + "x"
							+ label.getHeight() + ". They must have the same dimensions (check binning / ROI / pixel size).");
				suffixes[i] = QuantificationConfiguration.channelSuffix(qi != null ? qi.getChannel() : null, i);
			}

			Path maskFile = writeImage(label, temps);
			StringBuilder chans = new StringBuilder("[");
			for(int i = 0; i < quantificationImages.length; i++)
			{
				Path cf = writeImage(quantificationImages[i], temps);
				if(i > 0)
					chans.append(",");
				chans.append(imageMetaJson(quantificationImages[i], cf, suffixes[i]));
			}
			chans.append("]");

			List<String> kinds = CellProfilerFeatures.moduleKinds(cfg.isMeasureShape(),
					cfg.isMeasureIntensity(), cfg.isMeasureTexture());
			List<String> requested = CellProfilerFeatures.requestedFeatures(cfg.isMeasureShape(),
					cfg.isMeasureIntensity(), cfg.isMeasureTexture(), suffixes);

			StringBuilder req = new StringBuilder();
			req.append("{\"op\":\"quantify\",\"quantifier\":\"cellprofiler\",");
			req.append("\"mask\":").append(imageMetaJson(label, maskFile, null)).append(",");
			req.append("\"channels\":").append(chans).append(",");
			req.append("\"params\":{\"modules\":[").append(jsonStrings(kinds)).append("],");
			req.append("\"requested_features\":[").append(jsonStrings(requested)).append("]}}");

			String resp = client.request(req.toString(), timeoutMs);
			if(!AnalysisBackendClient.extractBool(resp, "ok", false))
				throw new QuantificationException("CellProfiler backend error: "
						+ AnalysisBackendClient.extractString(resp, "error"));

			String namesStr = AnalysisBackendClient.extractString(resp, "columns_names");
			if(namesStr == null || namesStr.isEmpty())
				return detectionResult;
			String[] names = namesStr.split("\\|");
			int cellCount = (int) AnalysisBackendClient.extractLong(resp, "cell_count", 0);
			int colsCount = (int) AnalysisBackendClient.extractLong(resp, "cols_count", names.length);
			int[] ids = toIntArray(AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "ids_b64")));
			double[] flat = toDoubleArray(AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "values_b64")));

			Table table = buildTable(detectionResult.getCellTable(), names, ids, flat, cellCount, colsCount);
			return new CellDetectionResult(table, label);
		}
		catch(QuantificationException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new QuantificationException("CellProfiler quantification failed: " + e.getMessage(), e);
		}
		finally
		{
			for(Path p : temps)
			{
				try { Files.deleteIfExists(p); } catch(@SuppressWarnings("unused") IOException ignored) { /* not fatal */ }
			}
		}
	}

	private Table buildTable(Table source, String[] featureNames, int[] ids, double[] flat, int nCells, int nCols)
			throws QuantificationException
	{
		// Carry the segmenter's core geometry through (so Label/Track visualizers
		// find cell.center.x/y, area, perimeter), then append the CellProfiler
		// features. Rows are matched to CellProfiler results by cell.index.
		String idName = CellDetectionTableColumns.TABLE_COLUMN_CELL_ID.getColumnName();
		String xName = CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX.getColumnName();
		String yName = CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX.getColumnName();
		String areaName = CellDetectionTableColumns.TABLE_COLUMN_AREA_PX.getColumnName();
		String perimName = "cell.perimeter";

		ColumnDefinition<?>[] cols = new ColumnDefinition<?>[5 + featureNames.length];
		cols[0] = CellDetectionTableColumns.TABLE_COLUMN_CELL_ID;
		cols[1] = CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX;
		cols[2] = CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX;
		cols[3] = CellDetectionTableColumns.TABLE_COLUMN_AREA_PX;
		cols[4] = ColumnDefinition.createDoubleColumnDefinition(perimName, "Cell perimeter (px).", true);
		for(int c = 0; c < featureNames.length; c++)
			cols[c + 5] = ColumnDefinition.createDoubleColumnDefinition(featureNames[c], featureNames[c], true);
		TableDefinition def = new TableDefinition(TABLE_ID, "Segmented + CellProfiler-quantified cells.", cols);

		Table table = new Table(def, source.getCreationRuntime(), source.getPositionInformation(), source.getExecutionInformation());
		try
		{
			// cell.index -> row index in the CellProfiler result
			java.util.Map<Integer, Integer> idToRow = new java.util.HashMap<Integer, Integer>();
			for(int r = 0; r < nCells; r++)
				idToRow.put(Integer.valueOf(ids[r]), Integer.valueOf(r));

			org.youscope.common.table.ColumnView idV = source.getColumnView(idName);
			org.youscope.common.table.ColumnView xV = tryColumn(source, xName);
			org.youscope.common.table.ColumnView yV = tryColumn(source, yName);
			org.youscope.common.table.ColumnView aV = tryColumn(source, areaName);
			org.youscope.common.table.ColumnView pV = tryColumn(source, perimName);

			int nRows = source.getNumRows();
			for(int r = 0; r < nRows; r++)
			{
				Serializable[] row = new Serializable[5 + featureNames.length];
				Serializable idv = idV.getValue(r);
				int cellId = (idv instanceof Number) ? ((Number) idv).intValue() : (r + 1);
				row[0] = Integer.valueOf(cellId);
				row[1] = asDouble(xV, r);
				row[2] = asDouble(yV, r);
				row[3] = asDouble(aV, r);
				row[4] = asDouble(pV, r);
				Integer cproRow = idToRow.get(Integer.valueOf(cellId));
				for(int c = 0; c < featureNames.length; c++)
				{
					if(cproRow == null || c >= nCols)
					{
						row[c + 5] = null;
						continue;
					}
					double v = flat[cproRow.intValue() * nCols + c];
					row[c + 5] = Double.isNaN(v) ? null : Double.valueOf(v);
				}
				table.addRow(row);
			}
		}
		catch(TableException e)
		{
			throw new QuantificationException("Could not assemble CellProfiler table: " + e.getMessage(), e);
		}
		return table;
	}

	private static org.youscope.common.table.ColumnView tryColumn(Table source, String name)
	{
		try
		{
			return source.getColumnView(name);
		}
		catch(@SuppressWarnings("unused") Exception e)
		{
			return null;   // segmenter didn't provide this core column
		}
	}

	private static Double asDouble(org.youscope.common.table.ColumnView view, int row)
	{
		if(view == null)
			return null;
		try
		{
			Serializable v = view.getValue(row);
			return (v instanceof Number) ? Double.valueOf(((Number) v).doubleValue()) : null;
		}
		catch(@SuppressWarnings("unused") Exception e)
		{
			return null;
		}
	}

	// Image serialisation plus helpers (@ToDo: migrate to shared new PythonBackendSupport)

	private static String jsonStrings(List<String> items)
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < items.size(); i++)
		{
			if(i > 0)
				sb.append(",");
			sb.append("\"").append(AnalysisBackendClient.escapeJson(items.get(i))).append("\"");
		}
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

	private static double[] toDoubleArray(byte[] b)
	{
		ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
		double[] out = new double[b.length / 8];
		for(int i = 0; i < out.length; i++)
			out[i] = bb.getDouble();
		return out;
	}

	private Path writeImage(ImageEvent<?> image, List<Path> temps) throws IOException
	{
		byte[] le = serializeImageLE(image);
		Path p = Files.createTempFile("ys_cp_", ".raw");
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

	private static String imageMetaJson(ImageEvent<?> image, Path file, String suffix)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{\"path\":\"").append(AnalysisBackendClient.escapeJson(file.toString())).append("\",");
		sb.append("\"width\":").append(image.getWidth()).append(",");
		sb.append("\"height\":").append(image.getHeight()).append(",");
		sb.append("\"bpp\":").append(image.getBytesPerPixel()).append(",");
		sb.append("\"bitdepth\":").append(image.getBitDepth()).append(",");
		sb.append("\"tx\":").append(image.isTransposeX()).append(",");
		sb.append("\"ty\":").append(image.isTransposeY()).append(",");
		sb.append("\"sw\":").append(image.isSwitchXY());
		if(suffix != null)
			sb.append(",\"suffix\":\"").append(AnalysisBackendClient.escapeJson(suffix)).append("\"");
		sb.append("}");
		return sb.toString();
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
			InputStream is = CellProfilerQuantifierAddon.class.getResourceAsStream(resourceName);
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
