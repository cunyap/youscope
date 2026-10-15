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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentCreationException;
import org.youscope.addon.component.ComponentMetadata;
import org.youscope.addon.measurement.MeasurementAddonUIPage;
import org.youscope.addon.microplate.MicroplateResource;
import org.youscope.addon.pathoptimizer.PathOptimizerConfiguration;
import org.youscope.addon.pathoptimizer.PathOptimizerResource;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.PositionInformation;
import org.youscope.common.Well;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.configuration.FocusConfiguration;
import org.youscope.common.measurement.SimpleMeasurementContext;
import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.microplate.WellWithGroup;
import org.youscope.common.microscope.Device;
import org.youscope.plugin.microplate.measurement.MicroplateMeasurementConfiguration;
import org.youscope.plugin.microplate.measurement.PathDisplayUI;
import org.youscope.plugin.microplate.measurement.PathTable;
import org.youscope.plugin.microplate.measurement.PathTable.Column;
import org.youscope.plugin.microplate.measurement.PositionFineConfigurationAddon;
import org.youscope.plugin.microplate.measurement.XYAndFocusPosition;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DescriptionPanel;
import org.youscope.uielements.DynamicPanel;
import org.youscope.uielements.FocusField;
import org.youscope.uielements.SubConfigurationPanel;

/**
 * Position configuration page. Reuses the microplate measurement's live-view
 * fine-position editor ({@link PositionFineConfigurationAddon}) so the user can
 * drive to the central cross, let YouScope compute the other pattern positions,
 * and fine-adjust each one under the live image. Argolight has no per-well
 * tiles,
 * so the tile handling of the original page is omitted.
 *
 * @author Andreas P. Cuny
 */
class ArgolightPositionPage extends MeasurementAddonUIPage<MicroplateMeasurementConfiguration> {
	private static final long serialVersionUID = 1L;

	private final YouScopeClient client;
	private final YouScopeServer server;
	private FocusConfiguration focusConfiguration;
	private String stageDevice;
	private final HashMap<PositionInformation, XYAndFocusPosition> configuredPositions = new HashMap<>(64);
	private MicroplateLayout microplateLayout = null;
	private Set<Well> selectedWells = new HashSet<>();
	private YouScopeFrame frame;
	private MicroplateMeasurementConfiguration currentConfiguration;
	private PathOptimizerConfiguration pathOptimizerConfiguration;
	private static final String DEFAULT_PATH_OPTIMIZER = "YouScope.path.ZigZagPath";
	private SubPage lastSubPage = null;

	ArgolightPositionPage(YouScopeClient client, YouScopeServer server) {
		this.client = client;
		this.server = server;
	}

	private interface SubPage {
		void close();
	}

