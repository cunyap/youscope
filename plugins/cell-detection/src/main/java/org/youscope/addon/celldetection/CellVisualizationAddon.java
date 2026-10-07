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
 ******************************************************************************/
/**
 * 
 */
package org.youscope.addon.celldetection;

import java.rmi.RemoteException;

import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.Resource;

/**
 * A cell visualization addon is a class supporting to visualize previously detected cells in microscope images by generating new images.
 * @author Moritz Lang
 *
 */
public interface CellVisualizationAddon extends Resource
{
	/**
	 * Visualizes the cell in this image according to the configuration of the addon.
	 * @param image The image in which the cells should be visualized.
	 * @param detectionResult The output of a detection algorithm for this image.
	 * @return An image, in which the detected cells are highlighted in one or the other way.
	 * @throws CellVisualizationException
	 * @throws RemoteException 
	 */
	public ImageEvent<?> visualizeCells(ImageEvent<?> image, CellDetectionResult detectionResult) throws CellVisualizationException, RemoteException;
}
