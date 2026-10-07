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
package org.youscope.plugin.composedimaging;

import java.awt.Dimension;
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
public class ComposedImagingJobAddonFactory extends ComponentAddonFactoryAdapter 
{
	/**
	 * Constructor.
	 */
	public ComposedImagingJobAddonFactory()
	{
		super(ComposedImagingJobConfigurationAddon.class, CREATOR, ComposedImagingJobConfigurationAddon.getMetadata());
	}
	
	private static final CustomAddonCreator<ComposedImagingJobConfiguration, ComposedImagingJob> CREATOR = new CustomAddonCreator<ComposedImagingJobConfiguration,ComposedImagingJob>()
	{
		@Override
		public ComposedImagingJob createCustom(PositionInformation positionInformation, ComposedImagingJobConfiguration configuration,
				ConstructionContext constructionContext) throws ConfigurationException, AddonException 
		{
			ComposedImagingJobImpl job;
			try
			{
				job = new ComposedImagingJobImpl(positionInformation);
				double dx = configuration.getPixelSize() * configuration.getNumPixels().width * (1 - configuration.getOverlap());
				double dy = configuration.getPixelSize() * configuration.getNumPixels().height * (1 - configuration.getOverlap());
				job.setChannel(configuration.getChannelGroup(), configuration.getChannel());
				job.setExposure(configuration.getExposure());
				job.setDeltaX(dx);
				job.setDeltaY(dy);
				job.setSubImageNumber(new Dimension(configuration.getNx(), configuration.getNy()));
					
				if(configuration.isSaveImages())
					job.addImageListener(constructionContext.getMeasurementSaver().getSaveImageListener(configuration.getImageSaveName()));
				
				
			}
			catch(ComponentRunningException e)
			{
				throw new AddonException("Could not create job, since newly created job is already running.", e);
			}
			catch(RemoteException e)
			{
				throw new AddonException("Could not create job due to remote error.", e);
			}
			return job;
		}

		@Override
		public Class<ComposedImagingJob> getComponentInterface() {
			return ComposedImagingJob.class;
		}
	};
}
