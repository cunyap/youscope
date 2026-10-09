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
package org.youscope.common.microscope;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author langmo
 * 
 */
public interface DeviceLoader extends Remote
{
	/**
	 * Returns a list of all available device drivers.
	 * @return List of all available device drivers.
	 * @throws RemoteException
	 * @throws MicroscopeDriverException
	 */
	public AvailableDeviceDriver[] getAvailableDeviceDrivers() throws RemoteException, MicroscopeDriverException;

	/**
	 * Removes a previously added device.
	 * @param name Name of the device.
	 * @throws RemoteException
	 * @throws MicroscopeDriverException
	 * @throws MicroscopeLockedException
	 */
	public void removeDevice(String name) throws RemoteException, MicroscopeDriverException, MicroscopeLockedException;
}