	/**
	 * A note (used on the Argolight position page only) explaining that the
	 * camera / channel / exposure chosen in the "Configure Positions" live view are
	 * applied to all selected well groups' imaging jobs on save.
	 */
	private static JLabel channelExposureNote() {
		JLabel note = new JLabel("<html><div style='width:380px;'>"
				+ "<b>Note:</b> the <b>camera</b>, <b>channel</b> and <b>exposure time</b> you set in the live view "
				+ "opened by <i>Configure Positions</i> are applied to the imaging jobs of all selected pattern groups "
				+ "when you save (close) that dialog. Use them to define the acquisition channel and exposure for the "
				+ "whole calibration."
				+ "</div></html>");
		note.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 2, 6, 2));
		return note;
	}

	private class NoPositionSelectedPanel extends DynamicPanel implements SubPage {
		private static final long serialVersionUID = 1L;

		NoPositionSelectedPanel() {
			DescriptionPanel descriptionPanel = new DescriptionPanel("No Patterns Selected",
					"At least one Argolight pattern has to be selected. Go to the previous pages and select at least one pattern.");
			JScrollPane scrollPane = new JScrollPane(descriptionPanel);
			scrollPane.setPreferredSize(new Dimension(400, 150));
			addFill(scrollPane);
		}

		@Override
		public void close() {
			// nothing.
		}
	}

	private class NoPositionsConfiguredPanel extends DynamicPanel implements SubPage {
		private static final long serialVersionUID = 1L;
		private final JButton newFineConfiguration = new JButton("Configure Positions");
		private final JCheckBox focusStoreField = new JCheckBox("Store focus position for every pattern.", false);
		private final JLabel focusDeviceLabel = new JLabel("Focus device:");
		private FocusField focusDeviceField;
		private JComboBox<String> stageDeviceField;

		NoPositionsConfiguredPanel() {
			DescriptionPanel descriptionPanel = new DescriptionPanel("Pattern Positions not Configured",
					"Press <i>" + newFineConfiguration.getText()
							+ "</i> to define the exact positions where images are taken. "
							+ "First, drive the stage to the center of the pattern indicated at the top-left (the central cross). "
							+ "YouScope then computes the positions of all other patterns from the Argolight layout, which you can fine-adjust under the live image.\n"
							+ "<b>Warning: Incorrectly providing the position of the first pattern can damage the microscope (e.g. stage colliding with objectives)!</b>");
			JScrollPane scrollPane = new JScrollPane(descriptionPanel);
			scrollPane.setPreferredSize(new Dimension(400, 150));
			addFill(scrollPane);
			add(channelExposureNote());

			try {
				stageDeviceField = new JComboBox<String>(getStageDevices());
			} catch (Exception e1) {
				client.sendError("Could not load stage device list. Initializing with empty list.", e1);
				stageDeviceField = new JComboBox<String>();
			}
			if (stageDeviceField.getItemCount() > 1) {
				add(new JLabel("Stage Device:"));
				add(stageDeviceField);
			}
			if (stageDevice == null) {
				try {
					stageDevice = server.getMicroscope().getStageDevice().getDeviceID();
				} catch (Exception e) {
					client.sendError("Could not determine standard stage device.", e);
				}
			}
			stageDeviceField.setSelectedItem(stageDevice);

			add(focusStoreField);
			focusDeviceField = new FocusField(focusConfiguration, client, server);
			focusStoreField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					focusDeviceField.setVisible(focusStoreField.isSelected());
					focusDeviceLabel.setVisible(focusStoreField.isSelected());
					fireSizeChanged();
				}
			});
			add(focusDeviceLabel);
			add(focusDeviceField);
			if (focusConfiguration != null)
				focusStoreField.doClick();

			add(newFineConfiguration);
			newFineConfiguration.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						runFineConfiguration(true);
					} catch (AddonException e) {
						client.sendError("Could not start pattern position configuration.", e);
					}
				}
			});
		}

		@Override
		public void close() {
			focusConfiguration = focusStoreField.isSelected() ? focusDeviceField.getFocusConfiguration() : null;
			if (stageDeviceField.getSelectedItem() != null)
				stageDevice = stageDeviceField.getSelectedItem().toString();
		}
	}

	private String[] getStageDevices() throws RemoteException {
		Device[] devices = server.getMicroscope().getStageDevices();
		String[] deviceNames = new String[devices.length];
		for (int i = 0; i < devices.length; i++)
			deviceNames[i] = devices[i].getDeviceID();
		return deviceNames;
	}

	private class PositionsConfiguredPanel extends DynamicPanel implements SubPage {
		private static final long serialVersionUID = 1L;
		private SubConfigurationPanel<PathOptimizerConfiguration> pathOptimizerPanel = null;
		private final JButton editFineConfiguration = new JButton("Edit Positions");
		private final JButton deleteFineConfiguration = new JButton("Delete Positions");
		private final JButton showOptimizedPathButton = new JButton("Visualize Path");
		private final PathTable pathTable;

		PositionsConfiguredPanel() {
			add(channelExposureNote());
			add(new JLabel("Configured Positions:"));
			ArrayList<Column> columns = new ArrayList<>();
			columns.add(Column.WELL);
			columns.add(Column.X);
			columns.add(Column.Y);
			if (focusConfiguration != null)
				columns.add(Column.FOCUS);

			pathTable = new PathTable(configuredPositions, frame, columns.toArray(new Column[columns.size()]));
			pathTable.addLayoutChangedListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					editFineConfiguration.setEnabled(false);
				}
			});
			addFill(pathTable);

			pathOptimizerPanel = new SubConfigurationPanel<PathOptimizerConfiguration>("Path through patterns:", null,
					PathOptimizerConfiguration.class, client, frame);
			pathOptimizerPanel.setConfiguration(pathOptimizerConfiguration);
			add(pathOptimizerPanel);

			showOptimizedPathButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					PathOptimizerConfiguration cfg = pathOptimizerPanel.getConfiguration();
					if (cfg == null) {
						client.sendError("No path optimizer selected.");
						return;
					}
					PathOptimizerResource optimizer;
					try {
						optimizer = server.getComponentProvider(null).createComponent(new PositionInformation(), cfg,
								PathOptimizerResource.class);
					} catch (RemoteException | ComponentCreationException | ConfigurationException e2) {
						client.sendError("Could not create path optimizer with ID " + cfg.getTypeIdentifier() + ".",
								e2);
						return;
					}
					try {
						PathDisplayUI content = new PathDisplayUI(client, server);
						YouScopeFrame childFrame = content.toFrame();
						frame.addChildFrame(childFrame);
						childFrame.setVisible(true);
						content.calculatePath(optimizer, configuredPositions);
					} catch (AddonException e1) {
						client.sendError("Could not display path.", e1);
					}
				}
			});
			deleteFineConfiguration.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (!configuredPositions.isEmpty()
							&& JOptionPane.showConfirmDialog(ArgolightPositionPage.this,
									"Really delete all configured positions?",
									"Confirm Deletion", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
						return;
					configuredPositions.clear();
					updateLayout();
				}
			});
			editFineConfiguration.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						runFineConfiguration(false);
					} catch (AddonException e) {
						client.sendError("Could not start pattern position configuration.", e);
					}
				}
			});

			editFineConfiguration.setEnabled(isCompatible());
			showOptimizedPathButton.setEnabled(pathOptimizerPanel.getConfiguration() != null);
			JPanel buttonsPanel = new JPanel(new GridLayout(1, 3));
			buttonsPanel.add(showOptimizedPathButton);
			buttonsPanel.add(deleteFineConfiguration);
			buttonsPanel.add(editFineConfiguration);
			add(buttonsPanel);
		}

		@Override
		public void close() {
			pathOptimizerConfiguration = pathOptimizerPanel.getConfiguration();
		}
	}

	@Override
	public void loadData(MicroplateMeasurementConfiguration configuration) {
		currentConfiguration = configuration;
		pathOptimizerConfiguration = configuration.getPathOptimizerConfiguration();
		focusConfiguration = configuration.getFocusConfiguration();
		stageDevice = configuration.getStageDevice();
		configuredPositions.clear();
		configuredPositions.putAll(configuration.getPositions());

		selectedWells = new HashSet<Well>();
		for (WellWithGroup well : configuration.getSelectedWells())
			selectedWells.add(well.getWell());

		try {
			if (configuration.getMicroplateConfiguration() != null) {
				MicroplateResource microplateResource = server.getComponentProvider(null)
						.createComponent(new PositionInformation(), configuration.getMicroplateConfiguration(),
								MicroplateResource.class);
				SimpleMeasurementContext measurementContext = new SimpleMeasurementContext();
				microplateResource.initialize(measurementContext);
				microplateLayout = microplateResource.getMicroplateLayout();
				microplateResource.uninitialize(measurementContext);
			} else
				microplateLayout = null;
		} catch (Exception e) {
			client.sendError("Could not get information on Argolight layout.", e);
			microplateLayout = null;
		}
		updateLayout();
	}

	@Override
	public boolean saveData(MicroplateMeasurementConfiguration configuration) {
		if (lastSubPage != null)
			lastSubPage.close();
		configuration.setFocusConfiguration(focusConfiguration);
		configuration.setStageDevice(stageDevice);
		configuration.setPathOptimizerConfiguration(pathOptimizerConfiguration);
		configuration.setPositions(configuredPositions);
		return true;
	}

	@Override
	public void setToDefault(MicroplateMeasurementConfiguration configuration) {
		try {
			ComponentMetadata<? extends PathOptimizerConfiguration> metadata = client.getAddonProvider()
					.getComponentMetadata(DEFAULT_PATH_OPTIMIZER, PathOptimizerConfiguration.class);
			configuration.setPathOptimizerConfiguration(metadata.getConfigurationClass().newInstance());
		} catch (@SuppressWarnings("unused") Exception e) {
			// leave default unset.
		}
	}

	private void updateLayout() {
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				removeAll();
				setLayout(new BorderLayout());
				if (lastSubPage != null)
					lastSubPage.close();
				if (configuredPositions.isEmpty()) {
					if (microplateLayout != null && selectedWells.isEmpty()) {
						NoPositionSelectedPanel panel = new NoPositionSelectedPanel();
						add(panel, BorderLayout.CENTER);
						lastSubPage = panel;
					} else {
						NoPositionsConfiguredPanel panel = new NoPositionsConfiguredPanel();
						add(panel, BorderLayout.CENTER);
						lastSubPage = panel;
					}
				} else {
					PositionsConfiguredPanel panel = new PositionsConfiguredPanel();
					add(panel, BorderLayout.CENTER);
					lastSubPage = panel;
				}
				revalidate();
				if (frame != null)
					frame.pack();
			}
		};
		if (SwingUtilities.isEventDispatchThread())
			runner.run();
		else
			SwingUtilities.invokeLater(runner);
	}

	@Override
	public String getPageName() {
		return "Pattern Positions";
	}

	@Override
	public void createUI(final YouScopeFrame frame) {
		setLayout(new BorderLayout());
		this.frame = frame;
		setBorder(new TitledBorder("Position Configuration"));
	}

	private void runFineConfiguration(boolean forceNew) throws AddonException {
		String focusDevice = focusConfiguration == null ? null : focusConfiguration.getFocusDevice();
		final PositionFineConfigurationAddon positionFineConfiguration = new PositionFineConfigurationAddon(client,
				server);
		positionFineConfiguration.setStageDevice(stageDevice);
		positionFineConfiguration.setFocusDevice(focusDevice);
		if (microplateLayout != null)
			positionFineConfiguration.setSelectedWells(microplateLayout, selectedWells);
		if (!forceNew)
			positionFineConfiguration.setPositions(configuredPositions);
		positionFineConfiguration.addSaveListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Propagate the live-view camera/channel/exposure to all selected
				// well groups' imaging jobs (and remember them for later pre-fill).
				if (currentConfiguration instanceof ArgolightCalibrationConfiguration) {
					PositionFineConfigurationAddon.LiveChannelSettings live = positionFineConfiguration
							.getLiveChannelSettings();
					if (live != null && (live.hasChannel() || live.getExposureMs() > 0)) {
						((ArgolightCalibrationConfiguration) currentConfiguration).setAcquisitionChannelExposure(
								live.getChannelGroup(), live.getChannel(), live.getCamera(), live.getExposureMs());
					}
				}

				Map<PositionInformation, XYAndFocusPosition> newPositions = positionFineConfiguration
						.getConfiguredPositions();
				if (newPositions == null || newPositions.isEmpty())
					return;
				configuredPositions.clear();
				configuredPositions.putAll(newPositions);
				updateLayout();
			}
		});
		YouScopeFrame fineConfigFrame = positionFineConfiguration.toFrame();
		fineConfigFrame.setVisible(true);
	}

	private boolean isCompatible() {
		if (microplateLayout == null)
			return false;
		for (Map.Entry<PositionInformation, XYAndFocusPosition> entry : configuredPositions.entrySet()) {
			PositionInformation posInfo = entry.getKey();
			double focus = entry.getValue().getFocus();
			if (posInfo.getWell() == null)
				return false;
			if (!selectedWells.contains(posInfo.getWell()))
				return false;
			if (posInfo.getNumPositions() != 0)
				return false;
			if ((java.lang.Double.isNaN(focus) && focusConfiguration != null)
					|| (!java.lang.Double.isNaN(focus) && focusConfiguration == null))
				return false;
		}
		for (Well well : selectedWells) {
			if (!configuredPositions.containsKey(new PositionInformation(well)))
				return false;
		}
		return true;
	}
}
