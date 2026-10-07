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
package org.youscope.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.youscope.addon.microscopeaccess.AvailableDeviceDriverInternal;
import org.youscope.addon.microscopeaccess.DeviceLoaderInternal;
import org.youscope.common.microscope.AvailableDeviceDriver;
import org.youscope.common.microscope.DeviceLoader;
import org.youscope.common.microscope.MicroscopeDriverException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * @author langmo
 */
class DeviceLoaderRMI extends UnicastRemoteObject implements DeviceLoader
{

	/**
	 * Serial Version UID.
	 */
	private static final long		serialVersionUID	= -975901235633383293L;

	private final DeviceLoaderInternal	deviceManager;
	private final int					accessID;
	private final MicroscopeRMI microscope;
	/**
	 * Constructor.
	 * 
	 * @throws RemoteException
	 */
	DeviceLoaderRMI(DeviceLoaderInternal deviceManager, MicroscopeRMI microscope, int accessID) throws RemoteException
	{
		super();
		this.accessID = accessID;
		this.deviceManager = deviceManager;
		this.microscope = microscope;
	}

	@Override
	public AvailableDeviceDriver[] getAvailableDeviceDrivers() throws RemoteException, MicroscopeDriverException
	{
		AvailableDeviceDriverInternal[] driversInternal = deviceManager.getAvailableDeviceDrivers();
		AvailableDeviceDriver[] drivers = new AvailableDeviceDriver[driversInternal.length];
		for(int i = 0; i < driversInternal.length; i++)
		{
			drivers[i] = new AvailableDeviceDriverRMI(driversInternal[i], microscope, accessID);
		}
		return drivers;
	}

	@Override
	public void removeDevice(String name) throws MicroscopeDriverException, MicroscopeLockedException
	{
		deviceManager.removeDevice(name, accessID);
	}
}
