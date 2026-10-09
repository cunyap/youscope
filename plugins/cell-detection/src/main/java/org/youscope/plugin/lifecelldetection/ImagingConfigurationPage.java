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
 *     Andreas P. Cuny - New Python API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.lifecelldetection;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.youscope.addon.AddonException;
import org.youscope.addon.celldetection.CellDetectionConfiguration;
import org.youscope.addon.celldetection.CellVisualizationConfiguration;
import org.youscope.addon.component.ComponentAddonUI;
import org.youscope.addon.component.ComponentAddonUIListener;
import org.youscope.addon.component.ComponentMetadata;
import org.youscope.addon.measurement.MeasurementAddonUIPage;
import org.youscope.clientinterfaces.StandardProperty;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.microscope.Channel;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.StandardFormats;

/**
 * @author Moritz Land and Andreas P. Cuny
 *
 */
class ImagingConfigurationPage extends MeasurementAddonUIPage<CellDetectionMeasurementConfiguration> {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -8833466993051293407L;

	private JTextField imageNameField = new JTextField();

	private JLabel imageNameLabel = new JLabel(
			"Image name used for saving:");
	private GridBagConstraints newLineConstr = StandardFormats.getNewLineConstraint();

	private JComboBox<String> configGroupField = new JComboBox<String>();
	private JComboBox<String> cameraField = new JComboBox<String>();

	private JComboBox<String> channelField = new JComboBox<String>();

	private JFormattedTextField exposureField = new JFormattedTextField(StandardFormats.getDoubleFormat());

	private JCheckBox saveImagesField = new JCheckBox("Save images", true);

	private JCheckBox createVisualizationImageField = new JCheckBox("Create Detection Visualization Image", true);

	private JLabel visualizationAlgorithmLabel = new JLabel("Cell Visualization Algorithm:");

	private JComboBox<CellDetectionAlgorithm> detectionAlgorithmField = new JComboBox<CellDetectionAlgorithm>();

	private JComboBox<String> visualizationAlgorithmField = new JComboBox<String>();

	private static String CONFIGURE_ALGORITHM = "Configure Algorithm";
	private static final String DEFAULT_CAMERA_LABEL = "(default camera)";

	private JButton configureDetectionAlgorithmButton = new JButton(CONFIGURE_ALGORITHM);
	private JButton configureVisualizationAlgorithmButton = new JButton(CONFIGURE_ALGORITHM);

	private YouScopeFrame parentFrame;

	private CellDetectionConfiguration cellDetectionConfiguration = null;
	private CellVisualizationConfiguration cellVisualizationConfiguration = null;
	/**
	 * True while loadData() is populating the widgets, so combo listeners don't
	 * discard the just-loaded algorithm configurations.
	 */
	private boolean loadingData = false;

	// Optional quantification and tracking (integrated into this page)
	private final javax.swing.JPanel analysisContainer = new javax.swing.JPanel(new java.awt.BorderLayout());
	private CellDetectionJobConfiguration analysisCarrier = new CellDetectionJobConfiguration();
	private QuantificationPanel quantificationImagesPanel = null;
	private QuantificationAlgorithmConfigurationPanel quantificationAlgorithmPanel = null;
	private TrackingAlgorithmConfigurationPanel trackingAlgorithmPanel = null;

	private final YouScopeClient client;
	private final YouScopeServer server;

	ImagingConfigurationPage(YouScopeClient client, YouScopeServer server) {
		this.server = server;
		this.client = client;
	}

