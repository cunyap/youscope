/*******************************************************************************
 * Copyright (c) 2020 Moritz Lang.
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
package org.youscope.common.microscope;

import java.rmi.RemoteException;



/**
 * A pseudo-device with the main target to host other, peripheral devices
 * @author Moritz Lang
 *
 */
public interface HubDevice extends Device
{
	/**
	 * Returns all peripheral device drivers associated to this hub device.
	 * @return peripheral device drivers.
	 * @throws MicroscopeDriverException
	 * @throws RemoteException
	 */
	AvailableDeviceDriver[] getPeripheralDeviceDrivers() throws MicroscopeDriverException, RemoteException;

	/**
	 * Retruns the peripheral device driver with the given library and driver id. If this hub
	 * doesn't have a corresponding peripheral device driver, null is returned.
	 * @param driverID The ID of the peripheral driver. The library ID is assumed to be the same as the one of this hub.
	 * @return The peripheral driver.
	 * @throws MicroscopeDriverException
	 * @throws RemoteException
	 */
	AvailableDeviceDriver getPeripheralDeviceDriver(String driverID) throws MicroscopeDriverException, RemoteException;
}
