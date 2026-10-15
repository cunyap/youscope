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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.youscope.addon.measurement.MeasurementAddonUIPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.Well;
import org.youscope.common.configuration.FocusConfiguration;
import org.youscope.common.microplate.WellWithGroup;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * Pattern-selection page. Shows the Argolight pattern map (icons and tooltips);
 * clicking a pattern turns it green. A combo box lets the user pick which plate
 * definition (bundled default, or a user XML) to use. Each selected pattern
 * carries the well group defined for it, so the imaging-protocol page can show
 * one tab per pattern type.
 *
 * @author Andreas P. Cuny
 */
class ArgolightPatternSelectionPage extends MeasurementAddonUIPage<ArgolightCalibrationConfiguration> {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final YouScopeClient client;
	private final YouScopeServer server;

	private JComboBox<String> definitionChooser;
	private ArgolightPatternSelectionUI selectionUI;
	private final JLabel countLabel = new JLabel(" ");

	ArgolightPatternSelectionPage(YouScopeClient client, YouScopeServer server) {
		this.client = client;
		this.server = server;
	}

	@Override
	public void loadData(ArgolightCalibrationConfiguration configuration) {
		ArgolightMicroplateConfiguration microplate = configuration.getOrCreateArgolightMicroplate();
		String definitionName = microplate.getDefinitionName();

		definitionChooser.setSelectedItem(definitionName);
		if (definitionChooser.getSelectedItem() == null && definitionChooser.getItemCount() > 0)
			definitionChooser.setSelectedIndex(0);

		loadDefinitionIntoUI((String) definitionChooser.getSelectedItem());

		Set<WellWithGroup> selected = configuration.getSelectedWells();
		if (selected != null && !selected.isEmpty())
			selectionUI.setSelectedWellWithGroups(selected);
		updateCount();
	}

	@Override
	public boolean saveData(ArgolightCalibrationConfiguration configuration) {
		String definitionName = (String) definitionChooser.getSelectedItem();
		ArgolightMicroplateConfiguration microplate = configuration.getOrCreateArgolightMicroplate();
		microplate.setDefinitionName(definitionName);
		configuration.setMicroplateConfiguration(microplate);

		configuration.setSelectedWells(selectionUI.getSelectedWellWithGroups());
		return true;
	}

	@Override
	public void setToDefault(ArgolightCalibrationConfiguration configuration) {
		configuration.getOrCreateArgolightMicroplate();
		// Start with nothing selected; the user picks the patterns to image.
		configuration.setSelectedWells(new HashSet<WellWithGroup>());
		// Provide a sensible default focus device.
		try {
			String focusDevice = server.getMicroscope().getFocusDevice().getDeviceID();
			FocusConfiguration focusConfiguration = new FocusConfiguration();
			focusConfiguration.setFocusDevice(focusDevice);
			configuration.setFocusConfiguration(focusConfiguration);
		} catch (Exception e) {
			// ignore: focus device stays unset.
		}
	}

	@Override
	public String getPageName() {
		return "Argolight Patterns";
	}

	@Override
	public void createUI(YouScopeFrame frame) {
		setLayout(new BorderLayout());

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(new JLabel("Plate definition:"));
		definitionChooser = new JComboBox<>(ArgolightPlateManager.getAvailableDefinitionNames());
		definitionChooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadDefinitionIntoUI((String) definitionChooser.getSelectedItem());
				updateCount();
			}
		});
		topPanel.add(definitionChooser);

		JButton selectAll = new JButton("Select all");
		selectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionUI.selectAll();
			}
		});
		JButton selectNone = new JButton("Clear");
		selectNone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectionUI.selectNone();
			}
		});
		topPanel.add(selectAll);
		topPanel.add(selectNone);

		ArgolightPlateDefinition initial;
		try {
			initial = ArgolightPlateManager.getDefinition((String) definitionChooser.getSelectedItem());
		} catch (ArgolightException e) {
			initial = new ArgolightPlateDefinition();
		}
		selectionUI = new ArgolightPatternSelectionUI(initial);
		selectionUI.addSelectionChangedListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateCount();
			}
		});

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(
				new JLabel(
						"Click a pattern to (de)select it. Selected patterns are green; 'z' marks z-stack patterns."),
				BorderLayout.NORTH);
		bottom.add(countLabel, BorderLayout.SOUTH);

		add(topPanel, BorderLayout.NORTH);
		add(new JScrollPane(selectionUI), BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		setBorder(new TitledBorder("Argolight Patterns"));
	}

	private void loadDefinitionIntoUI(String definitionName) {
		if (selectionUI == null || definitionName == null)
			return;
		try {
			ArgolightPlateDefinition definition = ArgolightPlateManager.getDefinition(definitionName);
			selectionUI.setDefinition(definition);
		} catch (ArgolightException e) {
			// keep previous definition.
		}
	}

	private void updateCount() {
		if (selectionUI == null)
			return;
		Set<Well> selected = selectionUI.getSelectedWells();
		int total = selectionUI.getDefinition().getNumPatterns();
		countLabel.setText(selected.size() + " of " + total + " patterns selected.");
	}
}
