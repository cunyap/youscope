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
package org.youscope.plugin.matlabfocusscores;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface to temporary save the focus score.
 * @author Moritz Lang
 *
 */
public interface FocusSink extends Remote
{
	/**
	 * Sets the focus score.
	 * @param score
	 * @throws RemoteException
	 */
	public void setScore(double score) throws RemoteException;
	
	/**
	 * Returns the last focus score.
	 * @return Last focus score, or -1, if yet not set.
	 * @throws RemoteException
	 */
	public double getLastScore() throws RemoteException;
}
