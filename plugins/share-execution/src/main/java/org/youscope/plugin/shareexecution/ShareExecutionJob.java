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
package org.youscope.plugin.shareexecution;

import java.rmi.RemoteException;

import org.youscope.common.ComponentRunningException;
import org.youscope.common.job.CompositeJob;
import org.youscope.common.job.Job;


/**
 * A job which executes its child job only for a certain share of all positions per execution
 * 
 * @author Moritz Lang
 */
public interface ShareExecutionJob extends Job, CompositeJob
{
	/**
	 * Returns the number of times this job gets totally executed per iteration.
	 * @return share of executions per iteration.
	 * @throws RemoteException 
	 */
	public int getNumShare() throws RemoteException;

	/**
	 * Sets the number of times this job gets totally executed per iteration.
	 * @param numShare share of executions per iteration.
	 * @throws RemoteException 
	 * @throws ComponentRunningException 
	 */
	public void setNumShare(int numShare) throws RemoteException, ComponentRunningException;


	/**
	 * Returns the ID for this share job. Share jobs with different IDs act independently from one another.
	 * @return share ID of this job.
	 * @throws RemoteException 
	 */
	public int getShareID() throws RemoteException;


	/**
	 * Sets the ID for this share job. Share jobs with different IDs act independently from one another.
	 * @param shareID share ID of this job.
	 * @throws RemoteException 
	 * @throws ComponentRunningException 
	 */
	public void setShareID(int shareID) throws RemoteException, ComponentRunningException;
	
	/**
	 * Returns true if for each well the share of the jobs which get executed is determined separately.
	 * @return True if different counting for each well.
	 * @throws RemoteException 
	 */
	public boolean isSeparateForEachWell() throws RemoteException;
	

	/**
	 * If set to true, for each well the share of the jobs which get executed is determined separately.
	 * @param separateForEachWell True if different counting for each well.
	 * @throws RemoteException 
	 * @throws ComponentRunningException 
	 */
	public void setSeparateForEachWell(boolean separateForEachWell) throws RemoteException, ComponentRunningException;
	

}
