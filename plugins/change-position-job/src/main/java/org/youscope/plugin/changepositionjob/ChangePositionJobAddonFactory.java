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
package org.youscope.plugin.changepositionjob;

import java.rmi.RemoteException;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.ComponentRunningException;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.job.basicjobs.ChangePositionJob;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * @author Moritz Lang
 */
public class ChangePositionJobAddonFactory extends ComponentAddonFactoryAdapter 
{
	/**
	 * Constructor.
	 */
	public ChangePositionJobAddonFactory()
	{
		super(ChangePositionJobConfigurationAddon.class, CREATOR, ChangePositionJobConfigurationAddon.getMetadata());
	}
	
	private static final CustomAddonCreator<ChangePositionJobConfiguration, ChangePositionJob> CREATOR = new CustomAddonCreator<ChangePositionJobConfiguration,ChangePositionJob>()
	{
		@Override
		public ChangePositionJob createCustom(PositionInformation positionInformation, ChangePositionJobConfiguration configuration,
				ConstructionContext constructionContext) throws ConfigurationException, AddonException 
		{
			ChangePositionJob job;
			try
			{
				job = new ChangePositionJobImpl(positionInformation);
				if(configuration.isAbsolute())
					job.setPosition(configuration.getX(), configuration.getY());
				else
					job.setRelativePosition(configuration.getX(), configuration.getY());
				job.setStageDevice(configuration.getStageDevice());
			}
			catch(ComponentRunningException e)
			{
				throw new AddonException("Could not create change position job since newly created job is already running.", e);
			} catch (RemoteException e) {
				throw new AddonException("Could not create change position job due to remote exception.", e);
			}
			return job;
		}

		@Override
		public Class<ChangePositionJob> getComponentInterface() {
			return ChangePositionJob.class;
		}
	};
}
