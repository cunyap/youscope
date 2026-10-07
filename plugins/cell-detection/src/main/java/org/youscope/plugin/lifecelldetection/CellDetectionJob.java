/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
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

import org.youscope.addon.celldetection.CellDetectionAddon;
import org.youscope.addon.celldetection.CellVisualizationAddon;
import org.youscope.addon.celldetection.QuantificationAddon;
import org.youscope.addon.celldetection.TrackingAddon;
import org.youscope.common.ComponentRunningException;
import org.youscope.common.image.ImageListener;
import org.youscope.common.image.ImageProducer;
import org.youscope.common.job.CompositeJob;
import org.youscope.common.job.Job;
import org.youscope.common.table.TableProducer;

/**
 * This job detects cells in the image passed to it. It passes information about
 * the detected cells to its table listeners. If configured respectively,
 * it also produces an image in which the detected cells are highlighted to its
 * image listeners.
 * 
 * @author Moritz Lang and Andreas P. Cuny
 */
public interface CellDetectionJob extends Job, ImageProducer, TableProducer, CompositeJob {
	/**
	 * Adds a listener which is invoked if a new segmentation image was created.
	 * A listener can always be added, even if the respective measurement is already
	 * running.
	 * Note: if the cells are not visualized, this is the same as adding the
	 * listener with addImageListener().
	 * 
	 * @param listener Listener which should be notified.
	 * @throws RemoteException
	 */
	void addSegmentationImageListener(ImageListener listener) throws RemoteException;

	/**
	 * Removes a previously added image listener.
	 * 
	 * @param listener Listener which was previously added.
	 * @throws RemoteException
	 */
	void removeSegmentationImageListener(ImageListener listener) throws RemoteException;

	/**
	 * Adds a listener which is invoked if a new control image was created.
	 * A listener can always be added, even if the respective measurement is already
	 * running.
	 * Note: if the cells are visualized, this is the same as adding the listener
	 * with addImageListener().
	 * 
	 * @param listener Listener which should be notified.
	 * @throws RemoteException
	 */
	void addControlImageListener(ImageListener listener) throws RemoteException;

	/**
	 * Removes a previously added image listener.
	 * 
	 * @param listener Listener which was previously added.
	 * @throws RemoteException
	 */
	void removeControlImageListener(ImageListener listener) throws RemoteException;

	/**
	 * Sets the algorithm which should be used for cell detection.
	 * 
	 * @param detectionAlgorithm
	 * @throws ComponentRunningException
	 * @throws RemoteException
	 */
	void setDetectionAlgorithm(CellDetectionAddon detectionAlgorithm) throws ComponentRunningException, RemoteException;

	/**
	 * Returns the algorithm which should be used for cell detection, or null.
	 * 
	 * @return cell detection algorithm or null.
	 * @throws RemoteException
	 */
	CellDetectionAddon getDetectionAlgorithm() throws RemoteException;

	/**
	 * Sets the algorithm which should be used for cell visualization.
	 * 
	 * @param visualizationAlgorithm
	 * @throws ComponentRunningException
	 * @throws RemoteException
	 */
	void setVisualizationAlgorithm(CellVisualizationAddon visualizationAlgorithm)
			throws ComponentRunningException, RemoteException;

	/**
	 * Returns the algorithm which should be used for cell visualization, or null.
	 * 
	 * @return cell detection algorithm or null.
	 * @throws RemoteException
	 */
	CellVisualizationAddon getVisualizationAlgorithm() throws RemoteException;

	/**
	 * Sets the (optional) quantification algorithm run between detection and
	 * visualization.
	 * 
	 * @param quantificationAlgorithm quantifier, or null to skip quantification.
	 */
	void setQuantificationAlgorithm(QuantificationAddon quantificationAlgorithm)
			throws ComponentRunningException, RemoteException;

	/**
	 * Returns the quantification algorithm, or null.
	 */
	QuantificationAddon getQuantificationAlgorithm() throws RemoteException;

	/** Sets the (optional) tracking algorithm run after quantification. */
	void setTrackingAlgorithm(TrackingAddon trackingAlgorithm) throws ComponentRunningException, RemoteException;

	/** Returns the tracking algorithm, or null. */
	TrackingAddon getTrackingAlgorithm() throws RemoteException;

	/**
	 * Sets a short string describing the images which are made by this job.
	 * 
	 * @param description The description which should be returned for the images
	 *                    produced by this job, or null, to switch to the default
	 *                    description.
	 * @throws RemoteException
	 * @throws ComponentRunningException
	 */
	void setImageDescription(String description) throws RemoteException, ComponentRunningException;

	/**
	 * Sets the minimal time (in ms) the execution of the job should take. If the
	 * execution of the job takes less, the rest of the time span will be waited.
	 * This function is mainly ment for real-time applications.
	 * 
	 * @param minimalTimeMS The minimal time in ms the execution of the job has to
	 *                      take, or -1.
	 * @throws ComponentRunningException
	 * @throws RemoteException
	 */
	public void setMinimalTimeMS(long minimalTimeMS) throws ComponentRunningException, RemoteException;

	/**
	 * Returns the minimal time (in ms) the execution of the job should take. If the
	 * execution of the job takes less, the rest of the time span will be waited.
	 * This function is mainly ment for real-time applications.
	 * 
	 * @return The minimal time in ms the execution of the job has to take, or -1.
	 * @throws RemoteException
	 */
	public long getMinimalTimeMS() throws RemoteException;
}
