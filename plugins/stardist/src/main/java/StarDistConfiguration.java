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
package org.youscope.plugin.stardist;

import org.youscope.addon.celldetection.CellDetectionConfiguration;
import org.youscope.common.table.TableDefinition;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;

/**
 * Configuration for the StarDist cell-detection addon.
 *
 * <p>Only parameters that StarDist's {@code StarDist2D.predict_instances(...)}
 * actually understands are exposed:
 * <ul>
 *   <li><b>model</b>: A pretrained model name ({@code 2D_versatile_fluo},
 *       {@code 2D_versatile_he}, {@code 2D_paper_dsb2018}) or a path to a
 *       custom user-trained model folder.</li>
 *   <li><b>prob_thresh</b>: Object-probability threshold (0..1). Higher =
 *       fewer, more confident detections. -1 = use the model's optimized
 *       default.</li>
 *   <li><b>nms_thresh</b>: Non-maximum-suppression / overlap threshold (0..1).
 *       Higher = allows more overlap between neighbouring cells. -1 = use the
 *       model's optimized default.</li>
 *   <li><b>n_tiles</b>: Number of tiles per axis for large frames; keeps GPU/CPU
 *       memory bounded. 0/1 = no tiling.</li>
 *   <li><b>percentiles</b>: Low/high percentiles for StarDist's standard
 *       intensity normalization (defaults 1.0 / 99.8).</li>
 * </ul>
 *
 * <p>The remaining fields (GPU, CPU cores, timeout, invert, downscale,
 * persistent server, host/port) are execution/pre-processing options, not
 * StarDist model parameters.
 */
@XStreamAlias("stardist")
public class StarDistConfiguration extends CellDetectionConfiguration {
    private static final long serialVersionUID = 2L;

    public static final String TYPE_IDENTIFIER = "YouScope.StarDist";

    /** Sentinel: a threshold &lt; 0 means "let StarDist use the model's own
     *  optimized default" instead of overriding it. */
    public static final double USE_MODEL_DEFAULT = -1.0;

    /** Pre-trained model name or path to a custom user-trained model folder. */
    @XStreamAlias("model-type")
    @XStreamAsAttribute
    private String modelType = "2D_versatile_fluo";

    /**
     * Object probability threshold (0..1). Detections whose predicted
     * probability is below this are discarded; higher values yield fewer but
     * more confident cells. {@link #USE_MODEL_DEFAULT} (-1) uses the value that
     * ships with the (pretrained or custom) model.
     */
    @XStreamAlias("prob-threshold")
    @XStreamAsAttribute
    private double probThreshold = USE_MODEL_DEFAULT;

    /**
     * Non-maximum-suppression / overlap threshold (0..1). Controls how much
     * neighbouring detections may overlap before one is suppressed. Higher =
     * more overlap allowed. {@link #USE_MODEL_DEFAULT} (-1) uses the model's
     * own optimized value.
     */
    @XStreamAlias("nms-threshold")
    @XStreamAsAttribute
    private double nmsThreshold = USE_MODEL_DEFAULT;

    /**
     * Number of tiles per image axis used during prediction. Large frames are
     * split into this many tiles per axis so a full frame fits in GPU/CPU
     * memory (the result is stitched back together). 0 or 1 = no tiling.
     */
    @XStreamAlias("n-tiles")
    @XStreamAsAttribute
    private int nTiles = 1;

    /**
     * Low percentile for StarDist's intensity normalization (csbdeep
     * {@code normalize}). Default 1.0. Together with the high percentile this
     * is the main "make the model see the cells" knob for both pretrained and
     * custom models.
     */
    @XStreamAlias("percentile-low")
    @XStreamAsAttribute
    private double percentileLow = 1.0;

    /** High percentile for intensity normalization. Default 99.8. */
    @XStreamAlias("percentile-high")
    @XStreamAsAttribute
    private double percentileHigh = 99.8;

    /** Whether to use GPU acceleration (requires a CUDA-enabled torch/tensorflow). */
    @XStreamAlias("use-gpu")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean useGpu = false;

    /** Whether to produce a label/mask image. */
    @XStreamAlias("generate-label-image")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean generateLabelImage = true;

    /**
     * Whether to use a persistent background server process that keeps the
     * StarDist model loaded in memory between calls. Strongly recommended
     * for live-cell / feedback microscopy with short imaging cycles, since
     * loading the model fresh on every call can take several seconds.
     * When false, a new Python process is started for every detection
     * (simpler, more isolated, but slower for repeated calls).
     */
    @XStreamAlias("use-persistent-server")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean usePersistentServer = true;

