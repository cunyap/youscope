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
 *     Andreas P. Cuny - update API supporting post-processors
 ******************************************************************************/
package org.youscope.plugin.usercontrolmeasurement;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.border.TitledBorder;

import org.youscope.addon.measurement.MeasurementAddonUIPage;
import org.youscope.addon.measurement.pages.AutomaticPostProcessingPanel;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.saving.SaveSettingsConfiguration;
import org.youscope.uielements.StandardFormats;
import org.youscope.uielements.SubConfigurationPanel;

class GeneralSettingsPage extends MeasurementAddonUIPage<UserControlMeasurementConfiguration> {
	/**
	 * Serial Verision UID.
	 */
	private static final long serialVersionUID = 885352612109223078L;
	private final YouScopeClient client;
	private SubConfigurationPanel<SaveSettingsConfiguration> saveSettingPanel = null;
	private final AutomaticPostProcessingPanel postProcessorPanel = new AutomaticPostProcessingPanel();

	GeneralSettingsPage(YouScopeClient client) {
		this.client = client;
	}

	@Override
	public void loadData(UserControlMeasurementConfiguration configuration) {
		saveSettingPanel.setConfiguration(configuration.getSaveSettings());
		postProcessorPanel.loadData(configuration);
	}

	@Override
	public boolean saveData(UserControlMeasurementConfiguration configuration) {

		configuration.setSaveSettings(saveSettingPanel.getConfiguration());
		postProcessorPanel.saveData(configuration);
		return true;
	}

	@Override
	public void setToDefault(UserControlMeasurementConfiguration configuration) {
		// do nothing
	}

	@Override
	public String getPageName() {
		return "Measurement Properties";
	}

	@Override
	public void createUI(YouScopeFrame frame) {
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints constr = StandardFormats.getNewLineConstraint();
		GridBagConstraints bottomConstr = StandardFormats.getBottomContstraint();

		// Automatic post-processing.
		StandardFormats.addGridBagElement(postProcessorPanel, layout, constr, this);

		// Panel to choose save settings
		saveSettingPanel = new SubConfigurationPanel<SaveSettingsConfiguration>("Save type:", null,
				SaveSettingsConfiguration.class, client, frame);
		StandardFormats.addGridBagElement(saveSettingPanel, layout, bottomConstr, this);
		setBorder(new TitledBorder("Measurement Properties"));
	}
}
