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
package org.youscope.server;

/**
 * @author langmo
 * 
 */
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.script.ScriptEngineFactory;

import org.youscope.addon.serveraddon.ServerAddon;
import org.youscope.serverinterfaces.YouScopeServerProperties;

/**
 * @author langmo
 */
class ServerConfigurationImpl extends UnicastRemoteObject implements YouScopeServerProperties
{

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 856785424972712007L;

    /**
     * @throws RemoteException
     */
    protected ServerConfigurationImpl() throws RemoteException
    {
        super();
    }

    @Override
	public String[] getSupportedImageFormats()
    {
    	String[] fileSuffixes = ImageIO.getWriterFileSuffixes();
    	HashSet<String> uniqueSuffixes = new HashSet<>(fileSuffixes.length);
    	for(String fileSuffix : fileSuffixes)
    	{
    		if(fileSuffix!= null && fileSuffix.length() >= 1)
    			uniqueSuffixes.add(fileSuffix.toLowerCase());
    	}
    	fileSuffixes = uniqueSuffixes.toArray(new String[uniqueSuffixes.size()]);
    	Arrays.sort(fileSuffixes);
        return fileSuffixes;
    }

    @Override
	public byte[] getIP() throws UnknownHostException
    {
        InetAddress addr = InetAddress.getLocalHost();
        // Get IP Address
        return addr.getAddress();
    }

    @Override
	public String[] getSupportedScriptEngines() throws RemoteException
    {
        Vector<String> engineNames = new Vector<String>();
        for (ScriptEngineFactory factory : ServerSystem.getScriptEngineManager()
                .getEngineFactories())
        {
            engineNames.add(factory.getEngineName());
        }

        return engineNames.toArray(new String[engineNames.size()]);
    }

    @Override
	public <T extends ServerAddon> T getServerAddon(Class<T> addonInterface)
            throws RemoteException
    {
        return ServerSystem.getGeneralAddon(addonInterface);
    }

    @Override
	public <T extends ServerAddon> T[] getServerAddons(Class<T> addonInterface)
            throws RemoteException
    {
        return ServerSystem.getGeneralAddons(addonInterface);
    }

    @Override
	public ServerAddon[] getGeneralAddons() throws RemoteException
    {
        return ServerSystem.getGeneralAddons();
    }
}
