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

import org.youscope.addon.microscopeaccess.StateDeviceInternal;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.StateDevice;

/**
 * @author langmo
 */
class StateDeviceRMI extends DeviceRMI implements StateDevice
{
    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1756414115236261908L;

    private StateDeviceInternal stateDevice;

    StateDeviceRMI(StateDeviceInternal stateDevice, MicroscopeRMI microscope, int accessID) throws RemoteException
    {
        super(stateDevice, microscope, accessID);
        this.stateDevice = stateDevice;
    }

    @Override
	public int getNumStates() throws RemoteException
    {
        return stateDevice.getNumStates();
    }

    @Override
	public int getState() throws MicroscopeException, RemoteException
    {
        return stateDevice.getState();
    }

    @Override
	public String[] getStateLabels() throws RemoteException
    {
        return stateDevice.getStateLabels();
    }

    @Override
	public String getStateLabel() throws MicroscopeException, RemoteException
    {
        return stateDevice.getStateLabel();
    }

    @Override
	public String getStateLabel(int state) throws RemoteException
    {
        return stateDevice.getStateLabel(state);
    }

    @Override
	public void setStateLabel(int state, String label) throws MicroscopeException, RemoteException,
            MicroscopeLockedException
    {
        stateDevice.setStateLabel(state, label, accessID);
    }

    @Override
	public void setStateLabels(String[] labels) throws MicroscopeException, RemoteException,
            MicroscopeLockedException
    {
        stateDevice.setStateLabels(labels, accessID);
    }

    @Override
	public void setState(int state) throws MicroscopeException, RemoteException,
            MicroscopeLockedException
    {
        stateDevice.setState(state, accessID);
    }

    @Override
	public void setState(String label) throws MicroscopeException, RemoteException,
            MicroscopeLockedException, DeviceException
    {
        stateDevice.setState(label, accessID);
    }
}
