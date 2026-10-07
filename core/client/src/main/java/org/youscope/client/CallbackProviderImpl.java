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
package org.youscope.client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.ServiceLoader;

import org.youscope.addon.callback.CallbackAddonFactory;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.callback.Callback;
import org.youscope.common.callback.CallbackCreationException;
import org.youscope.common.callback.CallbackProvider;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * @author Moritz Lang
 *
 */
class CallbackProviderImpl extends UnicastRemoteObject implements CallbackProvider
{

	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= 6240666905518828924L;
	
	private HashMap<String, CallbackAddonFactory> callbackFactories = null;

	private final YouScopeClient client;
	private final  YouScopeServer server;
	/**
	 * @throws RemoteException
	 */
	protected CallbackProviderImpl(YouScopeClient client, YouScopeServer server) throws RemoteException
	{
		super();
		this.client = client;
		this.server = server;
	}
	
	private void initialize()
	{
		if(callbackFactories != null)
			return;
		callbackFactories = new HashMap<String, CallbackAddonFactory>();
		for(CallbackAddonFactory callbackFactory : ServiceLoader.load(CallbackAddonFactory.class, CallbackProviderImpl.class.getClassLoader()))
		{
			for(String typeIdentifier : callbackFactory.getSupportedTypeIdentifiers())
			{
				callbackFactories.put(typeIdentifier, callbackFactory);
			}
		}
	}

	@Override
	public Callback createCallback(String typeIdentifier) throws CallbackCreationException {
		initialize();
		CallbackAddonFactory factory = callbackFactories.get(typeIdentifier);
		if(factory == null)
			throw new CallbackCreationException("No callback available with type identifier "+typeIdentifier+".");
		return factory.createCallback(typeIdentifier, client, server);
	}

	@Override
	public <T extends Callback> T createCallback(String typeIdentifier, Class<T> callbackInterface)
			throws RemoteException, CallbackCreationException {
		Callback callback = createCallback(typeIdentifier);
		if(callbackInterface.isInstance(callback))
			return callbackInterface.cast(callback);
		throw new CallbackCreationException("Callback with callback type identifier " + typeIdentifier+" is of class "+callback.getClass().getName()+", which does not implement interface "+callbackInterface.getName()+".");
	}
}
