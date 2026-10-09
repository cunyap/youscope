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
 *     Andereas P. Cuny - add calibration factor
 ******************************************************************************/
/**
 * 
 */
package org.youscope.server;

import java.rmi.RemoteException;

import org.youscope.addon.microscopeaccess.FocusDeviceInternal;
import org.youscope.common.microscope.FocusDevice;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * @author langmo
 */
class FocusDeviceRMI extends DeviceRMI implements FocusDevice {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -7624825693540201850L;

    private FocusDeviceInternal focusDevice;

    FocusDeviceRMI(FocusDeviceInternal focusDevice, MicroscopeRMI microscope, int accessID) throws RemoteException {
        super(focusDevice, microscope, accessID);
        this.focusDevice = focusDevice;
    }

    @Override
    public double getFocusPosition() throws MicroscopeException, InterruptedException {
        return focusDevice.getFocusPosition();
    }

    @Override
    public void setFocusPosition(double position) throws MicroscopeLockedException,
            MicroscopeException, InterruptedException {
        focusDevice.setFocusPosition(position, accessID);

    }

    @Override
    public void setRelativeFocusPosition(double offset) throws MicroscopeLockedException,
            RemoteException, MicroscopeException, InterruptedException {
        focusDevice.setRelativeFocusPosition(offset, accessID);
    }

    @Override
    public double getUnitMagnifier() {
        return focusDevice.getUnitMagnifier();
    }

    @Override
    public void setUnitMagnifier(double unitMagnifier) throws MicroscopeLockedException {
        focusDevice.setUnitMagnifier(unitMagnifier, accessID);
    }
}
