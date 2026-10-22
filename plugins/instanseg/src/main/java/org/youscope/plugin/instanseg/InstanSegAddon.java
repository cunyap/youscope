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
package org.youscope.plugin.instanseg;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.youscope.addon.analysis.AnalysisBackendClient;
import org.youscope.addon.analysis.PythonBackendSupport;
import org.youscope.addon.celldetection.CellDetectionAddon;
import org.youscope.addon.celldetection.CellDetectionException;
import org.youscope.addon.celldetection.CellDetectionResult;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.resource.ResourceException;
import org.youscope.common.saving.MeasurementSaver;
import org.youscope.common.table.Table;
import org.youscope.common.table.TableDefinition;
import org.youscope.common.table.TableException;
import org.youscope.common.table.TableListener;

/**
 * InstanSeg cell-detection addon.
 *
 * <p>
 * Talks to its own standalone persistent CPython server
 * ({@code instanseg_server.py}, port 59718 by default) through the shared
 * {@link AnalysisBackendClient} / {@link PythonBackendSupport} plumbing. torch
 * and the InstanSeg model stay loaded between frames and between measurements.
 *
 * <p>
 * Segment-only: returns a label mask plus the core-geometry table
 * ({@code cell.index}, {@code cell.center.x/y}, {@code cell.area},
 * {@code cell.perimeter}). Intensity is measured by the selected quantifier.
 *
 * <p>
 * Optional {@code graalpy_config.txt} keys (all fall back sensibly):
 * <ul>
 * <li>{@code instanseg_cpython_executable} (then
 * {@code celldetection_cpython_executable}, then
 * {@code cpython_executable})</li>
 * <li>{@code instanseg_backend_port} (default 59718),
 * {@code instanseg_backend_host} (then {@code celldetection_backend_host};
 * note: a non-local host cannot read YouScope's temp image files)</li>
 * <li>{@code instanseg_work_dir}, {@code instanseg_server_start_seconds}
 * (default 120), {@code instanseg_timeout_seconds} (default 900),
 * {@code instanseg_idle_timeout_seconds} (default 7200, 0 = never)</li>
 * </ul>
 *
 * @author Andreas P. Cuny
 */
class InstanSegAddon extends ResourceAdapter<InstanSegConfiguration> implements CellDetectionAddon {
    private static final long serialVersionUID = 1L;

    /**
     * Plugin version. Must equal SCRIPT_VERSION in instanseg_server.py: on a
     * mismatch {@link AnalysisBackendClient#ensureUp} replaces the running (stale)
     * server with the one bundled in this JAR.
     */
    static final String PLUGIN_VERSION = "v0.1.0";

    private static final int DEFAULT_PORT = 59718;
    private static final String SERVER_RESOURCE = "/org/youscope/plugin/instanseg/instanseg_server.py";

    private AnalysisBackendClient client = null;
    private String workingDir = null;
    private int startTimeoutMs = 120000;
    private int detectionTimeoutMs = 900000;

    private final ArrayList<TableListener> tableListeners = new ArrayList<TableListener>();

    /** Measurement saver from the ConstructionContext; may be null. */
    private MeasurementSaver measurementSaver = null;
    private MeasurementContext measurementContext = null;
    private String outputDir = null;
    private boolean reproFilesWritten = false;

    InstanSegAddon(PositionInformation positionInformation, InstanSegConfiguration configuration)
            throws ConfigurationException, RemoteException {
        this(positionInformation, configuration, null);
    }

    InstanSegAddon(PositionInformation positionInformation, InstanSegConfiguration configuration,
            MeasurementSaver measurementSaver) throws ConfigurationException, RemoteException {
        super(positionInformation, configuration, InstanSegConfiguration.TYPE_IDENTIFIER,
                InstanSegConfiguration.class, "InstanSeg cell detection.");
        this.measurementSaver = measurementSaver;
    }

    // Resource lifecycle

    @Override
    public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
        if (isInitialized())
            return;
        super.initialize(measurementContext);
        this.measurementContext = measurementContext;

        String ys = System.getProperty("user.dir", ".");
        String host = PythonBackendSupport.readConfigWithFallback(ys, "instanseg_backend_host",
                "celldetection_backend_host");
        if (host == null)
            host = "127.0.0.1";
        boolean local = host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost");

