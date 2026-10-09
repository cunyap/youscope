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
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - New Python API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.glowvisualizer;

import java.awt.image.BufferedImage;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import org.youscope.common.table.TableException;
import org.youscope.common.util.ImageConvertException;
import org.youscope.common.util.ImageTools;

/**
 * Visualizes detected cells with a soft glow.
 *
 * <p>
 * Replacement for the Matlab-backed implementation. Like QuickDetect,
 * the rendering runs in the YouScope CPython environment and is launched from
 * Java
 * via {@link ProcessBuilder} (not the GraalPy scripting engine). Java reads the
 * detection table, marshals the image + cell positions to temp files, runs
 * {@code glow_bridge.py}, streams its output to the YouScope log, and reads
 * back the RGB result.
 *
 * @author Moritz Lang and Andreas P. Cuny
 */
class GlowVisualizerAddon extends ResourceAdapter<GlowVisualizerConfiguration> implements CellVisualizationAddon {
	private static final long serialVersionUID = 783142044778589073L;

	static final String PLUGIN_VERSION = "v0.5.0";

	private static final String BRIDGE_RESOURCE = "/org/youscope/plugin/glowvisualizer/glow_bridge.py";
	private static final String ALGO_RESOURCE = "/org/youscope/addon/celldetection/utils/cell_detection_cpython.py";

	private String cpythonExe = null;
	private String workingDir = null;
	private String bridgeScript = null;
	private int timeoutMs = 300000;

	GlowVisualizerAddon(PositionInformation positionInformation, GlowVisualizerConfiguration configuration)
			throws ConfigurationException, RemoteException {
		super(positionInformation, configuration, GlowVisualizerConfiguration.CONFIGURATION_ID,
				GlowVisualizerConfiguration.class, "Glow Visualizer");
	}

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		super.initialize(measurementContext);
		if (cpythonExe != null)
			return;

		String youScopeDir = System.getProperty("user.dir", ".");
		cpythonExe = readConfig(youScopeDir, "cpython_executable");
		if (cpythonExe == null || !new File(cpythonExe).exists()) {
			cpythonExe = "python";
			sendMessage("GlowVisualizer: cpython_executable not found in graalpy_config.txt; using system 'python'.");
		}
		timeoutMs = readConfigInt(youScopeDir, "celldetection_timeout_seconds", 300) * 1000;
		String workDirCfg = readConfig(youScopeDir, "celldetection_work_dir");
		workingDir = resolveWritableDir(workDirCfg, youScopeDir + File.separator + "scripts");

		bridgeScript = workingDir + File.separator + "glow_bridge.py";
		extractScript(BRIDGE_RESOURCE, bridgeScript);
		extractScript(ALGO_RESOURCE, workingDir + File.separator + "cell_detection_cpython.py");

