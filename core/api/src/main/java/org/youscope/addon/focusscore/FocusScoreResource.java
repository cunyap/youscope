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
package org.youscope.addon.focusscore;

import java.rmi.RemoteException;

import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.Resource;
import org.youscope.common.resource.ResourceException;

/**
 * A cell focus score resource is a class supporting scoring the quality of the focal plane by an image taken in the plane.
 * @author Moritz Lang
 *
 */
public interface FocusScoreResource  extends Resource 
{
		
	/**
	 * Calculates the focus score for the given image. The higher the score, the better the focus.
	 * The focus score must be larger or equal zero. The upper bound-if one exists-is implementation dependent.
	 * It can be assumed that the focus score is approximately continuous (except noise in imaging) for different focus values.
	 * Depending on the algorithm, there might exist more than one (local) maximum for different focus values.
	 * @param image The image which should be analyzed.
	 * @return The focus score of the image, given the algorithm of this addon.
	 * @throws ResourceException
	 * @throws RemoteException
	 */
	public double calculateScore(ImageEvent<?> image) throws ResourceException, RemoteException;
}
