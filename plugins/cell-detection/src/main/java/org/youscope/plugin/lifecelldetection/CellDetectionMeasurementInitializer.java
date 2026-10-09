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

import java.rmi.RemoteException;

import org.youscope.addon.AddonException;
import org.youscope.addon.celldetection.CellDetectionAddon;
import org.youscope.addon.celldetection.CellDetectionConfiguration;
import org.youscope.addon.celldetection.CellVisualizationAddon;
import org.youscope.addon.celldetection.CellVisualizationConfiguration;
import org.youscope.addon.celldetection.QuantificationAddon;
import org.youscope.addon.celldetection.TrackingAddon;
import org.youscope.addon.component.ComponentCreationException;
import org.youscope.addon.measurement.MeasurementInitializer;
import org.youscope.common.ComponentRunningException;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.job.JobConfiguration;
import org.youscope.common.image.ImageProducer;
import org.youscope.common.job.Job;
import org.youscope.common.job.basicjobs.ContinuousImagingJob;
import org.youscope.common.job.basicjobs.ImagingJob;
import org.youscope.common.measurement.Measurement;
import org.youscope.common.task.Task;
import org.youscope.common.task.RegularPeriodConfiguration;
import org.youscope.common.task.TaskException;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * @author Moritz Lang and Andreas P. Cuny
 * 
 */
