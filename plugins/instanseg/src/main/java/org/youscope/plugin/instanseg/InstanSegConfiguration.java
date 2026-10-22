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

import org.youscope.addon.celldetection.CellDetectionConfiguration;
import org.youscope.common.table.TableDefinition;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;

/**
 * Configuration for the InstanSeg cell-detection addon.
 *
 * <p>
 * InstanSeg (https://github.com/instanseg/instanseg) is an embedding-based
 * instance segmentation model for nuclei and whole cells. Threshold /
 * size overrides use <b>-1 = "use the model's own default"</b>, so an untouched
 * configuration reproduces InstanSeg's published behaviour.
 */
@XStreamAlias("instanseg")
public class InstanSegConfiguration extends CellDetectionConfiguration {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_IDENTIFIER = "YouScope.InstanSeg";

    /** Allowed values for {@link #getTarget()}. */
    public static final String TARGET_NUCLEI = "nuclei";
    public static final String TARGET_CELLS = "cells";

    /** Allowed values for {@link #getProcessingMode()}. */
    public static final String PROCESSING_AUTO = "auto";
    public static final String PROCESSING_WHOLE = "whole";
    public static final String PROCESSING_TILED = "tiled";

    /**
     * Built-in model name ("fluorescence_nuclei_and_cells", "brightfield_nuclei",
     * "single_channel_nuclei") or an absolute path to a folder containing a custom
     * TorchScript {@code instanseg.pt}.
     */
    @XStreamAlias("model-type")
    @XStreamAsAttribute
    private String modelType = "fluorescence_nuclei_and_cells";

    /** What the label image / table describe: "nuclei" or "cells". */
    @XStreamAlias("target")
    @XStreamAsAttribute
    private String target = TARGET_NUCLEI;

    /**
     * Pixel size of the detection image in micrometres per pixel. The image is
     * rescaled to the model's training resolution and the labels are scaled back.
     * 0 = do not rescale (use the image as-is).
     */
    @XStreamAlias("pixel-size-um")
    @XStreamAsAttribute
    private double pixelSizeUm = 0.0;

    /** Torch device: "cpu", "cuda", "mps" or "auto". */
    @XStreamAlias("device")
    @XStreamAsAttribute
    private String device = "cpu";

    /** "auto" (tile big images), "whole" or "tiled". */
    @XStreamAlias("processing-mode")
    @XStreamAsAttribute
    private String processingMode = PROCESSING_AUTO;

    @XStreamAlias("tile-size")
    @XStreamAsAttribute
    private int tileSize = 512;

    /** Percentile-normalise the image (InstanSeg's default and recommended). */
    @XStreamAlias("normalise")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean normalise = true;

    /**
     * Invert contrast before segmentation (e.g. for dark-on-bright brightfield).
     */
    @XStreamAlias("invert")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean invert = false;

    /**
     * Number of channels the grayscale detection image is replicated to.
     * 0 = automatic (3 for "brightfield" models, else 1).
     */
    @XStreamAlias("input-channels")
    @XStreamAsAttribute
    private int inputChannels = 0;

    // Post-processing overrides, -1 = model default
    @XStreamAlias("seed-threshold")
    @XStreamAsAttribute
    private double seedThreshold = -1;

    @XStreamAlias("overlap-threshold")
    @XStreamAsAttribute
    private double overlapThreshold = -1;

    @XStreamAlias("mask-threshold")
    @XStreamAsAttribute
    private double maskThreshold = -1;

    @XStreamAlias("min-size")
    @XStreamAsAttribute
    private int minSize = -1;

    @XStreamAlias("peak-distance")
    @XStreamAsAttribute
    private int peakDistance = -1;

    @XStreamAlias("cleanup-fragments")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean cleanupFragments = true;

    @XStreamAlias("resolve-cell-and-nucleus")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean resolveCellAndNucleus = true;

    /** Whether to produce a label/mask image. */
    @XStreamAlias("generate-label-image")
    @XStreamAsAttribute
    @XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
    private boolean generateLabelImage = true;

