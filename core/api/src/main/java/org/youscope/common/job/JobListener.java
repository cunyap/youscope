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
package org.youscope.common.job;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;

import org.youscope.common.ExecutionInformation;

/**
 * A listener to get informed about the state of a job.
 * @author langmo
 * 
 */
public interface JobListener extends EventListener, Remote
{
	/**
	 * Called when job is initialized.
	 * @throws RemoteException
	 */
	void jobInitialized() throws RemoteException;

	/**
	 * Called when job is uninitialized.
	 * Might be called more than once.
	 * @throws RemoteException
	 */
	void jobUninitialized() throws RemoteException;

	/**
	 * Called when evaluation of job is started.
	 * @param executionInformation Information about the number of the execution of the job.
	 * @throws RemoteException
	 */
	void jobStarted(ExecutionInformation executionInformation) throws RemoteException;

	/**
	 * Called when one evaluation of the job is finished.
	 * @param executionInformation Information about the number of the execution of the job.
	 * @throws RemoteException
	 */
	void jobFinished(ExecutionInformation executionInformation) throws RemoteException;
}
