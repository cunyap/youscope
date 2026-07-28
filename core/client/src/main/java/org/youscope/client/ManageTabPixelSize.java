/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - update API supporting additional magnification
 ******************************************************************************/
/**
 * 
 */
package org.youscope.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.microscope.PixelSize;
import org.youscope.uielements.DescriptionPanel;
import org.youscope.uielements.DeviceSettingsPanel;
import org.youscope.uielements.DynamicPanel;
import org.youscope.uielements.ImageLoadingTools;
import org.youscope.uielements.StandardFormats;

/**
 * @author Moritz Lang
 *
 */
class ManageTabPixelSize extends ManageTabElement {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 1217549458836713214L;
	private final DeviceSettingsPanel deviceSettingsPanel;
	private final JList<String> pixelSizeSettingsField = new JList<String>();
	private final JFormattedTextField pixelSizeField = new JFormattedTextField(StandardFormats.getDoubleFormat());
	// Optional magnification components. When all three are filled, the pixel size
	// is derived from them
	// and shown read-only; when any is blank, the plain editable pixel-size field
	// is used, exactly as
	// before, so existing settings without components are unaffected.
	private final JFormattedTextField cameraPitchField = new JFormattedTextField(StandardFormats.getDoubleFormat());
	private final JFormattedTextField objectiveMagField = new JFormattedTextField(StandardFormats.getDoubleFormat());
	private final JFormattedTextField additionalMagField = new JFormattedTextField(StandardFormats.getDoubleFormat());
	private boolean actualizing = false;
	private boolean somethingChanged = false;
	private boolean contentChanged = false;

	private String[] pixelSizeSettings = new String[0];
	private String currentPixelSizeSetting = null;

	private final JButton addPixelSizeButton;
	private final JButton deletePixelSizeButton;

	/**
	 * Displays pixel size settings which can be simultaneously active. Empty when
	 * the configuration is
	 * unambiguous.
	 */
	private final JLabel conflictLabel = new JLabel(" ");

	private final YouScopeFrame frame;

