/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.lifecelldetection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import org.youscope.addon.celldetection.TrackingConfiguration;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.uielements.DynamicPanel;
import org.youscope.uielements.SingleComponentDefinitionPanel;

/** Config panel to choose the (optional) online tracking algorithm. */
class TrackingAlgorithmConfigurationPanel extends DynamicPanel {
	private static final long serialVersionUID = 3441062694566624399L;

	private final JCheckBox trackField = new JCheckBox("Track cells across frames", true);
	private final SingleComponentDefinitionPanel<TrackingConfiguration> trackingConfigurationField;

	TrackingAlgorithmConfigurationPanel(CellDetectionJobConfiguration configuration, YouScopeClient client,
			final YouScopeFrame parentFrame) {
		add(trackField);
		trackingConfigurationField = new SingleComponentDefinitionPanel<TrackingConfiguration>(
				TrackingConfiguration.class, configuration.getTrackingAlgorithmConfiguration(), client, parentFrame);
		trackingConfigurationField.setLabel("Tracking algorithm:");
		trackField.setOpaque(false);
		trackField.setSelected(configuration.getTrackingAlgorithmConfiguration() != null);
		if (configuration.getTrackingAlgorithmConfiguration() == null)
			trackingConfigurationField.setVisible(false);
		addFill(trackingConfigurationField);
		addFillEmpty();
		trackField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				trackingConfigurationField.setVisible(trackField.isSelected());
				parentFrame.pack();
			}
		});
	}

	void commitChanges(CellDetectionJobConfiguration configuration) {
		if (trackField.isSelected())
			configuration.setTrackingAlgorithmConfiguration(trackingConfigurationField.getConfiguration());
		else
			configuration.setTrackingAlgorithmConfiguration(null);
	}
}