        String py = PythonBackendSupport.readConfigWithFallback(ys, "instanseg_cpython_executable",
                "celldetection_cpython_executable", "cpython_executable");
        if (local && (py == null || !new File(py).exists()))
            throw new ResourceException("InstanSeg: no CPython executable found. Set "
                    + "'instanseg_cpython_executable' (or 'cpython_executable') in graalpy_config.txt to a "
                    + "Python with InstanSeg installed (pip install instanseg-torch). Current value: " + py);

        int port = PythonBackendSupport.readConfigIntWithFallback(ys, DEFAULT_PORT, "instanseg_backend_port");
        startTimeoutMs = PythonBackendSupport.readConfigIntWithFallback(ys, 120, "instanseg_server_start_seconds")
                * 1000;
        int idle = PythonBackendSupport.readConfigIntWithFallback(ys, 7200, "instanseg_idle_timeout_seconds");

        // Config-UI timeout wins; 0 falls back to graalpy_config.txt, then the default.
        int cfgTimeout = getConfiguration().getDetectionTimeoutSeconds();
        detectionTimeoutMs = (cfgTimeout > 0 ? cfgTimeout
                : PythonBackendSupport.readConfigIntWithFallback(ys, 900, "instanseg_timeout_seconds",
                        "celldetection_timeout_seconds"))
                * 1000;

        workingDir = PythonBackendSupport.resolveWritableDir(
                PythonBackendSupport.readConfig(ys, "instanseg_work_dir"), ys + File.separator + "scripts");
        // Always re-extracted, so a stale on-disk copy can never shadow a rebuilt JAR.
        String serverScript = workingDir + File.separator + "instanseg_server.py";
        PythonBackendSupport.extractScript(InstanSegAddon.class, SERVER_RESOURCE, serverScript);

        String logFile = workingDir + File.separator + "instanseg_server.log";
        client = new AnalysisBackendClient(host, port, py, serverScript, logFile, idle);
        try {
            String v = client.ensureUp(PLUGIN_VERSION, startTimeoutMs);
            sendMessage("InstanSeg " + PLUGIN_VERSION + ": backend " + v + " at " + host + ":" + port
                    + ". Server log: " + logFile);
        } catch (IOException e) {
            client = null;
            throw new ResourceException("InstanSeg: could not start/reach the Python backend: " + e.getMessage(), e);
        }

