/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - New Python API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.quickdetect;

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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
 * Quick cell-detection addon (v0.6.0).
 *
 * <p>
 * Speaks the YouScope Analysis-Backend
 * A persistent CPython socket server ({@code quickdetect_server.py}) is started
 * once and reused across the whole measurement. If the server cannot be
 * started/reached, this addon transparently falls back to the proven one-shot
 * bridge ({@code quick_detect_bridge.py}), so it can
 * never regress below a working state. Either way it returns a standard
 * {@link CellDetectionResult}, so Glow and Label visualizers work unchanged.
 *
 * @author Moritz Lang and Andreas P. Cuny
 */
class QuickDetectAddon extends ResourceAdapter<QuickDetectConfiguration> implements CellDetectionAddon {
	private static final long serialVersionUID = -527721856301731020L;

	static final String PLUGIN_VERSION = "v0.6.9";

	private static final String SERVER_RESOURCE = "/org/youscope/plugin/quickdetect/quickdetect_server.py";
	private static final String BRIDGE_RESOURCE = "/org/youscope/plugin/quickdetect/quick_detect_bridge.py";
	private static final String ALGO_RESOURCE = "/org/youscope/addon/celldetection/utils/cell_detection_cpython.py";

	private String cpythonExe = null;
	private String workingDir = null;
	private String bridgeScript = null;
	private int timeoutMs = 300000;

	private AnalysisBackendClient client = null;
	private boolean serverAvailable = false;

	private final ArrayList<TableListener> tableListeners = new ArrayList<TableListener>();

	QuickDetectAddon(PositionInformation positionInformation, QuickDetectConfiguration configuration)
			throws ConfigurationException, RemoteException {
		super(positionInformation, configuration, QuickDetectConfiguration.TYPE_IDENTIFIER,
				QuickDetectConfiguration.class, "Quick-Detect cell detection.");
	}

	// Lifecycle

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		if (isInitialized())
			return;
		super.initialize(measurementContext);

		String youScopeDir = System.getProperty("user.dir", ".");
		cpythonExe = readConfig(youScopeDir, "cpython_executable");
		if (cpythonExe == null || !new File(cpythonExe).exists()) {
			cpythonExe = "python";
			sendMessage("QuickDetect: cpython_executable not found in graalpy_config.txt; using system 'python'.");
		}
		timeoutMs = readConfigInt(youScopeDir, "celldetection_timeout_seconds", 300) * 1000;
		String workDirCfg = readConfig(youScopeDir, "celldetection_work_dir");
		workingDir = resolveWritableDir(workDirCfg, youScopeDir + File.separator + "scripts");

		// Always re-extract, so a stale on-disk copy can never shadow a rebuilt JAR.
		String serverScript = workingDir + File.separator + "quickdetect_server.py";
		bridgeScript = workingDir + File.separator + "quick_detect_bridge.py";
		extractScript(SERVER_RESOURCE, serverScript);
		extractScript(BRIDGE_RESOURCE, bridgeScript);
		// cell_detection_cpython.py now also carries the quantifier
		// (geometry/quantify),
		// so no separate quantify module needs to be packaged or extracted.
		extractScript(ALGO_RESOURCE, workingDir + File.separator + "cell_detection_cpython.py");

		int port = readConfigInt(youScopeDir, "celldetection_backend_port", 59713);
		int startTimeout = readConfigInt(youScopeDir, "celldetection_server_start_seconds", 60) * 1000;
		int idleTimeout = readConfigInt(youScopeDir, "celldetection_idle_timeout_seconds", 1800);
		String host = readConfig(youScopeDir, "celldetection_backend_host");
		if (host == null)
			host = "127.0.0.1";
		String logFile = workingDir + File.separator + "quickdetect_server.log";