		sendMessage("GlowVisualizer " + PLUGIN_VERSION + " initialized. cpython=" + cpythonExe
				+ ", workdir=" + workingDir + ".");
	}

	@Override
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		cpythonExe = null;
		workingDir = null;
		bridgeScript = null;
		super.uninitialize(measurementContext);
	}

	@Override
	public boolean isInitialized() {
		return cpythonExe != null;
	}

	@Override
	public ImageEvent<?> visualizeCells(ImageEvent<?> image, CellDetectionResult detectionResult)
			throws CellVisualizationException {
		if (!isInitialized())
			throw new CellVisualizationException("Addon not yet initialized.");
		if (image == null)
			throw new CellVisualizationException("Image in which cells should be visualized is null.");
		if (detectionResult == null)
			throw new CellVisualizationException("Cell detection result is null.");

		double[] positionsFlat = extractCellPositions(detectionResult);
		int nCells = positionsFlat.length / 3;

		List<Path> temps = new ArrayList<Path>();
		try {
			Path imgFile = Files.createTempFile("ys_glow_img_", ".raw");
			temps.add(imgFile);
			Files.write(imgFile, serializeImageLE(image));

			Path posFile = Files.createTempFile("ys_glow_pos_", ".raw");
			temps.add(posFile);
			Files.write(posFile, serializePositionsLE(positionsFlat));

			Path rgbFile = Files.createTempFile("ys_glow_rgb_", ".raw");
			temps.add(rgbFile);
			Path resultFile = Files.createTempFile("ys_glow_res_", ".txt");
			temps.add(resultFile);

			StringBuilder args = new StringBuilder();
			args.append("{");
			args.append("\"result_file\":\"").append(escJ(resultFile.toString())).append("\",");
			args.append("\"glow_strength\":").append(getConfiguration().getGlowStrength()).append(",");
			args.append("\"cell_count\":").append(nCells).append(",");
			args.append("\"positions_path\":\"").append(escJ(posFile.toString())).append("\",");
			args.append("\"rgb_path\":\"").append(escJ(rgbFile.toString())).append("\",");
			args.append("\"image\":").append(imageMetaJson(image, imgFile));
			args.append("}");

			Path argsFile = Files.createTempFile("ys_glow_args_", ".json");
			temps.add(argsFile);
			Files.write(argsFile, args.toString().getBytes("UTF-8"));

			runBridge(argsFile);

			ResultData result = parseResult(resultFile);
			if (!result.ok)
				throw new CellVisualizationException("Glow bridge error: " + result.error);

			byte[] rgb = Files.readAllBytes(rgbFile);
			return buildRgbImage(rgb, result.rgbWidth, result.rgbHeight);
		} catch (CellVisualizationException e) {
			throw e;
		} catch (Exception e) {
			throw new CellVisualizationException("GlowVisualizer failed: " + e.getMessage(), e);
		} finally {
			for (Path p : temps) {
				try {
					Files.deleteIfExists(p);
				} catch (@SuppressWarnings("unused") IOException ignored) {
					// not fatal
				}
			}
		}
	}

	/**
	 * [x, y, area] triples for the first appearance of each cell (matches the
	 * original MATLAB behaviour: skip rows whose quantification-image id is set).
	 */
	private static double[] extractCellPositions(CellDetectionResult detectionResult)
			throws CellVisualizationException {
		Table table;
		try {
			table = detectionResult.getCellTable();
		} catch (Exception e) {
			throw new CellVisualizationException("Could not read detection table: " + e.getMessage(), e);
		}
		if (table == null || table.getNumRows() <= 0)
			return new double[0];

		ColumnView<Double> xposColumn;
		ColumnView<Double> yposColumn;
		ColumnView<Double> areaColumn;
		try {
			xposColumn = table.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX);
			yposColumn = table.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX);
			areaColumn = table.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_AREA_PX);
		} catch (TableException ex) {
			throw new CellVisualizationException("Detection table missing position/area columns.", ex);
		}

		ColumnView<Integer> quantColumn;
		try {
			quantColumn = table.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_QUANTIFICATION_IMAGE_ID);
		} catch (@SuppressWarnings("unused") TableException ex) {
			quantColumn = null;
		}

		int numRows = table.getNumRows();
		ArrayList<double[]> positions = new ArrayList<double[]>();
		for (int row = 0; row < numRows; row++) {
			if (quantColumn != null) {
				Integer quant = quantColumn.getValue(row);
				if (quant != null && quant.intValue() != 0)
					continue;
			}
			Double x = xposColumn.getValue(row);
			Double y = yposColumn.getValue(row);
			Double area = areaColumn.getValue(row);
			if (x == null || y == null)
				continue;
			positions.add(new double[] { x.doubleValue(), y.doubleValue(), area == null ? 0.0 : area.doubleValue() });
		}

		double[] flat = new double[positions.size() * 3];
		for (int i = 0; i < positions.size(); i++) {
			double[] p = positions.get(i);
			flat[i * 3] = p[0];
			flat[i * 3 + 1] = p[1];
			flat[i * 3 + 2] = p[2];
		}
		return flat;
	}

	private ImageEvent<?> buildRgbImage(byte[] rgbRowMajor, int width, int height) throws CellVisualizationException {
		try {
			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			int idx = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int r = rgbRowMajor[idx++] & 0xFF;
					int g = rgbRowMajor[idx++] & 0xFF;
					int b = rgbRowMajor[idx++] & 0xFF;
					img.setRGB(x, y, (r << 16) | (g << 8) | b);
				}
			}
			return ImageTools.toYouScopeImage(img);
		} catch (ImageConvertException e) {
			throw new CellVisualizationException("Could not convert glow image: " + e.getMessage(), e);
		}
	}

	// Process

	private void runBridge(Path argsFile) throws CellVisualizationException {
		ProcessBuilder pb = new ProcessBuilder(cpythonExe, "-u", bridgeScript);
		pb.environment().put("YOUSCOPE_ARGS_FILE", argsFile.toString());
		pb.environment().put("PYTHONUNBUFFERED", "1");
		if (workingDir != null)
			pb.directory(new File(workingDir));
		pb.redirectErrorStream(true);

		final Process proc;
		try {
			proc = pb.start();
		} catch (IOException e) {
			throw new CellVisualizationException("Could not start CPython (" + cpythonExe + "): " + e.getMessage(), e);
		}

		final StringBuilder tail = new StringBuilder();
		Thread drain = new Thread(new Runnable() {
			@Override
			public void run() {
				try (BufferedReader r = new BufferedReader(
						new InputStreamReader(proc.getInputStream(), Charset.forName("UTF-8")))) {
					String line;
					while ((line = r.readLine()) != null) {
						synchronized (tail) {
							tail.append(line).append('\n');
							if (tail.length() > 4000)
								tail.delete(0, tail.length() - 4000);
						}
						sendMessage(line);
					}
				} catch (@SuppressWarnings("unused") IOException ignored) {
					// process ended
				}
			}
		}, "glow-bridge-drain");
		drain.setDaemon(true);
		drain.start();

		boolean finished;
		try {
			finished = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			proc.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new CellVisualizationException("GlowVisualizer interrupted while waiting for CPython.");
		}
		if (!finished) {
			proc.destroyForcibly();
			throw new CellVisualizationException("Glow bridge timed out after " + (timeoutMs / 1000)
					+ " s. Last output:\n" + tailString(tail));
		}
		try {
			drain.join(2000);
		} catch (@SuppressWarnings("unused") InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
		int exit = proc.exitValue();
		if (exit != 0)
			throw new CellVisualizationException(
					"Glow bridge exited with code " + exit + ". Last output:\n" + tailString(tail));
	}

	private static String tailString(StringBuilder tail) {
		synchronized (tail) {
			return tail.toString();
		}
	}

	// Image serialisation plus helpers (@ToDo: migrate to shared new
	// PythonBackendSupport)

	private static byte[] serializeImageLE(ImageEvent<?> image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int bpp = image.getBytesPerPixel();
		Object raw = image.getImageData();
		int numPixels = width * height;
		byte[] data = new byte[numPixels * bpp];
		ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		if (raw instanceof byte[])
			System.arraycopy(raw, 0, data, 0, Math.min(((byte[]) raw).length, data.length));
		else if (raw instanceof short[]) {
			short[] src = (short[]) raw;
			int n = Math.min(src.length, numPixels);
			for (int i = 0; i < n; i++)
				buffer.putShort(src[i]);
		} else if (raw instanceof int[]) {
			int[] src = (int[]) raw;
			int n = Math.min(src.length, numPixels);
			for (int i = 0; i < n; i++)
				buffer.putInt(src[i]);
		} else
			throw new IllegalArgumentException(
					"Unsupported image data type: " + (raw == null ? "null" : raw.getClass().getName()));
		return data;
	}

	private static byte[] serializePositionsLE(double[] flat) {
		byte[] data = new byte[flat.length * 8];
		ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		for (double v : flat)
			buffer.putDouble(v);
		return data;
	}

	private static String imageMetaJson(ImageEvent<?> image, Path file) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"path\":\"").append(escJ(file.toString())).append("\",");
		sb.append("\"width\":").append(image.getWidth()).append(",");
		sb.append("\"height\":").append(image.getHeight()).append(",");
		sb.append("\"bpp\":").append(image.getBytesPerPixel()).append(",");
		sb.append("\"bitdepth\":").append(image.getBitDepth()).append(",");
		sb.append("\"tx\":").append(image.isTransposeX()).append(",");
		sb.append("\"ty\":").append(image.isTransposeY()).append(",");
		sb.append("\"sw\":").append(image.isSwitchXY());
		sb.append("}");
		return sb.toString();
	}

	// Result parsing

	private static final class ResultData {
		boolean ok = false;
		String error = "";
		int rgbWidth = 0;
		int rgbHeight = 0;
	}

	private ResultData parseResult(Path resultFile) throws IOException, CellVisualizationException {
		if (!Files.exists(resultFile))
			throw new CellVisualizationException("Glow produced no result file (the bridge may have crashed).");
		ResultData r = new ResultData();
		for (String line : Files.readAllLines(resultFile)) {
			line = line.trim();
			if (line.isEmpty())
				continue;
			int sp = line.indexOf(' ');
			String key = sp < 0 ? line : line.substring(0, sp);
			String rest = sp < 0 ? "" : line.substring(sp + 1);
			if ("ok".equals(key))
				r.ok = "1".equals(rest.trim());
			else if ("error".equals(key))
				r.error = rest;
			else if ("rgb".equals(key)) {
				String[] t = rest.split(" ");
				if (t.length >= 2) {
					r.rgbWidth = (int) parseDoubleSafe(t[0]);
					r.rgbHeight = (int) parseDoubleSafe(t[1]);
				}
			}
		}
		return r;
	}

	// Config and fs helpers

	private static String readConfig(String youScopeDir, String key) {
		File cfg = new File(youScopeDir, "graalpy_config.txt");
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
			// fall through
		}
		return null;
	}

	private static int readConfigInt(String youScopeDir, String key, int def) {
		String v = readConfig(youScopeDir, key);
		if (v == null)
			return def;
		try {
			return Integer.parseInt(v.trim());
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return def;
		}
	}

	private String resolveWritableDir(String preferred, String fallback) {
		String[] candidates = { preferred, fallback,
				System.getProperty("java.io.tmpdir") + File.separator + "youscope-celldetection" };
		for (String c : candidates) {
			if (c == null || c.trim().isEmpty())
				continue;
			File d = new File(c.trim());
			try {
				if (!d.exists())
					d.mkdirs();
				if (d.isDirectory() && d.canWrite())
					return d.getAbsolutePath();
			} catch (@SuppressWarnings("unused") Exception ignored) {
				// try next
			}
		}
		return new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
	}

	private void extractScript(String resourceName, String targetPath) throws ResourceException {
		File f = new File(targetPath);
		try {
			if (f.getParentFile() != null)
				f.getParentFile().mkdirs();
			InputStream is = GlowVisualizerAddon.class.getResourceAsStream(resourceName);
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

	private static double parseDoubleSafe(String s) {
		try {
			return Double.parseDouble(s.trim());
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return 0.0;
		}
	}

	private static String escJ(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
