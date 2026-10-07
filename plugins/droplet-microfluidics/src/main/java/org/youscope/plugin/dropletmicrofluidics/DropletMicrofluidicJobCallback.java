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
package org.youscope.plugin.dropletmicrofluidics;

import java.rmi.RemoteException;

import org.youscope.common.ExecutionInformation;
import org.youscope.common.callback.Callback;
import org.youscope.common.callback.CallbackException;
import org.youscope.common.table.Table;

/**
 * Callback providing an UI for droplet based microfluidic measurements.
 * 
 * @author Moritz Lang
 */
public interface DropletMicrofluidicJobCallback extends Callback
{
	
	/**
	 * Type identifier for callback.
	 */
	public static final String	TYPE_IDENTIFIER	= "YouScope.DropletBasedMicrofluidicsJob.Callback";
    /**
     * Sends the table produced by a droplet based microfluidic job (see {@link DropletMicrofluidicTable#getTableDefinition()} to the callback for visualization.
     * @param executionInformation Execution information of droplet based microfluidics job.
     * @param table table containing droplet data.
     * @throws RemoteException
     * @throws CallbackException 
     */
    public void dropletMeasured(ExecutionInformation executionInformation, Table table) throws RemoteException, CallbackException;
}
