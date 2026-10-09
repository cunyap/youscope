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
 *     Andreas P. Cuny - support for CAME
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.microplate.measurement;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

import org.youscope.addon.AddonException;
import org.youscope.addon.measurement.MeasurementAddonUIPage;
import org.youscope.addon.microplate.MicroplateResource;
import org.youscope.addon.microplate.MicroplateWellSelectionUI;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.FocusConfiguration;
import org.youscope.common.measurement.SimpleMeasurementContext;
import org.youscope.common.Well;
import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.microplate.WellActivationLayout;
import org.youscope.common.microplate.WellLayout;
import org.youscope.common.microplate.WellWithGroup;
import org.youscope.plugin.microplate.measurement.MicroplateMeasurementConfiguration;
import org.youscope.plugin.microplate.measurement.TileConfiguration;
import org.youscope.plugin.microplate.measurement.TileDefinitionUI;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DynamicPanel;

/**
 * @author Moritz Lang
 *
 */
class WellSelectionPage extends MeasurementAddonUIPage<MicroplateMeasurementConfiguration> {

	/**
	 * Serial Verision UID.
	 */
	private static final long serialVersionUID = -4078877503793485077L;

	private MicroplateLayout microplateLayout = null;
	private JLabel microplateSelectionLabel = new JLabel("Select wells to measure:");
	private MicroplateWellSelectionUI microplateSelectionUI = null;
	private Component microplateSelectionUIComponent = null;

	private JLabel tileSelectionLabel = new JLabel("Select tiles in a well to measure:");
	private TileDefinitionUI tileSelectionUI;
	private Component tileSelectionUIComponent;

	private JCheckBox tileMeasurementField = new JCheckBox("Image multiple tiles per well.");

	// CAME: opt-in to (re-)enable structurally inactive positions (pillars,
	// registration-only wells).
	private JCheckBox allowInactiveField = new JCheckBox(
			"Allow imaging of inactive / structural positions (testing & maintenance).");

	private final YouScopeClient client;
	private final YouScopeServer server;

	WellSelectionPage(YouScopeClient client, YouScopeServer server) {
		this.server = server;
		this.client = client;
	}

	/**
	 * CAME: returns the wells declared inactive by the current layout (empty if the
	 * layout does not
	 * implement {@link WellActivationLayout}).
	 */
	private Set<Well> getInactiveWells() {
		HashSet<Well> inactive = new HashSet<Well>();
		if (microplateLayout instanceof WellActivationLayout) {
			Set<Well> declared = ((WellActivationLayout) microplateLayout).getInactiveWells();
			if (declared != null)
				inactive.addAll(declared);
		}
		return inactive;
	}

	/**
	 * CAME: applies the inactive-well policy to the selection UI. When "allow
	 * inactive" is off, the
	 * inactive wells are disabled (dark-gray, unselectable); when on, they are
	 * re-enabled.
	 */
	private void applyInactivePolicy() {
		if (microplateSelectionUI == null)
			return;
		Set<Well> inactive = getInactiveWells();
		boolean allow = allowInactiveField.isSelected();
		for (Well well : inactive) {
			microplateSelectionUI.setEnabled(well, allow);
		}
	}

	@Override
	public void loadData(MicroplateMeasurementConfiguration configuration) {
		try {
			if (configuration.getMicroplateConfiguration() != null) {
				MicroplateResource microplateResource = server.getComponentProvider(null).createComponent(
						new PositionInformation(), configuration.getMicroplateConfiguration(),
						MicroplateResource.class);
				SimpleMeasurementContext measurementContext = new SimpleMeasurementContext();
				microplateResource.initialize(measurementContext);
				microplateLayout = microplateResource.getMicroplateLayout();
				microplateResource.uninitialize(measurementContext);
			} else
				microplateLayout = null;
		} catch (Exception e) {
			client.sendError("Could not get information on microscope layout.", e);
			microplateLayout = null;
		}
		microplateSelectionUI.setMicroplateLayout(microplateLayout);
		microplateSelectionUI.setSelectedWellWithGroups(configuration.getSelectedWells());

		// CAME: show the opt-in only when the layout actually declares inactive
		// positions,
		// then enforce the policy (disable inactive wells unless the user opts in).
		boolean hasInactive = !getInactiveWells().isEmpty();
		allowInactiveField.setVisible(hasInactive);
		if (!hasInactive)
			allowInactiveField.setSelected(false);
		applyInactivePolicy();

		boolean tileMeasurement = false;
		if (microplateLayout != null) {
			double width;
			double height;
			if (microplateLayout.getNumWells() >= 1) {
				WellLayout firstWell = microplateLayout.getWell(0);
				width = firstWell.getWidth();
				height = firstWell.getHeight();
			} else {
				width = 9000.;
				height = 9000.;
			}
			tileSelectionUI.setWellSize(width, height);

			TileConfiguration tileConfiguration = configuration.getTileConfiguration();
			if (tileConfiguration == null) {
				tileSelectionUI.setTileLayout(3, 3);
			} else {
				tileMeasurement = true;
				tileSelectionUI.setTileLayout(tileConfiguration.getNumTilesX(), tileConfiguration.getNumTilesY());
				tileSelectionUI.setSelectedTiles(configuration.getSelectedTiles());
			}
		}

		if (microplateLayout == null) {
			microplateSelectionLabel.setVisible(false);
			microplateSelectionUIComponent.setVisible(false);

			allowInactiveField.setVisible(false);

			tileMeasurementField.setVisible(false);

			tileSelectionUIComponent.setVisible(false);
			tileSelectionLabel.setVisible(false);
		} else {
			microplateSelectionLabel.setVisible(true);
			microplateSelectionUIComponent.setVisible(true);

			tileMeasurementField.setVisible(true);

			if (tileMeasurement) {
				tileMeasurementField.setSelected(true);
				tileSelectionUIComponent.setVisible(true);
				tileSelectionLabel.setVisible(true);
			} else {
				tileMeasurementField.setSelected(false);
				tileSelectionUIComponent.setVisible(false);
				tileSelectionLabel.setVisible(false);
			}
		}
	}

