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
package org.youscope.plugin.waitsincelastaction;

import java.rmi.RemoteException;

import org.youscope.common.ComponentRunningException;
import org.youscope.common.job.Job;


/**
 * This job stops the time since it was executed in any well, and waits until the time difference since the last execution
 * passes a threshold.
 * 
 * @author Moritz Lang
 */
public interface WaitSinceLastActionJob extends Job
{
	/**
	 * Returns the wait time in ms.
	 * @return Wait time in ms.
	 * @throws RemoteException 
	 */
	public long getWaitTime() throws RemoteException;
		
	/**
	 * Sets the wait time in ms. Must be larger or equal 0.
	 * @param waitTime Wait time in ms.
	 * @throws RemoteException 
	 * @throws ComponentRunningException 
	 */
	public void setWaitTime(long waitTime) throws RemoteException, ComponentRunningException;
	
	
	/**
	 * Returns the initial wait time in ms.
	 * @return initial wait time in ms.
	 * @throws RemoteException 
	 */
	public long getInitialWaitTime() throws RemoteException;
	

	/**
	 * Sets the initial wait time in ms.
	 * @param initialWaitTime the wait time at the first execution
	 * @throws RemoteException 
	 * @throws ComponentRunningException 
	 */
	public void setInitialWaitTime(long initialWaitTime) throws RemoteException, ComponentRunningException;
	

	/**
	 * Returns true if the wait timer is reset after a complete iteration.
	 * @return True if wait timer is reset.
	 * @throws RemoteException 
	 */
	public boolean isResetAfterIteration() throws RemoteException;
	

	/**
	 * Set to true to reset the wait timer after each iteration.
	 * @param resetAfterIteration true to reset wait timer.
	 * @throws RemoteException 
	 * @throws ComponentRunningException 
	 */
	public void setResetAfterIteration(boolean resetAfterIteration) throws RemoteException, ComponentRunningException;
	

	/**
	 * Returns the ID of this action. The job waits with respect to the execution time of the last action with the same ID.
	 * @return ID of action.
	 * @throws RemoteException 
	 */
	public int getActionID()  throws RemoteException;
	

	/**
	 * Sets the ID of this action. The job waits with respect to the execution time of the last action with the same ID.
	 * @param actionID ID of action.
	 * @throws RemoteException 
	 * @throws ComponentRunningException 
	 */
	public void setActionID(int actionID) throws RemoteException, ComponentRunningException;
	
}
