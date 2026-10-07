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
 *     Andreas P. Cuny - add sync between selected and configured positions
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.microplate.measurement;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

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
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.configuration.FocusConfiguration;
import org.youscope.common.measurement.SimpleMeasurementContext;
import org.youscope.common.Well;
import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.microplate.WellLayout;
import org.youscope.common.microplate.WellWithGroup;
import org.youscope.common.microscope.Device;
import org.youscope.plugin.microplate.measurement.MicroplateMeasurementConfiguration;
import org.youscope.plugin.microplate.measurement.PathDisplayUI;
import org.youscope.plugin.microplate.measurement.PathTable;
import org.youscope.plugin.microplate.measurement.PositionFineConfigurationAddon;
import org.youscope.plugin.microplate.measurement.TileConfiguration;
import org.youscope.plugin.microplate.measurement.XYAndFocusPosition;
import org.youscope.plugin.microplate.measurement.PathTable.Column;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DescriptionPanel;
import org.youscope.uielements.DynamicPanel;
import org.youscope.uielements.FocusField;
import org.youscope.uielements.SubConfigurationPanel;

/**
 * @author Moritz Lang
 *
 */
class PathPage extends MeasurementAddonUIPage<MicroplateMeasurementConfiguration> {

	/**
	 * Serial Verision UID.
	 */
	private static final long serialVersionUID = -771185914335388563L;

	private final YouScopeClient client;
	private final YouScopeServer server;
	private FocusConfiguration focusConfiguration;
	private String stageDevice;
	private final HashMap<PositionInformation, XYAndFocusPosition> configuredPositions = new HashMap<>(200);
	private MicroplateLayout microplateLayout = null;
	private TileConfiguration tileConfiguration = null;
	/**
	 * Authoritative selected-wells set, keeping the well group of each well. Kept
	 * consistent with
	 * {@link #configuredPositions} at all times (see
	 * {@link #reconcileSelectionWithPositions()}).
	 */
	private Set<WellWithGroup> selectedWells = new LinkedHashSet<>();
	private Set<Well> selectedTiles;
	private YouScopeFrame frame;
	private PathOptimizerConfiguration pathOptimizerConfiguration;
	private final static String DEFAULT_PATH_OPTIMIZER = "YouScope.path.ZigZagPath";
	private SubPage lastSubPage = null;

	PathPage(YouScopeClient client, YouScopeServer server) {
		this.client = client;
		this.server = server;
	}

	private interface SubPage {
		void close();
	}

	/**
	 * Returns the wells (without their group) of the current selection.
	 * 
	 * @return set of selected wells.
	 */
	private Set<Well> selectedWellSet() {
		Set<Well> wells = new HashSet<>(selectedWells.size());
		for (WellWithGroup wellWithGroup : selectedWells)
			wells.add(wellWithGroup.getWell());
		return wells;
	}

	/**
	 * Returns the groups a manually added well may be assigned to: every group
	 * currently used in the selection,
	 * plus {@link WellWithGroup.WellGroup#GROUP0}. Ordered by group id. Restricting
	 * the choice to groups that
	 * already exist in the experiment keeps the selection consistent with the
	 * per-group imaging protocols.
	 * 
	 * @return array of available well groups.
	 */
	private WellWithGroup.WellGroup[] availableGroups() {
		// All well groups are valid choices when manually adding a position.
		// Do not restrict this list to groups that happened to be selected initially.
		TreeSet<WellWithGroup.WellGroup> groups = new TreeSet<>();
		for (WellWithGroup.WellGroup group : WellWithGroup.WellGroup.values())
			groups.add(group);
		return groups.toArray(new WellWithGroup.WellGroup[groups.size()]);
	}

