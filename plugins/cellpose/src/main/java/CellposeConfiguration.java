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
package org.youscope.plugin.cellpose;

import org.youscope.addon.celldetection.CellDetectionConfiguration;
import org.youscope.common.table.TableDefinition;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;

/**
 * Configuration for the Cellpose 4 cell-detection addon.
 */
@XStreamAlias("cellpose")
public class CellposeConfiguration extends CellDetectionConfiguration {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_IDENTIFIER = "YouScope.Cellpose";

    /** Pre-trained model name or path to custom model. */
    @XStreamAlias("model-type")
    @XStreamAsAttribute
    private String modelType = "cpsam";

    /** Expected cell diameter in pixels. 0 = auto-detect. */
    @XStreamAlias("cell-diameter")
    @XStreamAsAttribute
    private double cellDiameter = 0.0;

    /** Flow error threshold for mask reconstruction. */
    @XStreamAlias("flow-threshold")
    @XStreamAsAttribute
    private double flowThreshold = 0.4;

    /** Cell probability threshold. */
    @XStreamAlias("cellprob-threshold")
    @XStreamAsAttribute
    private double cellprobThreshold = 0.0;

    /** Whether to use GPU acceleration. */
    @XStreamAlias("use-gpu")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean useGpu = false;

    /** Whether to produce a label/mask image. */
    @XStreamAlias("generate-label-image")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean generateLabelImage = true;

    /** Whether to save the Cellpose flow image. */
    @XStreamAlias("save-flow-image")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean saveFlowImage = false;

    /**
     * Whether to use a persistent background server process that keeps the
     * Cellpose model loaded in memory between calls. Strongly recommended
     * for live-cell / feedback microscopy with short imaging cycles, since
     * loading the model fresh on every call can take 5-30+ seconds.
     * When false, a new Python process is started for every detection
     * (simpler, more isolated, but much slower for repeated calls).
     */
    @XStreamAlias("use-persistent-server")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean usePersistentServer = true;

    /**
     * Maximum time (seconds) to wait for a single detection to return before
     * treating it as a timeout. cpsam on CPU can take several minutes per
     * 2048x2048 frame, so the default is deliberately large. Set 0 to fall
     * back to the global "cellpose_timeout_seconds" in graalpy_config.txt.
     */
    @XStreamAlias("detection-timeout-seconds")
    @XStreamAsAttribute
    private int detectionTimeoutSeconds = 900;

    /**
     * Number of CPU cores (threads) the Cellpose Python process may use. 0 = all
     * available cores (the default). Lower it to leave headroom for other tasks
     * or to reduce memory pressure (e.g. if the machine runs out of memory / the
     * Windows paging file is too small during model load).
     */
    @XStreamAlias("cpu-cores")
    @XStreamAsAttribute
    private int cpuCores = 0;

    /**
     * Fast mode: disables Cellpose's resampling step (resample=False). Faster
     * on CPU at the cost of slightly coarser mask outlines. Useful for
     * feedback / event-based microscopy where per-cycle latency matters.
     */
    @XStreamAlias("fast-mode")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean fastMode = false;

    /**
     * Invert image contrast before segmentation. Brightfield cells are usually
     * darker/haloed on a bright background, the opposite of what Cellpose was
     * mostly trained on; inverting often makes faint brightfield cells detectable.
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
     * Host of the Cellpose server. "127.0.0.1" runs/uses a local server. Set to
     * the hostname/IP of a remote GPU box (running cellpose_server.py bound to
     * 0.0.0.0) to offload inference; a non-local host is never auto-started.
     */
    @XStreamAlias("server-host")
    @XStreamAsAttribute
    private String serverHost = "127.0.0.1";

    @XStreamAlias("server-port")
    @XStreamAsAttribute
    private int serverPort = 59712;

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

    @Override
    public String getTypeIdentifier() {
        return TYPE_IDENTIFIER;
    }

    public int getDetectionTimeoutSeconds() {
        return detectionTimeoutSeconds;
    }

    public void setDetectionTimeoutSeconds(int v) {
        this.detectionTimeoutSeconds = v;
    }

    /** CPU cores for the Cellpose process; 0 = all available. */
    public int getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(int v) {
        this.cpuCores = v < 0 ? 0 : v;
    }

    public boolean isFastMode() {
        return fastMode;
    }

    public void setFastMode(boolean v) {
        this.fastMode = v;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String v) {
        this.modelType = v;
    }

    /** Returns cell diameter (alias for cellDiameter, used by CellposeAddon). */
    public double getDiameter() {
        return cellDiameter;
    }

    public void setDiameter(double v) {
        this.cellDiameter = v;
    }

    public double getCellDiameter() {
        return cellDiameter;
    }

    public void setCellDiameter(double v) {
        this.cellDiameter = v;
    }

    public double getFlowThreshold() {
        return flowThreshold;
    }

    public void setFlowThreshold(double v) {
        this.flowThreshold = v;
    }

    public double getCellprobThreshold() {
        return cellprobThreshold;
    }

    public void setCellprobThreshold(double v) {
        this.cellprobThreshold = v;
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

    public boolean isSaveFlowImage() {
        return saveFlowImage;
    }

    public void setSaveFlowImage(boolean v) {
        this.saveFlowImage = v;
    }

    public boolean isUsePersistentServer() {
        return usePersistentServer;
    }

    public void setUsePersistentServer(boolean v) {
        this.usePersistentServer = v;
    }

    @Override
    public TableDefinition getProducedTableDefinition() {
        return CellposeTable.getTableDefinition();
    }
}