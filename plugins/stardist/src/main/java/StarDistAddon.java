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
package org.youscope.plugin.stardist;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;

import org.youscope.addon.celldetection.CellDetectionAddon;
import org.youscope.addon.celldetection.CellDetectionException;
import org.youscope.addon.celldetection.CellDetectionResult;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.resource.ResourceException;
import org.youscope.common.saving.MeasurementSaver;
import org.youscope.common.table.Table;
import org.youscope.common.table.TableDefinition;
import org.youscope.common.table.TableException;
import org.youscope.common.table.TableListener;

/**
 * StarDist cell-detection addon.
 *
 * <p>
 * Passes the image to a CPython environment (installed by
 * setup_graalpy_venv.ps1) via ProcessBuilder, reads back a JSON result,
 * and builds a {@link CellDetectionResult} with a per-cell {@link Table}
 * and an optional uint16 label image.
 *
 * <p>
 * No GraalPy / scripting engine involved -- all heavy computation runs
 * in CPython where torch, stardist, and C extensions work normally.
 */
class StarDistAddon extends ResourceAdapter<StarDistConfiguration>
        implements CellDetectionAddon {
    private static final long serialVersionUID = 1L;

    /**
     * Plugin version: Logged at init and, together with the server's
     * SCRIPT_VERSION line, lets you confirm the rebuilt JAR is actually live
     * (a stale .py inside the JAR was the cause of earlier confusion).
     */
    static final String PLUGIN_VERSION = "v0.5.1";

    private String cpythonExe = null;
    private String bridgeScript = null;
    private String serverScript = null;
    private final ArrayList<TableListener> tableListeners = new ArrayList<TableListener>();

    /**
     * Shared persistent server process, reused across all StarDistAddon instances.
     */
    private static volatile Process serverProcess = null;
    private static final Object serverLock = new Object();
    private static final int SERVER_PORT = 59712;
    /**
     * Time to wait for the server to print READY. The first run downloads the
     * 2D_versatile_he SAM weights (large), so keep this generous. Override with
     * "stardist_server_start_seconds" in graalpy_config.txt.
     */
    private static int SERVER_START_TIMEOUT_MS = 300000; // 5 min (was 60s)

    /**
     * Per-detection socket read timeout in ms. 2D_versatile_he on CPU can take
     * several
     * minutes for a single full-frame image, so 2 min (the old hard-coded
     * value) is far too short and produced "Read timed out". Overridable via
     * "stardist_timeout_seconds" in graalpy_config.txt (default 900 s = 15 min).
     */
    private int detectionTimeoutMs = 900000;

    /**
     * File the persistent server's stdout/stderr is mirrored to (see initialize).
     */
    private static volatile String serverLogPath = null;

    /**
     * Writable directory for runtime outputs (server log, env snapshot, and the
     * stardist_last_input.tif debug image) and extracted scripts. Set in
     * initialize; also used as the server process working directory so relative
     * writes land somewhere deterministic and writable.
     */
    private String workingDir = null;

    /** Measurement context (used to best-effort locate the measurement folder). */
    private MeasurementContext measurementContext = null;

    /**
     * Measurement saver from the ConstructionContext; the reliable route to the
     * measurement results folder. May be null (older construction path).
     */
    private MeasurementSaver measurementSaver = null;

    /**
     * Resolved directory for this measurement's reproducibility outputs
     * (stardist_environment.txt / stardist_requirements.txt /
     * stardist_last_input.tif).
     * Set lazily on first detection: the measurement results folder if it can be
     * located, else {@link #workingDir}.
     */
    private String outputDir = null;
    private boolean reproFilesCopied = false;
    private boolean logRelocated = false;

    /**
     * Label mask bytes returned inline by the persistent server for the current
     * frame ([w:int32][h:int32][w*h*uint16] big-endian). Avoids a sidecar file.
     */
    private byte[] lastMaskBytes = null;

    /**
     * Preprocessed input bytes returned inline ([w:int32][h:int32][w*h*uint8]);
     * Java writes it as stardist_last_input.png in the results folder.
     */
    private byte[] lastDebugBytes = null;

    /**
     * Port where our verified (matching-version) server is running. -1 = none.
     * May differ from the configured port if that one is held by a stale server.
     */
    private volatile int activePort = -1;

    /**
     * True once the JVM shutdown hook (stops servers on YouScope exit) is added.
     */
    private static volatile boolean shutdownHookRegistered = false;

    StarDistAddon(PositionInformation positionInformation,
            StarDistConfiguration configuration)
            throws RemoteException, org.youscope.common.configuration.ConfigurationException {
        this(positionInformation, configuration, null);
    }

    /**
     * Preferred constructor: the {@link StarDistAddonFactory} passes the
     * measurement's {@link MeasurementSaver} (obtained from the
     * ConstructionContext) so reproducibility outputs can be written into the
     * measurement results folder rather than the YouScope program dir.
     */
    StarDistAddon(PositionInformation positionInformation,
            StarDistConfiguration configuration, MeasurementSaver measurementSaver)
            throws RemoteException, org.youscope.common.configuration.ConfigurationException {
        super(positionInformation, configuration,
                StarDistConfiguration.TYPE_IDENTIFIER,
                StarDistConfiguration.class,
                "StarDist cell detection.");
        this.measurementSaver = measurementSaver;
    }

    // Resource lifecycle

    @Override
    public void initialize(MeasurementContext measurementContext)
            throws ResourceException, RemoteException {
        if (isInitialized())
            return;
        super.initialize(measurementContext);
        this.measurementContext = measurementContext;

        String youScopeDir = System.getProperty("user.dir", ".");
        cpythonExe = readConfig(youScopeDir, "cpython_executable");
        if (cpythonExe == null || !new File(cpythonExe).exists()) {
            cpythonExe = "python"; // fallback: system python on PATH
            sendMessage("StarDistAddon: cpython_executable not in graalpy_config.txt, "
                    + "using system 'python'. Run setup_graalpy_venv.ps1 to configure.");
        }

        // Per-detection timeout: the config UI value wins; 0 falls back to the
        // global override in graalpy_config.txt, then to the built-in default.
        int cfgTimeout = getConfiguration().getDetectionTimeoutSeconds();
        detectionTimeoutMs = (cfgTimeout > 0 ? cfgTimeout
                : readConfigInt(youScopeDir, "stardist_timeout_seconds", 900)) * 1000;
        SERVER_START_TIMEOUT_MS = readConfigInt(youScopeDir, "stardist_server_start_seconds", 300) * 1000;

        // Working directory for runtime outputs (log, env snapshot, debug image)
        // and extracted scripts. Prefer the YouScope dir; if it is not writable
        // (e.g. a locked-down Program Files install) fall back to the system temp
        // dir. Can be overridden with "stardist_work_dir" in graalpy_config.txt.
        String workDirCfg = readConfig(youScopeDir, "stardist_work_dir");
        workingDir = resolveWritableDir(workDirCfg, youScopeDir);

        // Persist [stardist_server] output so the model/version, per-call inference
        // times and any Python tracebacks survive after the measurement finishes.
        serverLogPath = workingDir + File.separator + "stardist_server.log";

        sendMessage("StarDistAddon: per-detection read timeout = "
                + (detectionTimeoutMs / 1000) + " s, server-start timeout = "
                + (SERVER_START_TIMEOUT_MS / 1000) + " s. Server log: " + serverLogPath);

        // Extracted-script location: overridable via "stardist_scripts_dir";
        // otherwise <workingDir>/scripts. Scripts are ALWAYS re-extracted (see
        // extractScript) so a stale on-disk copy can never shadow a rebuilt JAR.
        String scriptsDirCfg = readConfig(youScopeDir, "stardist_scripts_dir");
        String scriptsDir = resolveWritableDir(scriptsDirCfg,
                workingDir + File.separator + "scripts");
        bridgeScript = scriptsDir + File.separator + "run_stardist.py";
        serverScript = scriptsDir + File.separator + "stardist_server.py";
        extractScript("/org/youscope/plugin/stardist/run_stardist.py", bridgeScript);
        extractScript("/org/youscope/plugin/stardist/stardist_server.py", serverScript);

        if (getConfiguration().isUsePersistentServer()) {
            startServerIfNeeded();
        }
        sendMessage("StarDistAddon " + PLUGIN_VERSION + " initialized. Bridge: " + bridgeScript
                + (getConfiguration().isUsePersistentServer()
                        ? " (persistent server mode)"
                        : " (one-shot subprocess mode)"));
    }

    /**
     * Returns the first writable directory among {@code preferred} then
     * {@code fallback}, else the system temp dir. Creates it if needed.
     */
    private String resolveWritableDir(String preferred, String fallback) {
        String[] candidates = { preferred, fallback,
                System.getProperty("java.io.tmpdir") + File.separator + "youscope-stardist" };
        for (String c : candidates) {
            if (c == null || c.trim().isEmpty())
                continue;
            File d = new File(c.trim());
            try {
                if (!d.exists())
                    d.mkdirs();
                if (d.isDirectory() && d.canWrite())
                    return d.getAbsolutePath();
            } catch (Exception ignored) {
            }
        }
        return new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
    }

    @Override
    public void uninitialize(MeasurementContext measurementContext)
            throws ResourceException, RemoteException {
        cpythonExe = null;
        bridgeScript = null;
        outputDir = null;
        reproFilesCopied = false;
        logRelocated = false;
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
    public CellDetectionResult detectCells(ImageEvent<?> detectionImage,
            ImageEvent<?>[] quantificationImages)
            throws CellDetectionException, RemoteException {
        if (!isInitialized())
            throw new CellDetectionException("Not initialized.");
        if (detectionImage == null)
            throw new CellDetectionException("Detection image is null.");

        Path tempImage = null;
        Path tempResult = null;
        Path tempArgs = null;
        Path tempMasks = null;
        lastMaskBytes = null; // cleared per frame; set by callServer if inline
        lastDebugBytes = null;
        try {
            // Serialize image to temp file
            // Format: 3 big-endian ints (width, height, bytesPerPixel) + raw pixels
            int w = detectionImage.getWidth();
            int h = detectionImage.getHeight();

            // getImageData() returns byte[] for 8-bit cameras, short[] for
            // 16-bit cameras (e.g. Hamamatsu) and int[] for 32-bit ones.
            // Serialise to big-endian bytes matching the Python bridge's
            // read_pixels() contract (bpp 1 -> uint8, 2 -> >u2, 4 -> >u4) and
            // derive bpp from the actual array type so the header length and
            // the pixel buffer length always agree (Python reshapes assuming
            // len == w*h*bpp).
            Object raw = detectionImage.getImageData();
            int bpp;
            byte[] pixels;
            if (raw instanceof byte[]) {
                // 8-bit path: unchanged from the previously working behaviour.
                bpp = detectionImage.getBytesPerPixel();
                pixels = (byte[]) raw;
            } else if (raw instanceof short[]) {
                short[] s = (short[]) raw;
                bpp = 2;
                pixels = new byte[s.length * 2];
                for (int i = 0, o = 0; i < s.length; i++, o += 2) {
                    pixels[o] = (byte) ((s[i] >> 8) & 0xFF); // big-endian high byte
                    pixels[o + 1] = (byte) (s[i] & 0xFF); // low byte
                }
            } else if (raw instanceof int[]) {
                int[] s = (int[]) raw;
                bpp = 4;
                pixels = new byte[s.length * 4];
                for (int i = 0, o = 0; i < s.length; i++, o += 4) {
                    pixels[o] = (byte) ((s[i] >> 24) & 0xFF);
                    pixels[o + 1] = (byte) ((s[i] >> 16) & 0xFF);
                    pixels[o + 2] = (byte) ((s[i] >> 8) & 0xFF);
                    pixels[o + 3] = (byte) (s[i] & 0xFF);
                }
            } else {
                throw new CellDetectionException(
                        "Unsupported image pixel type: "
                                + (raw == null ? "null" : raw.getClass().getName())
                                + ". StarDist supports 8-bit (byte[]), 16-bit (short[]) "
                                + "and 32-bit (int[]) images.");
            }

            byte[] header = new byte[12];
            writeInt(header, 0, w);
            writeInt(header, 4, h);
            writeInt(header, 8, bpp);

            tempImage = Files.createTempFile("ys_sd_img_", ".bin");
            byte[] full = new byte[header.length + pixels.length];
            System.arraycopy(header, 0, full, 0, header.length);
            System.arraycopy(pixels, 0, full, header.length, pixels.length);
            Files.write(tempImage, full);

            tempResult = Files.createTempFile("ys_sd_res_", ".json");

            StarDistConfiguration cfg = getConfiguration();

            // Label image is returned as a compact uint16 binary sidecar file
            // (big-endian: [w:int32][h:int32][w*h*uint16]) instead of a giant
            // JSON int array: O(W*H) faster and far smaller for 2048 x 2048 frames.
            if (cfg.isGenerateLabelImage())
                tempMasks = Files.createTempFile("ys_sd_masks_", ".bin");

            // Write args JSON to file (avoids path quoting issues)
            tempArgs = Files.createTempFile("ys_sd_args_", ".json");
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"image_bin\":\"").append(escJ(tempImage.toString())).append("\",");
            sb.append("\"result_json\":\"").append(escJ(tempResult.toString())).append("\",");
            sb.append("\"model\":\"").append(escJ(cfg.getModelType())).append("\",");
            sb.append("\"prob_thresh\":").append(cfg.getProbThreshold()).append(",");
            sb.append("\"nms_thresh\":").append(cfg.getNmsThreshold()).append(",");
            sb.append("\"n_tiles\":").append(cfg.getNTiles()).append(",");
            sb.append("\"percentile_low\":").append(cfg.getPercentileLow()).append(",");
            sb.append("\"percentile_high\":").append(cfg.getPercentileHigh()).append(",");
            sb.append("\"gpu\":").append(cfg.isUseGpu()).append(",");
            sb.append("\"invert\":").append(cfg.isInvert()).append(",");
            sb.append("\"downscale\":").append(cfg.getDownscaleFactor()).append(",");
            String outDir = currentOutputDir();
            if (outDir != null)
                sb.append("\"debug_dir\":\"").append(escJ(outDir)).append("\",");
            sb.append("\"generate_label\":").append(cfg.isGenerateLabelImage());
            if (tempMasks != null)
                sb.append(",\"masks_bin_out\":\"").append(escJ(tempMasks.toString())).append("\"");
            sb.append("}");
            Files.write(tempArgs, sb.toString().getBytes("UTF-8"));

            // Run via persistent server (fast) or one-shot subprocess (slow)
            String json;
            if (getConfiguration().isUsePersistentServer() && startServerIfNeeded()) {
                json = callServer(tempImage.toString(),
                        tempMasks == null ? null : tempMasks.toString(), cfg);
            } else {
                json = callOneShotSubprocess(tempArgs, tempResult);
            }

            String errMsg = parseString(json, "error");
            if (errMsg != null && errMsg.length() > 0)
                throw new CellDetectionException("StarDist error: " + errMsg);

            int nCells = parseInt(json, "n_cells");
            sendMessage("StarDist detected " + nCells + " cells.");

            // Surface the server-side per-step timing (handle_request stages) that
            // the server now returns in the response - request-time server stdout
            // does not reach the log, so we relay it here.
            String stages = extractArray(json, "server_stages");
            if (stages != null && stages.length() > 0)
                appendServerLog("StarDist server steps: " + stages);

            // Version check: a stale/duplicate server on the port (e.g. left over
            // from a crashed run) can answer instead of this one and break the
            // mask transport. Detect it and force a restart before the next frame.
            String srvVer = parseString(json, "script_version");
            if (srvVer != null && !srvVer.equals(PLUGIN_VERSION)) {
                appendServerLog("WARNING: answered by a DIFFERENT StarDist server "
                        + "version (" + srvVer + ", expected " + PLUGIN_VERSION
                        + "). A stale server is running on port "
                        + getConfiguration().getServerPort()
                        + "; restarting it now.");
                restartServer();
            }

            // Decode the label mask + diagnostic image (zlib+base64 in the JSON).
            lastMaskBytes = unpack(parseString(json, "masks_b64"));
            lastDebugBytes = unpack(parseString(json, "debug_b64"));
            boolean whole = json.trim().endsWith("}");
            appendServerLog("StarDist result received: " + nCells + " cells, "
                    + "response " + (json.length() / 1024) + " KB"
                    + (whole ? "" : " (WARNING: response looks truncated!)")
                    + ", segmentation mask "
                    + (lastMaskBytes == null ? "MISSING"
                            : (lastMaskBytes.length / 1024) + " KB decoded OK")
                    + ".");

            // Write env + requirements into the results folder (from base64).
            writeReproFiles(json);
            // Reproducibility: record model, version, timing and the debug-image
            // path for every frame. Logged Java-side so it always lands in the
            // file even if the server's own stdout is buffered.
            String debugError = parseString(json, "debug_error");
            // Write the diagnostic input image ourselves from the inline bytes the
            // server returned (Python request-time file writes were unreliable on
            // this install). PNG via ImageIO is always available and viewable.
            String debugFinal = currentOutputDir() + File.separator
                    + "stardist_last_input.png";
            boolean debugExists = writeDebugPng(lastDebugBytes, debugFinal);
            appendServerLog("detection: model=" + cfg.getModelType()
                    + " n_cells=" + nCells
                    + " stardist_version=" + parseString(json, "stardist_version")
                    + " timing=" + extractRaw(json, "timing")
                    + " debug_image=" + debugFinal
                    + " debug_image_written=" + debugExists
                    + (debugError != null && debugError.length() > 0
                            ? " debug_error=" + debugError
                            : ""));
            if (!debugExists)
                appendServerLog("NOTE: stardist_last_input.png not written"
                        + (debugError != null && debugError.length() > 0
                                ? " (server error: " + debugError + ")"
                                : " (no inline image bytes)")
                        + ". Diagnostic only; does not affect detection or the "
                        + "label/control image.");
            sendMessage("StarDist input image: " + debugFinal
                    + (debugExists ? "" : " (not written - see stardist_server.log)"));
            if (nCells == 0)
                sendMessage("StarDist returned 0 cells for this frame. If you expected "
                        + "detections, check the [stardist_server] log and confirm the "
                        + "image is not blank/over-normalized.");

            // Build Table
            Table table = buildTable(json, detectionImage);
            sendTableToListeners(table);

            // Build label ImageEvent (optional)
            // Prefer the mask bytes returned inline over the socket (no file);
            // fall back to the sidecar file for the one-shot path.
            byte[] maskBytes = lastMaskBytes;
            String maskSource = "inline";
            if (maskBytes == null && tempMasks != null && Files.exists(tempMasks)) {
                try {
                    maskBytes = Files.readAllBytes(tempMasks);
                    maskSource = "sidecar-file";
                } catch (Exception e) {
                    maskBytes = null;
                }
            }
            ImageEvent<?> labelImage = null;
            if (cfg.isGenerateLabelImage() && nCells > 0
                    && maskBytes != null && maskBytes.length >= 8) {
                labelImage = buildLabelImage(maskBytes, detectionImage);
            } else {
                appendServerLog("Label/control image not generated: "
                        + (!cfg.isGenerateLabelImage()
                                ? "'generate label image' is off in the config."
                                : nCells == 0 ? "no cells were detected."
                                        : "no segmentation mask was returned (see above)."));
            }
            if (cfg.isGenerateLabelImage() && nCells > 0 && labelImage == null)
                appendServerLog("Label/control image could not be built - "
                        + "no segmentation/control image will be saved this frame.");

            return new CellDetectionResult(table, labelImage);
        } catch (CellDetectionException e) {
            throw e;
        } catch (Exception e) {
            throw new CellDetectionException(
                    "StarDist bridge failed: " + e.getMessage(), e);
        } finally {
            deleteSilently(tempImage);
            deleteSilently(tempResult);
            deleteSilently(tempArgs);
            deleteSilently(tempMasks);
        }
    }

    // Persistent server communication

    /**
     * Ensures the persistent stardist_server.py is running, starting it if
     * necessary. Returns true if the server is confirmed reachable, false
     * if it could not be started (caller should fall back to one-shot mode).
     */
    /**
     * Sends a one-line control command ({"cmd":"..."}) to the server and returns
     * the response JSON (or null). Used for ping/shutdown handshakes.
     */
    private String sendServerCommand(String host, int port, String cmd, int timeoutMs) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
            s.setSoTimeout(timeoutMs);
            byte[] req = ("{\"cmd\":\"" + cmd + "\"}").getBytes("UTF-8");
            java.io.OutputStream os = s.getOutputStream();
            os.write(intToBytes(req.length));
            os.write(req);
            os.flush();
            java.io.InputStream in = s.getInputStream();
            int len = bytesToInt(readExact(in, 4));
            return new String(readExact(in, len), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the SCRIPT_VERSION reported by a server on the port, or null. */
    private String pingServerVersion(String host, int port) {
        String r = sendServerCommand(host, port, "ping", 4000);
        return r == null ? null : parseString(r, "script_version");
    }

    /**
     * Forces a fresh server for the next frame: asks the current one to shut down
     * (so a stale/incompatible server releases the port) and drops our handle so
     * {@link #startServerIfNeeded()} re-scans the ports next time.
     */
    private void restartServer() {
        String host = getConfiguration().getServerHost();
        int port = (activePort > 0) ? activePort : getConfiguration().getServerPort();
        try {
            sendServerCommand(host, port, "shutdown", 4000);
        } catch (Exception ignored) {
        }
        synchronized (serverLock) {
            if (serverProcess != null) {
                try {
                    serverProcess.destroy();
                } catch (Exception ignored) {
                }
                serverProcess = null;
            }
            activePort = -1;
        }
    }

    /**
     * Registers (once) a JVM shutdown hook that stops the StarDist server(s) when
     * YouScope exits, so no Python server keeps running in the background. It asks
     * every port in the scan range to shut down and force-kills the process we
     * started. (If YouScope is killed rather than exited cleanly, the hook cannot
     * run; the server tolerates that now - it writes to a file after startup, so a
     * dead stdout reader no longer breaks it, and it is reused safely next time.)
     */
    private void registerServerShutdownHook() {
        synchronized (serverLock) {
            if (shutdownHookRegistered)
                return;
            shutdownHookRegistered = true;
        }
        final String host = getConfiguration().getServerHost();
        final int basePort = getConfiguration().getServerPort();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int p = basePort; p < basePort + PORT_SCAN_RANGE; p++) {
                try {
                    sendServerCommand(host, p, "shutdown", 1200);
                } catch (Exception ignored) {
                }
            }
            Process pr = serverProcess;
            if (pr != null) {
                try {
                    pr.destroyForcibly();
                } catch (Exception ignored) {
                }
            }
        }, "stardist-server-shutdown"));
    }

    /** Pure port check (no dependency on our own serverProcess handle). */
    private boolean portOpen(String host, int port) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 800);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static final int PORT_SCAN_RANGE = 6;

    private boolean startServerIfNeeded() {
        registerServerShutdownHook();
        String host = getConfiguration().getServerHost();
        int basePort = getConfiguration().getServerPort();
        boolean local = host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost");

        // Remote host: never auto-start; just verify a server is reachable.
        if (!local) {
            if (portOpen(host, basePort)) {
                activePort = basePort;
                return true;
            }
            sendMessage("No StarDist server reachable at " + host + ":" + basePort
                    + " (remote host is not auto-started). Start stardist_server.py "
                    + "on that machine with --host 0.0.0.0 --port " + basePort + ".");
            return false;
        }

        synchronized (serverLock) {
            // Fast path: our verified server is still up and the right version.
            if (activePort > 0 && serverProcess != null && serverProcess.isAlive()
                    && portOpen(host, activePort)
                    && PLUGIN_VERSION.equals(pingServerVersion(host, activePort)))
                return true;

            // Scan a small range of ports for a matching server, or a free slot to
            // start one. This steps past a base port that is held by a stale server
            // we cannot shut down (older versions have no shutdown command).
            for (int port = basePort; port < basePort + PORT_SCAN_RANGE; port++) {
                if (portOpen(host, port)) {
                    String v = pingServerVersion(host, port);
                    if (PLUGIN_VERSION.equals(v)) {
                        activePort = port;
                        appendServerLog("Using running StarDist server " + PLUGIN_VERSION
                                + " on port " + port + ".");
                        return true;
                    }
                    // Wrong / unknown version: ask it to shut down, then wait.
                    appendServerLog("Port " + port + " has a StarDist server reporting "
                            + (v == null ? "an unknown/old version" : "version " + v)
                            + " (need " + PLUGIN_VERSION + "); asking it to shut down.");
                    sendServerCommand(host, port, "shutdown", 3000);
                    boolean freed = false;
                    for (int i = 0; i < 30; i++) { // up to ~4.5s
                        if (!portOpen(host, port)) {
                            freed = true;
                            break;
                        }
                        try {
                            Thread.sleep(150);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    if (!freed) {
                        appendServerLog("Port " + port + " is still held by a server that "
                                + "will not shut down (likely a pre-" + PLUGIN_VERSION
                                + " build). Trying the next port. To clear it, run "
                                + "shutdown_stardist.py or kill the python processes.");
                        continue; // try next port
                    }
                }
                // Port is free then start a fresh, matching server here and verify it.
                if (startFreshServerOnPort(host, port)) {
                    activePort = port;
                    return true;
                }
                // else: could not start/verify here; try the next port
            }
            sendMessage("Could not start a matching StarDist server on ports " + basePort
                    + "-" + (basePort + PORT_SCAN_RANGE - 1) + ". Using one-shot mode "
                    + "for this call. (Stale servers? Run shutdown_stardist.py or kill "
                    + "python, then retry.)");
            return false;
        }
    }

    /**
     * Starts a fresh server on the given port, waits for READY (failing fast if the
     * process dies, e.g. out-of-memory), then pings to confirm it is our version.
     * Returns true only when a matching server is confirmed answering.
     */
    private boolean startFreshServerOnPort(String host, int port) {
        try {
            sendMessage("Starting persistent StarDist server " + PLUGIN_VERSION
                    + " on port " + port + " (model stays loaded between calls)...");
            ProcessBuilder pb = new ProcessBuilder(
                    cpythonExe, "-u", serverScript,
                    "--host", "127.0.0.1",
                    "--port", String.valueOf(port),
                    "--model", getConfiguration().getModelType(),
                    "--gpu", String.valueOf(getConfiguration().isUseGpu()),
                    "--preload");
            pb.environment().put("PYTHONUNBUFFERED", "1");
            pb.environment().put("STARDIST_NUM_THREADS",
                    String.valueOf(getConfiguration().getCpuCores()));
            if (workingDir != null)
                pb.directory(new File(workingDir));
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            serverProcess = proc;

            final java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(1);
            appendServerLog("==== StarDist server starting: model="
                    + getConfiguration().getModelType() + " gpu="
                    + getConfiguration().isUseGpu() + " port=" + port + " @ "
                    + new java.util.Date() + " ====");
            Thread logThread = new Thread(() -> {
                try (java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        sendMessage("[stardist_server] " + line);
                        appendServerLog(line);
                        if (line.contains("READY"))
                            ready.countDown();
                    }
                } catch (IOException ignored) {
                }
            });
            logThread.setDaemon(true);
            logThread.start();

            boolean started = false;
            long deadline = System.currentTimeMillis() + SERVER_START_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                if (ready.await(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    started = true;
                    break;
                }
                if (!proc.isAlive()) {
                    appendServerLog("StarDist server on port " + port + " exited before "
                            + "READY (exit code " + proc.exitValue() + ") - see the "
                            + "traceback above (often out-of-memory, or the port was "
                            + "already taken). Trying the next port / one-shot.");
                    return false;
                }
            }
            if (!started) {
                sendMessage("StarDist server did not signal READY within "
                        + (SERVER_START_TIMEOUT_MS / 1000) + "s on port " + port + ".");
                return false;
            }
            // Confirm the server that answers on this port is really ours before use,
            // so we never proceed against a stale server mid-restart.
            for (int i = 0; i < 10; i++) {
                if (PLUGIN_VERSION.equals(pingServerVersion(host, port)))
                    return true;
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {
                }
            }
            appendServerLog("Server on port " + port + " signalled READY but does not "
                    + "report version " + PLUGIN_VERSION + " (a stale server may share "
                    + "the port). Trying the next port.");
            return false;
        } catch (Exception e) {
            sendErrorMessage("Could not start StarDist server on port " + port + ": "
                    + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Quick check whether a StarDist server is accepting connections at host:port.
     */
    private boolean isPortAlive(String host, int port) {
        // A remote server has no local Process handle, so only port-check for it.
        boolean local = host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost");
        if (local && (serverProcess == null || !serverProcess.isAlive()))
            return false;
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 800);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Sends a detection request to the persistent server over a local TCP
     * socket and returns the JSON response. Much faster than spawning a new
     * process per call because the StarDist model stays loaded in memory.
     */
    private String callServer(String imagePath, String masksOut, StarDistConfiguration cfg)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"image_bin\":\"").append(escJ(imagePath)).append("\",");
        sb.append("\"model\":\"").append(escJ(cfg.getModelType())).append("\",");
        sb.append("\"prob_thresh\":").append(cfg.getProbThreshold()).append(",");
        sb.append("\"nms_thresh\":").append(cfg.getNmsThreshold()).append(",");
        sb.append("\"n_tiles\":").append(cfg.getNTiles()).append(",");
        sb.append("\"percentile_low\":").append(cfg.getPercentileLow()).append(",");
        sb.append("\"percentile_high\":").append(cfg.getPercentileHigh()).append(",");
        sb.append("\"gpu\":").append(cfg.isUseGpu()).append(",");
        sb.append("\"invert\":").append(cfg.isInvert()).append(",");
        sb.append("\"downscale\":").append(cfg.getDownscaleFactor()).append(",");
        String outDir = currentOutputDir();
        if (outDir != null)
            sb.append("\"debug_dir\":\"").append(escJ(outDir)).append("\",");
        sb.append("\"generate_label\":").append(cfg.isGenerateLabelImage());
        if (masksOut != null)
            sb.append(",\"masks_bin_out\":\"").append(escJ(masksOut)).append("\"");
        sb.append("}");
        byte[] request = sb.toString().getBytes("UTF-8");

        int usePort = (activePort > 0) ? activePort : cfg.getServerPort();
        try (java.net.Socket socket = new java.net.Socket(cfg.getServerHost(), usePort)) {
            socket.setSoTimeout(detectionTimeoutMs); // configurable; default 15 min
            java.io.OutputStream out = socket.getOutputStream();
            out.write(intToBytes(request.length));
            out.write(request);
            out.flush();

            java.io.InputStream in = socket.getInputStream();
            try {
                byte[] lenBuf = readExact(in, 4);
                int len = bytesToInt(lenBuf);
                byte[] respBuf = readExact(in, len);
                return new String(respBuf, "UTF-8");
            } catch (java.net.SocketTimeoutException te) {
                throw new IOException("No response from StarDist server within "
                        + (detectionTimeoutMs / 1000) + " s. The '2D_versatile_he' model on CPU "
                        + "can take several minutes per image. Increase "
                        + "'stardist_timeout_seconds' in graalpy_config.txt, enable GPU, "
                        + "or reduce image size. Check the [stardist_server] log lines "
                        + "for inference progress.", te);
            }
        }
    }

    /** Fallback: one-shot subprocess that reloads the model every call (slow). */
    private String callOneShotSubprocess(Path tempArgs, Path tempResult)
            throws IOException, InterruptedException, CellDetectionException {
        ProcessBuilder pb = new ProcessBuilder(cpythonExe, "-u", bridgeScript);
        pb.environment().put("YOUSCOPE_ARGS_FILE", tempArgs.toString());
        pb.environment().put("PYTHONUNBUFFERED", "1");
        pb.environment().put("STARDIST_NUM_THREADS",
                String.valueOf(getConfiguration().getCpuCores()));
        if (workingDir != null)
            pb.directory(new File(workingDir));
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        InputStream is = proc.getInputStream();
        int n;
        while ((n = is.read(buf)) != -1)
            baos.write(buf, 0, n);
        int exitCode = proc.waitFor();

        String procOut = baos.toString("UTF-8").trim();
        if (procOut.length() > 0) {
            sendMessage("StarDist: " + procOut);
            appendServerLog("[one-shot] " + procOut.replace("\n", " | "));
        }

        if (exitCode != 0)
            throw new CellDetectionException(
                    "StarDist bridge exited with code " + exitCode
                            + ". Output: " + procOut.substring(Math.max(0, procOut.length() - 400)));

        byte[] resultBytes = Files.readAllBytes(tempResult);
        return new String(resultBytes, "UTF-8").trim();
    }

    private static byte[] intToBytes(int v) {
        return new byte[] {
                (byte) ((v >> 24) & 0xFF), (byte) ((v >> 16) & 0xFF),
                (byte) ((v >> 8) & 0xFF), (byte) (v & 0xFF) };
    }

    private static int bytesToInt(byte[] b) {
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }

    private static byte[] readExact(java.io.InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r < 0)
                throw new IOException("Connection closed while reading");
            off += r;
        }
        return buf;
    }

    // Table / image builders

    private Table buildTable(String json, ImageEvent<?> srcImage)
            throws TableException {
        TableDefinition def = StarDistTable.getTableDefinition();
        Table table = new Table(def,
                srcImage.getCreationTime(),
                srcImage.getPositionInformation(),
                srcImage.getExecutionInformation());

        // Parse cell_props array. Locate the key, then the first '[' after it
        // (tolerating a space after the colon, e.g. json.dumps output).
        int keyIdx = json.indexOf("\"cell_props\":");
        if (keyIdx < 0)
            return table;
        int arrStart = json.indexOf("[", keyIdx);
        if (arrStart < 0)
            return table;
        arrStart += 1;
        int arrEnd = findMatchingBracket(json, arrStart - 1);
        if (arrEnd < 0)
            return table;

        String arrStr = json.substring(arrStart, arrEnd).trim();
        if (arrStr.isEmpty())
            return table;

        // Split on top-level }{ to get individual cell objects
        int pos = 0;
        while (pos < arrStr.length()) {
            int objStart = arrStr.indexOf("{", pos);
            if (objStart < 0)
                break;
            int objEnd = findMatchingBracket(arrStr, objStart);
            if (objEnd < 0)
                break;
            String obj = arrStr.substring(objStart + 1, objEnd);

            long cellId = parseLong(obj, "id");
            double cx = parseDouble(obj, "center_x");
            double cy = parseDouble(obj, "center_y");
            long area = parseLong(obj, "area");
            double perim = parseDouble(obj, "perimeter");

            // Segment-only: mask and core geometry only. Column order must match
            // StarDistTable.getTableDefinition(): cell.index, cell.center.x,
            // cell.center.y, cell.area, cell.perimeter. Per-cell intensity
            // (mean/total) is no longer emitted here. The selected quantification
            // plugin measures it from this mask. cell.index is a (required,
            // non-null) Integer; the rest are Doubles. These are the standard
            // identifiers the LabelVisualizer reads to draw the "Cell N" / "Area"
            // labels on the control image. (The server may still return intensity
            // fields; they are simply ignored here.)
            table.addRow(
                    Integer.valueOf((int) cellId),
                    Double.valueOf(cx),
                    Double.valueOf(cy),
                    Double.valueOf((double) area),
                    perim > 0 ? Double.valueOf(perim) : null);
            pos = objEnd + 1;
        }
        return table;
    }

    /**
     * Reads the uint16 label image from the binary sidecar the Python side
     * wrote. Layout (big-endian): [w:int32][h:int32][w*h * uint16], which is
     * already the byte order YouScope 16-bit images use, so the pixel bytes are
     * copied through without a per-pixel loop or JSON parsing.
     */
    private static byte[] decodeB64(String s) {
        if (s == null || s.length() == 0)
            return null;
        try {
            return java.util.Base64.getDecoder().decode(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** base64-decode then zlib-inflate a packed payload from the server. */
    private static byte[] unpack(String b64) {
        byte[] comp = decodeB64(b64);
        if (comp == null)
            return null;
        try {
            java.util.zip.Inflater inf = new java.util.zip.Inflater();
            inf.setInput(comp);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(Math.max(64, comp.length * 4));
            byte[] buf = new byte[65536];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0) {
                    if (inf.finished() || inf.needsDictionary())
                        break;
                    if (inf.needsInput())
                        break;
                }
                bos.write(buf, 0, n);
            }
            inf.end();
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Writes the preprocessed input image the server returned inline
     * ([w:int32][h:int32][w*h*uint8], big-endian dims) as a grayscale PNG at
     * {@code outPath}. Java-side and ImageIO so it works regardless of the Python
     * process's file permissions. Returns true on success.
     */
    private boolean writeDebugPng(byte[] blob, String outPath) {
        try {
            if (blob == null || blob.length < 8)
                return false;
            int w = ((blob[0] & 0xFF) << 24) | ((blob[1] & 0xFF) << 16)
                    | ((blob[2] & 0xFF) << 8) | (blob[3] & 0xFF);
            int h = ((blob[4] & 0xFF) << 24) | ((blob[5] & 0xFF) << 16)
                    | ((blob[6] & 0xFF) << 8) | (blob[7] & 0xFF);
            if (w <= 0 || h <= 0 || blob.length < 8L + (long) w * h)
                return false;
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
                    w, h, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
            byte[] px = new byte[w * h];
            System.arraycopy(blob, 8, px, 0, w * h);
            bi.getRaster().setDataElements(0, 0, w, h, px);
            return javax.imageio.ImageIO.write(bi, "png", new File(outPath));
        } catch (Throwable t) {
            appendServerLog("stardist_last_input.png write failed: " + t.getMessage());
            return false;
        }
    }

    private ImageEvent<?> buildLabelImage(byte[] file, ImageEvent<?> srcImage) {
        try {
            if (file == null || file.length < 8) {
                appendServerLog("label image: no mask data ("
                        + (file == null ? "null" : file.length + " bytes") + ")");
                return null;
            }
            int fw = ((file[0] & 0xFF) << 24) | ((file[1] & 0xFF) << 16)
                    | ((file[2] & 0xFF) << 8) | (file[3] & 0xFF);
            int fh = ((file[4] & 0xFF) << 24) | ((file[5] & 0xFF) << 16)
                    | ((file[6] & 0xFF) << 8) | (file[7] & 0xFF);
            long expected = (long) fw * fh * 2;
            if (fw <= 0 || fh <= 0 || file.length < 8L + expected) {
                appendServerLog("label image: bad sidecar (fw=" + fw + " fh=" + fh
                        + " bytes=" + file.length + ")");
                return null;
            }
            int n = fw * fh;

            // Determine the largest label so we can pick the smallest pixel type
            // the visualizers support. LabelVisualizer accepts 1 byte/pixel
            // (byte[]) or 2 bytes/pixel (short[]) only, and casts getImageData()
            // to that exact array type so the array type must match the depth.
            int maxLabel = 0;
            for (int i = 0, o = 8; i < n; i++, o += 2) {
                int v = ((file[o] & 0xFF) << 8) | (file[o + 1] & 0xFF);
                if (v > maxLabel)
                    maxLabel = v;
            }

            ImageEvent<?> label;
            if (maxLabel <= 255) {
                // 8-bit byte[] label map (matches CellX-style output; the low byte
                // is the id since ids <= 255). Simplest path the visualizer accepts.
                byte[] px = new byte[n];
                for (int i = 0; i < n; i++)
                    px[i] = file[8 + 2 * i + 1]; // low byte of big-endian uint16
                label = ImageEvent.createImage(px, fw, fh, 8);
                appendServerLog("Label/control image built: " + fw + "x" + fh
                        + ", " + maxLabel + " labelled cells (8-bit). Segmentation "
                        + "and control images will be saved.");
            } else {
                // 16-bit short[] label map for > 255 cells.
                short[] labels = new short[n];
                for (int i = 0, o = 8; i < n; i++, o += 2)
                    labels[i] = (short) (((file[o] & 0xFF) << 8) | (file[o + 1] & 0xFF));
                label = ImageEvent.createImage(labels, fw, fh, 16);
                appendServerLog("Label/control image built: " + fw + "x" + fh
                        + ", " + maxLabel + " labelled cells (16-bit). Segmentation "
                        + "and control images will be saved.");
            }
            label.setPositionInformation(srcImage.getPositionInformation());
            label.setExecutionInformation(srcImage.getExecutionInformation());
            return label;
        } catch (Throwable e) {
            // Log to stardist_server.log too (sendErrorMessage may not be wired to
            // the measurement log in every configuration).
            appendServerLog("label image: FAILED to build - "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
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
        return StarDistTable.getTableDefinition();
    }

    private void sendTableToListeners(Table table) {
        synchronized (tableListeners) {
            for (Iterator<TableListener> it = tableListeners.iterator(); it.hasNext();) {
                try {
                    it.next().newTableProduced(table.clone());
                } catch (RemoteException e) {
                    it.remove();
                }
            }
        }
    }

    // Utilities

    /**
     * Best-effort location of this measurement's results folder (where images and
     * cell-table.csv are written), so reproducibility files can live alongside
     * them. YouScope does not hand a detection *resource* its save path directly
     * (only jobs get a MeasurementSaver via ConstructionContext), so this probes
     * the MeasurementContext reflectively: any of getMeasurementSaver()/getSaver()
     * -> getLastMeasurementFileLocations() -> getMeasurementBaseFolder(). All via
     * reflection so it compiles regardless of the exact API and degrades to null
     * if unavailable. Returns an absolute folder path, or null.
     */
    private String resolveMeasurementFolder() {
        // Preferred: the MeasurementSaver captured at construction. Queried lazily
        // (at detection time the measurement is RUNNING, so the folder is final).
        if (measurementSaver != null) {
            try {
                Object locs = measurementSaver.getLastMeasurementFileLocations();
                if (locs != null) {
                    Object folder = tryInvoke(locs, "getMeasurementBaseFolder");
                    if (folder == null)
                        folder = tryInvoke(locs, "getMeasurementFolder");
                    if (folder != null) {
                        File f = new File(folder.toString());
                        if (f.isDirectory() && f.canWrite())
                            return f.getAbsolutePath();
                    }
                }
            } catch (Throwable t) {
                /* fall through to reflection / null */
            }
        }
        // Fallback: probe the MeasurementContext reflectively (older API shapes).
        if (measurementContext == null)
            return null;
        try {
            Object saver = tryInvoke(measurementContext, "getMeasurementSaver");
            if (saver == null)
                saver = tryInvoke(measurementContext, "getSaver");
            if (saver == null)
                return null;
            Object locs = tryInvoke(saver, "getLastMeasurementFileLocations");
            if (locs == null)
                return null;
            Object folder = tryInvoke(locs, "getMeasurementBaseFolder");
            if (folder == null)
                folder = tryInvoke(locs, "getMeasurementFolder");
            if (folder == null)
                return null;
            File f = new File(folder.toString());
            return (f.isDirectory() && f.canWrite()) ? f.getAbsolutePath() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object tryInvoke(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Directory for this frame's reproducibility outputs: the measurement folder
     * if locatable, else the working dir. Copies the env snapshot + requirements
     * into it once per measurement so they sit next to the images / cell-table.
     */
    private String currentOutputDir() {
        if (outputDir == null) {
            String mf = resolveMeasurementFolder();
            outputDir = (mf != null) ? mf : workingDir;
        }
        if (!logRelocated && outputDir != null && workingDir != null
                && !outputDir.equals(workingDir)) {
            // Bring the server log into the results folder and keep appending there.
            copyIfPresent(workingDir, outputDir, "stardist_server.log");
            serverLogPath = outputDir + File.separator + "stardist_server.log";
            appendServerLog("output dir resolved to measurement folder: " + outputDir);
            logRelocated = true;
        }
        return outputDir;
    }

    /**
     * Writes stardist_environment.txt / stardist_requirements.txt into the results
     * folder from the base64 text the server returns. Java-side write (reliable),
     * once per measurement.
     */
    private void writeReproFiles(String json) {
        if (reproFilesCopied)
            return;
        String dir = currentOutputDir();
        if (dir == null)
            return;
        boolean env = writeBytes(unpack(parseString(json, "env_b64")),
                dir + File.separator + "stardist_environment.txt");
        boolean req = writeBytes(unpack(parseString(json, "req_b64")),
                dir + File.separator + "stardist_requirements.txt");
        appendServerLog("Reproducibility files saved to results folder: "
                + "stardist_environment.txt=" + (env ? "yes" : "no")
                + ", stardist_requirements.txt=" + (req ? "yes" : "no")
                + " (" + dir + ")");
        if (env && req)
            reproFilesCopied = true;
    }

    private boolean writeBytes(byte[] data, String path) {
        if (data == null || data.length == 0)
            return false;
        try {
            Files.write(new File(path).toPath(), data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void copyIfPresent(String fromDir, String toDir, String name) {
        try {
            File src = new File(fromDir, name);
            if (src.exists())
                Files.copy(src.toPath(), new File(toDir, name).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            sendMessage("StarDistAddon: could not copy " + name + " to measurement "
                    + "folder: " + e.getMessage());
        }
    }

    /**
     * Extracts a bundled Python script from the plugin JAR to {@code targetPath},
     * ALWAYS overwriting any existing file so a rebuilt JAR's scripts are always
     * the ones that run (stale on-disk copies previously kept shadowing updates).
     */
    private void extractScript(String resourceName, String targetPath)
            throws ResourceException {
        File f = new File(targetPath);
        try {
            if (f.getParentFile() != null)
                f.getParentFile().mkdirs();
            InputStream is = StarDistAddon.class.getResourceAsStream(resourceName);
            if (is == null)
                throw new ResourceException(resourceName + " not found in plugin JAR");
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

    private static String readConfig(String dir, String key) {
        try {
            File cfg = new File(dir, "graalpy_config.txt");
            if (!cfg.exists())
                return null;
            for (String line : Files.readAllLines(cfg.toPath()))
                if (line.startsWith(key + "="))
                    return line.substring(key.length() + 1).trim();
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Appends a timestamped line to the persistent server log (best-effort). */
    private static synchronized void appendServerLog(String line) {
        String path = serverLogPath;
        if (path == null)
            return;
        try (java.io.FileWriter fw = new java.io.FileWriter(path, true);
                java.io.BufferedWriter bw = new java.io.BufferedWriter(fw)) {
            bw.write(System.currentTimeMillis() + "\t" + line);
            bw.newLine();
        } catch (IOException ignored) {
        }
    }

    private static int readConfigInt(String dir, String key, int defaultVal) {
        String raw = readConfig(dir, key);
        if (raw == null)
            return defaultVal;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultVal;
        }
    }

    private static void writeInt(byte[] b, int off, int v) {
        b[off] = (byte) ((v >> 24) & 0xFF);
        b[off + 1] = (byte) ((v >> 16) & 0xFF);
        b[off + 2] = (byte) ((v >> 8) & 0xFF);
        b[off + 3] = (byte) (v & 0xFF);
    }

    private static String escJ(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void deleteSilently(Path p) {
        if (p != null)
            try {
                Files.deleteIfExists(p);
            } catch (Exception ignored) {
            }
    }

    /** Find the matching closing bracket for '[' or '{' at pos. */
    private static int findMatchingBracket(String s, int pos) {
        char open = s.charAt(pos);
        char close = open == '[' ? ']' : '}';
        int depth = 0;
        for (int i = pos; i < s.length(); i++) {
            if (s.charAt(i) == open)
                depth++;
            if (s.charAt(i) == close) {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return -1;
    }

    private static int parseInt(String json, String key) {
        return (int) parseLong(json, key);
    }

    private static long parseLong(String json, String key) {
        String m = "\"" + key + "\":";
        int idx = json.indexOf(m);
        if (idx < 0)
            return 0;
        int s = skipWs(json, idx + m.length()); // tolerate space after colon
        int e = s;
        while (e < json.length() &&
                (Character.isDigit(json.charAt(e)) || json.charAt(e) == '-'))
            e++;
        try {
            return Long.parseLong(json.substring(s, e).trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static double parseDouble(String json, String key) {
        String m = "\"" + key + "\":";
        int idx = json.indexOf(m);
        if (idx < 0)
            return 0.0;
        int s = skipWs(json, idx + m.length()); // tolerate space after colon
        int e = s;
        while (e < json.length() &&
                (Character.isDigit(json.charAt(e)) || json.charAt(e) == '-'
                        || json.charAt(e) == '.' || json.charAt(e) == 'e'
                        || json.charAt(e) == 'E' || json.charAt(e) == '+'))
            e++;
        try {
            return Double.parseDouble(json.substring(s, e).trim());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    /**
     * Best-effort raw value (object or scalar) for a key, used only for logging.
     */
    private static int skipWs(String json, int i) {
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r')
                i++;
            else
                break;
        }
        return i;
    }

    /** Extracts the raw [...] array text for a key (whitespace-tolerant). */
    private static String extractArray(String json, String key) {
        int idx = json.indexOf("\"" + key + "\":");
        if (idx < 0)
            return null;
        int s = json.indexOf("[", idx);
        if (s < 0)
            return null;
        int depth = 0;
        for (int i = s; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[')
                depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0)
                    return json.substring(s, i + 1);
            }
        }
        return null;
    }

    private static String extractRaw(String json, String key) {
        String m = "\"" + key + "\":";
        int idx = json.indexOf(m);
        if (idx < 0)
            return "";
        int s = skipWs(json, idx + m.length()); // tolerate space after colon
        if (s >= json.length())
            return "";
        if (json.charAt(s) == '{') {
            int e = findMatchingBracket(json, s);
            return e < 0 ? "" : json.substring(s, e + 1);
        }
        int e = s;
        while (e < json.length() && json.charAt(e) != ',' && json.charAt(e) != '}')
            e++;
        return json.substring(s, e).trim();
    }

    private static String parseString(String json, String key) {
        String m = "\"" + key + "\":";
        int idx = json.indexOf(m);
        if (idx < 0)
            return null;
        int s = skipWs(json, idx + m.length()); // tolerate space after colon
        if (s >= json.length() || json.charAt(s) != '"')
            return null;
        s++; // opening quote of value
        int e = json.indexOf("\"", s);
        return e < 0 ? null : json.substring(s, e);
    }
}