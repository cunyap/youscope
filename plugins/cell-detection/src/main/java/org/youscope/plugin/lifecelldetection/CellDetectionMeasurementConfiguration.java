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
package org.youscope.plugin.lifecelldetection;

import java.util.ArrayList;

import org.youscope.addon.celldetection.CellDetectionConfiguration;
import org.youscope.addon.celldetection.CellVisualizationConfiguration;
import org.youscope.addon.celldetection.QuantificationConfiguration;
import org.youscope.addon.celldetection.TrackingConfiguration;
import org.youscope.common.job.JobConfiguration;
import org.youscope.common.measurement.MeasurementConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;

/**
 * This class represents the configuration of a continuous measurement, for
 * which the cells in the images are life detected.
 * 
 * @author Moritz Lang and Andreas P. Cuny
 */
@XStreamAlias("cell-detection-measurement")
public class CellDetectionMeasurementConfiguration extends MeasurementConfiguration {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -6934063013699987401L;

	/**
	 * The identifier for this measurement type.
	 */
	public static final String TYPE_IDENTIFIER = "YouScope.ContinuousLifeCellDetectionMeasurement";

	/**
	 * The camera device to image with. Empty string = the microscope's default
	 * (standard) camera.
	 */
	@XStreamAlias("camera")
	private String camera = "";

	/**
	 * The channel where the images should be made. Set to an empty string if
	 * the current channel should be taken.
	 */
	@XStreamAlias("channel")
	private String channel = "";

	/**
	 * The config group where the channel is defined.
	 */
	@XStreamAlias("channel-group")
	private String channelGroup = "";
	/**
	 * The exposure time in ms.
	 */
	@XStreamAlias("exposure")
	private double exposure = 20;

	@XStreamAlias("detection-algorithm-configuration")
	private CellDetectionConfiguration detectionAlgorithmConfiguration = null;

	@XStreamAlias("detection-visualization-configuration")
	private CellVisualizationConfiguration visualizationAlgorithmConfiguration = null;

	@XStreamAlias("quantification-algorithm-configuration")
	private QuantificationConfiguration quantificationAlgorithmConfiguration = null;

	@XStreamAlias("tracking-algorithm-configuration")
	private TrackingConfiguration trackingAlgorithmConfiguration = null;

	/**
	 * Optional image-producing jobs (one per additional quantification channel /
	 * imaging event). When non-empty, the measurement acquires one coherent frame
	 * set per cycle (detection channel first, then these) instead of burst imaging.
	 */
	@XStreamAlias("quantification-images")
	private final ArrayList<JobConfiguration> quantificationJobs = new ArrayList<JobConfiguration>();

	/**
	 * Whether images should be saved to disk or not.
	 */
	@XStreamAlias("save-images")
	@XStreamConverter(value = BooleanConverter.class, booleans = { false }, strings = { "yes", "no" })
	private boolean saveImages = true;

	/**
	 * The name under which the image is saved.
	 */
	@XStreamAlias("image-save-name")
	private String imageSaveName = "";

	/**
	 * The time between two successive images.
	 */
	@XStreamAlias("period-ms")
	private int imagingPeriod = -1;

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	/**
	 * @return the camera device to image with ("" = default/standard camera).
	 */
	public String getCamera() {
		return camera;
	}

	/**
	 * @param camera the camera device to image with ("" or null = default camera).
	 */
	public void setCamera(String camera) {
		this.camera = camera == null ? "" : camera;
	}

	/**
	 * The channel where the images should be made. Set to an empty string if
	 * the current channel should be taken.
	 * 
	 * @param channelGroup The group in which the channel is defined.
	 * @param channel      The channel.
	 */
	public void setChannel(String channelGroup, String channel) {
		this.channel = channel;
		this.channelGroup = channelGroup;
	}

	/**
	 * The channel where the images should be made.
	 * 
	 * @return The channel.
	 */
	public String getChannel() {
		return channel;
	}

	/**
	 * @return the group of the channel
	 */
	public String getChannelGroup() {
		return channelGroup;
	}

	/**
	 * @param exposure
	 *                 the exposure to set
	 */
	public void setExposure(double exposure) {
		this.exposure = exposure;
	}

	/**
	 * @return the exposure
	 */
	public double getExposure() {
		return exposure;
	}

	/**
	 * @param saveImages
	 *                   the saveImages to set
	 */
	public void setSaveImages(boolean saveImages) {
		this.saveImages = saveImages;
	}

