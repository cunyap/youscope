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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

/**
 * @author Moritz Lang
 *
 */
class CellXLastResultImpl extends UnicastRemoteObject implements CellXLastResult
{
	private final HashMap<String, Object> lastResult = new HashMap<String, Object>();
	
	/**
	 * SerializableVersion UID.
	 */
	private static final long	serialVersionUID	= 1110798742199344228L;

	/**
	 * Constructor.
	 * @throws RemoteException
	 */
	public CellXLastResultImpl() throws RemoteException
	{
		super();
	}

	@Override
	public synchronized void setData(String key, Object value) throws RemoteException
	{
		lastResult.put(key, value);
	}
	
	@Override
	public synchronized Object getData(String key) throws RemoteException
	{
		return lastResult.get(key);
	}
}