		client = new AnalysisBackendClient(host, port, cpythonExe, serverScript, logFile, idleTimeout);
		try {
			String v = client.ensureUp(PLUGIN_VERSION, startTimeout);
			serverAvailable = true;
			sendMessage("QuickDetect " + PLUGIN_VERSION + " initialized (persistent backend " + v
					+ " at " + host + ":" + port + ").");
		} catch (IOException e) {
			serverAvailable = false;
			sendMessage("QuickDetect " + PLUGIN_VERSION + ": persistent backend unavailable (" + e.getMessage()
					+ "); using one-shot mode.");
		}
	}

	@Override
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		// Leave the server running for reuse by later measurements; the JVM
		// shutdown hook stops it when YouScope exits (and its idle timer is a
		// backstop).
		client = null;
		serverAvailable = false;
		cpythonExe = null;
		workingDir = null;
		bridgeScript = null;
		super.uninitialize(measurementContext);
	}

	// CellDetectionAddon

	@Override
	public CellDetectionResult detectCells(ImageEvent<?> detectionImage)
			throws CellDetectionException, RemoteException {
		return detectCells(detectionImage, new ImageEvent[0]);
	}

	@Override
	public CellDetectionResult detectCells(ImageEvent<?> detectionImage, ImageEvent<?>[] quantificationImages)
			throws CellDetectionException, RemoteException {
		if (!isInitialized())
			throw new CellDetectionException("Addon not yet initialized.");
		if (detectionImage == null)
			throw new CellDetectionException("Image in which cells should be detected is null.");
		if (detectionImage.getExecutionInformation() == null || detectionImage.getPositionInformation() == null)
			throw new CellDetectionException("Metadata of the detection image is not initialized.");
		if (quantificationImages == null)
			quantificationImages = new ImageEvent[0];

		QuickDetectConfiguration cfg = getConfiguration();
		List<Path> temps = new ArrayList<Path>();
		try {
			Path detFile = writeImage(detectionImage, temps);
			Path[] quantFiles = new Path[quantificationImages.length];
			for (int i = 0; i < quantificationImages.length; i++)
				quantFiles[i] = writeImage(quantificationImages[i], temps);

			if (serverAvailable) {
				try {
					return detectViaServer(cfg, detectionImage, detFile, quantificationImages, quantFiles);
				} catch (Exception serverErr) {
					sendMessage("QuickDetect: backend call failed (" + serverErr.getMessage()
							+ "); retrying one-shot for this frame.");
				}
			}
			return detectViaOneShot(cfg, detectionImage, detFile, quantificationImages, quantFiles, temps);
		} catch (CellDetectionException e) {
			throw e;
		} catch (Exception e) {
			throw new CellDetectionException("QuickDetect failed: " + e.getMessage(), e);
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

	// Server path

	private CellDetectionResult detectViaServer(QuickDetectConfiguration cfg, ImageEvent<?> detectionImage,
			Path detFile, ImageEvent<?>[] quantImages, Path[] quantFiles) throws Exception {
		StringBuilder quant = new StringBuilder("[");
		for (int i = 0; i < quantImages.length; i++) {
			if (i > 0)
				quant.append(",");
			quant.append(imageMetaJson(quantImages[i], quantFiles[i]));
		}
		quant.append("]");

		boolean generateLabel = cfg.isGenerateLabelImage();
		StringBuilder req = new StringBuilder();
		req.append("{\"op\":\"detect\",\"algorithm\":\"quickdetect\",");
		req.append("\"detection\":").append(imageMetaJson(detectionImage, detFile)).append(",");
		req.append("\"quant\":").append(quant).append(",");
		req.append("\"params\":{");
		req.append("\"threshold\":").append(cfg.getDetectionThreshold()).append(",");
		req.append("\"internal_binning\":").append(cfg.getInternalBinning()).append(",");
		req.append("\"max_cell_diameter\":").append(cfg.getMaxCellDiameter()).append(",");
		req.append("\"min_cell_diameter\":").append(cfg.getMinCellDiameter()).append(",");
		req.append("\"generate_label\":").append(generateLabel);
		req.append("}}");

		String resp = client.request(req.toString(), timeoutMs);
		if (!AnalysisBackendClient.extractBool(resp, "ok", false))
			throw new CellDetectionException("backend error: " + AnalysisBackendClient.extractString(resp, "error"));

		int nCells = (int) AnalysisBackendClient.extractLong(resp, "n_cells", 0);
		long timing = AnalysisBackendClient.extractLong(resp, "timing_ms", 0);
		String version = AnalysisBackendClient.extractString(resp, "version");
		sendMessage("QuickDetect: " + nCells + " cells (backend " + version + ", " + timing + " ms compute).");

		Table table = new Table(QuickDetectTable.getTableDefinition(), detectionImage.getCreationTime(),
				detectionImage.getPositionInformation(), detectionImage.getExecutionInformation());
		String rowsB64 = AnalysisBackendClient.extractString(resp, "rows_b64");
		if (rowsB64 != null && !rowsB64.isEmpty()) {
			byte[] rows = AnalysisBackendClient.inflateBase64(rowsB64);
			ByteBuffer bb = ByteBuffer.wrap(rows).order(ByteOrder.LITTLE_ENDIAN);
			try {
				while (bb.remaining() >= 40) {
					int cellId = bb.getInt();
					int quantId = bb.getInt();
					double x = bb.getDouble();
					double y = bb.getDouble();
					double area = bb.getDouble();
					double fluo = bb.getDouble();
					table.addRow(new Serializable[] { Integer.valueOf(cellId),
							quantId < 0 ? null : Integer.valueOf(quantId),
							Double.valueOf(x), Double.valueOf(y), Double.valueOf(area),
							Double.isNaN(fluo) ? null : Double.valueOf(fluo) });
				}
			} catch (TableException e) {
				throw new CellDetectionException("Could not build cell table: " + e.getMessage(), e);
			}
		}
		sendTableToListeners(table);

		ImageEvent<?> labelImage = null;
		if (AnalysisBackendClient.extractBool(resp, "has_label", false)) {
			int w = (int) AnalysisBackendClient.extractLong(resp, "label_width", 0);
			int h = (int) AnalysisBackendClient.extractLong(resp, "label_height", 0);
			String labelB64 = AnalysisBackendClient.extractString(resp, "label_b64");
			if (labelB64 != null && w > 0 && h > 0)
				labelImage = ImageEvent.createImage(AnalysisBackendClient.inflateBase64(labelB64), w, h, 8);
		}
		return new CellDetectionResult(table, labelImage);
	}

	// One-shot fallback (v0.5.0 bridge)

	private CellDetectionResult detectViaOneShot(QuickDetectConfiguration cfg, ImageEvent<?> detectionImage,
			Path detFile, ImageEvent<?>[] quantImages, Path[] quantFiles, List<Path> temps) throws Exception {
		StringBuilder quant = new StringBuilder("[");
		for (int i = 0; i < quantImages.length; i++) {
			if (i > 0)
				quant.append(",");
			quant.append(imageMetaJson(quantImages[i], quantFiles[i]));
		}
		quant.append("]");

		boolean generateLabel = cfg.isGenerateLabelImage();
		Path labelFile = Files.createTempFile("ys_qd_label_", ".raw");
		temps.add(labelFile);
		Path resultFile = Files.createTempFile("ys_qd_res_", ".txt");
		temps.add(resultFile);

		StringBuilder args = new StringBuilder();
		args.append("{\"result_file\":\"").append(AnalysisBackendClient.escapeJson(resultFile.toString()))
				.append("\",");
		args.append("\"threshold\":").append(cfg.getDetectionThreshold()).append(",");
		args.append("\"internal_binning\":").append(cfg.getInternalBinning()).append(",");
		args.append("\"max_cell_diameter\":").append(cfg.getMaxCellDiameter()).append(",");
		args.append("\"min_cell_diameter\":").append(cfg.getMinCellDiameter()).append(",");
		args.append("\"generate_label\":").append(generateLabel).append(",");
		args.append("\"label_path\":\"").append(AnalysisBackendClient.escapeJson(labelFile.toString())).append("\",");
		args.append("\"detection\":").append(imageMetaJson(detectionImage, detFile)).append(",");
		args.append("\"quant\":").append(quant).append("}");

		Path argsFile = Files.createTempFile("ys_qd_args_", ".json");
		temps.add(argsFile);
		Files.write(argsFile, args.toString().getBytes("UTF-8"));

		runBridge(argsFile);

		OneShotResult result = parseOneShotResult(resultFile);
		if (!result.ok)
			throw new CellDetectionException("QuickDetect bridge error: " + result.error);
		sendMessage("QuickDetect: " + result.nCells + " cells (one-shot " + result.version + ").");

		Table table = new Table(QuickDetectTable.getTableDefinition(), detectionImage.getCreationTime(),
				detectionImage.getPositionInformation(), detectionImage.getExecutionInformation());
		try {
			for (Serializable[] row : result.rows)
				table.addRow(row);
		} catch (TableException e) {
			throw new CellDetectionException("Could not build cell table: " + e.getMessage(), e);
		}
		sendTableToListeners(table);

		ImageEvent<?> labelImage = null;
		if (generateLabel && result.labelWidth > 0 && Files.exists(labelFile))
			labelImage = ImageEvent.createImage(Files.readAllBytes(labelFile), result.labelWidth, result.labelHeight,
					8);
		return new CellDetectionResult(table, labelImage);
	}

	private void runBridge(Path argsFile) throws CellDetectionException {
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
			throw new CellDetectionException("Could not start CPython (" + cpythonExe + "): " + e.getMessage(), e);
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
					// ended
				}
			}
		}, "quickdetect-bridge-drain");
		drain.setDaemon(true);
		drain.start();
		boolean finished;
		try {
			finished = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			proc.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new CellDetectionException("QuickDetect interrupted.");
		}
		if (!finished) {
			proc.destroyForcibly();
			throw new CellDetectionException("QuickDetect one-shot timed out after " + (timeoutMs / 1000) + " s.");
		}
		try {
			drain.join(2000);
		} catch (@SuppressWarnings("unused") InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
		if (proc.exitValue() != 0)
			throw new CellDetectionException("QuickDetect one-shot exited with code " + proc.exitValue() + ".");
	}

	private static final class OneShotResult {
		boolean ok = false;
		String error = "";
		String version = "?";
		int nCells = 0;
		int labelWidth = 0;
		int labelHeight = 0;
		final List<Serializable[]> rows = new ArrayList<Serializable[]>();
	}

	private OneShotResult parseOneShotResult(Path resultFile) throws IOException, CellDetectionException {
		if (!Files.exists(resultFile))
			throw new CellDetectionException("QuickDetect produced no result file.");
		OneShotResult r = new OneShotResult();
		for (String line : Files.readAllLines(resultFile)) {
			line = line.trim();
			if (line.isEmpty())
				continue;
			int sp = line.indexOf(' ');
			String key = sp < 0 ? line : line.substring(0, sp);
			String rest = sp < 0 ? "" : line.substring(sp + 1);
			if ("version".equals(key))
				r.version = rest;
			else if ("ok".equals(key))
				r.ok = "1".equals(rest.trim());
			else if ("error".equals(key))
				r.error = rest;
			else if ("ncells".equals(key))
				r.nCells = (int) parseDoubleSafe(rest);
			else if ("label".equals(key)) {
				String[] t = rest.split(" ", 3);
				if (t.length >= 2) {
					r.labelWidth = (int) parseDoubleSafe(t[0]);
					r.labelHeight = (int) parseDoubleSafe(t[1]);
				}
			} else if ("cell".equals(key)) {
				String[] t = rest.split(" ");
				if (t.length >= 6) {
					r.rows.add(new Serializable[] {
							Integer.valueOf((int) parseDoubleSafe(t[0])),
							"-".equals(t[1]) ? null : Integer.valueOf((int) parseDoubleSafe(t[1])),
							Double.valueOf(parseDoubleSafe(t[2])), Double.valueOf(parseDoubleSafe(t[3])),
							Double.valueOf(parseDoubleSafe(t[4])),
							"-".equals(t[5]) ? null : Double.valueOf(parseDoubleSafe(t[5])) });
				}
			}
		}
		return r;
	}

	// Image serialisation plus helpers (@ToDo: migrate to shared new
	// PythonBackendSupport)

	private Path writeImage(ImageEvent<?> image, List<Path> temps) throws IOException {
		byte[] le = serializeImageLE(image);
		Path p = Files.createTempFile("ys_qd_img_", ".raw");
		temps.add(p);
		Files.write(p, le);
		return p;
	}

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

	// Table listeners

	private void sendTableToListeners(Table table) {
		synchronized (tableListeners) {
			for (Iterator<TableListener> it = tableListeners.iterator(); it.hasNext();) {
				TableListener listener = it.next();
				try {
					listener.newTableProduced(table.clone());
				} catch (@SuppressWarnings("unused") RemoteException e) {
					it.remove();
				}
			}
		}
	}

	@Override
	public void addTableListener(TableListener listener) throws RemoteException {
		synchronized (tableListeners) {
			tableListeners.add(listener);
		}
	}

	@Override
	public void removeTableListener(TableListener listener) throws RemoteException {
		synchronized (tableListeners) {
			tableListeners.remove(listener);
		}
	}

	@Override
	public TableDefinition getProducedTableDefinition() throws RemoteException {
		return QuickDetectTable.getTableDefinition();
	}

	// Config and fs

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
				// next
			}
		}
		return new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
	}

	private void extractScript(String resourceName, String targetPath) throws ResourceException {
		File f = new File(targetPath);
		try {
			if (f.getParentFile() != null)
				f.getParentFile().mkdirs();
			InputStream is = QuickDetectAddon.class.getResourceAsStream(resourceName);
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
}
