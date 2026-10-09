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
 ******************************************************************************/
/**
 * 
 */
package org.youscope.addon.celldetection;

import java.rmi.RemoteException;

import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.Resource;
import org.youscope.common.table.TableProducer;

/**
 * A cell detection addon is a class supporting to detect cells in microscope images.
 * @author Moritz Lang
 *
 */
public interface CellDetectionAddon extends Resource, TableProducer
{
	/**
	 * Detects the cell in the detectionImage image according to the configuration of the addon.
	 * Same as calling detectCells(detectionImage, new ImageEvent[0]).
	 * @param detectionImage The image which should be analyzed.
	 * @return The result of the cell detection algorithm.
	 * @throws CellDetectionException
	 * @throws RemoteException 
	 */
	public CellDetectionResult detectCells(ImageEvent<?> detectionImage) throws CellDetectionException, RemoteException;
	
	/**
	 * Detects the cell in the detectionImage image according to the configuration of the addon.
	 * The additional images in quantificationImages are used to quantify e.g. the fluorescence level in a certain channel.
	 * Note that not all algorithms support quantification of further images. These algorithms should implement this method, however, are not
	 * required to use the images for quantification...
	 * @param detectionImage The image which should be analyzed.
	 * @param quantificationImages Images which should be quantified.
	 * @return The result of the cell detection algorithm.
	 * @throws CellDetectionException
	 * @throws RemoteException 
	 */
	public CellDetectionResult detectCells(ImageEvent<?> detectionImage, ImageEvent<?>[] quantificationImages) throws CellDetectionException, RemoteException;
}
