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
package org.youscope.plugin.dropletmicrofluidics;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;

import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.ExecutionInformation;
import org.youscope.common.callback.CallbackException;
import org.youscope.common.table.Table;
class DropletMicrofluidicJobCallbackImpl implements DropletMicrofluidicJobCallback
{
    private final YouScopeClient client;
    private int chipID = 1;
    private static final HashMap<Integer, DropletMicrofluidicJobCallbackUI> userInterfaces = new HashMap<>();
    
    DropletMicrofluidicJobCallbackImpl(final YouScopeClient client)
    {
        this.client = client;
    }
    
    @Override
	public void dropletMeasured(ExecutionInformation executionInformation, Table table) throws CallbackException
	{
    	DropletMicrofluidicJobCallbackUI ui = userInterfaces.get(chipID);
    	if(ui == null)
    		throw new CallbackException("Callback not initialized");
    	ui.dropletMeasured(executionInformation, table);
	}

	@Override
	public void pingCallback() throws RemoteException {
		// do nothing.
	}

	@Override
	public synchronized void initializeCallback(Serializable... arguments) throws RemoteException, CallbackException 
	{
		if(arguments != null && arguments.length > 0 && arguments[0] instanceof Integer)
		{
			chipID = (Integer)arguments[0];
		}
		
		DropletMicrofluidicJobCallbackUI ui;
		synchronized(userInterfaces)
		{
			ui = userInterfaces.get(chipID);
			if(ui == null || ui.isReceivedData())
			{
				int[] connectedSyringes = null;
				if(arguments != null && arguments.length > 1 && arguments[1] instanceof int[])
				{
					connectedSyringes = (int[])arguments[1];
				}
				
				ui = new DropletMicrofluidicJobCallbackUI(client, chipID, connectedSyringes);
				userInterfaces.put(chipID, ui);
			}
		}
		ui.initializeCallback(arguments);
	}

	@Override
	public synchronized void uninitializeCallback() throws RemoteException, CallbackException 
	{
		DropletMicrofluidicJobCallbackUI ui = userInterfaces.get(chipID);
		if(ui != null)
			ui.uninitializeCallback();
	}

	@Override
	public String getTypeIdentifier() throws RemoteException {
		return TYPE_IDENTIFIER;
	}
}
