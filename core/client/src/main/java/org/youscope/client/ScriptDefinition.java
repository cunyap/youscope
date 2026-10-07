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

import java.io.Serializable;

/**
 * @author Moritz Lang
 *
 */
class ScriptDefinition implements Cloneable, Serializable
{
	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= -8948143769514581841L;
	private String engine = "";
	private String scriptFile = "";
	private String shortCutFile = null;
	private String name = "";
	public ScriptDefinition(String name, String engine, String scriptFile)
	{
		this.setName(name);
		this.setEngine(engine);
		this.setScriptFile(scriptFile);
		this.setShortCutFile(name+".xml");
	}
	public void setEngine(String engine)
	{
		this.engine = engine;
	}
	public String getEngine()
	{
		return engine;
	}
	public void setScriptFile(String scriptFile)
	{
		this.scriptFile = scriptFile;
	}
	public String getScriptFile()
	{
		return scriptFile;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getName()
	{
		return name;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	@Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
	public void setShortCutFile(String shortCutFile)
	{
		this.shortCutFile = shortCutFile;
	}
	public String getShortCutFile()
	{
		return shortCutFile;
	}
}
