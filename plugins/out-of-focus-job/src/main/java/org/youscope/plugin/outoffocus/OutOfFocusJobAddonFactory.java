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
/**
 * 
 */
package org.youscope.plugin.outoffocus;

import java.rmi.RemoteException;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.ComponentRunningException;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * @author Moritz Lang
 */
public class OutOfFocusJobAddonFactory extends ComponentAddonFactoryAdapter 
{
	private static final CustomAddonCreator<OutOfFocusJobConfiguration, OutOfFocusJob> CREATOR = new CustomAddonCreator<OutOfFocusJobConfiguration, OutOfFocusJob>()
	{

		@Override
		public OutOfFocusJob createCustom(PositionInformation positionInformation,
				OutOfFocusJobConfiguration configuration, ConstructionContext constructionContext)
						throws ConfigurationException, AddonException 
		{
			try
			{
				OutOfFocusJobImpl job = new OutOfFocusJobImpl(positionInformation);
				
				if(configuration.getFocusConfiguration()==null)
				{
					job.setFocusDevice(null);
					job.setFocusAdjustmentTime(0);
				}
				else
				{
					job.setFocusDevice(configuration.getFocusConfiguration().getFocusDevice());
					job.setFocusAdjustmentTime(configuration.getFocusConfiguration().getAdjustmentTime());
				}
				job.setOffset(configuration.getPosition());
				
				job.setChannel(configuration.getChannelGroup(), configuration.getChannel());
				job.setExposure(configuration.getExposure());
				if(configuration.isSaveImages())
				{
					job.addImageListener(constructionContext.getMeasurementSaver().getSaveImageListener(configuration.getImageSaveName()));
					job.setImageDescription(configuration.getImageSaveName() + " (" + job.getImageDescription() + ")");
				}
				
				return job;
				
			}
			catch(RemoteException e)
			{
				throw new AddonException("Could not create job due to remote exception.", e);
			} catch (ComponentRunningException e) {
				throw new AddonException("Could not initialize newly created job since job is already running.", e);
			}
		}

		@Override
		public Class<OutOfFocusJob> getComponentInterface() {
			return OutOfFocusJob.class;
		}
		
	};

	/**
	 * Constructor.
	 */
	public OutOfFocusJobAddonFactory()
	{
		super(OutOfFocusJobConfigurationAddon.class, CREATOR, OutOfFocusJobConfigurationAddon.getMetadata());
	}
}
