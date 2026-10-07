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

import org.youscope.addon.microscopeaccess.PreInitDevicePropertyInternal;
import org.youscope.common.microscope.MicroscopeDriverException;
import org.youscope.common.microscope.PreInitDeviceProperty;
import org.youscope.common.microscope.PropertyType;

/**
 * @author langmo
 */
class PreInitDevicePropertyRMI extends UnicastRemoteObject implements PreInitDeviceProperty
{

	/**
	 * Serial Version UID.
	 */
	private static final long					serialVersionUID	= -2204579569726358661L;

	private final PreInitDevicePropertyInternal	deviceProperty;

	/**
	 * Constructor.
	 * @param deviceProperty 
	 * @param accessID  
	 * @throws RemoteException 
	 */
	public PreInitDevicePropertyRMI(PreInitDevicePropertyInternal deviceProperty, int accessID) throws RemoteException
	{
		super();
		this.deviceProperty = deviceProperty;
	}

	@Override
	public String getPropertyID() throws MicroscopeDriverException
	{
		return deviceProperty.getPropertyID();
	}

	@Override
	public PropertyType getType() throws MicroscopeDriverException
	{
		return deviceProperty.getType();
	}

	@Override
	public String[] getAllowedPropertyValues() throws MicroscopeDriverException
	{
		return deviceProperty.getAllowedPropertyValues();
	}

	@Override
	public String getDefaultValue() throws MicroscopeDriverException
	{
		return deviceProperty.getDefaultValue();
	}
}
