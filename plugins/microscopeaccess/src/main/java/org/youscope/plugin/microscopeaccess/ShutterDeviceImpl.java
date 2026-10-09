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

import mmcorej.CMMCore;

import org.youscope.addon.microscopeaccess.ShutterDeviceInternal;
import org.youscope.common.microscope.DeviceType;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * @author Moritz Lang
 *
 */
class ShutterDeviceImpl extends DeviceImpl implements ShutterDeviceInternal
{
	ShutterDeviceImpl(MicroscopeImpl microscope, String deviceName, String libraryID, String driverID, HubDeviceImpl hub)
	{
		super(microscope, deviceName, libraryID, driverID, DeviceType.ShutterDevice, hub);
	}

	@Override
	public void setOpen(boolean open, int accessID) throws MicroscopeException, MicroscopeLockedException, InterruptedException
	{
		if(Thread.interrupted())
			throw new InterruptedException();
		try
		{
			CMMCore core = microscope.startWrite(accessID);
			core.setShutterDevice(getDeviceID());
			core.setShutterOpen(open);
		}
		catch(MicroscopeLockedException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new MicroscopeException("Could not open/close shutter device " + getDeviceID() + ".", e);
		}
		finally
		{
			deviceStateModified();
			microscope.unlockWrite();
		}
	}

	@Override
	public boolean isOpen() throws MicroscopeException, InterruptedException
	{
		if(Thread.interrupted())
			throw new InterruptedException();
		try
		{
			CMMCore core = microscope.startRead();
			core.setShutterDevice(getDeviceID());
			return core.getShutterOpen();
		}
		catch(Exception e)
		{
			throw new MicroscopeException("Could not detect if shutter device " + getDeviceID() + " is open.", e);
		}
		finally
		{
			microscope.unlockRead();
		}
	}
}