        // Pre-load the model now (downloads the weights on first use), so the first
        // frame of the measurement is not delayed and setup problems (InstanSeg not
        // installed, no network for the download, wrong model name) surface at start.
        try {
            sendMessage("InstanSeg: loading model '" + getConfiguration().getModelType() + "' on device '"
                    + getConfiguration().getDevice() + "' (first use downloads the weights)...");
            String resp = client.request("{\"op\":\"load\",\"params\":"
                    + paramsJson(getConfiguration(), true, false) + "}", detectionTimeoutMs);
            if (!AnalysisBackendClient.extractBool(resp, "ok", false))
                throw new ResourceException("InstanSeg: could not load model: " + errorOf(resp));
            sendMessage("InstanSeg: model ready on " + AnalysisBackendClient.extractString(resp, "device")
                    + " (init " + AnalysisBackendClient.extractLong(resp, "init_ms", 0) + " ms).");
        } catch (IOException e) {
            throw new ResourceException("InstanSeg: model pre-load failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
        // Leave the server running for reuse by later measurements; the JVM shutdown
        // hook stops it when YouScope exits (and its idle timer is a backstop).
        client = null;
        workingDir = null;
        outputDir = null;
        reproFilesWritten = false;
        this.measurementContext = null;
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
        if (!isInitialized() || client == null)
            throw new CellDetectionException("Addon not yet initialized.");
        if (detectionImage == null)
            throw new CellDetectionException("Image in which cells should be detected is null.");
        if (detectionImage.getExecutionInformation() == null || detectionImage.getPositionInformation() == null)
            throw new CellDetectionException("Metadata of the detection image is not initialized.");

        InstanSegConfiguration cfg = getConfiguration();
        List<Path> temps = new ArrayList<Path>();
        try {
            Path file = PythonBackendSupport.writeImageTemp(detectionImage, "ys_instanseg_", temps);
            boolean wantEnv = !reproFilesWritten;
            String req = "{\"op\":\"segment\",\"image\":" + PythonBackendSupport.imageMetaJson(detectionImage, file)
                    + ",\"params\":" + paramsJson(cfg, cfg.isGenerateLabelImage(), wantEnv) + "}";

            String resp = callBackend(req);
            if (!AnalysisBackendClient.extractBool(resp, "ok", false))
                throw new CellDetectionException("InstanSeg error: " + errorOf(resp));

            int nCells = (int) AnalysisBackendClient.extractLong(resp, "n_cells", 0);
            long timing = AnalysisBackendClient.extractLong(resp, "timing_ms", 0);
            long inference = AnalysisBackendClient.extractLong(resp, "inference_ms", 0);
            String warning = AnalysisBackendClient.extractString(resp, "warning");
            if (warning != null && !warning.isEmpty())
                sendMessage("InstanSeg warning: " + warning);
            sendMessage("InstanSeg: " + nCells + " " + AnalysisBackendClient.extractString(resp, "target")
                    + " (backend " + AnalysisBackendClient.extractString(resp, "version") + ", " + timing
                    + " ms, inference " + inference + " ms"
                    + (AnalysisBackendClient.extractBool(resp, "tiled", false) ? ", tiled" : "") + ").");

            if (wantEnv)
                writeReproFiles(resp);

            Table table = buildTable(resp, detectionImage);
            sendTableToListeners(table);

            ImageEvent<?> labelImage = null;
            if (cfg.isGenerateLabelImage() && AnalysisBackendClient.extractBool(resp, "has_label", false)) {
                int w = (int) AnalysisBackendClient.extractLong(resp, "width", 0);
                int h = (int) AnalysisBackendClient.extractLong(resp, "height", 0);
                String labelB64 = AnalysisBackendClient.extractString(resp, "label_b64");
                if (labelB64 != null && w > 0 && h > 0)
                    labelImage = buildLabelImage(AnalysisBackendClient.inflateBase64(labelB64), w, h, detectionImage);
            }
            return new CellDetectionResult(table, labelImage);
        } catch (CellDetectionException e) {
            throw e;
        } catch (Exception e) {
            throw new CellDetectionException("InstanSeg failed: " + e.getMessage(), e);
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

    // Backend communication

    /**
     * Sends a request. If the server has gone away (idle-timeout, crash, killed
     * by the user between long imaging intervals) it is restarted once and the
     * request repeated. A read timeout is nit retried (it would just double the
     * wait).
     */
    private String callBackend(String request) throws IOException {
        try {
            return client.request(request, detectionTimeoutMs);
        } catch (SocketTimeoutException te) {
            throw new IOException("No response from the InstanSeg server within " + (detectionTimeoutMs / 1000)
                    + " s. On CPU a large frame can take minutes: increase 'Detection timeout', use a GPU "
                    + "device, or tile / downscale. See instanseg_server.log in the working directory.", te);
        } catch (IOException first) {
            sendMessage("InstanSeg: backend call failed (" + first.getMessage() + "); restarting the server once.");
            client.ensureUp(PLUGIN_VERSION, startTimeoutMs);
            return client.request(request, detectionTimeoutMs);
        }
    }

    private static String errorOf(String resp) {
        String err = AnalysisBackendClient.extractString(resp, "error");
        return err == null || err.isEmpty() ? "unknown backend error" : err;
    }

    /** Builds the "params" JSON object shared by the "load" and "segment" ops. */
    private static String paramsJson(InstanSegConfiguration cfg, boolean generateLabel, boolean wantEnv) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"model\":\"").append(AnalysisBackendClient.escapeJson(cfg.getModelType())).append("\",");
        sb.append("\"device\":\"").append(AnalysisBackendClient.escapeJson(cfg.getDevice())).append("\",");
        sb.append("\"target\":\"").append(cfg.getTarget()).append("\",");
        sb.append("\"pixel_size\":").append(cfg.getPixelSizeUm()).append(",");
        sb.append("\"processing\":\"").append(cfg.getProcessingMode()).append("\",");
        sb.append("\"tile_size\":").append(cfg.getTileSize()).append(",");
        sb.append("\"normalise\":").append(cfg.isNormalise()).append(",");
        sb.append("\"invert\":").append(cfg.isInvert()).append(",");
        sb.append("\"input_channels\":").append(cfg.getInputChannels()).append(",");
        // -1 = "model default": the server only forwards values >= 0.
        sb.append("\"seed_threshold\":").append(cfg.getSeedThreshold()).append(",");
        sb.append("\"overlap_threshold\":").append(cfg.getOverlapThreshold()).append(",");
        sb.append("\"mask_threshold\":").append(cfg.getMaskThreshold()).append(",");
        sb.append("\"min_size\":").append(cfg.getMinSize()).append(",");
        sb.append("\"peak_distance\":").append(cfg.getPeakDistance()).append(",");
        sb.append("\"cleanup_fragments\":").append(cfg.isCleanupFragments()).append(",");
        sb.append("\"resolve_cell_and_nucleus\":").append(cfg.isResolveCellAndNucleus()).append(",");
        sb.append("\"threads\":").append(cfg.getCpuCores()).append(",");
        sb.append("\"want_env\":").append(wantEnv).append(",");
        sb.append("\"generate_label\":").append(generateLabel);
        sb.append("}");
        return sb.toString();
    }

    // Table / image builders

    /**
     * Builds the core-geometry table. Column order must match
     * {@link InstanSegTable#getTableDefinition()}: cell.index, cell.center.x,
     * cell.center.y, cell.area, cell.perimeter. cell.index == label id in the mask.
     */
    private Table buildTable(String resp, ImageEvent<?> srcImage) throws CellDetectionException, IOException {
        Table table = new Table(InstanSegTable.getTableDefinition(), srcImage.getCreationTime(),
                srcImage.getPositionInformation(), srcImage.getExecutionInformation());
        String idsB64 = AnalysisBackendClient.extractString(resp, "ids_b64");
        String valsB64 = AnalysisBackendClient.extractString(resp, "values_b64");
        if (idsB64 == null || valsB64 == null)
            return table;
        int[] ids = PythonBackendSupport.toIntArray(AnalysisBackendClient.inflateBase64(idsB64));
        double[] vals = PythonBackendSupport.toDoubleArray(AnalysisBackendClient.inflateBase64(valsB64));
        if (vals.length < ids.length * 4)
            throw new CellDetectionException("Backend returned inconsistent geometry (" + ids.length + " ids, "
                    + vals.length + " values).");
        try {
            for (int i = 0; i < ids.length; i++) {
                double perimeter = vals[4 * i + 3];
                table.addRow(new Serializable[] { Integer.valueOf(ids[i]), Double.valueOf(vals[4 * i]),
                        Double.valueOf(vals[4 * i + 1]), Double.valueOf(vals[4 * i + 2]),
                        Double.isNaN(perimeter) || perimeter <= 0 ? null : Double.valueOf(perimeter) });
            }
        } catch (TableException e) {
            throw new CellDetectionException("Could not build cell table: " + e.getMessage(), e);
        }
        return table;
    }

    /**
     * Builds the label ImageEvent from the server's little-endian uint16 label
     * array (row-major, h*w). The visualizers accept only 1 byte/pixel (byte[]) or
     * 2 bytes/pixel (short[]) and cast getImageData() to that exact type, so the
     * smallest fitting depth is chosen (same rule as the Cellpose plugin).
     * An all-background mask is still emitted so every frame has a segmentation
     * image.
     */
    private ImageEvent<?> buildLabelImage(byte[] le, int w, int h, ImageEvent<?> srcImage) {
        try {
            int n = w * h;
            if (le == null || le.length < 2 * n) {
                sendMessage("InstanSeg: label image not built (expected " + (2 * n) + " bytes, got "
                        + (le == null ? 0 : le.length) + ").");
                return null;
            }
            ByteBuffer bb = ByteBuffer.wrap(le).order(ByteOrder.LITTLE_ENDIAN);
            short[] labels = new short[n];
            bb.asShortBuffer().get(labels);
            int maxLabel = 0;
            for (int i = 0; i < n; i++) {
                int v = labels[i] & 0xFFFF;
                if (v > maxLabel)
                    maxLabel = v;
            }
            ImageEvent<?> label;
            if (maxLabel <= 255) {
                byte[] px = new byte[n];
                for (int i = 0; i < n; i++)
                    px[i] = (byte) labels[i];
                label = ImageEvent.createImage(px, w, h, 8);
            } else {
                label = ImageEvent.createImage(labels, w, h, 16);
            }
            label.setPositionInformation(srcImage.getPositionInformation());
            label.setExecutionInformation(srcImage.getExecutionInformation());
            return label;
        } catch (Throwable e) {
            sendErrorMessage("Could not build label image: " + e.getMessage(),
                    (e instanceof Exception) ? (Exception) e : new Exception(e));
            return null;
        }
    }

    // TableProducer

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
        return InstanSegTable.getTableDefinition();
    }