	/**
	 * Reconciles the selected-wells set with the configured positions so that both
	 * describe exactly the same set
	 * of wells. This is the single place that guarantees selection/position
	 * consistency:
	 * <ul>
	 * <li>Wells that are configured but not selected are added (keeping an already
	 * known group, else GROUP0).</li>
	 * <li>Wells that are selected but have no configured position are dropped
	 * (their group is preserved only if a
	 * position exists). This is the repair for legacy configurations: because a
	 * measurement is driven by the
	 * configured positions, an orphaned selection would silently never be imaged,
	 * so the configured positions are
	 * the source of truth.</li>
	 * </ul>
	 * Has no effect in custom-position mode (no microplate layout), where positions
	 * are not bound to wells.
	 * 
	 * @param warnOnDrop if true, the user is informed when orphaned wells are
	 *                   dropped.
	 * @return true if the selection was changed.
	 */
	/**
	 * Reconciles the selected-wells set with configured positions without
	 * discarding
	 * wells selected by the user. Configured positions are always considered part
	 * of the selection, while a selected well is allowed to have no configured
	 * position yet so it can be configured by the fine-position wizard.
	 *
	 * <p>
	 * This is important when the user goes back to the well-selection page and
	 * adds wells after positions have already been configured: those newly selected
	 * wells must remain selected so that "Edit Positions" can be entered and the
	 * missing positions can be created.
	 * </p>
	 *
	 * @return true if the selection was changed.
	 */
	private boolean reconcileSelectionWithPositions() {
		if (microplateLayout == null) {
			boolean changed = !selectedWells.isEmpty();
			selectedWells.clear();
			return changed;
		}

		HashMap<Well, WellWithGroup.WellGroup> groupOfWell = new HashMap<>();
		for (WellWithGroup wellWithGroup : selectedWells)
			groupOfWell.put(wellWithGroup.getWell(), wellWithGroup.getGroup());

		boolean changed = false;
		// Every configured well must remain selected, but selected wells without a
		// position are intentionally preserved as pending configuration.
		for (PositionInformation posInfo : configuredPositions.keySet()) {
			Well well = posInfo.getWell();
			if (well == null || groupOfWell.containsKey(well))
				continue;
			WellWithGroup.WellGroup group = WellWithGroup.WellGroup.GROUP0;
			selectedWells.add(new WellWithGroup(well, group));
			groupOfWell.put(well, group);
			changed = true;
		}
		return changed;
	}

	private class NoPositionSelectedPanel extends DynamicPanel implements SubPage {
		/**
		 * Serial Versio UID.
		 */
		private static final long serialVersionUID = -1528365488715327529L;

		NoPositionSelectedPanel() {
			DescriptionPanel descriptionPanel = new DescriptionPanel("No Wells/Tiles Selected",
					"At least one well/tile has to be selected. If the option to image multiple tiles per well was selected, additionally at least one tile has to be selected.\n"
							+ "Go to the previous page and select at least one well/tile.");
			JScrollPane scrollPane = new JScrollPane(descriptionPanel);
			scrollPane.setPreferredSize(new Dimension(400, 150));
			addFill(scrollPane);
		}

		@Override
		public void close() {
			// do nothing.
		}

	}

	private class NoPositionsConfiguredPanel extends DynamicPanel implements SubPage {
		/**
		 * Serial Version UID.
		 */
		private static final long serialVersionUID = -5287492043333512642L;
		private final JButton newFineConfiguration = new JButton("Configure Positions");
		private JCheckBox focusStoreField = new JCheckBox("Store focus position for every well/tile.", false);

		private final JLabel focusDeviceLabel = new JLabel("Focus device:");
		private FocusField focusDeviceField;
		private JComboBox<String> stageDeviceField;

		NoPositionsConfiguredPanel() {
			DescriptionPanel descriptionPanel = new DescriptionPanel("Microplate Positions not Configured",
					"Press <i>" + newFineConfiguration.getText()
							+ "</i> to define the exact positions where images are taken. "
							+ "In the first mandatory step, the stage has to be manually moved to the center of the first well/tile indicated at the top-left. Based on this information, YouScope automatically calculates the positions of all other wells/tiles."
							+ "In the second optional step, the positions of all other wells/tiles can be adjusted. Alternatively, press <i>Save Positions</i> to accept the automatically calculated positions.\n"
							+ "<b>Warning: Incorrectly providing the position of the first well/tile can damage the microscope (e.g. stage colliding with objectives)!</b>");
			JScrollPane scrollPane = new JScrollPane(descriptionPanel);
			scrollPane.setPreferredSize(new Dimension(400, 150));
			addFill(scrollPane);

			try {
				stageDeviceField = new JComboBox<String>(getStageDevices());
			} catch (Exception e1) {
				client.sendError("Could not load stage device list. Initializing with empty list", e1);
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
					if (!focusStoreField.isSelected()) {
						focusDeviceField.setVisible(false);
						focusDeviceLabel.setVisible(false);
					} else {
						focusDeviceField.setVisible(true);
						focusDeviceLabel.setVisible(true);
					}
					fireSizeChanged();
				}
			});
			add(focusDeviceLabel);
			add(focusDeviceField);
			if (focusConfiguration != null) {
				focusStoreField.doClick();
			}

