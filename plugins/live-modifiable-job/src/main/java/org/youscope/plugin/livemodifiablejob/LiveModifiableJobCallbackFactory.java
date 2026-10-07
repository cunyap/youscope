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

import org.youscope.addon.callback.CallbackAddonFactory;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.callback.Callback;
import org.youscope.common.callback.CallbackCreationException;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * @author Moritz Lang
 */
public class LiveModifiableJobCallbackFactory implements CallbackAddonFactory
{
    private LiveModifiableJobCallbackImpl callback = null;
	
	@Override
	public String[] getSupportedTypeIdentifiers() {
		return new String[]{LiveModifiableJobCallback.TYPE_IDENTIFIER};
	}

	@Override
	public boolean isSupportingTypeIdentifier(String typeIdentifier) {
		return LiveModifiableJobCallback.TYPE_IDENTIFIER.equals(typeIdentifier);
	}

	@Override
	public synchronized Callback createCallback(String typeIdentifier,
			YouScopeClient client, YouScopeServer server) throws CallbackCreationException {
		if(!isSupportingTypeIdentifier(typeIdentifier))
			throw new CallbackCreationException("Factory does not support type identifier "+typeIdentifier+".");
		if(callback != null)
			return callback;
		try {
			callback = new LiveModifiableJobCallbackImpl(client, server);
		} catch (RemoteException e) {
			throw new CallbackCreationException("Could not create callback due to remote exception.", e);
		}
		return callback;
	}
}
