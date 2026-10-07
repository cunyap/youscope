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
package org.youscope.addon.focussearch;

import java.rmi.RemoteException;

import org.youscope.common.resource.Resource;
import org.youscope.common.resource.ResourceException;

/**
 * A focus search algorithm addon is a class defining an algorithm to autofocus based on a given focus score.
 * @author Moritz Lang
 *
 */
public interface FocusSearchResource extends Resource
{
	/**
	 * Runs the focus score algorithm using the given oracle to determine the focus scores.
	 * @param oracle An oracle to determine the focus score at a given (relative) focal plane.
	 * @return The focus position of the best focal plane.
	 * @throws ResourceException 
	 * @throws RemoteException 
	 */
	public double runAutofocus(FocusSearchOracle oracle) throws ResourceException, RemoteException;
}
