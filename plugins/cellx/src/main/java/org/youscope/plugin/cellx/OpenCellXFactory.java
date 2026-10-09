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
package org.youscope.plugin.cellx;

import java.awt.Desktop;

import org.youscope.addon.AddonException;
import org.youscope.addon.AddonMetadata;
import org.youscope.addon.postprocessing.PostProcessorAddonFactory;
import org.youscope.addon.tool.ToolAddonUI;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.saving.MeasurementFileLocations;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * @author Moritz Lang
 *
 */
public class OpenCellXFactory implements PostProcessorAddonFactory
{

	@Override
	public ToolAddonUI createPostProcessorUI(String ID, YouScopeClient client, YouScopeServer server, MeasurementFileLocations measurementFileLocations) throws AddonException
	{
		if(OpenCellX.TYPE_IDENTIFIER.equals(ID))
		{
			return new OpenCellX(client, server, measurementFileLocations.getMeasurementBaseFolder());
		}
		throw new AddonException("Type identifer "+ID+" not supported by this factory.");
	}

	@Override
	public String[] getSupportedTypeIdentifiers()
	{
		if(Desktop.isDesktopSupported())
			return new String[]{OpenCellX.TYPE_IDENTIFIER};
		return new String[0];
	}

	@Override
	public boolean isSupportingTypeIdentifier(String ID)
	{
		if(OpenCellX.TYPE_IDENTIFIER.equals(ID))
			return true;
		return false;
	}

	@Override
	public AddonMetadata getPostProcessorMetadata(String typeIdentifier) throws AddonException {
		if(OpenCellX.TYPE_IDENTIFIER.equals(typeIdentifier))
			return OpenCellX.getMetadata();
		throw new AddonException("Type identifer "+typeIdentifier+" not supported by this factory.");
	}

}
