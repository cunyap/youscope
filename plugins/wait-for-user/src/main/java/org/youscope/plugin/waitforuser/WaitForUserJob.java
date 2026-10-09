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
package org.youscope.plugin.waitforuser;

import java.rmi.RemoteException;

import org.youscope.common.ComponentRunningException;
import org.youscope.common.job.Job;


/**
 * @author langmo
 */
public interface WaitForUserJob extends Job
{

	/**
	 * Returns the message displayed to the user.
	 * 
	 * @return message to be displayed.
	 * @throws RemoteException
	 */
	String getMessage() throws RemoteException;

	/**
	 * Sets the message to be displayed to the user.
	 * 
	 * @param message Message to be displayed.
	 * @throws RemoteException
	 * @throws ComponentRunningException 
	 */
	void setMessage(String message) throws RemoteException,	ComponentRunningException;
	
	/**
	 * Sets the callback to the client used by this job. Must be set before initialization.
	 * @param callback Callback to be used to wait for the user.
	 * @throws RemoteException
	 * @throws ComponentRunningException 
	 */
	void setMeasurementCallback(WaitForUserCallback callback) throws RemoteException, ComponentRunningException;
}
