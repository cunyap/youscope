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
package org.youscope.server;

import java.rmi.RemoteException;

import org.youscope.addon.microscopeaccess.SerialDeviceInternal;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.SerialDevice;

/**
 * @author Moritz Lang
 * 
 */
class SerialDeviceRMI extends DeviceRMI implements SerialDevice
{

	/**
	 * Serial Version UID.
	 */
	private static final long		serialVersionUID	= -7624821113540201850L;

	private SerialDeviceInternal	serialDevice;

	SerialDeviceRMI(SerialDeviceInternal serialDevice, MicroscopeRMI microscope, int accessID) throws RemoteException
	{
		super(serialDevice, microscope, accessID);
		this.serialDevice = serialDevice;
	}

	@Override
	public void sendCommand(String command) throws MicroscopeLockedException, MicroscopeException, InterruptedException, RemoteException
	{
		serialDevice.sendCommand(command, accessID);
	}

}
