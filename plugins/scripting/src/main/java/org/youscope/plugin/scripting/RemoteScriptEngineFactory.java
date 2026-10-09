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
package org.youscope.plugin.scripting;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.youscope.addon.callback.CallbackAddonFactory;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.callback.Callback;
import org.youscope.common.callback.CallbackCreationException;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * Factory to create remote script engines as callbacks.
 * @author Moritz Lang
 *
 */
public class RemoteScriptEngineFactory implements CallbackAddonFactory
{
	private String[] supportedTypeIdentifiers = null;
	private Hashtable<String, ScriptEngineFactory> scriptEngineFactories = null;
	private synchronized void initialize()
	{
		if(scriptEngineFactories != null)
			return;
		
		ScriptEngineManager mgr = new ScriptEngineManager(RemoteScriptEngineFactory.class.getClassLoader());		
		ArrayList<String> typeIdentifiers = new ArrayList<String>();
		scriptEngineFactories = new Hashtable<String, ScriptEngineFactory>();
		for(ScriptEngineFactory factory : mgr.getEngineFactories())
		{
			scriptEngineFactories.put(factory.getEngineName(), factory);
			typeIdentifiers.add(factory.getEngineName());
		}
		supportedTypeIdentifiers = typeIdentifiers.toArray(new String[typeIdentifiers.size()]);
	}
	
	@Override
	public Callback createCallback(String typeIdentifier, YouScopeClient client,
			YouScopeServer server) throws CallbackCreationException {
		initialize();
		ScriptEngineFactory factory = scriptEngineFactories.get(typeIdentifier);
		if(factory == null)
			throw new CallbackCreationException("Remote script engines with type identifiers " + typeIdentifier + " not supported by this callback factory.");
		try {
			return new RemoteScriptEngineImpl(factory.getScriptEngine(), typeIdentifier); 
		} catch (RemoteException e) {
			throw new CallbackCreationException("Remote exception occured while constructing remote script engine.", e);
		}
		catch(Throwable e)
		{
			throw new CallbackCreationException("Could not create script engine.", e);
		}
	}

	@Override
	public String[] getSupportedTypeIdentifiers() {
		initialize();
		return supportedTypeIdentifiers;
	}

	@Override
	public boolean isSupportingTypeIdentifier(String typeIdentifier) 
	{
		initialize();
		for(String supported : supportedTypeIdentifiers)
		{
			if(supported.equals(typeIdentifier))
				return true;
		}
		return false;
	}

}