public class CellDetectionMeasurementInitializer
		implements MeasurementInitializer<CellDetectionMeasurementConfiguration> {

	@Override
	public void initializeMeasurement(Measurement measurement, CellDetectionMeasurementConfiguration configuration,
			ConstructionContext constructionContext) throws ConfigurationException, AddonException {
		PositionInformation positionInformation = new PositionInformation(null);

		// Get detection & visualization algorithms.
		CellDetectionConfiguration detectionAlgorithmConfiguration = configuration.getDetectionAlgorithmConfiguration();
		CellVisualizationConfiguration visualizationAlgorithmConfiguration = configuration
				.getVisualizationAlgorithmConfiguration();
		if (detectionAlgorithmConfiguration == null) {
			throw new ConfigurationException("Configuration of cell detection algorithm is null.");
		}

		CellDetectionAddon cellDetectionAddon;
		try {
			cellDetectionAddon = constructionContext.getComponentProvider().createComponent(positionInformation,
					detectionAlgorithmConfiguration, CellDetectionAddon.class);
		} catch (Exception e1) {
			throw new AddonException("Could not create cell detection algorithm.", e1);
		}

		CellVisualizationAddon cellVisualizationAddon;
		if (visualizationAlgorithmConfiguration != null) {
			try {
				cellVisualizationAddon = constructionContext.getComponentProvider().createComponent(positionInformation,
						visualizationAlgorithmConfiguration, CellVisualizationAddon.class);
			} catch (Exception e) {
				throw new AddonException("Could not create cell visualization algorithm.", e);
			}
		} else
			cellVisualizationAddon = null;

		// Optional quantification and tracking algorithms (both may be null).
		QuantificationAddon cellQuantificationAddon = null;
		if (configuration.getQuantificationAlgorithmConfiguration() != null) {
			try {
				cellQuantificationAddon = constructionContext.getComponentProvider().createComponent(
						positionInformation, configuration.getQuantificationAlgorithmConfiguration(),
						QuantificationAddon.class);
			} catch (Exception e) {
				throw new AddonException("Could not create cell quantification algorithm.", e);
			}
		}
		TrackingAddon cellTrackingAddon = null;
		if (configuration.getTrackingAlgorithmConfiguration() != null) {
			try {
				cellTrackingAddon = constructionContext.getComponentProvider().createComponent(positionInformation,
						configuration.getTrackingAlgorithmConfiguration(), TrackingAddon.class);
			} catch (Exception e) {
				throw new AddonException("Could not create cell tracking algorithm.", e);
			}
		}

		// Zero quantification channels: burst (continuous) detection imaging.
		// One or more: one coherent frame set per cycle: a standard imaging job
		// for the detection channel, followed by one per quantification channel.
		JobConfiguration[] quantificationJobConfigs = configuration.getQuantificationJobs();
		boolean perCycle = quantificationJobConfigs != null && quantificationJobConfigs.length > 0;

		RegularPeriodConfiguration period = new RegularPeriodConfiguration();
		int taskPeriod = configuration.getImagingPeriod();
		if (taskPeriod <= 0) {
			// burst / as-fast-as-possible
			period.setPeriod(0);
			period.setFixedTimes(false);
		} else {
			// fixed mode
			period.setPeriod(taskPeriod);
			period.setFixedTimes(true);
		}

		period.setStartTime(0);
		Task task;
		try {
			task = measurement.addTask(period.getPeriod(), period.isFixedTimes(), period.getStartTime(),
					period.getNumExecutions());

			CellDetectionJobImpl cellDetectionJob = new CellDetectionJobImpl(new PositionInformation(null));
			cellDetectionJob.setDetectionAlgorithm(cellDetectionAddon);
			cellDetectionJob.setVisualizationAlgorithm(cellVisualizationAddon);
			if (cellQuantificationAddon != null)
				cellDetectionJob.setQuantificationAlgorithm(cellQuantificationAddon);
			if (cellTrackingAddon != null)
				cellDetectionJob.setTrackingAlgorithm(cellTrackingAddon);

			if (visualizationAlgorithmConfiguration != null && configuration.isSaveImages())
				cellDetectionJob
						.addImageListener(constructionContext.getMeasurementSaver().getSaveImageListener("Detect"));

			if (!perCycle) {
				// Burst detection-only imaging (unchanged behaviour)
				ContinuousImagingJob continuousJob;
				try {
					continuousJob = constructionContext.getComponentProvider().createJob(new PositionInformation(),
							ContinuousImagingJob.DEFAULT_TYPE_IDENTIFIER, ContinuousImagingJob.class);
				} catch (ComponentCreationException e) {
					throw new AddonException(
							"Continuous life cell detection measurement needs the continuous imaging plugin.", e);
				}
				if (configuration.getCamera() != null && configuration.getCamera().length() > 0)
					continuousJob.setCamera(configuration.getCamera());
				continuousJob.setChannel(configuration.getChannelGroup(), configuration.getChannel());
				continuousJob.setExposure(configuration.getExposure());
				continuousJob.setBurstImaging(taskPeriod <= 0);
				if (configuration.isSaveImages())
					continuousJob.addImageListener(constructionContext.getMeasurementSaver()
							.getSaveImageListener(configuration.getImageSaveName()));
				cellDetectionJob.addJob(continuousJob);
			} else {
				// Per-cycle acquisition: detection channel first, then each
				// quantification channel; one coherent frame set per execution
				ImagingJob detectionJob;
				try {
					detectionJob = constructionContext.getComponentProvider().createJob(new PositionInformation(),
							ImagingJob.DEFAULT_TYPE_IDENTIFIER, ImagingJob.class);
				} catch (ComponentCreationException e) {
					throw new AddonException(
							"Cell detection measurement with quantification channels needs the imaging job plugin.", e);
				}
				if (configuration.getCamera() != null && configuration.getCamera().length() > 0)
					detectionJob.setCamera(configuration.getCamera());
				detectionJob.setChannel(configuration.getChannelGroup(), configuration.getChannel());
				detectionJob.setExposure(configuration.getExposure());
				if (configuration.isSaveImages())
					detectionJob.addImageListener(constructionContext.getMeasurementSaver()
							.getSaveImageListener(configuration.getImageSaveName()));
				cellDetectionJob.addJob(detectionJob);

				// Quantification imaging jobs (must be image producers), in order
				for (JobConfiguration quantConfig : quantificationJobConfigs) {
					Job quantJob;
					try {
						quantJob = constructionContext.getComponentProvider().createJob(new PositionInformation(),
								quantConfig);
					} catch (ComponentCreationException e) {
						throw new AddonException("Could not create quantification imaging job.", e);
					}
					if (!(quantJob instanceof ImageProducer))
						throw new ConfigurationException("A configured quantification imaging job ("
								+ quantConfig.getTypeIdentifier() + ") does not produce images.");
					cellDetectionJob.addJob(quantJob);
				}
			}

			try {
				task.addJob(cellDetectionJob);
			} catch (TaskException e) {
				throw new AddonException("Could not add job to task.", e);
			}
		} catch (RemoteException e) {
			throw new AddonException("Remote exception while creating job.", e);
		} catch (ComponentRunningException e) {
			throw new AddonException("Could not initialize measurement since measurement is already running.", e);
		}
	}
}