	ManageTabPixelSize(YouScopeFrame frame) {
		this.frame = frame;
		pixelSizeSettingsField.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() || actualizing || pixelSizeSettingsField.getSelectedIndex() < 0)
					return;
				showPixelSize(pixelSizeSettings[pixelSizeSettingsField.getSelectedIndex()]);
			}
		});
		pixelSizeSettingsField.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		deviceSettingsPanel = new DeviceSettingsPanel(new YouScopeClientConnectionImpl(),
				YouScopeClientImpl.getServer(), true);
		deviceSettingsPanel.setEditable(false);
		deviceSettingsPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (actualizing || currentPixelSizeSetting == null)
					return;
				somethingChanged = true;
				contentChanged = true;
			}
		});

		// Buttons
		Icon addPixelSizeIcon = ImageLoadingTools.getResourceIcon("icons/block--plus.png", "Add Pixel Size Setting");
		Icon deletePixelSizeIcon = ImageLoadingTools.getResourceIcon("icons/block--minus.png",
				"Delete Pixel Size Setting");
		if (addPixelSizeIcon == null)
			addPixelSizeButton = new JButton("New");
		else
			addPixelSizeButton = new JButton(addPixelSizeIcon);
		addPixelSizeButton.setOpaque(false);
		addPixelSizeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				YouScopeFrame modalFrame = ManageTabPixelSize.this.frame.createModalChildFrame();
				@SuppressWarnings("unused")
				PixelSizeNamingFrame pixelSizeNamingFrame = new PixelSizeNamingFrame(modalFrame);
				modalFrame.setVisible(true);
			}
		});

		if (deletePixelSizeIcon == null)
			deletePixelSizeButton = new JButton("Delete");
		else
			deletePixelSizeButton = new JButton(deletePixelSizeIcon);
		deletePixelSizeButton.setOpaque(false);
		deletePixelSizeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int row = pixelSizeSettingsField.getSelectedIndex();
				if (row < 0 || row >= pixelSizeSettings.length)
					return;
				String pixelSizeID = pixelSizeSettings[row];
				int shouldDelete = JOptionPane.showConfirmDialog(null,
						"Should the pixel size setting " + pixelSizeID + " really be deleted?",
						"Delete Pixel Size Setting", JOptionPane.YES_NO_OPTION);
				if (shouldDelete != JOptionPane.YES_OPTION)
					return;

				try {
					YouScopeClientImpl.getMicroscope().getPixelSizeManager().removePixelSize(pixelSizeID);
				} catch (Exception e1) {
					ClientSystem.err.println("Could not remove pixel size setting " + pixelSizeID + ".", e1);
				}
				currentPixelSizeSetting = null;
				initializeContent();
			}
		});

		JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 2, 2));
		buttonPanel.setOpaque(false);
		for (int i = 0; i < 3; i++) {
			JPanel emptyPanel = new JPanel();
			emptyPanel.setOpaque(false);
			buttonPanel.add(emptyPanel);
		}
		buttonPanel.add(addPixelSizeButton);
		buttonPanel.add(deletePixelSizeButton);

		JPanel mainPanel = new JPanel(new GridLayout(1, 2, 2, 2));
		mainPanel.setOpaque(false);

		JPanel pixelSizeSelectionPanel = new JPanel(new BorderLayout());
		pixelSizeSelectionPanel.setOpaque(false);
		pixelSizeSelectionPanel.setBorder(new TitledBorder("Step 1: Select Pixel Size Setting"));
		pixelSizeSelectionPanel.add(new JScrollPane(pixelSizeSettingsField), BorderLayout.CENTER);
		pixelSizeSelectionPanel.add(buttonPanel, BorderLayout.SOUTH);
		mainPanel.add(pixelSizeSelectionPanel);

		JPanel pixelSizePanel = new JPanel(new GridLayout(0, 2, 2, 2));
		pixelSizePanel.setOpaque(false);
		pixelSizeField.setOpaque(false);
		pixelSizeField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (actualizing)
					return;
				somethingChanged = true;
				contentChanged = true;
			}
		});
		// The action listener above only fires when the user presses Enter. A
		// JFormattedTextField
		// commits its value on focus loss without firing an action event, so typing a
		// value and then
		// clicking another setting in the list would discard the edit without any
		// warning. Listening
		// to the committed value as well closes that gap. The actualizing guard is
		// essential:
		// showPixelSize() calls setValue(), which fires this listener too.
		pixelSizeField.addPropertyChangeListener("value", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (actualizing || currentPixelSizeSetting == null)
					return;
				somethingChanged = true;
				contentChanged = true;
			}
		});
		pixelSizePanel.add(new JLabel("Pixel size (um)"));
		pixelSizePanel.add(pixelSizeField);

		// Component fields. Editing any of them recomputes the derived pixel size and,
		// when all three
		// are present, makes the pixel-size field read-only so the user cannot
		// desynchronise it.
		java.beans.PropertyChangeListener componentListener = new java.beans.PropertyChangeListener() {
			@Override
			public void propertyChange(java.beans.PropertyChangeEvent event) {
				if (actualizing || currentPixelSizeSetting == null)
					return;
				somethingChanged = true;
				contentChanged = true;
				updateDerivedPixelSize();
			}
		};
		cameraPitchField.addPropertyChangeListener("value", componentListener);
		objectiveMagField.addPropertyChangeListener("value", componentListener);
		additionalMagField.addPropertyChangeListener("value", componentListener);
		pixelSizePanel.add(new JLabel("Camera pixel pitch (um)"));
		pixelSizePanel.add(cameraPitchField);
		pixelSizePanel.add(new JLabel("Objective magnification"));
		pixelSizePanel.add(objectiveMagField);
		pixelSizePanel.add(new JLabel("Additional magnification"));
		pixelSizePanel.add(additionalMagField);

		DynamicPanel pixelSizeDefinitionPanel = new DynamicPanel();
		pixelSizeDefinitionPanel.setOpaque(false);
		pixelSizeDefinitionPanel.setBorder(new TitledBorder("Step 2: Configure Pixel Size Setting"));
		pixelSizeDefinitionPanel.add(pixelSizePanel);
		pixelSizeDefinitionPanel
				.add(new JLabel("<html>Device settings which have to be active for the given pixel size to apply:"));
		pixelSizeDefinitionPanel.addFill(deviceSettingsPanel);

		conflictLabel.setForeground(Color.RED);
		conflictLabel.setVerticalAlignment(SwingConstants.TOP);
		pixelSizeDefinitionPanel.add(conflictLabel);

		mainPanel.add(pixelSizeDefinitionPanel);

		DescriptionPanel descriptionPanel = new DescriptionPanel("Description",
				"Several tasks, such as stitching and FAIR metadata export, require YouScope to know the size of a pixel in micro-meter.\n"
						+ "This size is determined by the physical pixel size of the camera (typically 6.45 micro meters) divided by the total magnification, that is, the objective magnification multiplied by any additional magnification in the light path such as a 1.5x tube lens.\n"
						+ "\n"
						+ "Define one pixel size setting for each combination of camera, objective and additional magnification which occurs on your microscope. Each setting lists the device settings which have to be active for it to apply.\n"
						+ "\n"
						+ "Every setting must be distinguishable from every other setting by at least one device setting. If two settings can be active at the same time, for example because both only specify the objective while your microscope has two cameras with different pixel sizes, YouScope cannot determine which one applies and will report the pixel size as unknown instead of guessing. Add the distinguishing device, such as the camera, to the device settings of both.\n"
						+ "\n"
						+ "If your microscope has a manual magnification changer which YouScope cannot read out, define it as a manual state device so that its position is recorded with every measurement. Otherwise the pixel size cannot be verified after the acquisition.");

		setOpaque(false);
		setLayout(new BorderLayout(5, 5));
		JScrollPane scrollPane = new JScrollPane(descriptionPanel);
		scrollPane.setPreferredSize(new Dimension(400, 150));
		add(scrollPane, BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);
	}

	private class PixelSizeNamingFrame {
		private final JTextField pixelSizeSettingIDField = new JTextField("");
		private final YouScopeFrame frame;

		PixelSizeNamingFrame(YouScopeFrame frame) {
			this.frame = frame;
			frame.setTitle("New Pixel Size Setting");
			frame.setResizable(false);
			frame.setClosable(true);
			frame.setMaximizable(false);

			JButton addButton = new JButton("Add Pixel Size Setting");
			addButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String pixelSizeID = pixelSizeSettingIDField.getText();
					if (pixelSizeID.length() < 1) {
						JOptionPane.showMessageDialog(null, "The pixel size ID has to be at least one character long.",
								"Invalid Pixel Size Setting Name", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					try {
						YouScopeClientImpl.getMicroscope().getPixelSizeManager().addPixelSize(pixelSizeID);
					} catch (Exception e) {
						ClientSystem.err.println("Could not add pixel size setting.", e);
					}
					initializeContent();
					PixelSizeNamingFrame.this.frame.setVisible(false);
				}
			});

			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					PixelSizeNamingFrame.this.frame.setVisible(false);
				}
			});

			JPanel elementsPanel = new JPanel(new GridLayout(1, 2, 2, 2));
			elementsPanel.add(new JLabel("Pixel Size Setting Name:"));
			elementsPanel.add(pixelSizeSettingIDField);

			JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 2, 2));
			buttonsPanel.add(cancelButton);
			buttonsPanel.add(addButton);

			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.add(elementsPanel, BorderLayout.CENTER);
			contentPane.add(buttonsPanel, BorderLayout.SOUTH);
			frame.setContentPane(contentPane);
			frame.pack();
		}
	}

	/**
	 * Checks all pixel size settings for pairs which can be simultaneously active,
	 * and displays the
	 * result. Ambiguity is reported rather than blocked: a configuration which is
	 * already ambiguous
	 * would otherwise become uneditable.
	 */
	private void updateConflictWarning() {
		try {
			List<PixelSizeAmbiguityValidator.Conflict> conflicts = PixelSizeAmbiguityValidator.validate(
					YouScopeClientImpl.getMicroscope().getPixelSizeManager().getPixelSizes());
			if (conflicts.isEmpty()) {
				conflictLabel.setText(" ");
				return;
			}
			StringBuilder builder = new StringBuilder("<html><b>Ambiguous pixel size configuration:</b><ul>");
			for (PixelSizeAmbiguityValidator.Conflict conflict : conflicts) {
				builder.append("<li>").append(conflict.getMessage()).append("</li>");
			}
			builder.append("</ul></html>");
			conflictLabel.setText(builder.toString());
		} catch (Exception e) {
			ClientSystem.err.println("Could not check pixel size settings for ambiguity.", e);
			conflictLabel.setText(" ");
		}
	}

	/**
	 * Reads the three component fields; if all are present and the magnifications
	 * are non-zero, writes
	 * the derived pixel size into the (now read-only) pixel-size field. Otherwise
	 * leaves the pixel-size
	 * field editable and untouched.
	 */
	private void updateDerivedPixelSize() {
		Double pitch = toDouble(cameraPitchField.getValue());
		Double objectiveMag = toDouble(objectiveMagField.getValue());
		Double additionalMag = toDouble(additionalMagField.getValue());
		boolean complete = pitch != null && objectiveMag != null && additionalMag != null
				&& objectiveMag.doubleValue() != 0 && additionalMag.doubleValue() != 0;
		if (complete) {
			double derived = pitch.doubleValue() / (objectiveMag.doubleValue() * additionalMag.doubleValue());
			boolean wasActualizing = actualizing;
			actualizing = true;
			pixelSizeField.setValue(Double.valueOf(derived));
			actualizing = wasActualizing;
			pixelSizeField.setEditable(false);
			pixelSizeField.setToolTipText("Derived from the components below and therefore read-only.");
		} else {
			pixelSizeField.setEditable(currentPixelSizeSetting != null);
			pixelSizeField.setToolTipText("Set directly, or fill in all three components to derive it.");
		}
	}

	private static Double toDouble(Object value) {
		if (!(value instanceof Number))
			return null;
		return Double.valueOf(((Number) value).doubleValue());
	}

	private void showPixelSize(String pixelSizeID) {
		actualizing = true;
		if (currentPixelSizeSetting != null && somethingChanged) {
			try {
				PixelSize pixelSize = YouScopeClientImpl.getMicroscope().getPixelSizeManager()
						.getPixelSize(currentPixelSizeSetting);
				// Components first: setting all three derives and overwrites the stored pixel
				// size, so a
				// direct setPixelSize afterwards would be pointless when components are
				// complete. When
				// components are incomplete, they are cleared to null and the direct value is
				// kept.
				pixelSize.setCameraPixelPitchMicrons(toDouble(cameraPitchField.getValue()));
				pixelSize.setObjectiveMagnification(toDouble(objectiveMagField.getValue()));
				pixelSize.setAdditionalMagnification(toDouble(additionalMagField.getValue()));
				if (!pixelSize.hasMagnificationComponents())
					pixelSize.setPixelSize(((Number) pixelSizeField.getValue()).doubleValue());
				pixelSize.setPixelSizeSettings(deviceSettingsPanel.getSettings());
			} catch (Exception e) {
				ClientSystem.err.println("Could not set pixel size setting " + currentPixelSizeSetting + ".", e);
			}
		}
		somethingChanged = false;
		currentPixelSizeSetting = pixelSizeID;
		deviceSettingsPanel.clear();
		if (pixelSizeID == null) {
			pixelSizeField.setEditable(false);
			cameraPitchField.setValue(null);
			objectiveMagField.setValue(null);
			additionalMagField.setValue(null);
			cameraPitchField.setEditable(false);
			objectiveMagField.setEditable(false);
			additionalMagField.setEditable(false);
			deviceSettingsPanel.setEditable(false);
		} else {
			pixelSizeField.setEditable(true);
			cameraPitchField.setEditable(true);
			objectiveMagField.setEditable(true);
			additionalMagField.setEditable(true);
			deviceSettingsPanel.setEditable(true);
			try {
				deviceSettingsPanel.setSettings(YouScopeClientImpl.getMicroscope().getPixelSizeManager()
						.getPixelSize(pixelSizeID).getPixelSizeSettings());
			} catch (Exception e) {
				ClientSystem.err.println("Could not get device settings for pixel size setting " + pixelSizeID + ".",
						e);
			}
			try {
				PixelSize pixelSize = YouScopeClientImpl.getMicroscope().getPixelSizeManager()
						.getPixelSize(pixelSizeID);
				pixelSizeField.setValue(pixelSize.getPixelSize());
				cameraPitchField.setValue(pixelSize.getCameraPixelPitchMicrons());
				objectiveMagField.setValue(pixelSize.getObjectiveMagnification());
				additionalMagField.setValue(pixelSize.getAdditionalMagnification());
				updateDerivedPixelSize();
			} catch (Exception e) {
				ClientSystem.err.println("Could not get pixel size for setting " + pixelSizeID + ".", e);
			}
		}
		updateConflictWarning();
		actualizing = false;
	}

	@Override
	public void initializeContent() {
		actualizing = true;
		Vector<String> pixelSizeVector = new Vector<String>();
		try {
			for (PixelSize pixelSize : YouScopeClientImpl.getMicroscope().getPixelSizeManager().getPixelSizes()) {
				pixelSizeVector.addElement(pixelSize.getPixelSizeID());
			}

		} catch (Exception e) {
			ClientSystem.err.println("Could not obtain pixel size setting IDs.", e);
			pixelSizeVector.clear();
		}
		pixelSizeSettings = pixelSizeVector.toArray(new String[pixelSizeVector.size()]);
		pixelSizeSettingsField.setListData(pixelSizeSettings);

		if (pixelSizeSettings.length > 0) {
			pixelSizeSettingsField.setSelectedIndex(0);
			showPixelSize(pixelSizeSettings[0]);
		} else {
			showPixelSize(null);
		}
		updateConflictWarning();
		actualizing = false;

	}

	@Override
	public boolean storeContent() {
		showPixelSize(null);
		return contentChanged;
	}
}