    /**
     * Max. time (seconds) to wait for one segmentation. Also used for the model
     * pre-load at measurement start, which downloads the weights on first use.
     * 0 = use "instanseg_timeout_seconds" from graalpy_config.txt (default 900).
     */
    @XStreamAlias("detection-timeout-seconds")
    @XStreamAsAttribute
    private int detectionTimeoutSeconds = 900;

    /** CPU threads for torch. 0 = all available. */
    @XStreamAlias("cpu-cores")
    @XStreamAsAttribute
    private int cpuCores = 0;

    @Override
    public String getTypeIdentifier() {
        return TYPE_IDENTIFIER;
    }

    public String getModelType() {
        return modelType == null || modelType.trim().isEmpty() ? "fluorescence_nuclei_and_cells" : modelType.trim();
    }

    public void setModelType(String v) {
        this.modelType = v;
    }

    public String getTarget() {
        return TARGET_CELLS.equals(target) ? TARGET_CELLS : TARGET_NUCLEI;
    }

    public void setTarget(String v) {
        this.target = v;
    }

    public double getPixelSizeUm() {
        return pixelSizeUm > 0 ? pixelSizeUm : 0.0;
    }

    public void setPixelSizeUm(double v) {
        this.pixelSizeUm = v;
    }

    public String getDevice() {
        return device == null || device.trim().isEmpty() ? "cpu" : device.trim();
    }

    public void setDevice(String v) {
        this.device = v;
    }

    public String getProcessingMode() {
        if (PROCESSING_WHOLE.equals(processingMode) || PROCESSING_TILED.equals(processingMode))
            return processingMode;
        return PROCESSING_AUTO;
    }

    public void setProcessingMode(String v) {
        this.processingMode = v;
    }

    public int getTileSize() {
        return tileSize >= 64 ? tileSize : 512;
    }

    public void setTileSize(int v) {
        this.tileSize = v;
    }

    public boolean isNormalise() {
        return normalise;
    }

    public void setNormalise(boolean v) {
        this.normalise = v;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean v) {
        this.invert = v;
    }

    public int getInputChannels() {
        return inputChannels < 0 ? 0 : inputChannels;
    }

    public void setInputChannels(int v) {
        this.inputChannels = v;
    }

    public double getSeedThreshold() {
        return seedThreshold;
    }

    public void setSeedThreshold(double v) {
        this.seedThreshold = v;
    }

    public double getOverlapThreshold() {
        return overlapThreshold;
    }

    public void setOverlapThreshold(double v) {
        this.overlapThreshold = v;
    }

    public double getMaskThreshold() {
        return maskThreshold;
    }

    public void setMaskThreshold(double v) {
        this.maskThreshold = v;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int v) {
        this.minSize = v;
    }

    public int getPeakDistance() {
        return peakDistance;
    }

    public void setPeakDistance(int v) {
        this.peakDistance = v;
    }

    public boolean isCleanupFragments() {
        return cleanupFragments;
    }

    public void setCleanupFragments(boolean v) {
        this.cleanupFragments = v;
    }

    public boolean isResolveCellAndNucleus() {
        return resolveCellAndNucleus;
    }

    public void setResolveCellAndNucleus(boolean v) {
        this.resolveCellAndNucleus = v;
    }

    @Override
    public boolean isGenerateLabelImage() {
        return generateLabelImage;
    }

    public void setGenerateLabelImage(boolean v) {
        this.generateLabelImage = v;
    }

    public int getDetectionTimeoutSeconds() {
        return detectionTimeoutSeconds;
    }

    public void setDetectionTimeoutSeconds(int v) {
        this.detectionTimeoutSeconds = v;
    }

    /** CPU threads for the InstanSeg process; 0 = all available. */
    public int getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(int v) {
        this.cpuCores = v < 0 ? 0 : v;
    }

    @Override
    public TableDefinition getProducedTableDefinition() {
        return InstanSegTable.getTableDefinition();
    }
}
