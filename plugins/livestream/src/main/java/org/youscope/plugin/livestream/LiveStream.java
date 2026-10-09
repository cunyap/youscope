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
package org.youscope.plugin.livestream;

import java.awt.Component;

import org.youscope.addon.AddonException;
import org.youscope.addon.tool.ToolAddonUI;
import org.youscope.addon.tool.ToolMetadata;
import org.youscope.addon.tool.ToolMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.LiveStreamPanel;

/**
 * @author Moritz Lang
 */
class LiveStream implements ToolAddonUI
{
	private final YouScopeClient client;
	private final YouScopeServer server;
	private LiveStreamPanel liveStreamPanel = null;
	LiveStream(YouScopeClient client, YouScopeServer server)
	{
		this.client = client;
		this.server = server;
	}

	public final static String TYPE_IDENTIFIER = "YouScope.YouScopeLiveStream";
	
	static ToolMetadata getMetadata()
	{
		return new ToolMetadataAdapter(TYPE_IDENTIFIER, "YouScope LiveStream", new String[0], 
				"Displays the current camera image, with an option to change channel, exposure time and similar.",
				"icons/film.png");
	}

	@Override
	public YouScopeFrame toFrame() throws AddonException
	{
		liveStreamPanel = new LiveStreamPanel(client, server);
		liveStreamPanel.setUserChoosesFullScreen(true);
		return liveStreamPanel.toFrame();
	}

	@Override
	public Component toPanel(YouScopeFrame containingFrame) throws AddonException
	{
		liveStreamPanel = new LiveStreamPanel(client, server);
		liveStreamPanel.setUserChoosesFullScreen(false);
		containingFrame.addFrameListener(liveStreamPanel.getFrameListener());
		return liveStreamPanel;
	}

	@Override
	public ToolMetadata getAddonMetadata()
	{
		return getMetadata();
	}
}