	@Override
	public boolean saveData(MicroplateMeasurementConfiguration configuration) {
		if (microplateLayout != null) {
			configuration.setSelectedWells(microplateSelectionUI.getSelectedWellWithGroups());
			if (tileMeasurementField.isSelected()) {
				TileConfiguration tileConfiguration = new TileConfiguration();
				tileConfiguration.setNumTilesX(tileSelectionUI.getNumTilesX());
				tileConfiguration.setNumTilesY(tileSelectionUI.getNumTilesY());
				configuration.setTileConfiguration(tileConfiguration);
				configuration.setSelectedTiles(tileSelectionUI.getSelectedTiles());
			} else {
				configuration.setTileConfiguration(null);
				configuration.setSelectedTiles(new HashSet<Well>(0));
			}
		} else {
			configuration.setSelectedWells(new HashSet<WellWithGroup>(0));
			configuration.setTileConfiguration(null);
			configuration.setSelectedTiles(new HashSet<Well>(0));
		}

		return true;
	}

	@Override
	public void setToDefault(MicroplateMeasurementConfiguration configuration) {
		try {
			String focusDevice = server.getMicroscope().getFocusDevice().getDeviceID();
			FocusConfiguration focusConfiguration = new FocusConfiguration();
			focusConfiguration.setFocusDevice(focusDevice);
			configuration.setFocusConfiguration(focusConfiguration);
		} catch (Exception e) {
			client.sendError("Could not pre-initialize focus device. Letting these settings empty and continuing.", e);
		}
	}

	@Override
	public String getPageName() {
		return "Measured Positions";
	}

	@Override
	protected boolean isAppear(MicroplateMeasurementConfiguration configuration) {
		return configuration.getMicroplateConfiguration() != null;
	}

	@Override
	public void createUI(final YouScopeFrame frame) {
		setLayout(new BorderLayout());

		microplateSelectionUI = new MicroplateWellSelectionUI(client, server);
		microplateSelectionUI.setWellGroups(true);
		tileSelectionUI = new TileDefinitionUI(client, server);

		DynamicPanel mainPanel = new DynamicPanel();
		mainPanel.add(microplateSelectionLabel);
		try {
			microplateSelectionUIComponent = microplateSelectionUI.toPanel(frame);

		} catch (AddonException e2) {
			client.sendError("Cannot display well selection UI.", e2);
			microplateSelectionUIComponent = new JLabel("Cannot display well selection UI.");
		}
		mainPanel.addFill(microplateSelectionUIComponent);

		// CAME: opt-in to also image inactive/structural positions.
		allowInactiveField.setVisible(false);
		allowInactiveField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				applyInactivePolicy();
			}
		});
		mainPanel.add(allowInactiveField);

		mainPanel.add(tileMeasurementField);
		mainPanel.add(tileSelectionLabel);
		try {
			tileSelectionUIComponent = tileSelectionUI.toPanel(frame);

		} catch (AddonException e2) {
			client.sendError("Cannot display tile selection UI.", e2);
			microplateSelectionUIComponent = new JLabel("Cannot display tile selection UI.");
		}
		mainPanel.add(tileSelectionUIComponent);

		tileMeasurementField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (tileMeasurementField.isSelected()) {
					tileMeasurementField.setSelected(true);
					tileSelectionUIComponent.setVisible(true);
					tileSelectionLabel.setVisible(true);
				} else {
					tileMeasurementField.setSelected(false);
					tileSelectionUIComponent.setVisible(false);
					tileSelectionLabel.setVisible(false);
				}
				fireSizeChanged();
			}
		});
		add(mainPanel, BorderLayout.CENTER);
		setBorder(new TitledBorder("Wells and Tiles"));
	}
}
