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
package org.youscope.server;

import java.rmi.RemoteException;


import org.youscope.addon.microscopeaccess.AvailableDeviceDriverInternal;
import org.youscope.addon.microscopeaccess.HubDeviceInternal;
import org.youscope.common.microscope.AvailableDeviceDriver;
import org.youscope.common.microscope.HubDevice;
import org.youscope.common.microscope.MicroscopeDriverException;

class HubDeviceRMI extends DeviceRMI implements HubDevice
{
	/**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 9144637057274529300L;
    private final HubDeviceInternal hubDevice;
    HubDeviceRMI(HubDeviceInternal hubDevice, MicroscopeRMI microscope, int accessID)
            throws RemoteException
    {
        super(hubDevice, microscope, accessID);
        this.hubDevice = hubDevice;
    }
	@Override
	public AvailableDeviceDriver[] getPeripheralDeviceDrivers() throws MicroscopeDriverException, RemoteException
	{
		AvailableDeviceDriverInternal[] rawDrivers = hubDevice.getPeripheralDeviceDrivers();
		AvailableDeviceDriver[] drivers = new AvailableDeviceDriver[rawDrivers.length];
		for(int i=0; i<rawDrivers.length; i++)
		{
			drivers[i] = new AvailableDeviceDriverRMI(rawDrivers[i], microscope, accessID);
		}
		return drivers;
	}
	@Override
	public AvailableDeviceDriver getPeripheralDeviceDriver(String driverID)
			throws MicroscopeDriverException, RemoteException
	{
		return new AvailableDeviceDriverRMI(hubDevice.getPeripheralDeviceDriver(driverID), microscope, accessID);
	}
}
