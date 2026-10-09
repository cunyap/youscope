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
 *     Jonas Meier - initial API and implementation
 *     Andreas P. Cuny - migration to the current YouScope API
 ******************************************************************************/
package org.youscope.plugin.tiejob;

import java.rmi.RemoteException;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.ComponentCreationException;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.ComponentRunningException;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.job.basicjobs.ImagingJob;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * Factory for the modernized TIE component.
 *
 * @author Jonas Meier - original TIE plugin
 */
public class TieJobAddonFactory extends ComponentAddonFactoryAdapter
{
    private static final CustomAddonCreator<TieJobConfiguration, TieJob> CREATOR =
            new CustomAddonCreator<TieJobConfiguration, TieJob>()
    {
        @Override
        public TieJob createCustom(PositionInformation positionInformation,
                TieJobConfiguration configuration, ConstructionContext constructionContext)
                throws ConfigurationException, AddonException
        {
            try
            {
                TieJobImpl job = new TieJobImpl(positionInformation);
                job.setCameras(configuration.getCameras());
                job.setDefocusDistances(configuration.getDzNear(), configuration.getDzFar());

                ImagingJob imagingJob;
                try
                {
                    imagingJob = constructionContext.getComponentProvider().createJob(
                            positionInformation, ImagingJob.DEFAULT_TYPE_IDENTIFIER, ImagingJob.class);
                }
                catch (ComponentCreationException e)
                {
                    throw new AddonException("Could not create the ImagingJob required by TIE.", e);
                }

                imagingJob.setCameras(configuration.getCameras());
                imagingJob.setChannel(configuration.getChannelConfiguration().getChannelGroup(),
                        configuration.getChannelConfiguration().getChannel());
                imagingJob.setExposure(configuration.getExposure());
                job.addJob(imagingJob);

                if (configuration.isSaveImages())
                {
                    job.addImageListener(constructionContext.getMeasurementSaver()
                            .getSaveImageListener(configuration.getImageSaveName()));
                    job.setImageDescription(configuration.getImageSaveName() + " (" + job.getImageDescription() + ")");
                }
                return job;
            }
            catch (RemoteException e)
            {
                throw new AddonException("Could not create TIE job due to remote exception.", e);
            }
            catch (ComponentRunningException e)
            {
                throw new AddonException("Could not configure newly created TIE job because it is already running.", e);
            }
        }

        @Override
        public Class<TieJob> getComponentInterface()
        {
            return TieJob.class;
        }
    };

    public TieJobAddonFactory()
    {
        super(TieJobConfigurationAddon.class, CREATOR, TieJobConfigurationAddon.getMetadata());
    }
}
