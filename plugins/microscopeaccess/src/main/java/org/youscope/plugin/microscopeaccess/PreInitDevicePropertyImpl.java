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
package org.youscope.plugin.microscopeaccess;

import java.util.Arrays;

import org.youscope.addon.microscopeaccess.DeviceInternal;
import org.youscope.addon.microscopeaccess.PreInitDevicePropertyInternal;
import org.youscope.common.microscope.DeviceType;
import org.youscope.common.microscope.MicroscopeDriverException;
import org.youscope.common.microscope.PropertyType;

/**
 * @author langmo
 *
 */
class PreInitDevicePropertyImpl implements PreInitDevicePropertyInternal
{
	private final String propertyID;
	private final PropertyType type;
	private final String[] allowedValues;
	private final String  defaultValue;
	private final MicroscopeImpl microscope;
	PreInitDevicePropertyImpl(MicroscopeImpl microscope, String name, PropertyType type, String[] allowedValues, String defaultValue)
	{
		this.microscope = microscope;
		this.propertyID = name;
		this.type = type;
		this.allowedValues = allowedValues;
		this.defaultValue = defaultValue;
	}
	@Override
	public String getPropertyID()
	{
		return propertyID;
	}
	
	@Override
	public String getDefaultValue()
	{
		return defaultValue;
	}

	@Override
	public PropertyType getType()
	{
		return type;
	}
	
	
	private String[] getSerialPortNames()
	{
		DeviceInternal[] devices = microscope.getDevices(DeviceType.SerialDevice);
		String[] deviceNames = new String[devices.length];
		for(int i = 0; i < deviceNames.length; i++)
		{
			deviceNames[i] = devices[i].getDeviceID();
		}
		return deviceNames;
	}
	@Override
	public String[] getAllowedPropertyValues() throws MicroscopeDriverException
	{
		if(getPropertyID().equals("Port"))
		{
			// microManager does not automatically set the available ports property.
			return getSerialPortNames();
		}
		else if(allowedValues == null)
			return null;
		else
			return Arrays.copyOf(allowedValues, allowedValues.length);
	}
}
