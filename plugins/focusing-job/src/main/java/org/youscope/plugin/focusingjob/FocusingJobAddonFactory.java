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
package org.youscope.plugin.focusingjob;

import java.rmi.RemoteException;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.ComponentRunningException;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.job.basicjobs.FocusingJob;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * @author Moritz Lang
 */
public class FocusingJobAddonFactory extends ComponentAddonFactoryAdapter 
{
	/**
	 * Constructor.
	 */
	public FocusingJobAddonFactory()
	{
		super(FocusingJob.DEFAULT_TYPE_IDENTIFIER, FocusingJobConfiguration.class, CREATOR); 
	}
	 
	private static final CustomAddonCreator<FocusingJobConfiguration, FocusingJob> CREATOR = new CustomAddonCreator<FocusingJobConfiguration,FocusingJob>()
	{
		@Override
		public FocusingJob createCustom(PositionInformation positionInformation, FocusingJobConfiguration configuration,
				ConstructionContext constructionContext) throws ConfigurationException, AddonException 
		{
			FocusingJob job;
			try
			{
				job = new FocusingJobImpl(positionInformation);
				job.setPosition(configuration.getPosition(), configuration.isRelative());
				if(configuration.getFocusConfiguration() != null)
				{
					
					job.setFocusDevice(configuration.getFocusConfiguration().getFocusDevice());
					job.setFocusAdjustmentTime(configuration.getFocusConfiguration().getAdjustmentTime());
				}
				else
				{
					job.setFocusDevice(null);
					job.setFocusAdjustmentTime(0); 
				}
			}
			catch(ComponentRunningException e)
			{
				throw new AddonException("Could not create focusing job, since newly created job is already running.", e);
			} catch (RemoteException e1) {
				throw new AddonException("Could not create focusing job, due to remote exception.", e1);
			}
			return job;
		}

		@Override
		public Class<FocusingJob> getComponentInterface() {
			return FocusingJob.class;
		}
	};
}
