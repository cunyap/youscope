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

import javax.swing.Box;
import javax.swing.BoxLayout;
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
		SLIMController slimController = setSLIMController("NextGenArduino");
		
		// General Settings regarding the main frame (created by YouScope)
		setMaximizable(false);
		setResizable(true);
		setTitle("SLIM Control and Calibration Panel");
		
		

		// General Settings regarding the content pane
		DynamicPanel contentPane = new DynamicPanel();


		// Setting up the left side
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
		JPanel leftUpPanel = new ManualControlPanel(slimController);
		JPanel leftDownPanel = new HardwareSetUpPanel(slimController);
		leftPanel.add(leftUpPanel);
		leftUpPanel.setAlignmentX(0);
		leftPanel.add(leftDownPanel);
		leftDownPanel.setAlignmentX(0);

		
		// Setting up the right side
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
		rightPanel.add(new CalibrationOnePanel(slimController));
		rightPanel.add(new CalibrationTwoPanel(slimController));
		rightPanel.add(new AttenuationFactorPanel(slimController));

		// Adding the elements to the content panel.
		
		JSplitPane horizontalSplitPanel = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
		horizontalSplitPanel.setDividerLocation(0.5);
		horizontalSplitPanel.setResizeWeight(0.5);
		contentPane.add(horizontalSplitPanel);



		// finally, let us add a listener (ourselves) which gets notified when the frame containing this tool is made visible, or when it is closed.
		getContainingFrame().addFrameListener(this);




		return contentPane;
	}
	
	private Device getArduinoDevice(String deviceName) throws RemoteException, DeviceException{
		YouScopeServer server = getServer();
		Microscope microscope = server.getMicroscope();
		Device nextGenArduino = microscope.getDevice(deviceName);
		return nextGenArduino;
	}
	
	private SLIMController setSLIMController(String deviceName) {
		ArduinoController nextGenArduino = null;
		try {
			nextGenArduino = new ArduinoController(getArduinoDevice(deviceName));
		}
		catch (RemoteException | DeviceException e) { 
			  e.printStackTrace();
			  }
		SLIMController slimController = new SLIMController(nextGenArduino);
		return slimController;
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
