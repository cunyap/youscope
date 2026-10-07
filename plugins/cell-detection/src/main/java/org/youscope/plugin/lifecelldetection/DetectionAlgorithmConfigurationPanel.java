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

import org.youscope.addon.celldetection.CellDetectionConfiguration;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.uielements.SingleComponentDefinitionPanel;

class DetectionAlgorithmConfigurationPanel  extends SingleComponentDefinitionPanel<CellDetectionConfiguration> {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 8441062694566624365L;

	public DetectionAlgorithmConfigurationPanel(CellDetectionJobConfiguration configuration, YouScopeClient client, YouScopeFrame parentFrame) 
	{
		super(CellDetectionConfiguration.class, configuration.getDetectionAlgorithmConfiguration(), client, parentFrame);
		setLabel("Cell detection algorithm:");
	}
	public void commitChanges(CellDetectionJobConfiguration configuration)
	{
		configuration.setDetectionAlgorithmConfiguration(getConfiguration());
	}
}
