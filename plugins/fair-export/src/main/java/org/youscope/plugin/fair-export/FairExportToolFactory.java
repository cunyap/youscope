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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/

package org.youscope.plugin.fairexport;

import org.youscope.addon.AddonException;
import org.youscope.addon.tool.ToolAddonFactory;
import org.youscope.addon.tool.ToolAddonUI;
import org.youscope.addon.tool.ToolMetadata;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * Factory registering the FAIR export tool.
 * <p>
 * Discovered through {@link java.util.ServiceLoader}: the file
 * {@code META-INF/services/org.youscope.addon.tool.ToolAddonFactory} names this
 * class. This is a
 * separate registration from the post-processor factory, because tool and
 * post-processor are distinct
 * SPIs; both point at the same {@link FairExportEngine}.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class FairExportToolFactory implements ToolAddonFactory {
	@Override
	public ToolAddonUI createToolUI(String typeIdentifier, YouScopeClient client, YouScopeServer server)
			throws AddonException {
		if (!isSupportingTypeIdentifier(typeIdentifier))
			throw new AddonException("Tool type identifier " + typeIdentifier + " is not supported by this factory.");
		return new FairExportTool(client, server);
	}

	@Override
	public String[] getSupportedTypeIdentifiers() {
		return new String[] { FairExportTool.TYPE_IDENTIFIER };
	}

	@Override
	public boolean isSupportingTypeIdentifier(String typeIdentifier) {
		return FairExportTool.TYPE_IDENTIFIER.equals(typeIdentifier);
	}

	@Override
	public ToolMetadata getToolMetadata(String typeIdentifier) throws AddonException {
		if (!isSupportingTypeIdentifier(typeIdentifier))
			throw new AddonException("Tool type identifier " + typeIdentifier + " is not supported by this factory.");
		return FairExportTool.getMetadata();
	}
}
