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

package org.youscope.plugin.argolight;

import java.util.HashSet;
import java.util.Set;

import org.youscope.common.job.CompositeJobConfiguration;
import org.youscope.common.job.JobConfiguration;
import org.youscope.common.microplate.WellWithGroup;
import org.youscope.common.microplate.WellWithGroup.WellGroup;
import org.youscope.plugin.imagingjob.ImagingJobConfiguration;
import org.youscope.plugin.microplate.measurement.MicroplateMeasurementConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration of an Argolight calibration measurement.
 *
 * <p>
 * This is a thin specialization of the proven
 * {@link MicroplateMeasurementConfiguration}: it reuses the entire microplate
 * measurement runtime (positions, per-group jobs, path optimisation, saving,
 * post-processing) but pins the microplate layout to an Argolight pattern map
 * and carries its own type identifier so it appears as a distinct measurement
 * type in the New Measurement list.
 * </p>
 *
 * @author Andreas P. Cuny
 */
@XStreamAlias("argolight-calibration-measurement")
public class ArgolightCalibrationConfiguration extends MicroplateMeasurementConfiguration {
	private static final long serialVersionUID = 1L;

	/** Type identifier of the Argolight calibration measurement. */
	public static final String TYPE_IDENTIFIER = "YouScope.ArgolightCalibrationMeasurement";

	// Channel / exposure chosen in the position fine-configuration live view. When
	// set, these are used when pre-filling imaging jobs and are propagated to all
	// selected well groups' existing imaging jobs on save.
	@XStreamAlias("acquisition-channel-group")
	private String acquisitionChannelGroup = null;
	@XStreamAlias("acquisition-channel")
	private String acquisitionChannel = null;
	@XStreamAlias("acquisition-camera")
	private String acquisitionCamera = null;
	@XStreamAlias("acquisition-exposure-ms")
	private Double acquisitionExposureMs = null;

	/** @return the channel group chosen in the live view, or null. */
	public String getAcquisitionChannelGroup() {
		return acquisitionChannelGroup;
	}

	/** @return the channel chosen in the live view, or null. */
	public String getAcquisitionChannel() {
		return acquisitionChannel;
	}

	/** @return the camera chosen in the live view, or null. */
	public String getAcquisitionCamera() {
		return acquisitionCamera;
	}

	/** @return the exposure (ms) chosen in the live view, or null if unset. */
	public Double getAcquisitionExposureMs() {
		return acquisitionExposureMs;
	}

	/** @return true if a live-view channel has been captured. */
	public boolean hasAcquisitionChannel() {
		return acquisitionChannelGroup != null && acquisitionChannel != null;
	}

	/**
	 * Records the camera / channel / exposure chosen in the position fine-config
	 * live view and immediately propagates them to the imaging jobs of every
	 * selected well group.
	 * 
	 * @param channelGroup channel group (may be null).
	 * @param channel      channel (may be null).
	 * @param camera       camera device (may be null).
	 * @param exposureMs   exposure in milliseconds.
	 */
	public void setAcquisitionChannelExposure(String channelGroup, String channel, String camera, double exposureMs) {
		this.acquisitionChannelGroup = channelGroup;
		this.acquisitionChannel = channel;
		this.acquisitionCamera = camera;
		this.acquisitionExposureMs = exposureMs;
		applyChannelExposureToSelectedGroups();
	}

	/**
	 * Applies the recorded channel / exposure to the imaging jobs of all currently
	 * selected well groups. Existing jobs are updated in place; groups without jobs
	 * yet are pre-filled with the settings when the imaging-protocol page is shown.
	 */
	public void applyChannelExposureToSelectedGroups() {
		if (!hasAcquisitionChannel() && acquisitionExposureMs == null)
			return;
		Set<WellGroup> groups = new HashSet<>();
		for (WellWithGroup well : getSelectedWells())
			groups.add(well.getGroup());
		for (WellGroup group : groups) {
			JobConfiguration[] jobs = getJobs(group);
			if (jobs == null || jobs.length == 0)
				continue;
			for (JobConfiguration job : jobs)
				applyToJob(job);
			setJobs(group, jobs);
		}
	}

	private void applyToJob(JobConfiguration job) {
		if (job instanceof ImagingJobConfiguration) {
			ImagingJobConfiguration imaging = (ImagingJobConfiguration) job;
			if (hasAcquisitionChannel())
				imaging.setChannel(acquisitionChannelGroup, acquisitionChannel);
			if (acquisitionCamera != null && acquisitionCamera.length() > 0)
				imaging.setCamera(acquisitionCamera);
			if (acquisitionExposureMs != null)
				imaging.setExposure(acquisitionExposureMs.doubleValue());
		}
		if (job instanceof CompositeJobConfiguration) {
			for (JobConfiguration child : ((CompositeJobConfiguration) job).getJobs())
				applyToJob(child);
		}
	}

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	/**
	 * Convenience: returns the Argolight microplate configuration, creating a
	 * default one if the current microplate configuration is absent or of a
	 * different type.
	 * 
	 * @return the Argolight microplate configuration.
	 */
	public ArgolightMicroplateConfiguration getOrCreateArgolightMicroplate() {
		if (getMicroplateConfiguration() instanceof ArgolightMicroplateConfiguration)
			return (ArgolightMicroplateConfiguration) getMicroplateConfiguration();
		ArgolightMicroplateConfiguration argolight = new ArgolightMicroplateConfiguration();
		setMicroplateConfiguration(argolight);
		return argolight;
	}
}
