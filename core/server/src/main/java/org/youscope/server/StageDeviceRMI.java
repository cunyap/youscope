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

import java.awt.geom.Point2D.Double;
import java.rmi.RemoteException;

import org.youscope.addon.microscopeaccess.StageDeviceInternal;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.StageDevice;

/**
 * @author langmo
 */
class StageDeviceRMI extends DeviceRMI implements StageDevice
{
    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1261971329588759872L;

    private StageDeviceInternal stageDevice;

    StageDeviceRMI(StageDeviceInternal stageDevice, MicroscopeRMI microscope, int accessID) throws RemoteException
    {
        super(stageDevice, microscope, accessID);
        this.stageDevice = stageDevice;
    }

    @Override
	public Double getPosition() throws RemoteException, MicroscopeException, InterruptedException
    {
        return stageDevice.getPosition();
    }

    @Override
	public void setPosition(double x, double y) throws RemoteException, MicroscopeLockedException,
            MicroscopeException, InterruptedException
    {
        stageDevice.setPosition(x, y, accessID);
    }

    @Override
	public void setRelativePosition(double dx, double dy) throws RemoteException,
            MicroscopeLockedException, MicroscopeException, InterruptedException
    {
        stageDevice.setRelativePosition(dx, dy, accessID);
    }

    @Override
	public void setTransposeX(boolean transpose) throws MicroscopeLockedException
    {
        stageDevice.setTransposeX(transpose, accessID);
    }

    @Override
	public void setTransposeY(boolean transpose) throws MicroscopeLockedException
    {
        stageDevice.setTransposeY(transpose, accessID);
    }

    @Override
	public boolean isTransposeX()
    {
        return stageDevice.isTransposeX();
    }

    @Override
	public boolean isTransposeY()
    {
        return stageDevice.isTransposeY();
    }

    @Override
	public double getUnitMagnifier()
    {
        return stageDevice.getUnitMagnifier();
    }

    @Override
	public void setUnitMagnifier(double unitMagnifier) throws MicroscopeLockedException
    {
        stageDevice.setUnitMagnifier(unitMagnifier, accessID);
    }

}
