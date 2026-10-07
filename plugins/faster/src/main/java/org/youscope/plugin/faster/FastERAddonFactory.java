/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
/**
 * 
 */
package org.youscope.plugin.faster;

import org.youscope.addon.AddonException;
import org.youscope.addon.celldetection.CellDetectionAddon;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * YouScope addon factory for the FastER segmenter plugin. Registers the addon
 * under the {@link CellDetectionAddon} interface, exactly like Cellpose/StarDist.
 */
public class FastERAddonFactory extends ComponentAddonFactoryAdapter
{
	private static final CustomAddonCreator<FastERConfiguration, CellDetectionAddon> CREATOR =
		new CustomAddonCreator<FastERConfiguration, CellDetectionAddon>()
	{
		@Override
		public CellDetectionAddon createCustom(PositionInformation positionInformation,
				FastERConfiguration configuration, ConstructionContext constructionContext)
				throws ConfigurationException, AddonException
		{
			try
			{
				return new FastERAddon(positionInformation, configuration);
			}
			catch(Exception e)
			{
				throw new AddonException("Could not create FastER detection addon.", e);
			}
		}

		@Override
		public Class<CellDetectionAddon> getComponentInterface()
		{
			return CellDetectionAddon.class;
		}
	};

	/** Constructor: Called by YouScope's plugin loader at server startup. */
	public FastERAddonFactory()
	{
		super(FastERConfigurationAddon.class, CREATOR, FastERConfigurationAddon.getMetadata());
	}
}
