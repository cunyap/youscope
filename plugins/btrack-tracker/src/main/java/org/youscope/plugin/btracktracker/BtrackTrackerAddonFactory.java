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
package org.youscope.plugin.btracktracker;

import org.youscope.addon.AddonException;
import org.youscope.addon.celldetection.TrackingAddon;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * YouScope addon factory for the btrack tracker. Registers the addon under the
 * {@link TrackingAddon} interface, like the CellX / nearest-neighbour trackers.
 */
public class BtrackTrackerAddonFactory extends ComponentAddonFactoryAdapter
{
	private static final CustomAddonCreator<BtrackTrackerConfiguration, TrackingAddon> CREATOR =
		new CustomAddonCreator<BtrackTrackerConfiguration, TrackingAddon>()
	{
		@Override
		public TrackingAddon createCustom(PositionInformation positionInformation,
				BtrackTrackerConfiguration configuration, ConstructionContext constructionContext)
				throws ConfigurationException, AddonException
		{
			try
			{
				return new BtrackTrackerAddon(positionInformation, configuration);
			}
			catch(Exception e)
			{
				throw new AddonException("Could not create btrack tracker addon.", e);
			}
		}

		@Override
		public Class<TrackingAddon> getComponentInterface()
		{
			return TrackingAddon.class;
		}
	};

	/** Constructor: Called by YouScope's plugin loader at server startup. */
	public BtrackTrackerAddonFactory()
	{
		super(BtrackTrackerConfigurationAddon.class, CREATOR, BtrackTrackerConfigurationAddon.getMetadata());
	}
}
