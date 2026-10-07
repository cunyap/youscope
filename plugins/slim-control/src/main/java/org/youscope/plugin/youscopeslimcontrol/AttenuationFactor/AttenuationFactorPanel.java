/*******************************************************************************
 * Copyright (c) 2019 - 2020 Elisa Garulli.
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
 *     Elisa Garulli - initial API and implementation
 *     Andreas P. Cuny - migration to the current YouScope API
 ******************************************************************************/
package org.youscope.plugin.youscopeslimcontrol.AttenuationFactor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.plugin.slimidentification.AttenuationFactorWizard;
import org.youscope.plugin.slimjob.SlimProperties;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.PolarizerWheelPosition;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.SLIMController;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.SecondWheelPosition;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DoubleTextField;

/**
 * This represents the panel for the Attenuation Factor computing.
 * @author Elisa Garulli
 *
 */
public class AttenuationFactorPanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	YouScopeServer server;
	YouScopeClient client;
	JButton wizardButton;
	private final DoubleTextField attenuationFactorField = new DoubleTextField();
	Font fontTitle = new Font(null, 1, 12);
	Font fontText = new Font (null, 0, 12);

	/**
	 * Constructor. Adds all the elements created to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public AttenuationFactorPanel(SLIMController slimController, YouScopeClient client, YouScopeServer server) {
		this.server = server;
		this.client = client;
		slimCTRL = slimController;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setBorder(BorderFactory.createTitledBorder(null, "Attenuation Factor", 0, 0, fontTitle));
		JPanel infoAndButtonPanel = createInfoAndStartPanel();
		add(infoAndButtonPanel);
		add(Box.createHorizontalGlue());
		actualizeValues();
	}

	/**
	 * Creates the panel that contains the description of the computation, the start button and the option to ask for
	 * manual confirmation.
	 * @return infoAndButtonPanel a panel with description and button.
	 */
	private JPanel createInfoAndStartPanel() {
		JPanel infoAndButtonPanel = new JPanel();
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.PAGE_AXIS));
		infoAndButtonPanel.setAlignmentX(LEFT_ALIGNMENT);
		JTextArea descriptionText = new JTextArea("Calculate the Attenuation Factor.");
		descriptionText.setFont(fontText);
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		wizardButton = new JButton("Attenuation Factor Wizard");
		wizardButton.addActionListener(this);
		wizardButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				showAttenuationFactorWizard();
			}
		});
		infoAndButtonPanel.add(Box.createGlue());
		infoAndButtonPanel.add(descriptionText);
		descriptionText.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,10)));
		infoAndButtonPanel.add(Box.createGlue());
		attenuationFactorField.setMaximumSize(new Dimension(100, 30));
		attenuationFactorField.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(attenuationFactorField);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,10)));
		infoAndButtonPanel.add(wizardButton);
		return infoAndButtonPanel;
	}


	/**
	 * Programs the listeners to the buttons, allowing the starting of the process and the control of the motors from the
	 * GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == wizardButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY);
		}
	}

	/**
	 * Sets the Attenuation Factor value in the properties for a SLIM job
	 */
	private void actualizeValues()
	{
		SlimProperties properties = new SlimProperties(client);
		attenuationFactorField.setValue(properties.getAttenuationFactor());
	}

	/**
	 * Displays the Attenuation Factor Wizard and calls the function to set the attenuation factor value in
	 * the main panel.
	 */
	private void showAttenuationFactorWizard() {
		AttenuationFactorWizard wizard = new AttenuationFactorWizard(client, server);
		wizard.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				actualizeValues();
			}
		});
		YouScopeFrame frame = wizard.toFrame();
		frame.setVisible(true);
	}
}
