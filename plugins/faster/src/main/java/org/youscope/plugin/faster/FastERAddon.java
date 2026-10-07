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
package org.youscope.plugin.faster;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.youscope.addon.analysis.AnalysisBackendClient;
import org.youscope.addon.celldetection.CellDetectionAddon;
import org.youscope.addon.celldetection.CellDetectionException;
import org.youscope.addon.celldetection.CellDetectionResult;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.resource.ResourceException;
import org.youscope.common.table.Table;
import org.youscope.common.table.TableDefinition;
import org.youscope.common.table.TableException;
import org.youscope.common.table.TableListener;

/**
 * FastER segmenter addon. Serialises the detection image to the shared
 * cell-detection backend, which runs the headless FastER binary and returns a
 * 16-bit label image plus the core geometry (single source of truth, shared with
 * the CellX/StarDist/Cellpose geometry). The addon builds the label
 * {@link ImageEvent} and core {@link Table} and hands them to the quantifier/tracker.
 */
class FastERAddon extends ResourceAdapter<FastERConfiguration> implements CellDetectionAddon
{
	/** Required backend version (the 'faster' op was added in v0.7.1). */
	private static final String BACKEND_VERSION = "v0.7.4";

	private AnalysisBackendClient client = null;
	private int timeoutMs = 300000;
	private String workDir = null;
	private final ArrayList<TableListener> tableListeners = new ArrayList<TableListener>();

	FastERAddon(PositionInformation positionInformation, FastERConfiguration configuration)
			throws ConfigurationException, RemoteException
	{
		super(positionInformation, configuration, FastERConfiguration.TYPE_IDENTIFIER,
				FastERConfiguration.class, "FastER cell detection.");
	}

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException
	{
		if(isInitialized())
			return;
		super.initialize(measurementContext);
		String ys = System.getProperty("user.dir", ".");

		// Reuse the shared cell-detection backend (same server the quantifiers use)
		// so FastER's geometry is the single source of truth. Honour the same
		// celldetection_* config keys.
		String cpython = readConfig(ys, "cpython_executable");
		if(cpython == null || !new File(cpython).exists())
			cpython = "python";
		timeoutMs = readConfigInt(ys, "celldetection_timeout_seconds", 300) * 1000;
		int port = readConfigInt(ys, "celldetection_backend_port", 59713);
		int idle = readConfigInt(ys, "celldetection_idle_timeout_seconds", 1800);
		String host = readConfig(ys, "celldetection_backend_host");
		if(host == null)
			host = "127.0.0.1";
		String scriptsDir = resolveWritableDir(readConfig(ys, "celldetection_work_dir"), ys + File.separator + "scripts");
		workDir = scriptsDir;
		String serverScript = scriptsDir + File.separator + "quickdetect_server.py";
		String logFile = scriptsDir + File.separator + "quickdetect_server.log";

		try
		{
			extractScript("/org/youscope/plugin/quickdetect/quickdetect_server.py", serverScript);
			extractScript("/org/youscope/addon/celldetection/utils/cell_detection_cpython.py",
					scriptsDir + File.separator + "cell_detection_cpython.py");
		}
		catch(ResourceException e)
		{
			sendMessage("FastER: could not extract backend scripts (" + e.getMessage() + ").");
		}

		client = new AnalysisBackendClient(host, port, cpython, serverScript, logFile, idle);
		try
		{
			client.ensureUp(BACKEND_VERSION, readConfigInt(ys, "celldetection_server_start_seconds", 60) * 1000);
		}
		catch(IOException e)
		{
			sendMessage("FastER: backend not reachable (" + e.getMessage() + ").");
		}
	}

	@Override
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException
	{
		client = null;
		super.uninitialize(measurementContext);
	}

	@Override
	public TableDefinition getProducedTableDefinition() throws RemoteException
	{
		return FastERTable.getTableDefinition();
	}

