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
package org.youscope.common.task;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;

/**
 * A listener getting notified when execution of a task starts and when it finishes.
 * @author Moritz Lang
 * 
 */
public interface TaskListener extends EventListener, Remote
{	

	/**
	 * Called when the first job of a task starts to be executed.
	 * @param executionNumber The number of times the task has already been executed, starting at zero.
	 * @throws RemoteException
	 */
	void taskStarted(long executionNumber) throws RemoteException;
	
	/**
	 * Called when the last job of a task stops to be executed.
	 * @param executionNumber The number of times the task has already been executed, starting at zero.
	 * @throws RemoteException
	 */
	void taskFinished(long executionNumber) throws RemoteException;
}
