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

import org.youscope.addon.microscopeaccess.AutoFocusDeviceInternal;
import org.youscope.common.microscope.AutoFocusDevice;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * @author Moritz Lang
 */
class AutoFocusDeviceRMI extends DeviceRMI implements AutoFocusDevice
{

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 9144637057274529331L;

    private final AutoFocusDeviceInternal autoFocusDevice;

    AutoFocusDeviceRMI(AutoFocusDeviceInternal autoFocusDevice, MicroscopeRMI microscope, int accessID)
            throws RemoteException
    {
        super(autoFocusDevice, microscope, accessID);
        this.autoFocusDevice = autoFocusDevice;
    }

    @Override
	public double getLastScore() throws MicroscopeException, RemoteException
    {
        return autoFocusDevice.getLastScore();
    }

    @Override
	public double getCurrentScore() throws MicroscopeException, RemoteException
    {
        return autoFocusDevice.getCurrentScore();
    }

    @Override
	public void setEnabled(boolean enable) throws MicroscopeException, MicroscopeLockedException,
            RemoteException
    {
        autoFocusDevice.setEnabled(enable, accessID);
    }

    @Override
	public boolean isEnabled() throws MicroscopeException, RemoteException
    {
        return autoFocusDevice.isEnabled();
    }

    @Override
	public boolean isLocked() throws MicroscopeException, RemoteException
    {
        return autoFocusDevice.isLocked();
    }

    @Override
	public void runFullFocus() throws MicroscopeException, MicroscopeLockedException,
            RemoteException
    {
        autoFocusDevice.runFullFocus(accessID);
    }

    @Override
	public void runIncrementalFocus() throws MicroscopeException, MicroscopeLockedException,
            RemoteException
    {
        autoFocusDevice.runIncrementalFocus(accessID);
    }

    @Override
	public void setOffset(double offset) throws MicroscopeException, MicroscopeLockedException,
            RemoteException
    {
        autoFocusDevice.setOffset(offset, accessID);
    }

    @Override
	public double getOffset() throws MicroscopeException, RemoteException
    {
        return autoFocusDevice.getOffset();
    }
}
