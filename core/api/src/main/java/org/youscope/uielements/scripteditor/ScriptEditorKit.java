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
/**
 * 
 */
package org.youscope.uielements.scripteditor;

import java.util.ServiceLoader;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;

import org.youscope.common.scripting.ScriptStyle;

/**
 * @author langmo
 * 
 */
class ScriptEditorKit extends StyledEditorKit 
{
	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= -3142181509254481343L;
	
	private final ScriptStyle scriptStyle;
	public ScriptEditorKit(ScriptStyle scriptStyle)
	{
		this.scriptStyle = scriptStyle;
	}
	
	public static ScriptEditorKit getEditorKitByFileNameExtension(String fileNameExtension)
	{
		if(fileNameExtension != null && fileNameExtension.length() > 0 && !fileNameExtension.equals("."))
		{
			for(ScriptStyle scriptStyle : getScriptStyles())
			{
				if(scriptStyle.supportsFileNameExtension(fileNameExtension))
				{
					return new ScriptEditorKit(scriptStyle);
				}
			}
		}
		// Return default (plain) editor kit.
		return new ScriptEditorKit(null);
	}
	
	public static ScriptEditorKit getEditorKitByScriptStyleID(String scriptStyleID)
	{
		if(scriptStyleID != null && scriptStyleID.length() > 0)
		{
			for(ScriptStyle scriptStyle : getScriptStyles())
			{
				if(scriptStyle.getScriptStyleID().compareToIgnoreCase(scriptStyleID) == 0)
				{
					return new ScriptEditorKit(scriptStyle);
				}
			}
		}
		// Return default (plain) editor kit.
		return new ScriptEditorKit(null);
	}
	
	@Override
	public ViewFactory getViewFactory()
	{
		return new ScriptEditorViewFactory();
	}
	@Override
	public Document createDefaultDocument()
	{
		if(scriptStyle == null)
			return new DefaultStyledDocument();
		return new ScriptEditorDocument(scriptStyle);
	}
	
	private static Iterable<ScriptStyle> getScriptStyles()
    {
        ServiceLoader<ScriptStyle> scriptStyles = ServiceLoader.load(ScriptStyle.class, ScriptEditorKit.class.getClassLoader());
        return scriptStyles;
    }
}
