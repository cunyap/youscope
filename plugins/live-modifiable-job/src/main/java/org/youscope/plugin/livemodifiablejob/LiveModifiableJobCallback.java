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
package org.youscope.plugin.livemodifiablejob;

import java.rmi.RemoteException;

import org.youscope.common.callback.Callback;
import org.youscope.common.callback.CallbackException;

/**
 * Callback for registering jobs which can be live modified.
 * 
 * @author Moritz Lang
 */
public interface LiveModifiableJobCallback extends Callback
{
	
	/**
	 * type identifier of callback.
	 */
	public static final String TYPE_IDENTIFIER = "YouScope.LiveModifiableJob.Callback";
    /**
     * Registers a job to be modifiable by the UI.
     * 
     * @param job job to be registered
     * @throws RemoteException
     * @throws CallbackException 
     */
    public void registerJob(LiveModifiableJob job) throws RemoteException, CallbackException;
    
    /**
     * Registers a job to be modifiable by the UI.
     * 
     * @param job job to be unregistered
     * @throws RemoteException
     * @throws CallbackException 
     */
    public void unregisterJob(LiveModifiableJob job) throws RemoteException, CallbackException;
}