    private void sendTableToListeners(Table table) {
        if (table == null)
            return;
        synchronized (tableListeners) {
            for (Iterator<TableListener> it = tableListeners.iterator(); it.hasNext();) {
                try {
                    it.next().newTableProduced(table.clone());
                } catch (@SuppressWarnings("unused") RemoteException e) {
                    it.remove();
                }
            }
        }
    }

    // Reproducibility outputs (environment snapshot and pip freeze)

    /**
     * Writes instanseg_environment.txt / instanseg_requirements.txt into the
     * measurement results folder (or the working dir if that cannot be located)
     * from the base64 text the server returns with the first frame.
     */
    private void writeReproFiles(String resp) {
        if (reproFilesWritten)
            return;
        try {
            if (outputDir == null) {
                String mf = resolveMeasurementFolder();
                outputDir = mf != null ? mf : workingDir;
            }
            if (outputDir == null)
                return;
            boolean env = writeBytes(inflateOrNull(AnalysisBackendClient.extractString(resp, "env_b64")),
                    outputDir + File.separator + "instanseg_environment.txt");
            boolean req = writeBytes(inflateOrNull(AnalysisBackendClient.extractString(resp, "req_b64")),
                    outputDir + File.separator + "instanseg_requirements.txt");
            sendMessage("InstanSeg: reproducibility files in " + outputDir + " (environment=" + (env ? "yes" : "no")
                    + ", requirements=" + (req ? "yes" : "no") + ").");
            if (env && req)
                reproFilesWritten = true;
        } catch (Throwable t) {
            sendMessage("InstanSeg: could not write reproducibility files: " + t.getMessage());
        }
    }

