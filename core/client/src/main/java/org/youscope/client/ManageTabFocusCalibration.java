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
package org.youscope.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.youscope.common.microscope.FocusDevice;
import org.youscope.uielements.DescriptionPanel;
import org.youscope.uielements.StandardFormats;

/**
 * Manage-microscope tab to calibrate the native-unit-to-micrometre factor of
 * focus (Z) devices.
 * <p>
 * The factor is a mechanical step-to-distance calibration of the focus drive:
 * {@code microns = units *
 * factor}. It is <b>not</b> an optical magnification and has nothing to do with
 * objective magnification
 * or numerical aperture. Focus drives that already move in micrometres (e.g. a
 * Nikon Z drive doing true
 * 1&nbsp;&micro;m steps) keep the default factor of 1.0. Drives whose native
 * step is a different or
 * unknown physical distance (a Nikon PFS offset, or an arbitrary third party
 * focus motor) are calibrated
 * here, e.g. against an Argolight Z staircase. With the factor set, YouScope
 * reports and commands focus
 * positions in real micrometres, so a Z stack's step size is a true physical
 * distance and the FAIR
 * export records a correct PhysicalSizeZ.
 * </p>
 *
 * @author Andreas P. Cuny
 */
class ManageTabFocusCalibration extends ManageTabElement {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 7748301006936649168L;

	private final GridBagConstraints newLineConstr = StandardFormats.getNewLineConstraint();
	private final GridBagConstraints bottomConstr = StandardFormats.getBottomContstraint();

	private final JComboBox<String> focusChooser = new JComboBox<String>();
	private final JFormattedTextField unitField = new JFormattedTextField(StandardFormats.getDoubleFormat());

	private String lastFocus = null;
	private boolean loadingData = false;
	private boolean somethingChanged = false;

	ManageTabFocusCalibration() {
		setLayout(new BorderLayout(5, 5));
		setOpaque(false);

		GridBagLayout elementsLayout = new GridBagLayout();
		JPanel elementsPanel = new JPanel(elementsLayout);
		elementsPanel.setOpaque(false);
		elementsPanel.setBorder(new TitledBorder("Focus Device Unit Calibration"));

		focusChooser.setOpaque(false);
		focusChooser.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == ItemEvent.DESELECTED)
					return;
				selectFocus(event.getItem().toString());
			}
		});

		unitField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (!loadingData && lastFocus != null) {
					somethingChanged = true;
					try {
						FocusDevice focusDevice = YouScopeClientImpl.getMicroscope().getFocusDevice(lastFocus);
						focusDevice.setUnitMagnifier(((Number) unitField.getValue()).doubleValue());
					} catch (Exception e) {
						ClientSystem.err.println("Could not store unit calibration for focus device " + lastFocus + ".",
								e);
					}
				}
			}
		});

		StandardFormats.addGridBagElement(new JLabel("Select focus device:"), elementsLayout, newLineConstr,
				elementsPanel);
		StandardFormats.addGridBagElement(focusChooser, elementsLayout, newLineConstr, elementsPanel);
		StandardFormats.addGridBagElement(new JLabel("<html>Enter how many micrometers correspond to<br />"
				+ "one native focus unit (1 unit = X microns).<br />"
				+ "Leave at 1.0 for drives that already move in um<br />"
				+ "(e.g. a Nikon Z drive doing true 1 um steps);<br />"
				+ "calibrate otherwise (e.g. Nikon PFS offset or an<br />"
				+ "uncalibrated focus motor, using an Argolight Z staircase):</html>"),
				elementsLayout, newLineConstr, elementsPanel);
		StandardFormats.addGridBagElement(unitField, elementsLayout, newLineConstr, elementsPanel);
		JPanel emptyPanel = new JPanel();
		emptyPanel.setOpaque(false);
		StandardFormats.addGridBagElement(emptyPanel, elementsLayout, bottomConstr, elementsPanel);

		DescriptionPanel descriptionPanel = new DescriptionPanel("Description",
				"Focus (Z) devices move the focal plane by a physical distance per native step. Unlike the "
						+ "lateral pixel size, this is not an optical magnification: it does not depend on the objective "
						+ "magnification or numerical aperture, only on the mechanics of the focus drive.\n"
						+ "Most drives already move in micrometers, so their factor is 1.0. A Nikon PFS offset is "
						+ "effectively unit-less, and some third party focus motors move an arbitrary distance per unit; "
						+ "these need a calibration factor so that YouScope can express focus positions in real "
						+ "micrometers.\n"
						+ "To calibrate, move the focus device over a known physical distance (e.g. the steps of an "
						+ "Argolight Z staircase) and set the factor to (micrometers moved) / (native units moved). "
						+ "With a correct factor, Z stack step sizes and the exported PhysicalSizeZ become true physical "
						+ "distances.");
		JScrollPane scrollPane = new JScrollPane(descriptionPanel);
		scrollPane.setPreferredSize(new Dimension(400, 150));
		add(scrollPane, BorderLayout.NORTH);
		add(elementsPanel, BorderLayout.CENTER);
	}

	private synchronized void selectFocus(String focus) {
		loadingData = true;
		lastFocus = focus;
		if (focus != null) {
			unitField.setEditable(true);
			try {
				FocusDevice focusDevice = YouScopeClientImpl.getMicroscope().getFocusDevice(focus);
				unitField.setValue(focusDevice.getUnitMagnifier());
			} catch (Exception e) {
				ClientSystem.err.println("Could not load unit calibration for focus device " + lastFocus + ".", e);
			}
		} else {
			unitField.setEditable(false);
		}
		loadingData = false;
	}

	@Override
	public synchronized void initializeContent() {
		lastFocus = null;
		String[] names;
		try {
			FocusDevice[] focusDevices = YouScopeClientImpl.getMicroscope().getFocusDevices();
			names = new String[focusDevices.length];
			for (int i = 0; i < focusDevices.length; i++)
				names[i] = focusDevices[i].getDeviceID();
		} catch (Exception e) {
			ClientSystem.err.println("Could not get installed focus devices.", e);
			names = new String[0];
		}
		focusChooser.removeAllItems();
		for (String name : names)
			focusChooser.addItem(name);

		if (focusChooser.getItemCount() > 0)
			focusChooser.setSelectedIndex(0);
		else
			unitField.setEditable(false);
	}

	@Override
	public boolean storeContent() {
		return somethingChanged;
	}
}
