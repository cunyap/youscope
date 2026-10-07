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
package org.youscope.plugin.youscopeslimcontrol.HardwareControl;

import java.awt.event.ActionListener;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JRadioButton;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/**
 * This class represents the panel for the Hardware SetUp.
 * @author Elisa Garulli
 *
 */
public class HardwareSetUpPanel extends JPanel implements ActionListener {

    SLIMController slimCTRL = null;

    public static JRadioButton slimModeButton = new JRadioButton("SLIM mode");
    public static JRadioButton fluoModeButton = new JRadioButton("Fluorescence mode");
    public static JRadioButton calOneModeButton = new JRadioButton("Calibration One");
    public static JRadioButton calTwoModeButton = new JRadioButton("Calibration Two");
    public static ButtonGroup buttonGroupHardwareSetUp;

    Font fontTitle = new Font(null, 1, 12);

    /**
     * Constructor.
     * Adds all the elements created to a JPanel.
     * @param slimController an instance of SLIMController which allows to control the loaded device.
     */
    public HardwareSetUpPanel(SLIMController slimController) {
        slimCTRL = slimController;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createTitledBorder(null, "Hardware setup", 0, 0, fontTitle));

        createRadioButton();
        add(Box.createGlue());
    }

    /**
     * Set the title and the border of the panel
     */
    private void setTitle() {
        TitledBorder titlePanel = new TitledBorder("Hardware Setup");
        titlePanel.setTitleJustification(TitledBorder.LEFT);
        titlePanel.setTitlePosition(TitledBorder.TOP);
        setBorder(titlePanel);
    }

    /**
     * Creates the buttons to drive the motors in the appropriate position for the selected mode.
     */
    private void createRadioButton() {
        buttonGroupHardwareSetUp = new ButtonGroup();

        buttonGroupHardwareSetUp.add(slimModeButton);
        buttonGroupHardwareSetUp.add(fluoModeButton);
        buttonGroupHardwareSetUp.add(calOneModeButton);
        buttonGroupHardwareSetUp.add(calTwoModeButton);

        add(slimModeButton);
        add(fluoModeButton);
        add(calOneModeButton);
        add(calTwoModeButton);

        slimModeButton.addActionListener(this);
        fluoModeButton.addActionListener(this);
        calOneModeButton.addActionListener(this);
        calTwoModeButton.addActionListener(this);
    }

    /**
     * Programs the listeners to the buttons, allowing the control of the motors from the GUI.
     * Moreover it displays the corresponding animation.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == slimModeButton) {
            slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO);
            changeIcon("wheelOne", "polarizerTwo");

            slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY);
            changeIcon("wheelTwo", "empty");
        }

        if (e.getSource() == fluoModeButton) {
            slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY);
            changeIcon("wheelOne", "empty");

            slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY);
            changeIcon("wheelTwo", "empty");
        }

        if (e.getSource() == calOneModeButton) {
            slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE);
            changeIcon("wheelOne", "polarizerOne");

            slimCTRL.setSecondWheelPosition(SecondWheelPosition.LENS);
            changeIcon("wheelTwo", "lens");
        }

        if (e.getSource() == calTwoModeButton) {
            slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE);
            changeIcon("wheelOne", "polarizerOne");

            slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER);
            changeIcon("wheelTwo", "polarizer");
        }
    }

    /**
     * Takes
     * @param wheel
     * @param newPosition
     */
    private void changeIcon(String wheel, String newPosition) {

        if ("wheelOne".equals(wheel)) {

            if ("polarizerOne".equals(newPosition)) {

                if ("polarizerTwo".equals(WheelOnePanel.currentPositionServoOne)) {
                    WheelOnePanel.animationServoOne.setIcon(
                            new ImageIcon(WheelOnePanel.servoOnePolarizerTwoToPolarizerOne));
                    WheelOnePanel.servoOnePolarizerTwoToPolarizerOne.flush();
                }
                else if ("empty".equals(WheelOnePanel.currentPositionServoOne)) {
                    WheelOnePanel.animationServoOne.setIcon(
                            new ImageIcon(WheelOnePanel.servoOneEmptyToPolarizerOne));
                    WheelOnePanel.servoOneEmptyToPolarizerOne.flush();
                }

                WheelOnePanel.currentPositionServoOne = "polarizerOne";
            }

            if ("empty".equals(newPosition)) {

                if ("polarizerTwo".equals(WheelOnePanel.currentPositionServoOne)) {
                    WheelOnePanel.animationServoOne.setIcon(
                            new ImageIcon(WheelOnePanel.servoOnePolarizerTwoToEmpty));
                    WheelOnePanel.servoOnePolarizerTwoToEmpty.flush();
                }
                else if ("polarizerOne".equals(WheelOnePanel.currentPositionServoOne)) {
                    WheelOnePanel.animationServoOne.setIcon(
                            new ImageIcon(WheelOnePanel.servoOnePolarizerOneToEmpty));
                    WheelOnePanel.servoOnePolarizerOneToEmpty.flush();
                }

                WheelOnePanel.currentPositionServoOne = "empty";
            }

            if ("polarizerTwo".equals(newPosition)) {

                if ("polarizerOne".equals(WheelOnePanel.currentPositionServoOne)) {
                    WheelOnePanel.animationServoOne.setIcon(
                            new ImageIcon(WheelOnePanel.servoOnePolarizerOneToPolarizerTwo));
                    WheelOnePanel.servoOnePolarizerOneToPolarizerTwo.flush();
                }
                else if ("empty".equals(WheelOnePanel.currentPositionServoOne)) {
                    WheelOnePanel.animationServoOne.setIcon(
                            new ImageIcon(WheelOnePanel.servoOneEmptyToPolarizerTwo));
                    WheelOnePanel.servoOneEmptyToPolarizerTwo.flush();
                }

                WheelOnePanel.currentPositionServoOne = "polarizerTwo";
            }
        }

        if ("wheelTwo".equals(wheel)) {

            if ("polarizer".equals(newPosition)) {

                if ("lens".equals(WheelTwoPanel.currentPositionServoTwo)) {
                    WheelTwoPanel.animationServoTwo.setIcon(
                            new ImageIcon(WheelTwoPanel.servoTwoLensToPolarizer));
                    WheelTwoPanel.servoTwoLensToPolarizer.flush();
                }
                else if ("empty".equals(WheelTwoPanel.currentPositionServoTwo)) {
                    WheelTwoPanel.animationServoTwo.setIcon(
                            new ImageIcon(WheelTwoPanel.servoTwoEmtyToPolarizer));
                    WheelTwoPanel.servoTwoEmtyToPolarizer.flush();
                }

                WheelTwoPanel.currentPositionServoTwo = "polarizer";
            }

            if ("empty".equals(newPosition)) {

                if ("lens".equals(WheelTwoPanel.currentPositionServoTwo)) {
                    WheelTwoPanel.animationServoTwo.setIcon(
                            new ImageIcon(WheelTwoPanel.servoTwoLensToEmpty));
                    WheelTwoPanel.servoTwoLensToEmpty.flush();
                }
                else if ("polarizer".equals(WheelTwoPanel.currentPositionServoTwo)) {
                    WheelTwoPanel.animationServoTwo.setIcon(
                            new ImageIcon(WheelTwoPanel.servoTwoPolarizerToEmpty));
                    WheelTwoPanel.servoTwoPolarizerToEmpty.flush();
                }

                WheelTwoPanel.currentPositionServoTwo = "empty";
            }

            if ("lens".equals(newPosition)) {

                if ("polarizer".equals(WheelTwoPanel.currentPositionServoTwo)) {
                    WheelTwoPanel.animationServoTwo.setIcon(
                            new ImageIcon(WheelTwoPanel.servoTwoPolarizerToLens));
                    WheelTwoPanel.servoTwoPolarizerToLens.flush();
                }
                else if ("empty".equals(WheelTwoPanel.currentPositionServoTwo)) {
                    WheelTwoPanel.animationServoTwo.setIcon(
                            new ImageIcon(WheelTwoPanel.servoTwoEmptyToLens));
                    WheelTwoPanel.servoTwoEmptyToLens.flush();
                }

                WheelTwoPanel.currentPositionServoTwo = "lens";
            }
        }
    }
}