	@Override
	public void createUI(YouScopeFrame parentFrame) {
		this.parentFrame = parentFrame;

		GridBagLayout elementsLayout = new GridBagLayout();
		setLayout(elementsLayout);

		StandardFormats.addGridBagElement(new JLabel("Camera:"), elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(cameraField, elementsLayout, newLineConstr, this);

		StandardFormats.addGridBagElement(new JLabel("Channel Group:"), elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(configGroupField, elementsLayout, newLineConstr, this);

		StandardFormats.addGridBagElement(new JLabel("Channel:"), elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(channelField, elementsLayout, newLineConstr, this);

		StandardFormats.addGridBagElement(new JLabel("Exposure (ms):"), elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(exposureField, elementsLayout, newLineConstr, this);

		StandardFormats.addGridBagElement(saveImagesField, elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(imageNameLabel, elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(imageNameField, elementsLayout, newLineConstr, this);

		StandardFormats.addGridBagElement(new JLabel("Cell Detection Algorithm:"), elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(detectionAlgorithmField, elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(configureDetectionAlgorithmButton, elementsLayout, newLineConstr, this);

		StandardFormats.addGridBagElement(createVisualizationImageField, elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(visualizationAlgorithmLabel, elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(visualizationAlgorithmField, elementsLayout, newLineConstr, this);
		StandardFormats.addGridBagElement(configureVisualizationAlgorithmButton, elementsLayout, newLineConstr, this);
		createVisualizationImageField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				boolean selected = createVisualizationImageField.isSelected();
				visualizationAlgorithmLabel.setVisible(selected);
				visualizationAlgorithmField.setVisible(selected);
				configureVisualizationAlgorithmButton.setVisible(selected);
				fireSizeChanged();
			}
		});

		StandardFormats.addGridBagElement(analysisContainer, elementsLayout, newLineConstr, this);

		StandardFormats.addGridBagElement(new JPanel(), elementsLayout, StandardFormats.getBottomContstraint(), this);

		setBorder(new TitledBorder("Imaging Properties"));

		saveImagesField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				boolean selected = saveImagesField.isSelected();
				imageNameLabel.setVisible(selected);
				imageNameField.setVisible(selected);
				fireSizeChanged();
			}
		});

		configGroupField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				loadChannels();
			}
		});
		channelField.addItemListener(new ItemListener() {
			private String lastItem = null;

			@Override
			public void itemStateChanged(ItemEvent itemEvent) {
				if (itemEvent.getStateChange() == ItemEvent.DESELECTED) {
					lastItem = itemEvent.getItem().toString();
					if (lastItem.length() > 3)
						lastItem = lastItem.substring(0, 3);
				} else {
					if (imageNameField.getText().length() == 0
							|| (lastItem != null && lastItem.compareToIgnoreCase(imageNameField.getText()) == 0)) {
						String newName = itemEvent.getItem().toString();
						if (newName.length() > 3)
							newName = newName.substring(0, 3);
						imageNameField.setText(newName);
					}
				}
			}
		});

		detectionAlgorithmField.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent itemEvent) {
				if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
					if (detectionAlgorithmField.getSelectedIndex() < 0) {
						configureDetectionAlgorithmButton.setEnabled(false);
						return;
					}
					configureDetectionAlgorithmButton.setEnabled(true);

					if (loadingData)
						return;
					Object selDet = detectionAlgorithmField.getSelectedItem();
					String newAlgorithm = (selDet instanceof CellDetectionAlgorithm)
							? ((CellDetectionAlgorithm) selDet).getTypeIdentifier()
							: String.valueOf(selDet);
					if (cellDetectionConfiguration != null
							&& cellDetectionConfiguration.getTypeIdentifier().compareTo(newAlgorithm) != 0) {
						cellDetectionConfiguration = null;
					}
				}
			}
		});

		visualizationAlgorithmField.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent itemEvent) {
				if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
					if (visualizationAlgorithmField.getSelectedIndex() < 0) {
						configureVisualizationAlgorithmButton.setEnabled(false);
						return;
					}
					configureVisualizationAlgorithmButton.setEnabled(true);

					if (loadingData)
						return;
					String newAlgorithm = visualizationAlgorithmField.getSelectedItem().toString();
					if (cellVisualizationConfiguration != null
							&& cellVisualizationConfiguration.getTypeIdentifier().compareTo(newAlgorithm) != 0) {
						cellVisualizationConfiguration = null;
					}
				}
			}
		});

		configureDetectionAlgorithmButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configureDetectionAlgorithm();
			}
		});

		configureVisualizationAlgorithmButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configureVisualizationAlgorithm();
			}
		});
	}

	private void configureDetectionAlgorithm() {
		String algorithm = ((CellDetectionAlgorithm) detectionAlgorithmField.getSelectedItem()).getTypeIdentifier();

		ComponentAddonUI<? extends CellDetectionConfiguration> addon;
		try {
			addon = client.getAddonProvider().createComponentUI(algorithm, CellDetectionConfiguration.class);
		} catch (AddonException e) {
			client.sendError("Cannot configure cell detection algorithm.", e);
			return;
		}
		addon.addUIListener(new ComponentAddonUIListener<CellDetectionConfiguration>() {
			@Override
			public void configurationFinished(CellDetectionConfiguration configuration) {
				cellDetectionConfiguration = configuration;
			}
		});
		if (cellDetectionConfiguration != null) {
			try {
				addon.setConfiguration(cellDetectionConfiguration);
			} catch (Exception e1) {
				client.sendError(
						"Could not load cell detection configuration data to edit it. Creating new configuration.", e1);
			}
		}

		YouScopeFrame childFrame;
		try {
			childFrame = addon.toFrame();
		} catch (AddonException e) {
			client.sendError("Could not create cell detection algorithm configuration frame.", e);
			return;
		}
		if (ImagingConfigurationPage.this.parentFrame != null)
			ImagingConfigurationPage.this.parentFrame.addModalChildFrame(childFrame);
		childFrame.setVisible(true);
	}

	private void configureVisualizationAlgorithm() {
		String algorithm = visualizationAlgorithmField.getSelectedItem().toString();
		ComponentAddonUI<? extends CellVisualizationConfiguration> addon;
		try {
			addon = client.getAddonProvider().createComponentUI(algorithm, CellVisualizationConfiguration.class);
		} catch (AddonException e1) {
			client.sendError("Error loading cell visualization configuration UI.", e1);
			return;
		}
		if (cellVisualizationConfiguration != null && cellVisualizationConfiguration.getTypeIdentifier()
				.equals(addon.getAddonMetadata().getTypeIdentifier())) {
			try {
				addon.setConfiguration(cellVisualizationConfiguration);
			} catch (Exception e) {
				client.sendError("Error loading previous cell visualization configuration. Generating new one.", e);
			}
		}
		addon.addUIListener(new ComponentAddonUIListener<CellVisualizationConfiguration>() {
			@Override
			public void configurationFinished(CellVisualizationConfiguration configuration) {
				ImagingConfigurationPage.this.cellVisualizationConfiguration = configuration;
			}
		});
		YouScopeFrame childFrame;
		try {
			childFrame = addon.toFrame();
		} catch (AddonException e) {
			client.sendError("Error loading cell visualization configuration UI.", e);
			return;
		}
		parentFrame.addModalChildFrame(childFrame);
		childFrame.setVisible(true);
	}

	private class CellDetectionAlgorithm {
		private final ComponentMetadata<? extends CellDetectionConfiguration> metadata;

		public CellDetectionAlgorithm(ComponentMetadata<? extends CellDetectionConfiguration> metadata) {
			this.metadata = metadata;
		}

		@Override
		public String toString() {
			return metadata.getName();
		}

		public String getTypeIdentifier() {
			return metadata.getTypeIdentifier();
		}
	}

	private List<CellDetectionAlgorithm> loadCellDetectionAlgorithms() {
		ArrayList<CellDetectionAlgorithm> algorithms = new ArrayList<CellDetectionAlgorithm>();
		detectionAlgorithmField.removeAllItems();
		List<ComponentMetadata<? extends CellDetectionConfiguration>> addonMetadatas = client.getAddonProvider()
				.getComponentMetadata(CellDetectionConfiguration.class);
		for (ComponentMetadata<? extends CellDetectionConfiguration> addonMetadata : addonMetadatas) {
			CellDetectionAlgorithm algorithm = new CellDetectionAlgorithm(addonMetadata);
			algorithms.add(algorithm);
			detectionAlgorithmField.addItem(algorithm);
		}

		return algorithms;
	}

	private String[] loadCellVisualizationAlgorithms() {
		ArrayList<String> algorithms = new ArrayList<String>();
		List<ComponentMetadata<? extends CellVisualizationConfiguration>> addons = client.getAddonProvider()
				.getComponentMetadata(CellVisualizationConfiguration.class);
		for (ComponentMetadata<? extends CellVisualizationConfiguration> addon : addons) {
			algorithms.add(addon.getTypeIdentifier());
		}

		visualizationAlgorithmField.removeAllItems();
		for (String algorithm : algorithms) {
			visualizationAlgorithmField.addItem(algorithm);
		}

		return algorithms.toArray(new String[algorithms.size()]);
	}

	@Override
	public void loadData(CellDetectionMeasurementConfiguration configuration) {
		loadingData = true;
		try {
			loadCameras();
			String camera = configuration.getCamera();
			if (camera == null || camera.length() < 1)
				cameraField.setSelectedItem(DEFAULT_CAMERA_LABEL);
			else {
				cameraField.setSelectedItem(camera);
				if (cameraField.getSelectedIndex() < 0) {
					// configured camera not present on this system: add it so the
					// choice is preserved and visible
					cameraField.addItem(camera);
					cameraField.setSelectedItem(camera);
				}
			}

			loadConfigGroupNames();
			String configGroup = configuration.getChannelGroup();
			if (configGroup == null || configGroup.length() < 1)
				configGroup = (String) client.getPropertyProvider()
						.getProperty(StandardProperty.PROPERTY_LAST_CHANNEL_GROUP);
			for (int i = 0; i < configGroupField.getItemCount(); i++) {
				if (configGroup.compareTo(configGroupField.getItemAt(i).toString()) == 0)
					configGroupField.setSelectedIndex(i);
			}

			loadChannels();
			for (int i = 0; i < channelField.getItemCount(); i++) {
				if (configuration.getChannel().compareTo(channelField.getItemAt(i).toString()) == 0)
					channelField.setSelectedIndex(i);
			}

			String imageName = configuration.getImageSaveName();
			if (imageName.length() < 1) {
				imageName = channelField.getSelectedItem().toString();
				if (imageName.length() > 3)
					imageName = imageName.substring(0, 3);
			}
			imageNameField.setText(imageName);

			exposureField.setValue(configuration.getExposure());

			saveImagesField.setSelected(configuration.isSaveImages());

			// Detection algorithm: the combo holds CellDetectionAlgorithm objects, so
			// select the object whose type identifier matches the saved configuration
			// (setSelectedItem with the raw type-id String would never match, dropping
			// back to the first algorithm and losing the configuration).
			java.util.List<CellDetectionAlgorithm> detectionAlgorithms = loadCellDetectionAlgorithms();
			cellDetectionConfiguration = configuration.getDetectionAlgorithmConfiguration();
			if (cellDetectionConfiguration != null) {
				for (CellDetectionAlgorithm alg : detectionAlgorithms) {
					if (alg.getTypeIdentifier().equals(cellDetectionConfiguration.getTypeIdentifier())) {
						detectionAlgorithmField.setSelectedItem(alg);
						break;
					}
				}
			}
			if (detectionAlgorithmField.getSelectedIndex() < 0) {
				configureDetectionAlgorithmButton.setEnabled(false);
			} else {
				configureDetectionAlgorithmButton.setEnabled(true);
			}

			loadCellVisualizationAlgorithms();
			cellVisualizationConfiguration = configuration.getVisualizationAlgorithmConfiguration();
			if (cellVisualizationConfiguration != null) {
				createVisualizationImageField.setSelected(true);
				visualizationAlgorithmLabel.setVisible(true);
				visualizationAlgorithmField.setVisible(true);
				configureVisualizationAlgorithmButton.setVisible(true);
				visualizationAlgorithmField.setSelectedItem(cellVisualizationConfiguration.getTypeIdentifier());
			} else {
				createVisualizationImageField.setSelected(false);
				visualizationAlgorithmLabel.setVisible(false);
				visualizationAlgorithmField.setVisible(false);
				configureVisualizationAlgorithmButton.setVisible(false);
			}
			if (visualizationAlgorithmField.getSelectedIndex() < 0) {
				configureVisualizationAlgorithmButton.setEnabled(false);
			} else {
				configureVisualizationAlgorithmButton.setEnabled(true);
			}

			// Optional quantification and tracking: populate the carrier from the
			// measurement config and (re)build the three reused job-UI panels.
			analysisCarrier = new CellDetectionJobConfiguration();
			analysisCarrier
					.setQuantificationAlgorithmConfiguration(configuration.getQuantificationAlgorithmConfiguration());
			analysisCarrier.setTrackingAlgorithmConfiguration(configuration.getTrackingAlgorithmConfiguration());
			analysisCarrier.setJobs(configuration.getQuantificationJobs());
			buildAnalysisPanels();
		} finally {
			loadingData = false;
		}
	}

	/**
	 * (Re)builds the quantification-images / quantifier / tracker panels from the
	 * carrier and puts them into the analysis container on this page.
	 */
	private void buildAnalysisPanels() {
		quantificationImagesPanel = new QuantificationPanel(analysisCarrier, client, server, parentFrame);
		quantificationAlgorithmPanel = new QuantificationAlgorithmConfigurationPanel(analysisCarrier, client,
				parentFrame);
		trackingAlgorithmPanel = new TrackingAlgorithmConfigurationPanel(analysisCarrier, client, parentFrame);

		javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
		tabs.addTab("Quantification Images", new javax.swing.JScrollPane(quantificationImagesPanel));
		tabs.addTab("Quantification Algorithm", new javax.swing.JScrollPane(quantificationAlgorithmPanel));
		tabs.addTab("Tracking Algorithm", new javax.swing.JScrollPane(trackingAlgorithmPanel));

		analysisContainer.removeAll();
		analysisContainer.setBorder(new TitledBorder("Quantification & Tracking (optional)"));
		analysisContainer.add(tabs, java.awt.BorderLayout.CENTER);
		analysisContainer.revalidate();
		analysisContainer.repaint();
		fireSizeChanged();
	}

	@Override
	public boolean saveData(CellDetectionMeasurementConfiguration configuration) {
		if (cellDetectionConfiguration == null) {
			String message = "Either no cell detection algorithm is selected,\nor the selected algorithm is yet not configured.";
			JOptionPane.showMessageDialog(this, message, "No Cell Detection Algorithm Configured",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		configuration.setDetectionAlgorithmConfiguration(cellDetectionConfiguration);
		configuration.setVisualizationAlgorithmConfiguration(cellVisualizationConfiguration);
		Object selectedCamera = cameraField.getSelectedItem();
		if (selectedCamera == null || DEFAULT_CAMERA_LABEL.equals(selectedCamera))
			configuration.setCamera("");
		else
			configuration.setCamera(selectedCamera.toString());
		configuration.setChannel((String) configGroupField.getSelectedItem(), (String) channelField.getSelectedItem());
		configuration.setExposure(((Number) exposureField.getValue()).doubleValue());
		configuration.setSaveImages(saveImagesField.isSelected());
		configuration.setImageSaveName(imageNameField.getText());

		// Optional quantification and tracking
		if (quantificationImagesPanel != null) {
			quantificationImagesPanel.commitChanges(analysisCarrier);
			quantificationAlgorithmPanel.commitChanges(analysisCarrier);
			trackingAlgorithmPanel.commitChanges(analysisCarrier);
			configuration
					.setQuantificationAlgorithmConfiguration(analysisCarrier.getQuantificationAlgorithmConfiguration());
			configuration.setTrackingAlgorithmConfiguration(analysisCarrier.getTrackingAlgorithmConfiguration());
			configuration.setQuantificationJobs(analysisCarrier.getJobs());
		}

		client.getPropertyProvider().setProperty(StandardProperty.PROPERTY_LAST_CHANNEL_GROUP,
				configGroupField.getSelectedItem());

		return true;
	}

	@Override
	public void setToDefault(CellDetectionMeasurementConfiguration configuration) {
		// Do nothing

	}

	/**
	 * Populates the camera dropdown from the microscope's camera devices. The
	 * first entry, "(default camera)", maps to an empty camera id = the standard
	 * camera; explicit cameras follow. Needed on systems without a default set.
	 */
	private void loadCameras() {
		cameraField.removeAllItems();
		cameraField.addItem(DEFAULT_CAMERA_LABEL);
		try {
			org.youscope.common.microscope.CameraDevice[] cameras = server.getMicroscope().getCameraDevices();
			if (cameras != null) {
				for (org.youscope.common.microscope.CameraDevice cam : cameras)
					cameraField.addItem(cam.getDeviceID());
			}
		} catch (Exception e) {
			client.sendError("Could not obtain the list of camera devices.", e);
		}
	}

	private void loadConfigGroupNames() {
		String[] configGroupNames = null;
		try {
			configGroupNames = server.getMicroscope().getChannelManager().getChannelGroupIDs();
		} catch (Exception e) {
			client.sendError("Could not obtain config group names.", e);
		}

		if (configGroupNames == null || configGroupNames.length <= 0) {
			configGroupNames = new String[] { "" };
		}

		configGroupField.removeAllItems();
		for (String configGroupName : configGroupNames) {
			configGroupField.addItem(configGroupName);
		}
	}

	private void loadChannels() {
		String[] channelNames = null;

		Object selectedGroup = configGroupField.getSelectedItem();
		if (selectedGroup != null && selectedGroup.toString().length() > 0) {
			try {
				Channel[] channels = server.getMicroscope().getChannelManager().getChannels(selectedGroup.toString());
				channelNames = new String[channels.length];
				for (int i = 0; i < channels.length; i++) {
					channelNames[i] = channels[i].getChannelID();
				}
			} catch (Exception e) {
				client.sendError("Could not obtain channel names of microscope.", e);
			}
		}

		if (channelNames == null || channelNames.length <= 0) {
			channelNames = new String[] { "" };
		}

		channelField.removeAllItems();
		for (String channelName : channelNames) {
			channelField.addItem(channelName);
		}
	}

	@Override
	public String getPageName() {
		return "Imaging Definition";
	}
}