	/**
	 * Returns the name under which the image should be saved.
	 * 
	 * @return name of job.
	 */
	public String getImageSaveName() {
		return imageSaveName;
	}

	/**
	 * Sets the name under which the image should be saved.
	 * 
	 * @param name
	 */
	public void setImageSaveName(String name) {
		this.imageSaveName = name;
	}

	/**
	 * @param imagingPeriod The time between two successive images.
	 */
	public void setImagingPeriod(int imagingPeriod) {
		this.imagingPeriod = imagingPeriod;
	}

	/**
	 * @return The time between two successive images.
	 */
	public int getImagingPeriod() {
		return imagingPeriod;
	}

	/**
	 * Returns true if the images should be saved to the disk.
	 * 
	 * @return True if images should be saved.
	 */
	public boolean isSaveImages() {
		return saveImages;
	}

	/**
	 * Sets the configuration of the cell detection algorithm to use.
	 * 
	 * @param detectionAlgorithmConfiguration Configuration of the algorithm.
	 */
	public void setDetectionAlgorithmConfiguration(CellDetectionConfiguration detectionAlgorithmConfiguration) {
		this.detectionAlgorithmConfiguration = detectionAlgorithmConfiguration;
	}

	/**
	 * Return the configuration of the cell detection algorithm to use.
	 * 
	 * @return Configuration of the algorithm.
	 */
	public CellDetectionConfiguration getDetectionAlgorithmConfiguration() {
		return detectionAlgorithmConfiguration;
	}

	/**
	 * Returns the configuration of the cell visualization algorithm to use.
	 * Returns null if not generating a cell detection visualization image.
	 * 
	 * @return Cell visualization configuration, or null.
	 */
	public CellVisualizationConfiguration getVisualizationAlgorithmConfiguration() {
		return visualizationAlgorithmConfiguration;
	}

	/**
	 * Set the configuration of the cell visualization algorithm to use.
	 * Set to null to not generate a cell detection visualization image.
	 * 
	 * @param visualizationAlgorithmConfiguration Cell visualization configuration,
	 *                                            or null.
	 */
	public void setVisualizationAlgorithmConfiguration(
			CellVisualizationConfiguration visualizationAlgorithmConfiguration) {
		this.visualizationAlgorithmConfiguration = visualizationAlgorithmConfiguration;
	}

	/**
	 * Returns the configuration of the (optional) quantification algorithm, or
	 * null.
	 * 
	 * @return quantification configuration, or null.
	 */
	public QuantificationConfiguration getQuantificationAlgorithmConfiguration() {
		return quantificationAlgorithmConfiguration;
	}

	/**
	 * Sets the configuration of the (optional) quantification algorithm.
	 * 
	 * @param quantificationAlgorithmConfiguration quantification configuration, or
	 *                                             null.
	 */
	public void setQuantificationAlgorithmConfiguration(
			QuantificationConfiguration quantificationAlgorithmConfiguration) {
		this.quantificationAlgorithmConfiguration = quantificationAlgorithmConfiguration;
	}

	/**
	 * Returns the (optional) tracking algorithm configuration, or null.
	 * 
	 * @return tracking configuration, or null.
	 */
	public TrackingConfiguration getTrackingAlgorithmConfiguration() {
		return trackingAlgorithmConfiguration;
	}

	/**
	 * Sets the (optional) tracking algorithm configuration.
	 * 
	 * @param trackingAlgorithmConfiguration tracking configuration, or null.
	 */
	public void setTrackingAlgorithmConfiguration(TrackingConfiguration trackingAlgorithmConfiguration) {
		this.trackingAlgorithmConfiguration = trackingAlgorithmConfiguration;
	}

	/**
	 * Returns the image-producing jobs for the additional quantification channels
	 * (empty if none, i.e. burst detection-only imaging).
	 * 
	 * @return quantification imaging jobs.
	 */
	public JobConfiguration[] getQuantificationJobs() {
		return quantificationJobs.toArray(new JobConfiguration[quantificationJobs.size()]);
	}

	/**
	 * Sets the image-producing jobs for the additional quantification channels.
	 * 
	 * @param jobs quantification imaging jobs (may be null or empty).
	 */
	public void setQuantificationJobs(JobConfiguration[] jobs) {
		this.quantificationJobs.clear();
		if (jobs != null) {
			for (JobConfiguration job : jobs) {
				if (job != null)
					this.quantificationJobs.add(job);
			}
		}
	}
}
