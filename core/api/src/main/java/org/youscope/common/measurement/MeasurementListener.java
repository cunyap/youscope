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
package org.youscope.common.measurement;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;

/**
 * A listener which gets notified about the progress of a measurement.
 * 
 * @author Moritz Lang
 */
public interface MeasurementListener extends EventListener, Remote
{
	/**
	 * Function gets invoked when state of measurement changed.
	 * @param oldState Old state before the change.
	 * @param newState New state.
	 * @throws RemoteException
	 */
	void measurementStateChanged(MeasurementState oldState, MeasurementState newState) throws RemoteException;
	
	/**
	 * Function gets invoked if error occurs during the measurement. 
	 * 
	 * @param e Object describing the error.
	 * @throws RemoteException
	 */
	void measurementError(Exception e) throws RemoteException;
	
	/**
     * Function gets invoked when the structure of a measurement is modified during the execution of the measurement.
     * The structure is e.g. modified if sub-jobs are added or removed.
     * 
     * @throws RemoteException
     */
    void measurementStructureModified() throws RemoteException;
    
	
}
