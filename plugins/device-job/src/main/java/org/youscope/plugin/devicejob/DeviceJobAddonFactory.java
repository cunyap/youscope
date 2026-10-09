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
package org.youscope.plugin.devicejob;

import java.rmi.RemoteException;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.ComponentRunningException;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.job.basicjobs.DeviceSettingJob;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * @author Moritz Lang
 */
public class DeviceJobAddonFactory extends ComponentAddonFactoryAdapter 
{
	/**
	 * Constructor.
	 */
	public DeviceJobAddonFactory()
	{
		super(DeviceJobConfigurationAddon.class, CREATOR, DeviceJobConfigurationAddon.getMetadata());
	}
	
	private static final CustomAddonCreator<DeviceJobConfiguration, DeviceSettingJob> CREATOR = new CustomAddonCreator<DeviceJobConfiguration,DeviceSettingJob>()
	{
		@Override
		public DeviceSettingJob createCustom(PositionInformation positionInformation, DeviceJobConfiguration configuration,
				ConstructionContext constructionContext) throws ConfigurationException, AddonException 
		{
			DeviceSettingJob job;
			try {
				job = new DeviceJobImpl(positionInformation);
				job.setDeviceSettings(configuration.getDeviceSettings());
			}
			catch (RemoteException e1) 
			{
				throw new AddonException("Could not create device job due to remote error.", e1);
			} catch (ComponentRunningException e) {
				throw new AddonException("Could not create device job since newly created job is already running.", e);
			}
			return job;
		}

		@Override
		public Class<DeviceSettingJob> getComponentInterface() {
			return DeviceSettingJob.class;
		}
	};
}
