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

import org.youscope.addon.celldetection.QuantificationConfiguration;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.uielements.DynamicPanel;
import org.youscope.uielements.SingleComponentDefinitionPanel;

/**
 * Config panel to choose the (optional) quantification algorithm for the
 * cell-detection job. Mirrors {@link VisualizationAlgorithmConfigurationPanel}.
 */
class QuantificationAlgorithmConfigurationPanel extends DynamicPanel {
	private static final long serialVersionUID = 1441062694566624365L;

	private final JCheckBox quantifyField = new JCheckBox("Quantify detected cells", true);
	private final SingleComponentDefinitionPanel<QuantificationConfiguration> quantificationConfigurationField;

	QuantificationAlgorithmConfigurationPanel(CellDetectionJobConfiguration configuration, YouScopeClient client,
			final YouScopeFrame parentFrame) {
		add(quantifyField);
		quantificationConfigurationField = new SingleComponentDefinitionPanel<QuantificationConfiguration>(
				QuantificationConfiguration.class, configuration.getQuantificationAlgorithmConfiguration(), client,
				parentFrame);
		quantificationConfigurationField.setLabel("Quantification algorithm:");
		quantifyField.setOpaque(false);
		quantifyField.setSelected(configuration.getQuantificationAlgorithmConfiguration() != null);
		if (configuration.getQuantificationAlgorithmConfiguration() == null)
			quantificationConfigurationField.setVisible(false);
		addFill(quantificationConfigurationField);
		addFillEmpty();
		quantifyField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				boolean selected = quantifyField.isSelected();
				quantificationConfigurationField.setVisible(selected);
				parentFrame.pack();
			}
		});
	}

	void commitChanges(CellDetectionJobConfiguration configuration) {
		if (quantifyField.isSelected())
			configuration.setQuantificationAlgorithmConfiguration(quantificationConfigurationField.getConfiguration());
		else
			configuration.setQuantificationAlgorithmConfiguration(null);
	}
}
