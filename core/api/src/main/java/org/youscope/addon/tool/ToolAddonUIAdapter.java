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
package org.youscope.addon.tool;

import org.youscope.addon.AddonException;
import org.youscope.addon.AddonUIAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * An adapter class to simplify tool development.
 * @author Moritz Lang
 *
 */
public abstract class ToolAddonUIAdapter extends AddonUIAdapter<ToolMetadata> implements ToolAddonUI 
{
	/**
	 * Constructor.
	 * @param metadata The metadata of the tool.
	 * @param client The YouScope client.
	 * @param server The YouScope server.
	 * @throws AddonException
	 */
	public ToolAddonUIAdapter(final ToolMetadata metadata,  final YouScopeClient client, final YouScopeServer server) throws AddonException 
	{
		super(metadata, client, server);
		setCloseButtonLabel("Close");
		setShowCloseButton(false);
	}
}
