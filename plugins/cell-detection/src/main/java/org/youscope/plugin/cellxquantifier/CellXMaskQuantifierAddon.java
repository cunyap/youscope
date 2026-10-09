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
package org.youscope.plugin.cellxquantifier;

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
 * CellXMaskInterface quantifier: per-channel mean and integrated intensity per
 * cell,
 * computed on the segmentation mask by the shared CPython backend. Produces the
 * per-cell table (standard geometry columns + declared feature columns) so that
 * any segmenter's mask can be quantified uniformly.
 */
class CellXMaskQuantifierAddon extends ResourceAdapter<CellXMaskQuantifierConfiguration>
		implements QuantificationAddon {
	private static final long serialVersionUID = 88213244551201233L;
	static final String PLUGIN_VERSION = "v0.7.4";
	private static final String TABLE_ID = "YouScope.CellDetection.QuantifiedCellX";

	private AnalysisBackendClient client = null;
	private int timeoutMs = 300000;

	CellXMaskQuantifierAddon(PositionInformation positionInformation, CellXMaskQuantifierConfiguration configuration)
			throws ConfigurationException, RemoteException {
		super(positionInformation, configuration, CellXMaskQuantifierConfiguration.TYPE_IDENTIFIER,
				CellXMaskQuantifierConfiguration.class, "CellXMaskInterface quantifier.");
	}

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		if (isInitialized())
			return;
		super.initialize(measurementContext);
		String ys = System.getProperty("user.dir", ".");
		// Dedicated quantification environment (optional): the quant_* keys let the
		// quantifier run in its own Python venv with pinned scipy/skimage, decoupled
		// from any segmenter's environment and versions. Each quant_* key falls back
		// to the shared celldetection_* key, so existing single-env setups are
		// unchanged (same executable / port / work dir as before).
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
		// Own our backend scripts so the quantifier can start its server even when no
		// segmenter extracted them (e.g. a StarDist/Cellpose run with no QuickDetect).
		// The quantify code lives in cell_detection_cpython.py. Always overwrite so a
		// rebuilt JAR's scripts win.
		try {
			extractScript("/org/youscope/plugin/quickdetect/quickdetect_server.py", serverScript);
			extractScript("/org/youscope/addon/celldetection/utils/cell_detection_cpython.py",
					workdir + File.separator + "cell_detection_cpython.py");
		} catch (ResourceException e) {
			sendMessage("CellX quantifier: could not extract backend scripts (" + e.getMessage() + ").");
		}
		client = new AnalysisBackendClient(host, port, cpython, serverScript, logFile, idle);
		try {
			// Start (or reuse) our own quant server that is segmenter- and
			// version-independent.
			// Require the current backend version so a stale server (missing newer ops
			// such as 'track' or the 'cellx' quantifier) is replaced.
			client.ensureUp("v0.6.9", readConfigInt(ys, "celldetection_server_start_seconds", 60) * 1000);
		} catch (IOException e) {
			sendMessage("CellX quantifier: backend not reachable (" + e.getMessage() + ").");
		}
	}

	@Override
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		client = null;
		super.uninitialize(measurementContext);
	}

	@Override
	public CellDetectionResult quantify(CellDetectionResult detectionResult, ImageEvent<?>[] quantificationImages)
			throws QuantificationException, RemoteException {
		if (!isInitialized() || client == null)
			throw new QuantificationException("Quantifier not initialized.");
		ImageEvent<?> label = detectionResult.getLabelImage();
		if (label == null)
			throw new QuantificationException(
					"Segmenter produced no label image; cannot quantify. Enable label output on the segmentation algorithm.");
		if (quantificationImages == null)
			quantificationImages = new ImageEvent[0];

		CellXMaskQuantifierConfiguration cfg = getConfiguration();
		List<Path> temps = new ArrayList<Path>();
		try {
			for (int i = 0; i < quantificationImages.length; i++) {
				ImageEvent<?> qi = quantificationImages[i];
				if (qi != null && (qi.getWidth() != label.getWidth() || qi.getHeight() != label.getHeight()))
					throw new QuantificationException("Quantification channel " + i + " is " + qi.getWidth() + "x"
							+ qi.getHeight()
							+ " but the segmentation mask is " + label.getWidth() + "x" + label.getHeight()
							+ ". The segmentation image and the quantification image(s) must have the same dimensions "
							+ "(check that they use the same binning / camera ROI / pixel size).");
			}
			Path maskFile = writeImage(label, temps);
			StringBuilder chans = new StringBuilder("[");
			for (int i = 0; i < quantificationImages.length; i++) {
				Path cf = writeImage(quantificationImages[i], temps);
				if (i > 0)
					chans.append(",");
				chans.append(imageMetaJson(quantificationImages[i], cf,
						QuantificationConfiguration.channelSuffix(quantificationImages[i].getChannel(), i)));
			}
			chans.append("]");

			StringBuilder req = new StringBuilder();
			req.append("{\"op\":\"quantify\",\"quantifier\":\"cellx\",");
			req.append("\"mask\":").append(imageMetaJson(label, maskFile, null)).append(",");
			req.append("\"channels\":").append(chans).append(",");
			req.append("\"params\":{\"membrane_width\":").append(cfg.getMembraneWidth());
			req.append(",\"nuclear_volume_fraction\":").append(cfg.getNuclearVolumeFraction());
			req.append(",\"bright_area_percentage\":").append(cfg.getBrightAreaPercentage());
			req.append(",\"intensity_classes_count\":").append(cfg.getIntensityClassesCount());
			req.append(",\"smoothing_size\":").append(cfg.getSmoothingSize());
			req.append(",\"use_wiener\":").append(cfg.isUseWienerFilter());
			req.append(",\"scale_to_16bit\":").append(cfg.isScaleTo16Bit());
			req.append(",\"fluo_align_pixel_move\":").append(cfg.getFluoAlignPixelMove());
			req.append(",\"quantiles\":[");
			double[] qs = cfg.getQuantilePercents();
			for (int qi = 0; qi < qs.length; qi++) {
				if (qi > 0)
					req.append(",");
				req.append(qs[qi]);
			}
			req.append("]}}");

			String resp = client.request(req.toString(), timeoutMs);
			if (!AnalysisBackendClient.extractBool(resp, "ok", false))
				throw new QuantificationException(
						"Backend error: " + AnalysisBackendClient.extractString(resp, "error"));

			String namesStr = AnalysisBackendClient.extractString(resp, "columns_names");
			if (namesStr == null || namesStr.isEmpty())
				return detectionResult; // nothing measured
			String[] names = namesStr.split("\\|");
			int cellCount = (int) AnalysisBackendClient.extractLong(resp, "cell_count", 0);
			int colsCount = (int) AnalysisBackendClient.extractLong(resp, "cols_count", names.length);
			int[] ids = toIntArray(
					AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "ids_b64")));
			double[] flat = toDoubleArray(
					AnalysisBackendClient.inflateBase64(AnalysisBackendClient.extractString(resp, "values_b64")));

			Table table = buildTable(detectionResult.getCellTable(), names, ids, flat, cellCount, colsCount);
			return new CellDetectionResult(table, label);
		} catch (QuantificationException e) {
			throw e;
		} catch (Exception e) {
			throw new QuantificationException("Intensity quantification failed: " + e.getMessage(), e);
		} finally {
			for (Path p : temps) {
				try {
					Files.deleteIfExists(p);
				} catch (@SuppressWarnings("unused") IOException ignored) {
					/* not fatal */ }
			}
		}
	}

	private Table buildTable(Table source, String[] featureNames, int[] ids, double[] flat, int nCells, int nCols)
			throws QuantificationException {
		ColumnDefinition<?>[] cols = new ColumnDefinition<?>[1 + featureNames.length];
		cols[0] = ColumnDefinition.createIntegerColumnDefinition(
				CellDetectionTableColumns.TABLE_COLUMN_CELL_ID.getColumnName(), "Cell id.", false);
		for (int c = 0; c < featureNames.length; c++)
			cols[c + 1] = ColumnDefinition.createDoubleColumnDefinition(featureNames[c], featureNames[c], false);
		TableDefinition def = new TableDefinition(TABLE_ID, "Segmented + quantified cells.", cols);

		Table table = new Table(def, source.getCreationRuntime(), source.getPositionInformation(),
				source.getExecutionInformation());
		try {
			for (int r = 0; r < nCells; r++) {
				Serializable[] row = new Serializable[1 + nCols];
				row[0] = Integer.valueOf(ids[r]);
				for (int c = 0; c < nCols; c++)
					row[c + 1] = Double.valueOf(flat[r * nCols + c]);
				table.addRow(row);
			}
		} catch (TableException e) {
			throw new QuantificationException("Could not assemble quantified table: " + e.getMessage(), e);
		}
		return table;
	}

	// Image serialisation plus helpers (@ToDo: migrate to shared new
	// PythonBackendSupport)

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

	private Path writeImage(ImageEvent<?> image, List<Path> temps) throws IOException {
		byte[] le = serializeImageLE(image);
		Path p = Files.createTempFile("ys_q_", ".raw");
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

	private static String imageMetaJson(ImageEvent<?> image, Path file, String name) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"path\":\"").append(AnalysisBackendClient.escapeJson(file.toString())).append("\",");
		sb.append("\"width\":").append(image.getWidth()).append(",");
		sb.append("\"height\":").append(image.getHeight()).append(",");
		sb.append("\"bpp\":").append(image.getBytesPerPixel()).append(",");
		sb.append("\"bitdepth\":").append(image.getBitDepth()).append(",");
		sb.append("\"tx\":").append(image.isTransposeX()).append(",");
		sb.append("\"ty\":").append(image.isTransposeY()).append(",");
		sb.append("\"sw\":").append(image.isSwitchXY());
		if (name != null)
			sb.append(",\"name\":\"").append(AnalysisBackendClient.escapeJson(name)).append("\"");
		sb.append("}");
		return sb.toString();
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

	private static String firstNonNull(String a, String b) {
		return a != null ? a : b;
	}

	private void extractScript(String resourceName, String targetPath) throws ResourceException {
		File f = new File(targetPath);
		try {
			if (f.getParentFile() != null)
				f.getParentFile().mkdirs();
			InputStream is = CellXMaskQuantifierAddon.class.getResourceAsStream(resourceName);
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