    /**
     * Maximum time (seconds) to wait for a single detection to return before
     * treating it as a timeout. On CPU a large frame can take a while, so the
     * default is deliberately generous. Set 0 to fall back to the global
     * "stardist_timeout_seconds" in graalpy_config.txt.
     */
    @XStreamAlias("detection-timeout-seconds")
    @XStreamAsAttribute
    private int detectionTimeoutSeconds = 900;

    /**
     * Number of CPU cores (threads) the StarDist Python process may use. 0 = all
     * available cores (the default). Lower it to leave headroom for other tasks
     * or to reduce memory pressure during model load / inference.
     */
    @XStreamAlias("cpu-cores")
    @XStreamAsAttribute
    private int cpuCores = 0;

    /**
     * Invert image contrast before segmentation. StarDist (including
     * {@code 2D_versatile_he}) expects cells to be LIGHT on a DARK background,
     * like fluorescence. Brightfield cells are usually the opposite
     * (dark / haloed on a bright field), so inverting often makes faint
     * brightfield cells detectable.
     */
    @XStreamAlias("invert")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean invert = false;

    /**
     * Downscale factor applied before inference (masks are upscaled back).
     * 1.0 = full resolution. 0.5 = quarter the pixels ~ ~4x faster on CPU.
     */
    @XStreamAlias("downscale-factor")
    @XStreamAsAttribute
    private double downscaleFactor = 1.0;

    /**
     * Host of the StarDist server. "127.0.0.1" runs/uses a local server. Set to
     * the hostname/IP of a remote GPU box (running stardist_server.py bound to
     * 0.0.0.0) to offload inference; a non-local host is never auto-started.
     */
    @XStreamAlias("server-host")
    @XStreamAsAttribute
    private String serverHost = "127.0.0.1";

    @XStreamAlias("server-port")
    @XStreamAsAttribute
    private int serverPort = 59712;

    // Model / detection parameters

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String v) {
        this.modelType = v;
    }

    /** Object probability threshold (0..1); -1 = use model default. */
    public double getProbThreshold() {
        return probThreshold;
    }

    public void setProbThreshold(double v) {
        this.probThreshold = v;
    }

    /** NMS / overlap threshold (0..1); -1 = use model default. */
    public double getNmsThreshold() {
        return nmsThreshold;
    }

    public void setNmsThreshold(double v) {
        this.nmsThreshold = v;
    }

    /** Tiles per axis for large frames; 0/1 = no tiling. */
    public int getNTiles() {
        return nTiles;
    }

    public void setNTiles(int v) {
        this.nTiles = v < 1 ? 1 : v;
    }

    public double getPercentileLow() {
        return percentileLow;
    }

    public void setPercentileLow(double v) {
        this.percentileLow = v;
    }

    public double getPercentileHigh() {
        return percentileHigh;
    }

    public void setPercentileHigh(double v) {
        this.percentileHigh = v;
    }

    // Execution / pre-processing options

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean v) {
        this.invert = v;
    }

    public double getDownscaleFactor() {
        return downscaleFactor;
    }

    public void setDownscaleFactor(double v) {
        this.downscaleFactor = v;
    }

    public String getServerHost() {
        return serverHost == null || serverHost.trim().isEmpty() ? "127.0.0.1" : serverHost.trim();
    }

    public void setServerHost(String v) {
        this.serverHost = v;
    }

    public int getServerPort() {
        return serverPort > 0 ? serverPort : 59712;
    }

    public void setServerPort(int v) {
        this.serverPort = v;
    }

    public int getDetectionTimeoutSeconds() {
        return detectionTimeoutSeconds;
    }

    public void setDetectionTimeoutSeconds(int v) {
        this.detectionTimeoutSeconds = v;
    }

    /** CPU cores for the StarDist process; 0 = all available. */
    public int getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(int v) {
        this.cpuCores = v < 0 ? 0 : v;
    }

    public boolean isUseGpu() {
        return useGpu;
    }

    public void setUseGpu(boolean v) {
        this.useGpu = v;
    }

    @Override
    public boolean isGenerateLabelImage() {
        return generateLabelImage;
    }

    public void setGenerateLabelImage(boolean v) {
        this.generateLabelImage = v;
    }

    public boolean isUsePersistentServer() {
        return usePersistentServer;
    }

    public void setUsePersistentServer(boolean v) {
        this.usePersistentServer = v;
    }

    @Override
    public String getTypeIdentifier() {
        return TYPE_IDENTIFIER;
    }

    @Override
    public TableDefinition getProducedTableDefinition() {
        return StarDistTable.getTableDefinition();
    }
}
