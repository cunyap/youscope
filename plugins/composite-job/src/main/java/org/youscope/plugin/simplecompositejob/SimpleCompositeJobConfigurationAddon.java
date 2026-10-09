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
package org.youscope.plugin.simplecompositejob;

import java.awt.Component;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonUIAdapter;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.job.basicjobs.SimpleCompositeJob;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.JobsDefinitionPanel;

/**
 * @author Moritz Lang
 */
class SimpleCompositeJobConfigurationAddon extends ComponentAddonUIAdapter<SimpleCompositeJobConfiguration>
{
	private JobsDefinitionPanel jobPanel = null;
	
    SimpleCompositeJobConfigurationAddon(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(),  client, server);
	}
    
    static ComponentMetadataAdapter<SimpleCompositeJobConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<SimpleCompositeJobConfiguration>(SimpleCompositeJob.DEFAULT_TYPE_IDENTIFIER, 
				SimpleCompositeJobConfiguration.class, 
				SimpleCompositeJob.class, 
				"Job Container", 
				new String[]{"Containers"},
				"A job containing other jobs for grouping. The child-jobs are executed in the given order whenever the composite job is executed.",
				"icons/box.png");
	}
    
    @Override
	protected Component createUI(SimpleCompositeJobConfiguration configuration) throws AddonException 
    {
		setTitle("Job Container");
		setResizable(true);
		setMaximizable(false);
        
		jobPanel = new JobsDefinitionPanel(getClient(), getServer(), getContainingFrame());
        jobPanel.setJobs(configuration.getJobs());

        return jobPanel;
    }

	@Override
	protected void commitChanges(SimpleCompositeJobConfiguration configuration)
	{
		configuration.setJobs(jobPanel.getJobs());
	}

	@Override
	protected void initializeDefaultConfiguration(SimpleCompositeJobConfiguration configuration) throws AddonException {
		// do nothing.
	}
}
