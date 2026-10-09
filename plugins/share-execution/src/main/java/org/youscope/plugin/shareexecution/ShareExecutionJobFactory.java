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
package org.youscope.plugin.shareexecution;

import java.rmi.RemoteException;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.ComponentCreationException;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.ComponentRunningException;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.job.Job;
import org.youscope.common.job.JobConfiguration;
import org.youscope.common.job.JobException;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * @author Moritz Lang
 */
public class ShareExecutionJobFactory extends ComponentAddonFactoryAdapter 
{
	private static final CustomAddonCreator<ShareExecutionJobConfiguration, ShareExecutionJob> CREATOR = new CustomAddonCreator<ShareExecutionJobConfiguration, ShareExecutionJob>()
	{

		@Override
		public ShareExecutionJob createCustom(PositionInformation positionInformation,
				ShareExecutionJobConfiguration configuration, ConstructionContext constructionContext)
						throws ConfigurationException, AddonException 
		{
			try
			{
				ShareExecutionJob job = new ShareExecutionJobImpl(positionInformation);
				try
				{
					job.setNumShare(configuration.getNumShare());
					job.setShareID(configuration.getShareID());
					job.setSeparateForEachWell(configuration.isSeparateForEachWell());
				}
				catch(ComponentRunningException e1)
				{
					throw new AddonException("Newly created job already running.", e1);
				}
				for(JobConfiguration childJobConfig : configuration.getJobs())
				{
					Job childJob;
					try {
						childJob = constructionContext.getComponentProvider().createJob(positionInformation, childJobConfig);
					} catch (ComponentCreationException e1) {
						throw new AddonException("Could not create child job.", e1);
					}
					try {
						job.addJob(childJob);
					} catch (JobException e) {
						throw new AddonException("Could not add child job to job.", e);
					}
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
		public Class<ShareExecutionJob> getComponentInterface() {
			return ShareExecutionJob.class;
		}
		
	};

	/**
	 * Constructor.
	 */
	public ShareExecutionJobFactory()
	{
		super(ShareExecutionJobConfigurationAddon.class, CREATOR, ShareExecutionJobConfigurationAddon.getMetadata());
	}
}
