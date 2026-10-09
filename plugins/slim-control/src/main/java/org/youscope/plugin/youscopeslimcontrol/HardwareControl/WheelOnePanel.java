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
 *     Elisa Garulli - initial API and implementation
 *     Andreas P. Cuny - migration to the current YouScope API
 ******************************************************************************/
package org.youscope.plugin.youscopeslimcontrol.HardwareControl;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 *  This class represents the panel for the control of the Polarizers Wheel.
 * @author Elisa Garulli
 *
 */
public class WheelOnePanel extends JPanel implements ActionListener{
	private static Image loadImage(String name) {
		java.net.URL resource = WheelOnePanel.class.getResource("/Icons/" + name);
		if (resource == null)
			throw new IllegalStateException("Missing SLIM control resource: " + name);
		return new ImageIcon(resource).getImage();
	}


	SLIMController slimCTRL = null;
	public static JRadioButton polarizerOneButton = new JRadioButton("Polarizer Arm One");
	public static JRadioButton emptyButton = new JRadioButton("Empty");
	public static JRadioButton polarizerTwoButton = new JRadioButton("Polarizer Arm Two");
	public static ButtonGroup buttonGroupWheelOne;
	boolean firstCall = true;
	static String currentPositionServoOne = "empty";
	static Image initialPos = loadImage("servoOneInit.png");
	static Image servoOnePolarizerOneToPolarizerTwo = loadImage("servoOneP1P2x.gif");
	static Image servoOneEmptyToPolarizerOne = loadImage("servoOneEP1x.gif");
	static Image servoOnePolarizerOneToEmpty = loadImage("servoOneP1Ex.gif");
	static Image servoOnePolarizerTwoToPolarizerOne = loadImage("servoOneP2P1x.gif");
	static Image servoOneEmptyToPolarizerTwo = loadImage("servoOneEP2x.gif");
	static Image servoOnePolarizerTwoToEmpty = loadImage("servoOneP2Ex.gif");
	static JButton animationServoOne = new JButton(new ImageIcon(initialPos));


	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public WheelOnePanel(SLIMController slimController) {
		slimCTRL = slimController;
		setLayout(new BorderLayout());
		JLabel wheelName = new JLabel("Polarizers Arm");
		add(wheelName, BorderLayout.NORTH);
		JPanel buttonPanel = createRadioButton();
		JPanel animationPanel = animationPanel();
		add(buttonPanel, BorderLayout.WEST);
		add(animationPanel, BorderLayout.EAST);
	}

	/**
	 * Creates the buttons to drive the motor in the selected position and add the listeners.
	 */
	private JPanel createRadioButton() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
		buttonGroupWheelOne = new ButtonGroup();

		animationServoOne.setOpaque(false);
		animationServoOne.setContentAreaFilled(false);
		animationServoOne.setBorderPainted(true);
		animationServoOne.setFocusPainted(false);

		buttonGroupWheelOne.add(polarizerOneButton);
		buttonGroupWheelOne.add(emptyButton);
		buttonGroupWheelOne.add(polarizerTwoButton);

		buttonPanel.add(polarizerOneButton);
		buttonPanel.add(emptyButton);
		buttonPanel.add(polarizerTwoButton);
		buttonPanel.add(animationServoOne);

		polarizerOneButton.addActionListener(this);
		emptyButton.addActionListener(this);
		polarizerTwoButton.addActionListener(this);

		return buttonPanel;
	}
	/**
	 * Sets the panel where the animation gets displayed.
	 * @return the animation panel.
	 */
	private JPanel animationPanel() {
		JPanel animationPanel = new JPanel();
		animationPanel.setLayout(new BoxLayout(animationPanel, BoxLayout.PAGE_AXIS));
		animationServoOne.setOpaque(false);
		animationServoOne.setContentAreaFilled(false);
		animationServoOne.setBorderPainted(true);
		animationServoOne.setFocusPainted(false);
		animationPanel.add(animationServoOne);

		return animationPanel;
	}

	/**
	 * Programs the listeners to the buttons, allowing the control of the motors from the GUI and the displaying of the
	 * corresponding animation.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == polarizerOneButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE);
			if ("polarizerTwo".equals(currentPositionServoOne)) {
				animationServoOne.setIcon(new ImageIcon(servoOnePolarizerTwoToPolarizerOne));
				servoOnePolarizerTwoToPolarizerOne.flush();
			}
			else if ("empty".equals(currentPositionServoOne) || firstCall) {
				animationServoOne.setIcon(new ImageIcon(servoOneEmptyToPolarizerOne));
				servoOneEmptyToPolarizerOne.flush();
			}
			currentPositionServoOne = "polarizerOne";
			firstCall = false;
		}
		else if(e.getSource() == emptyButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY);
			if ("polarizerTwo".equals(currentPositionServoOne)) {
				animationServoOne.setIcon(new ImageIcon(servoOnePolarizerTwoToEmpty));
				servoOnePolarizerTwoToEmpty.flush();
			}
			else if ("polarizerOne".equals(currentPositionServoOne)) {
				animationServoOne.setIcon(new ImageIcon(servoOnePolarizerOneToEmpty));
				servoOnePolarizerOneToEmpty.flush();
			}
			currentPositionServoOne = "empty";
			firstCall = false;
		}
		else if(e.getSource() == polarizerTwoButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO);
			if ("polarizerOne".equals(currentPositionServoOne)) {
				animationServoOne.setIcon(new ImageIcon(servoOnePolarizerOneToPolarizerTwo));
				servoOnePolarizerOneToPolarizerTwo.flush();
			}
			else if ("empty".equals(currentPositionServoOne) || firstCall) {
				animationServoOne.setIcon(new ImageIcon(servoOneEmptyToPolarizerTwo));
				servoOneEmptyToPolarizerTwo.flush();
			}
			currentPositionServoOne = "polarizerTwo";
			firstCall = false;
		}
	}
}

