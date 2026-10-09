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
package org.youscope.plugin.lifecelldetection;

import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.image.ImageProducerConfiguration;
import org.youscope.common.job.JobConfiguration;
import org.youscope.uielements.SingleComponentDefinitionPanel;

class InputImageConfigurationPanel extends SingleComponentDefinitionPanel<JobConfiguration>
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -367954181545593023L;

	public InputImageConfigurationPanel(CellDetectionJobConfiguration configuration, YouScopeClient client, YouScopeFrame parentFrame) 
	{
		super(JobConfiguration.class, configuration.getDetectionJob(), client, parentFrame, ImageProducerConfiguration.class);
		setLabel("Select detection image job:");
	}
	public void commitChanges(CellDetectionJobConfiguration configuration)
	{
		configuration.setDetectionJob(getConfiguration());
	}
}
