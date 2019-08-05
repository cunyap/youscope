/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Moritz Lang - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.youscopeslimcontrol;

import org.youscope.common.microscope.Device;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;
import javax.swing.JRadioButton;

import org.youscope.addon.AddonException;
import org.youscope.addon.tool.ToolAddonUI;
import org.youscope.addon.tool.ToolAddonUIAdapter;
import org.youscope.addon.tool.ToolMetadata;
import org.youscope.addon.tool.ToolMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrameListener;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.Microscope;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DynamicPanel;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;


public class YouscopeSLIMControl extends ToolAddonUIAdapter implements YouScopeFrameListener
{

	public final static String TYPE_IDENTIFIER = "YouScope.SLIM.Control";

	public static ToolMetadata getMetadata()
	{
		return new ToolMetadataAdapter(TYPE_IDENTIFIER, "SLIM Control", new String[]{}, 
				"SLIM Control and Calibration",
				"icons/ghost.png");
	}

	public YouscopeSLIMControl(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server); 
	}
	@Override
	public java.awt.Component createUI()
	{
		// General Settings regarding the main frame (created by YouScope)
		setMaximizable(false);
		setResizable(true);
		setTitle("SLIM Control and Calibration Panel");
		
		// General Settings regarding the content pane
		DynamicPanel contentPane = new DynamicPanel();
		
		// Settings regarding the left side of the content pane
		JPanel leftPanel = new JPanel();

		// Settings regarding the right side of the content pane
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());
		
		CalibrationOneInterface CalOneInterface = new CalibrationOneInterface();
		JPanel CalibrationOnePanel = CalOneInterface.calibrationOneInterfaceCreator("title", "description", "Start Calibration One");
		rightPanel.add(CalibrationOnePanel, BorderLayout.NORTH);
		
		CalibrationTwoInterface CalTwoInterface = new CalibrationTwoInterface();
		JPanel CalibrationTwoPanel = CalTwoInterface.calibrationTwoInterfaceCreator("title", "description", "Start Calibration Two");
		rightPanel.add(CalibrationTwoPanel, BorderLayout.WEST);
		
		AttenuationFactorInterface AttFactorInterface = new AttenuationFactorInterface();
		JPanel AttenuationFactorPanel = AttFactorInterface.calibrationAttenuationFactorCreator("title", "description", "Start Calculation", "User Confirmation", "Confirm");
		rightPanel.add(AttenuationFactorPanel, BorderLayout.SOUTH);
		
		// Settings regarding the upper panel of the left side of the content pane
		JPanel leftUpPanel = new JPanel();
		leftUpPanel.setLayout(new GridBagLayout());
		TitledBorder titleLeftUpPanel = new TitledBorder("Manual Control");
		titleLeftUpPanel.setTitleJustification(TitledBorder.LEFT);
		titleLeftUpPanel.setTitlePosition(TitledBorder.TOP);
		leftUpPanel.setBorder(titleLeftUpPanel);
		
			// Content of the upper panel, left side.
		
		GridBagConstraints upperLeftGC = new GridBagConstraints();
		upperLeftGC.fill = GridBagConstraints.HORIZONTAL;

		PanelsManualWheels wheelsPanel = new PanelsManualWheels();
		
		String[] wheelOneButtonsNames = {"Position One", "Empty", "Position two"};
		ButtonGroup wheelOneButtons = new ButtonGroup();
		JPanel wheelOnePanel = wheelsPanel.wheelsPanelCreator(wheelOneButtonsNames, "Polarizer Wheel" , wheelOneButtons, false);
		upperLeftGC.gridx = 1;
		upperLeftGC.gridy = 1;
		leftUpPanel.add(wheelOnePanel, upperLeftGC);
		
		String[] wheelTwoButtonsNames = {"Lens One", "Empty", "Polarizer"};
		ButtonGroup wheelTwoButtons = new ButtonGroup();
		JPanel wheelTwoPanel = wheelsPanel.wheelsPanelCreator(wheelTwoButtonsNames, "Second Wheel" , wheelTwoButtons, false);
		upperLeftGC.gridx = 1;
		upperLeftGC.gridy = 2;
		leftUpPanel.add(wheelTwoPanel, upperLeftGC);
		
		String[] wheelThreeButtonsNames = {"Lens One", "Lens Two", "Lens Three", "Lens Four", "Lens Five", "Lens Six"};
		ButtonGroup wheelThreeButtons = new ButtonGroup();
		JPanel wheelThreePanel = wheelsPanel.wheelsPanelCreator(wheelThreeButtonsNames, "Lens Wheel" , wheelThreeButtons, false);
		upperLeftGC.gridx = 1;
		upperLeftGC.gridy = 3;
		leftUpPanel.add(wheelThreePanel, upperLeftGC);
		
		
		
			// Settings regarding the lower panel of the left side of the content pane
		JPanel leftDownPanel = new JPanel();
		leftDownPanel.setLayout(new BorderLayout());
		TitledBorder titleLeftDownPanel = new TitledBorder("Hardware Setup");
		titleLeftDownPanel.setTitleJustification(TitledBorder.LEFT);
		titleLeftDownPanel.setTitlePosition(TitledBorder.TOP);
		
		String[] HardwareSetUpButtonsNames = {"SLIM mode", "Fluorescence mode", "Calibration One", "Calibration Two"};
		ButtonGroup setupModeButtons = new ButtonGroup();
		JPanel setupModePanel = wheelsPanel.wheelsPanelCreator(HardwareSetUpButtonsNames, "" , setupModeButtons, true);
		leftDownPanel.add(setupModePanel, BorderLayout.WEST);
		leftDownPanel.setBorder(titleLeftDownPanel);


		
		JSplitPane horizontalSplitPanel = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
		horizontalSplitPanel.setDividerLocation(0.5);
		
		JSplitPane verticalLeftSplitPanel = new JSplitPane (JSplitPane.VERTICAL_SPLIT, leftUpPanel, leftDownPanel);
		verticalLeftSplitPanel.setDividerLocation(0.5);
		
		leftPanel.add(verticalLeftSplitPanel);
		contentPane.add(horizontalSplitPanel);
		

		// finally, let us add a listener (ourselves) which gets notified when the frame containing this tool is made visible, or when it is closed.
		getContainingFrame().addFrameListener(this);

		return contentPane;
	}

	


	@Override
	public void frameClosed()
	{
		// Put in things here which should be done when frame containing tool closes, e.g. some cleanup...
	}

	@Override
	public void frameOpened()
	{
		// Put in things here which should be done when the frame containing this tool opens. This might e.g.
		// be automatically taking an image or similar.
	}
}
