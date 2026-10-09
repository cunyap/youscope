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
package org.youscope.addon.focussearch;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.youscope.common.resource.ResourceException;

/**
 * Oracle for a focus score algorithm to get the focus score for a given focal plane.
 * @author langmo
 *
 */
public interface FocusSearchOracle extends Remote {
	/**
	 * Returns the focal score for a given focal plane. Higher scores indicate better focal planes.
	 * @param relativeFocusPosition The focal position (z-position) for which the focus score should be querried.
	 * @return The focus score of the given focal position.
	 * @throws ResourceException
	 * @throws RemoteException
	 */
	public double getFocusScore(double relativeFocusPosition) throws ResourceException, RemoteException;
}
