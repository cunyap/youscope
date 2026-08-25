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
package org.youscope.plugin.trackmatetracker;

import org.youscope.addon.AddonException;
import org.youscope.addon.celldetection.TrackingAddon;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * YouScope addon factory for the TrackMate tracker. Registers the addon under the
 * {@link TrackingAddon} interface, like the CellX / nearest-neighbour trackers.
 */
public class TrackmateTrackerAddonFactory extends ComponentAddonFactoryAdapter
{
	private static final CustomAddonCreator<TrackmateTrackerConfiguration, TrackingAddon> CREATOR =
		new CustomAddonCreator<TrackmateTrackerConfiguration, TrackingAddon>()
	{
		@Override
		public TrackingAddon createCustom(PositionInformation positionInformation,
				TrackmateTrackerConfiguration configuration, ConstructionContext constructionContext)
				throws ConfigurationException, AddonException
		{
			try
			{
				return new TrackmateTrackerAddon(positionInformation, configuration);
			}
			catch(Exception e)
			{
				throw new AddonException("Could not create TrackMate tracker addon.", e);
			}
		}

		@Override
		public Class<TrackingAddon> getComponentInterface()
		{
			return TrackingAddon.class;
		}
	};

	/** Constructor: Called by YouScope's plugin loader at server startup. */
	public TrackmateTrackerAddonFactory()
	{
		super(TrackmateTrackerConfigurationAddon.class, CREATOR, TrackmateTrackerConfigurationAddon.getMetadata());
	}
}