	@Override
	public void addTableListener(TableListener listener) throws RemoteException
	{
		synchronized(tableListeners)
		{
			tableListeners.add(listener);
		}
	}

	@Override
	public void removeTableListener(TableListener listener) throws RemoteException
	{
		synchronized(tableListeners)
		{
			tableListeners.remove(listener);
		}
	}

	private void sendTableToListeners(Table table)
	{
		if(table == null)
			return;
		synchronized(tableListeners)
		{
			for(Iterator<TableListener> it = tableListeners.iterator(); it.hasNext();)
			{
				try
				{
					it.next().newTableProduced(table.clone());
				}
				catch(RemoteException e)
				{
					it.remove();
				}
			}
		}
	}

	@Override
	public CellDetectionResult detectCells(ImageEvent<?> detectionImage)
			throws CellDetectionException, RemoteException
	{
		return detectCells(detectionImage, new ImageEvent[0]);
	}

	@Override
	public CellDetectionResult detectCells(ImageEvent<?> detectionImage,
			ImageEvent<?>[] quantificationImages) throws CellDetectionException, RemoteException
	{
		if(!isInitialized() || client == null)
			throw new CellDetectionException("FastER segmenter not initialized.");
		if(detectionImage == null)
			throw new CellDetectionException("FastER: no detection image.");

		FastERConfiguration cfg = getConfiguration();
		List<Path> temps = new ArrayList<Path>();
		try
		{
			Path imgFile = writeImage(detectionImage, temps);
			StringBuilder req = new StringBuilder();
			req.append("{\"op\":\"faster\",");
			req.append("\"image\":").append(imageMetaJson(detectionImage, imgFile)).append(",");
			req.append("\"params\":{");
			req.append("\"binary\":\"").append(AnalysisBackendClient.escapeJson(cfg.getFasterBinary())).append("\",");
			req.append("\"model\":\"").append(AnalysisBackendClient.escapeJson(cfg.getFasterModel())).append("\"");
			if(cfg.getFasterSvm() != null && !cfg.getFasterSvm().trim().isEmpty())
				req.append(",\"svm\":\"").append(AnalysisBackendClient.escapeJson(cfg.getFasterSvm().trim())).append("\"");
			String wd = (cfg.getWorkDir() != null && !cfg.getWorkDir().trim().isEmpty())
					? cfg.getWorkDir().trim() : workDir;
			if(wd != null)
				req.append(",\"work_dir\":\"").append(AnalysisBackendClient.escapeJson(wd)).append("\"");
			req.append("}}");

			String resp = client.request(req.toString(), timeoutMs);
			if(!AnalysisBackendClient.extractBool(resp, "ok", false))
				throw new CellDetectionException("FastER backend error: " + AnalysisBackendClient.extractString(resp, "error"));

			int[] ids = toIntArray(AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "ids_b64")));
			double[] vals = toDoubleArray(AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "values_b64")));
			Table table = buildTable(ids, vals, detectionImage);
			sendTableToListeners(table);

			ImageEvent<?> labelImage = null;
			if(cfg.isGenerateLabelImage() && ids.length > 0)
			{
				byte[] mask = AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "mask_b64"));
				labelImage = buildLabelImage(mask, detectionImage);
			}
			return new CellDetectionResult(table, labelImage);
		}
		catch(CellDetectionException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new CellDetectionException("FastER segmentation failed: " + e.getMessage(), e);
		}
		finally
		{
			for(Path p : temps)
			{
				try { Files.deleteIfExists(p); } catch(@SuppressWarnings("unused") IOException ignored) { /* best effort */ }
			}
		}
	}

	// Table + label assembly

	private Table buildTable(int[] ids, double[] vals, ImageEvent<?> src) throws TableException
	{
		// vals is [n x 4]: center.x, center.y, area, perimeter (column order matches
		// the backend core_geometry op and FastERTable).
		Table table = new Table(FastERTable.getTableDefinition(), src.getCreationTime(),
				src.getPositionInformation(), src.getExecutionInformation());
		for(int i = 0; i < ids.length; i++)
		{
			double cx = vals[i * 4];
			double cy = vals[i * 4 + 1];
			double area = vals[i * 4 + 2];
			double perim = vals[i * 4 + 3];
			table.addRow(
					Integer.valueOf(ids[i]),
					Double.valueOf(cx),
					Double.valueOf(cy),
					Double.valueOf(area),
					perim > 0 ? Double.valueOf(perim) : null);
		}
		return table;
	}

	/**
	 * Builds the label {@link ImageEvent} from the Cellpose-style sidecar the
	 * backend returns: [w:int32 BE][h:int32 BE][w*h * uint16 BE].
	 */
	private ImageEvent<?> buildLabelImage(byte[] file, ImageEvent<?> srcImage)
	{
		try
		{
			if(file == null || file.length < 8)
				return null;
			int fw = ((file[0] & 0xFF) << 24) | ((file[1] & 0xFF) << 16) | ((file[2] & 0xFF) << 8) | (file[3] & 0xFF);
			int fh = ((file[4] & 0xFF) << 24) | ((file[5] & 0xFF) << 16) | ((file[6] & 0xFF) << 8) | (file[7] & 0xFF);
			long expected = (long) fw * fh * 2;
			if(fw <= 0 || fh <= 0 || file.length < 8L + expected)
				return null;
			int n = fw * fh;

			int maxLabel = 0;
			for(int i = 0, o = 8; i < n; i++, o += 2)
			{
				int v = ((file[o] & 0xFF) << 8) | (file[o + 1] & 0xFF);
				if(v > maxLabel)
					maxLabel = v;
			}

			ImageEvent<?> label;
			if(maxLabel <= 255)
			{
				byte[] px = new byte[n];
				for(int i = 0; i < n; i++)
					px[i] = file[8 + 2 * i + 1]; // low byte of big-endian uint16
				label = ImageEvent.createImage(px, fw, fh, 8);
			}
			else
			{
				short[] labels = new short[n];
				for(int i = 0, o = 8; i < n; i++, o += 2)
					labels[i] = (short) (((file[o] & 0xFF) << 8) | (file[o + 1] & 0xFF));
				label = ImageEvent.createImage(labels, fw, fh, 16);
			}
			label.setPositionInformation(srcImage.getPositionInformation());
			label.setExecutionInformation(srcImage.getExecutionInformation());
			return label;
		}
		catch(Throwable e)
		{
			sendMessage("FastER: could not build label image - " + e.getMessage());
			return null;
		}
	}

	// Backend image serialisation (@ToDo: migrate to shared new PythonBackendSupport)

	private Path writeImage(ImageEvent<?> image, List<Path> temps) throws IOException
	{
		byte[] le = serializeImageLE(image);
		Path p = Files.createTempFile("ys_faster_", ".raw");
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
			int n = Math.min(s.length, w * h);
			for(int i = 0; i < n; i++)
				buf.putShort(s[i]);
		}
		else if(raw instanceof int[])
		{
			int[] s = (int[]) raw;
			int n = Math.min(s.length, w * h);
			for(int i = 0; i < n; i++)
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

	private static double[] toDoubleArray(byte[] b)
	{
		ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
		double[] out = new double[b.length / 8];
		for(int i = 0; i < out.length; i++)
			out[i] = bb.getDouble();
		return out;
	}

	// Config and script helpers (same conventions as the cell-detection addons) 

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
			InputStream is = FastERAddon.class.getResourceAsStream(resourceName);
			if(is == null)
				throw new ResourceException(resourceName + " not found in plugin JAR.");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int n;
			try(BufferedInputStream bis = new BufferedInputStream(is))
			{
				while((n = bis.read(buf)) != -1)
					baos.write(buf, 0, n);
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
