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
package org.youscope.plugin.lifecelldetection;

import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.image.ImageProducerConfiguration;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DynamicPanel;
import org.youscope.uielements.JobsDefinitionPanel;

class QuantificationPanel extends DynamicPanel {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -8640406993680299322L;
	private final JobsDefinitionPanel jobsPanel;
	public QuantificationPanel(CellDetectionJobConfiguration configuration, YouScopeClient client, YouScopeServer server, YouScopeFrame parentFrame) 
	{
		jobsPanel = new JobsDefinitionPanel(client, server, parentFrame, ImageProducerConfiguration.class);
		addFill(jobsPanel);
		jobsPanel.setJobs(configuration.getJobs());
	}
	public void commitChanges(CellDetectionJobConfiguration configuration)
	{
		configuration.setJobs(jobsPanel.getJobs());
	}
}
