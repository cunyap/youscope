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
package org.youscope.plugin.cellx;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Moritz Lang
 *
 */
interface CellXLastResult extends Remote
{

	/**
	 * Sets the specific key value pair.
	 * @param key
	 * @param value
	 * @throws RemoteException
	 */
	void setData(String key, Object value) throws RemoteException;

	/**
	 * Returns the value for the specific key, or null.
	 * @param key
	 * @return value for key, or null.
	 * @throws RemoteException
	 */
	Object getData(String key) throws RemoteException;
	
}
