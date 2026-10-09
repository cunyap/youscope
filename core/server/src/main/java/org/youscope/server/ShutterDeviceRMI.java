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

import org.youscope.addon.microscopeaccess.ShutterDeviceInternal;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.ShutterDevice;

/**
 * @author Moritz Lang
 */
class ShutterDeviceRMI extends DeviceRMI implements ShutterDevice
{
    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -7502926052853797709L;

    private final ShutterDeviceInternal shutterDevice;

    ShutterDeviceRMI(ShutterDeviceInternal shutterDevice, MicroscopeRMI microscope, int accessID) throws RemoteException
    {
        super(shutterDevice, microscope, accessID);
        this.shutterDevice = shutterDevice;
    }

    @Override
	public void setOpen(boolean open) throws MicroscopeException, MicroscopeLockedException,
            InterruptedException, RemoteException
    {
        shutterDevice.setOpen(open, accessID);
    }

    @Override
	public boolean isOpen() throws MicroscopeException, RemoteException, InterruptedException
    {
        return shutterDevice.isOpen();
    }
}
