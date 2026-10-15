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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/

package org.youscope.plugin.argolight;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

import org.youscope.addon.measurement.MeasurementAddonUIPage;
import org.youscope.addon.measurement.pages.AutomaticPostProcessingPanel;
import org.youscope.clientinterfaces.StandardProperty;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.saving.SaveSettingsConfiguration;
import org.youscope.common.task.RegularPeriodConfiguration;
import org.youscope.common.task.VaryingPeriodConfiguration;
import org.youscope.plugin.microplate.measurement.MicroplateMeasurementConfiguration;
import org.youscope.uielements.PeriodField;
import org.youscope.uielements.PeriodVaryingPanel;
import org.youscope.uielements.StandardFormats;
import org.youscope.uielements.SubConfigurationPanel;

/**
 * Measurement-properties page: number of executions (default 1), how to iterate
 * through the patterns, how long to stay on each pattern, FAIR post-processing
 * (which enables OME-TIFF export after the measurement finishes) and the save
 * settings (directory / folder structure; images are written as TIFF).
 *
 * <p>
 * Trimmed version of the microplate measurement's general-settings page: the
 * "measurement finishes" runtime alternative is dropped, executions are always
 * used, and the default is a single iteration.
 * </p>
 *
 * @author Andreas P. Cuny
 */
class ArgolightGeneralSettingsPage extends MeasurementAddonUIPage<MicroplateMeasurementConfiguration> {
	private static final long serialVersionUID = 1L;

	private final YouScopeClient client;

	private final JLabel numExecutionsFieldLabel = new JLabel("Number of executions (iterations):");
	private final JFormattedTextField numExecutionsField = new JFormattedTextField(StandardFormats.getIntegerFormat());

	private final JLabel fixedPeriodLabel = new JLabel("Fixed period length:");
	private final PeriodField periodField = new PeriodField();
	private final JRadioButton periodAFAP = new JRadioButton("As fast as possible.", false);
	private final JRadioButton periodFixed = new JRadioButton("Every fixed period.", false);
	private final JRadioButton periodVarying = new JRadioButton("Varying periods.", false);
	private final PeriodVaryingPanel periodVaryingDataPanel = new PeriodVaryingPanel();

	private final JLabel fixedWellTimeLabel = new JLabel("Fixed runtime per pattern:");
	private final PeriodField wellTimeField = new PeriodField();
	private final JRadioButton wellTimeAFAP = new JRadioButton("As long as it needs.", false);
	private final JRadioButton wellTimeFixed = new JRadioButton("Exactly a given time.", false);

	private final AutomaticPostProcessingPanel postProcessorPanel = new AutomaticPostProcessingPanel();
	private SubConfigurationPanel<SaveSettingsConfiguration> saveSettingPanel = null;

	ArgolightGeneralSettingsPage(YouScopeClient client) {
		this.client = client;
	}

	@Override
	public void loadData(MicroplateMeasurementConfiguration configuration) {
		if (configuration.getPeriod() == null)
			configuration.setPeriod(new RegularPeriodConfiguration());

		if (configuration.getPeriod().getNumExecutions() >= 1)
			numExecutionsField.setValue(configuration.getPeriod().getNumExecutions());
		else
			numExecutionsField.setValue(1);

		if (configuration.getPeriod() instanceof RegularPeriodConfiguration
				&& !((RegularPeriodConfiguration) configuration.getPeriod()).isFixedTimes()) {
			periodField.setDuration(10 * 60 * 1000);
			periodAFAP.doClick();
		} else if (configuration.getPeriod() instanceof RegularPeriodConfiguration) {
			periodField.setDuration(((RegularPeriodConfiguration) configuration.getPeriod()).getPeriod());
			periodFixed.doClick();
		} else if (configuration.getPeriod() instanceof VaryingPeriodConfiguration) {
			periodVaryingDataPanel.setPeriod((VaryingPeriodConfiguration) configuration.getPeriod());
			periodField.setDuration(10 * 60 * 1000);
			periodVarying.doClick();
		}

		if (configuration.getTimePerWell() == -1) {
			wellTimeField.setDuration(5000);
			wellTimeAFAP.doClick();
		} else {
			wellTimeField.setDuration(configuration.getTimePerWell());
			wellTimeFixed.doClick();
		}

		if (configuration.getSaveSettings() == null)
			saveSettingPanel.setConfiguration(client.getPropertyProvider()
					.getProperty(StandardProperty.PROPERTY_MEASUREMENT_STANDARD_SAVE_SETTINGS_TYPE).toString());
		else
			saveSettingPanel.setConfiguration(configuration.getSaveSettings());

		postProcessorPanel.loadData(configuration);
	}