    private static byte[] inflateOrNull(String b64) {
        if (b64 == null || b64.isEmpty())
            return null;
        try {
            return AnalysisBackendClient.inflateBase64(b64);
        } catch (@SuppressWarnings("unused") IOException e) {
            return null;
        }
    }

    private static boolean writeBytes(byte[] data, String path) {
        if (data == null || data.length == 0)
            return false;
        try {
            Files.write(new File(path).toPath(), data);
            return true;
        } catch (@SuppressWarnings("unused") Exception e) {
            return false;
        }
    }

    /**
     * Best-effort location of this measurement's results folder, via the
     * MeasurementSaver handed over by the factory (reliable) or, failing that,
     * reflectively through the MeasurementContext (older API shapes).
     */
    private String resolveMeasurementFolder() {
        if (measurementSaver != null) {
            try {
                String f = folderOf(measurementSaver.getLastMeasurementFileLocations());
                if (f != null)
                    return f;
            } catch (@SuppressWarnings("unused") Throwable t) {
                /* fall through */
            }
        }
        if (measurementContext == null)
            return null;
        try {
            Object saver = tryInvoke(measurementContext, "getMeasurementSaver");
            if (saver == null)
                saver = tryInvoke(measurementContext, "getSaver");
            return saver == null ? null : folderOf(tryInvoke(saver, "getLastMeasurementFileLocations"));
        } catch (@SuppressWarnings("unused") Throwable t) {
            return null;
        }
    }

    private static String folderOf(Object locations) {
        if (locations == null)
            return null;
        Object folder = tryInvoke(locations, "getMeasurementBaseFolder");
        if (folder == null)
            folder = tryInvoke(locations, "getMeasurementFolder");
        if (folder == null)
            return null;
        File f = new File(folder.toString());
        return f.isDirectory() && f.canWrite() ? f.getAbsolutePath() : null;
    }

    private static Object tryInvoke(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (@SuppressWarnings("unused") Throwable t) {
            return null;
        }
    }
}
