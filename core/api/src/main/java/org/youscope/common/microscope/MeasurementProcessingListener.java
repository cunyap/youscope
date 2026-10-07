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
package org.youscope.common.microscope;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;

/**
 * Listener for the state of measurement processing.
 * @author Moritz Lang
 */
public interface MeasurementProcessingListener extends EventListener, Remote
{
    /**
     * Called when the queue of measurements changed.
     * 
     * @throws RemoteException
     */
    void measurementQueueChanged() throws RemoteException;

    /**
     * Called when the current measurement changed.
     * 
     * @throws RemoteException
     */
    void currentMeasurementChanged() throws RemoteException;

    /**
     * Called when measurement processing stopped.
     * @throws RemoteException 
     */
	void measurementProcessingStopped() throws RemoteException;
}
