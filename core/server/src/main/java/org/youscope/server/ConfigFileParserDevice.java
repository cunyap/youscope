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

import java.util.Vector;

import org.youscope.addon.microscopeaccess.AvailableDeviceDriverInternal;
import org.youscope.addon.microscopeaccess.HubDeviceInternal;
import org.youscope.addon.microscopeaccess.MicroscopeInternal;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.DeviceSetting;
import org.youscope.common.microscope.MicroscopeDriverException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * Represents a device defined in the config file prior to its initialization.
 * @author Moritz Lang
 * 
 */
class ConfigFileParserDevice extends ConfigFileManipulator implements Comparable<ConfigFileParserDevice>
{
	private final String					deviceID;
	private final String					libraryID;
	private final String					driverID;
	private String hubID = null;
	private final MicroscopeInternal		microscope;
	private final Vector<DeviceSetting>	settings				= new Vector<DeviceSetting>();
	private final static String				LIBRARY_SERIAL_MANAGER	= "SerialManager";

	ConfigFileParserDevice(String deviceID, String libraryID, String driverID, MicroscopeInternal microscope)
	{
		this.microscope = microscope;
		this.deviceID = deviceID;
		this.libraryID = libraryID;
		this.driverID = driverID;
	}

	void addPreInitDeviceSetting(DeviceSetting setting)
	{
		settings.addElement(setting);
	}
	String getHubID()
	{
		return hubID;
	}
	void setHubID(String hubID)
	{
		this.hubID = hubID;
	}
	String getDeviceID()
	{
		return deviceID;
	}

	String getLibraryID()
	{
		return libraryID;
	}

	String getDriverID()
	{
		return driverID;
	}

	void initializeDevice(int accessID) throws MicroscopeLockedException, MicroscopeDriverException
	{
		// check settings
		for(DeviceSetting setting : settings)
		{
			if(setting.isAbsoluteValue() == false)
				throw new MicroscopeDriverException("Relative values are not allowed for properties when initializing a device.");
		}
		
		AvailableDeviceDriverInternal driver;
		if(hubID == null)
		{
			// Load driver
			driver = microscope.getDeviceLoader().getAvailableDeviceDriver(libraryID, driverID);
			if(driver == null)
				throw new MicroscopeDriverException("Could not find driver with ID " + driverID + " in library " + libraryID + ".");
		}
		else
		{
			try 
			{
				HubDeviceInternal hub = microscope.getHubDevice(hubID);
				driver = hub.getPeripheralDeviceDriver(driverID);
				if(driver == null)
					throw new MicroscopeDriverException("Hub with ID "+hubID+" does not have peripheral driver with ID " + driverID + " in library " + libraryID + ".");
			} 
			catch (DeviceException e) 
			{
				throw new MicroscopeDriverException("The hub of the declared device "+libraryID+"."+driverID+", "+hubID+", cannot be found.");
			} 
		}
		

		// initialize device
		driver.loadDevice(deviceID, accessID);
		driver.initializeDevice(settings.toArray(new DeviceSetting[0]), accessID);
	}

	@Override
	public int compareTo(ConfigFileParserDevice otherDevice)
	{
		// First create all serial ports.
		if(LIBRARY_SERIAL_MANAGER.equals(libraryID))
		{
			if(LIBRARY_SERIAL_MANAGER.equals(otherDevice.getLibraryID()))
				return 0;
			return -1;
		}
		if(LIBRARY_SERIAL_MANAGER.equals(otherDevice.getLibraryID()))
			return 1;
		// First, load everything which doesn't have a hub device, which guarantees that hubs are loaded before their respective peripherals
		if(getHubID() != null)
		{
			if(otherDevice.getHubID() != null)
				return 0;
			return 1;
		}
		if(otherDevice.getHubID() != null)
			return -1;
		// with respect to everything else, order probably doesn't matter
		return 0;
	}
}
