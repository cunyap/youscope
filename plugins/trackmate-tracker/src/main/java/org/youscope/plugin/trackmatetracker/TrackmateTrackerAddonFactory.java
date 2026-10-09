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
