/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.cameditor;

import org.youscope.addon.tool.ToolAddonFactoryAdapter;

/**
 * Service factory that registers the {@link CustomArbitraryMicroplateEditorTool} with YouScope so it
 * appears in the Tools menu next to "Custom Microplates".
 *
 * @author Andreas P. Cuny
 */
public class CustomArbitraryMicroplateEditorToolFactory extends ToolAddonFactoryAdapter
{
	/**
	 * Constructor.
	 */
	public CustomArbitraryMicroplateEditorToolFactory()
	{
		super(CustomArbitraryMicroplateEditorTool.class, CustomArbitraryMicroplateEditorTool.getMetadata());
	}
}
