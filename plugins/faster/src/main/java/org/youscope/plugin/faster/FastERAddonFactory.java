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
