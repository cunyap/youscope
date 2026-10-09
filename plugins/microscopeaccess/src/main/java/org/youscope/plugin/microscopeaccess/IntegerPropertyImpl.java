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
package org.youscope.plugin.microscopeaccess;

import org.youscope.addon.microscopeaccess.IntegerPropertyInternal;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.PropertyType;

/**
 * @author Moritz Lang
 *
 */
class IntegerPropertyImpl extends PropertyImpl implements IntegerPropertyInternal
{
	private final int lowerLimit;
	private final int upperLimit;
	IntegerPropertyImpl(MicroscopeImpl microscope, String deviceID, String propertyID, int lowerLimit, int upperLimit, boolean preInit, PropertyActionListener actionListener)
	{
		super(microscope, deviceID, propertyID, PropertyType.PROPERTY_INTEGER, true, preInit, actionListener);
		this.lowerLimit = lowerLimit;
		this.upperLimit = upperLimit;
	}

	@Override
	public int getIntegerValue() throws MicroscopeException, NumberFormatException, InterruptedException
	{
		return Integer.parseInt(getValue());
	}
	
	@Override
	public int getUpperLimit()
	{
		return upperLimit;
	}
	
	@Override
	public int getLowerLimit()
	{
		return lowerLimit;
	}
	
	@Override
	public void setValue(int value, int accessID) throws MicroscopeException, MicroscopeLockedException, InterruptedException
	{
		setStringValue(Integer.toString(value), accessID);
	}
	@Override
	public void setValueRelative(int offset, int accessID) throws MicroscopeException, MicroscopeLockedException, NumberFormatException, InterruptedException
	{
		setValue(getIntegerValue() + offset, accessID);
	}

	@Override
	public void setValue(String value, int accessID) throws MicroscopeException, MicroscopeLockedException, InterruptedException, DeviceException
	{
		try
		{
			setValue(Integer.parseInt(value), accessID);
		}
		catch(NumberFormatException e)
		{
			throw new DeviceException("Property " + getDeviceID() + "." + getPropertyID() + " can only be set to integer values.", e);
		}
	}
}