			add(newFineConfiguration);
			newFineConfiguration.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						runFineConfiguration(true);
					} catch (AddonException e) {
						client.sendError("Could not start microplate well/tile position configuration.", e);
					}
				}
			});
		}

		@Override
		public void close() {
			if (!focusStoreField.isSelected()) {
				focusConfiguration = null;
			} else {
				focusConfiguration = focusDeviceField.getFocusConfiguration();
			}
			stageDevice = stageDeviceField.getSelectedItem().toString();
		}
	}

	private String[] getStageDevices() throws RemoteException {
		Device[] devices = server.getMicroscope().getStageDevices();
		String[] deviceNames = new String[devices.length];
		for (int i = 0; i < devices.length; i++) {
			deviceNames[i] = devices[i].getDeviceID();
		}

		return deviceNames;
	}

	private class PositionsConfiguredPanel extends DynamicPanel implements SubPage {
		/**
		 * Serial Version UID.
		 */
		private static final long serialVersionUID = 6258765419338706296L;
		private SubConfigurationPanel<PathOptimizerConfiguration> pathOptimizerPanel = null;
		private final JButton editFineConfiguration = new JButton("Edit Positions");
		private final JButton deleteFineConfiguration = new JButton("Delete Positions");
		private final JButton showOptimizedPathButton = new JButton("Visualize Path");
		private final PathTable pathTable;

		PositionsConfiguredPanel() {
			add(new JLabel("Configured Positions:"));
			ArrayList<PathTable.Column> columns = new ArrayList<>();
			if (microplateLayout != null) {
				columns.add(Column.WELL);
				columns.add(Column.GROUP);
			} else
				columns.add(Column.MAIN_POSITION);
			if (tileConfiguration != null)
				columns.add(Column.TILE);
			columns.add(Column.X);
			columns.add(Column.Y);
			if (focusConfiguration != null)
				columns.add(Column.FOCUS);

			pathTable = new PathTable(configuredPositions, microplateLayout, tileConfiguration, selectedWells,
					availableGroups(), frame, columns.toArray(new Column[columns.size()]));
			pathTable.addLayoutChangedListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// A manual add/delete happened. Keep selection consistent with the positions,
					// then decide
					// whether the fine-configuration wizard can still be re-entered. Previously the
					// edit button was
					// simply disabled here, which permanently blocked re-editing and left the
					// selection out of sync.
					reconcileSelectionWithPositions();
					// Keep Edit Positions enabled when the user added wells after the
					// initial configuration. The fine-configuration editor must be
					// re-enterable so those wells can be positioned and focused.
					editFineConfiguration.setEnabled(isCompatible());
				}
			});
			addFill(pathTable);

			pathOptimizerPanel = new SubConfigurationPanel<PathOptimizerConfiguration>("Path through microplate:", null,
					PathOptimizerConfiguration.class, client, frame);
			pathOptimizerPanel.setConfiguration(pathOptimizerConfiguration);
			add(pathOptimizerPanel);

			showOptimizedPathButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					PathOptimizerConfiguration pathOptimizerConfiguration = pathOptimizerPanel.getConfiguration();
					if (pathOptimizerConfiguration == null) {
						client.sendError("No path optimizer selected.");
						return;
					}
					PathOptimizerResource optimizer;
					try {
						optimizer = server.getComponentProvider(null).createComponent(new PositionInformation(),
								pathOptimizerConfiguration, PathOptimizerResource.class);
					} catch (RemoteException | ComponentCreationException | ConfigurationException e2) {
						client.sendError("Could not create path optimizer with ID "
								+ pathOptimizerConfiguration.getTypeIdentifier() + ".", e2);
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
					if (!configuredPositions.isEmpty()) {
						if (JOptionPane.showConfirmDialog(PathPage.this, "Really delete all configured positions?",
								"Confirm Deleteion", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
							return;
					}
					configuredPositions.clear();
					// Deleting all positions removes all wells; keep the selection consistent.
					reconcileSelectionWithPositions();
					updateLayout();
				}
			});
			editFineConfiguration.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						runFineConfiguration(false);
					} catch (AddonException e) {
						client.sendError("Could not start microplate well/tile position configuration.", e);
					}
				}
			});

			editFineConfiguration.setEnabled(isCompatible());
			editFineConfiguration.setText(hasUnconfiguredSelectedWells() ? "Configure Positions" : "Edit Positions");
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
		pathOptimizerConfiguration = configuration.getPathOptimizerConfiguration();
		focusConfiguration = configuration.getFocusConfiguration();
		stageDevice = configuration.getStageDevice();
		configuredPositions.clear();
		configuredPositions.putAll(configuration.getPositions());

		// Keep the full well+group tuples (previously the group was discarded here,
		// which made a two-way sync
		// between the selection and the configured positions impossible).
		selectedWells = new LinkedHashSet<>(configuration.getSelectedWells());
		selectedTiles = configuration.getSelectedTiles();
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
		tileConfiguration = configuration.getTileConfiguration();

		// Keep configured wells selected, while preserving newly selected wells that
		// still need positions.
		reconcileSelectionWithPositions();

		updateLayout();
	}

	@Override
	public boolean saveData(MicroplateMeasurementConfiguration configuration) {
		if (lastSubPage != null)
			lastSubPage.close();

		// Make sure selection and positions are consistent before persisting them.
		reconcileSelectionWithPositions();

		configuration.setFocusConfiguration(focusConfiguration);
		configuration.setStageDevice(stageDevice);
		configuration.setPathOptimizerConfiguration(pathOptimizerConfiguration);
		configuration.setPositions(configuredPositions);
		// Persist the (reconciled) selection so it never diverges from the configured
		// positions again.
		if (microplateLayout != null)
			configuration.setSelectedWells(selectedWells);
		return true;
	}

	@Override
	public void setToDefault(MicroplateMeasurementConfiguration configuration) {
		try {
			ComponentMetadata<? extends PathOptimizerConfiguration> metadata = client.getAddonProvider()
					.getComponentMetadata(DEFAULT_PATH_OPTIMIZER, PathOptimizerConfiguration.class);
			PathOptimizerConfiguration pathConfiguration = metadata.getConfigurationClass().newInstance();
			configuration.setPathOptimizerConfiguration(pathConfiguration);

		} catch (@SuppressWarnings("unused") Exception e) {
			// do nothing. Default only not set.
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
					if ((microplateLayout != null && selectedWells.isEmpty())
							|| (tileConfiguration != null && selectedTiles.isEmpty())) {
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
		return "Microplate Type";
	}

	@Override
	public void createUI(final YouScopeFrame frame) {
		setLayout(new BorderLayout());
		this.frame = frame;

		setBorder(new TitledBorder("Position Configuration"));
	}

	/**
	 * Builds the position map supplied to the fine-position editor. Existing
	 * positions are preserved. Newly selected wells/tiles receive calculated
	 * initial coordinates so the editor never receives a null stage position.
	 */
	private HashMap<PositionInformation, XYAndFocusPosition> getPositionsForFineConfiguration() {
		HashMap<PositionInformation, XYAndFocusPosition> positions = new HashMap<>(configuredPositions);
		if (microplateLayout == null)
			return positions;

		double[] zero = referenceZeroForFineConfiguration();

		for (WellWithGroup wellWithGroup : selectedWells) {
			Well well = wellWithGroup.getWell();
			if (microplateLayout.getWell(well) == null)
				continue;

			if (tileConfiguration == null) {
				PositionInformation posInfo = new PositionInformation(well);
				if (!positions.containsKey(posInfo)) {
					double[] xy = computeXYForFineConfiguration(well, null, null, zero);
					if (xy != null)
						positions.put(posInfo, new XYAndFocusPosition(xy[0], xy[1], Double.NaN));
				}
			} else {
				for (Well tile : selectedTiles) {
					PositionInformation wellPos = new PositionInformation(well);
					PositionInformation tilePos = new PositionInformation(wellPos,
							PositionInformation.POSITION_TYPE_YTILE, tile.getWellY());
					tilePos = new PositionInformation(tilePos,
							PositionInformation.POSITION_TYPE_XTILE, tile.getWellX());

					if (!positions.containsKey(tilePos)) {
						double[] xy = computeXYForFineConfiguration(well, tile.getWellY(), tile.getWellX(), zero);
						if (xy != null)
							positions.put(tilePos, new XYAndFocusPosition(xy[0], xy[1], Double.NaN));
					}
				}
			}
		}
		return positions;
	}

	private double[] referenceZeroForFineConfiguration() {
		if (microplateLayout == null)
			return new double[] { 0, 0 };

		for (Entry<PositionInformation, XYAndFocusPosition> entry : configuredPositions.entrySet()) {
			PositionInformation posInfo = entry.getKey();
			Well well = posInfo.getWell();
			if (well == null)
				continue;

			WellLayout wellLayout = microplateLayout.getWell(well);
			if (wellLayout == null || entry.getValue() == null)
				continue;

			XYAndFocusPosition pos = entry.getValue();
			double zeroX;
			double zeroY;
			if (posInfo.getNumPositions() == 2 && tileConfiguration != null) {
				zeroX = pos.getX() - wellLayout.getX()
						- (posInfo.getPosition(1) + 0.5) * wellLayout.getWidth() / tileConfiguration.getNumTilesX();
				zeroY = pos.getY() - wellLayout.getY()
						- (posInfo.getPosition(0) + 0.5) * wellLayout.getHeight() / tileConfiguration.getNumTilesY();
			} else {
				zeroX = pos.getX() - wellLayout.getX() - wellLayout.getWidth() / 2;
				zeroY = pos.getY() - wellLayout.getY() - wellLayout.getHeight() / 2;
			}
			return new double[] { zeroX, zeroY };
		}
		return new double[] { 0, 0 };
	}

	private double[] computeXYForFineConfiguration(Well well, Integer tileY, Integer tileX, double[] zero) {
		WellLayout wellLayout = microplateLayout == null ? null : microplateLayout.getWell(well);
		if (wellLayout == null)
			return null;

		double x;
		double y;
		if (tileY != null && tileX != null && tileConfiguration != null) {
			x = zero[0] + wellLayout.getX()
					+ (tileX + 0.5) * wellLayout.getWidth() / tileConfiguration.getNumTilesX();
			y = zero[1] + wellLayout.getY()
					+ (tileY + 0.5) * wellLayout.getHeight() / tileConfiguration.getNumTilesY();
		} else {
			x = zero[0] + wellLayout.getX() + wellLayout.getWidth() / 2;
			y = zero[1] + wellLayout.getY() + wellLayout.getHeight() / 2;
		}
		return new double[] { x, y };
	}

	private void runFineConfiguration(boolean forceNew) throws AddonException {
		String focusDevice = focusConfiguration == null ? null : focusConfiguration.getFocusDevice();

		final PositionFineConfigurationAddon positionFineConfiguration = new PositionFineConfigurationAddon(client,
				server);
		positionFineConfiguration.setStageDevice(stageDevice);
		positionFineConfiguration.setFocusDevice(focusDevice);
		if (microplateLayout != null) {
			positionFineConfiguration.setSelectedWells(microplateLayout, selectedWellSet());
		}
		if (microplateLayout != null && tileConfiguration != null)
			positionFineConfiguration.setSelectedTiles(tileConfiguration, selectedTiles);
		if (!forceNew) {
			// Re-open the existing configuration even when newly selected wells have
			// no position yet. The editor must retain existing fine-tuned positions
			// while adding/configuring the newly selected wells.
			positionFineConfiguration.setPositions(getPositionsForFineConfiguration());
		}
		positionFineConfiguration.addSaveListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Map<PositionInformation, XYAndFocusPosition> newPositions = positionFineConfiguration
						.getConfiguredPositions();
				if (newPositions == null || newPositions.isEmpty())
					return;
				configuredPositions.clear();
				configuredPositions.putAll(newPositions);
				// The wizard configures exactly the selected wells; keep the selection
				// consistent (e.g. if it
				// only produced a subset, or if groups need to be back-filled for new wells).
				reconcileSelectionWithPositions();
				updateLayout();
			}
		});

		YouScopeFrame fineConfigFrame = positionFineConfiguration.toFrame();
		fineConfigFrame.setVisible(true);
	}

	/**
	 * Returns true when at least one selected well does not yet have a configured
	 * position. In that case the existing position configuration must be re-opened
	 * so the newly selected wells can be positioned and their focus can be
	 * fine-tuned.
	 */
	private boolean hasUnconfiguredSelectedWells() {
		Set<Well> selectedWellSet = selectedWellSet();
		for (Well well : selectedWellSet) {
			if (tileConfiguration == null) {
				if (!configuredPositions.containsKey(new PositionInformation(well)))
					return true;
			} else {
				for (Well tile : selectedTiles) {
					PositionInformation wellPos = new PositionInformation(well);
					PositionInformation tilePos = new PositionInformation(wellPos,
							PositionInformation.POSITION_TYPE_YTILE, tile.getWellY());
					tilePos = new PositionInformation(tilePos,
							PositionInformation.POSITION_TYPE_XTILE, tile.getWellX());
					if (!configuredPositions.containsKey(tilePos))
						return true;
				}
			}
		}
		return false;
	}

	private boolean isCompatible() {
		Set<Well> selectedWellSet = selectedWellSet();
		if (microplateLayout == null && tileConfiguration != null)
			return false;
		else if (microplateLayout == null) {
			for (Entry<PositionInformation, XYAndFocusPosition> entry : configuredPositions.entrySet()) {
				PositionInformation posInfo = entry.getKey();
				double focus = entry.getValue().getFocus();
				if (posInfo.getWell() != null)
					return false;
				if (posInfo.getNumPositions() != 1)
					return false;
				if (!PositionInformation.POSITION_TYPE_MAIN_POSITION.equals(posInfo.getPositionType(0)))
					return false;
				if ((Double.isNaN(focus) && focusConfiguration != null)
						|| (!Double.isNaN(focus) && focusConfiguration == null))
					return false;
			}
			// Check if positions in right order
			for (int pos = 0; pos < configuredPositions.size(); pos++) {
				if (!configuredPositions
						.containsKey(new PositionInformation(PositionInformation.POSITION_TYPE_MAIN_POSITION, pos)))
					return false;
			}
			return true;
		}
		// check if all entries valid
		for (Entry<PositionInformation, XYAndFocusPosition> entry : configuredPositions.entrySet()) {
			PositionInformation posInfo = entry.getKey();
			double focus = entry.getValue().getFocus();
			if (posInfo.getWell() == null) {
				return false;
			}
			if (!selectedWellSet.contains(posInfo.getWell())) {
				return false;
			}
			// A manually added well must exist in the plate definition.
			if (microplateLayout.getWell(posInfo.getWell()) == null) {
				return false;
			}
			if (posInfo.getNumPositions() == 2 && tileConfiguration != null) {
				if (!PositionInformation.POSITION_TYPE_YTILE.equals(posInfo.getPositionType(0))) {
					return false;
				}
				if (!PositionInformation.POSITION_TYPE_XTILE.equals(posInfo.getPositionType(1))) {
					return false;
				}
				if (!selectedTiles.contains(new Well(posInfo.getPosition(0), posInfo.getPosition(1)))) {
					return false;
				}
			} else if (posInfo.getNumPositions() != 0 || tileConfiguration != null) {
				return false;
			}
			if ((Double.isNaN(focus) && focusConfiguration != null)
					|| (!Double.isNaN(focus) && focusConfiguration == null)) {
				return false;
			}
		}
		// Do not require every selected well/tile to have a configured position here.
		// Selected wells without positions are precisely what the fine-configuration
		// wizard must be able to configure when the user re-enters edit mode.
		return true;
	}
}
