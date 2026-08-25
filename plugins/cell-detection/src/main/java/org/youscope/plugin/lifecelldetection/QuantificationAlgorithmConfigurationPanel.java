/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
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