	@Override
	public boolean saveData(MicroplateMeasurementConfiguration configuration) {
		if (periodAFAP.isSelected()) {
			RegularPeriodConfiguration period = new RegularPeriodConfiguration();
			period.setFixedTimes(false);
			period.setStartTime(0);
			period.setPeriod(0);
			configuration.setPeriod(period);
		} else if (periodFixed.isSelected()) {
			RegularPeriodConfiguration period = new RegularPeriodConfiguration();
			period.setFixedTimes(true);
			period.setStartTime(0);
			period.setPeriod(periodField.getDuration());
			configuration.setPeriod(period);
		} else {
			configuration.setPeriod(periodVaryingDataPanel.getPeriod());
		}

		if (wellTimeAFAP.isSelected())
			configuration.setTimePerWell(-1);
		else
			configuration.setTimePerWell(wellTimeField.getDuration());

		// Always finish by number of executions; never by runtime.
		configuration.setMaxRuntime(-1);
		int numExecutions = ((Number) numExecutionsField.getValue()).intValue();
		if (numExecutions < 1)
			numExecutions = 1;
		configuration.getPeriod().setNumExecutions(numExecutions);

		configuration.setSaveSettings(saveSettingPanel.getConfiguration());
		postProcessorPanel.saveData(configuration);
		return true;
	}

	@Override
	public void setToDefault(MicroplateMeasurementConfiguration configuration) {
		configuration.setPeriod(new RegularPeriodConfiguration());
		configuration.getPeriod().setNumExecutions(1);
		configuration.setTimePerWell(-1);
		configuration.setMaxRuntime(-1);
	}

	@Override
	public String getPageName() {
		return "Measurement Properties";
	}

	@Override
	public void createUI(YouScopeFrame frame) {
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints newLineConstr = StandardFormats.getNewLineConstraint();
		GridBagConstraints bottomConstr = StandardFormats.getBottomContstraint();

		StandardFormats.addGridBagElement(numExecutionsFieldLabel, layout, newLineConstr, this);
		StandardFormats.addGridBagElement(numExecutionsField, layout, newLineConstr, this);

		StandardFormats.addGridBagElement(new JLabel("Iterate through all patterns:"), layout, newLineConstr, this);
		StandardFormats.addGridBagElement(periodAFAP, layout, newLineConstr, this);
		StandardFormats.addGridBagElement(periodFixed, layout, newLineConstr, this);
		StandardFormats.addGridBagElement(periodVarying, layout, newLineConstr, this);
		StandardFormats.addGridBagElement(fixedPeriodLabel, layout, newLineConstr, this);
		StandardFormats.addGridBagElement(periodField, layout, newLineConstr, this);
		StandardFormats.addGridBagElement(periodVaryingDataPanel, layout, newLineConstr, this);
		ButtonGroup periodGroup = new ButtonGroup();
		periodGroup.add(periodAFAP);
		periodGroup.add(periodFixed);
		periodGroup.add(periodVarying);
		ActionListener periodListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				periodField.setVisible(periodFixed.isSelected());
				fixedPeriodLabel.setVisible(periodFixed.isSelected());
				periodVaryingDataPanel.setVisible(periodVarying.isSelected());
				fireSizeChanged();
			}
		};
		periodAFAP.addActionListener(periodListener);
		periodFixed.addActionListener(periodListener);
		periodVarying.addActionListener(periodListener);

		StandardFormats.addGridBagElement(new JLabel("Stay on one pattern:"), layout, newLineConstr, this);
		StandardFormats.addGridBagElement(wellTimeAFAP, layout, newLineConstr, this);
		StandardFormats.addGridBagElement(wellTimeFixed, layout, newLineConstr, this);
		StandardFormats.addGridBagElement(fixedWellTimeLabel, layout, newLineConstr, this);
		StandardFormats.addGridBagElement(wellTimeField, layout, newLineConstr, this);
		ButtonGroup wellTimeGroup = new ButtonGroup();
		wellTimeGroup.add(wellTimeAFAP);
		wellTimeGroup.add(wellTimeFixed);
		ActionListener wellTimeListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				wellTimeField.setVisible(wellTimeFixed.isSelected());
				fixedWellTimeLabel.setVisible(wellTimeFixed.isSelected());
				fireSizeChanged();
			}
		};
		wellTimeAFAP.addActionListener(wellTimeListener);
		wellTimeFixed.addActionListener(wellTimeListener);

		StandardFormats.addGridBagElement(postProcessorPanel, layout, newLineConstr, this);

		saveSettingPanel = new SubConfigurationPanel<SaveSettingsConfiguration>("Save type:", null,
				SaveSettingsConfiguration.class, client, frame);
		StandardFormats.addGridBagElement(saveSettingPanel, layout, bottomConstr, this);

		setBorder(new TitledBorder("Measurement Properties"));
	}
}
