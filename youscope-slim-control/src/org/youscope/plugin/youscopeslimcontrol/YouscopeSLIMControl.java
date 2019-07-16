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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

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
	
	/**
	 * Constructor. Provides the adapter class {@link ToolAddonUIAdapter} with all necessary information to provide the standard
	 * functionality which has to be exposed by every tool, such that we can concentrate on the fundamentals. 
	 * Do not initialize the UI elements here (do this in {@link #createUI()}). 
	 * @param client Interface to the YouScope client, e.g. allowing to open or close new windows, or permanently save settings.
	 * @param server Interface to the YouScope server, e.g. allowing access to the microscope.
	 * @throws AddonException 
	 */
	public YouscopeSLIMControl(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server); 
	}
	@Override
	public java.awt.Component createUI()
	{
		/**
		 *  This is the place to setup the UI elements of the first window of your tool.
		 */
		
		// Title of the window, as well as basic configuration.
		setMaximizable(false);
		setResizable(true);
		setTitle("SLIM Control and Calibration Panel");
			        
		// All UI elements should be placed inside a panel, typically referred to as content pane.
				// A {@link DynamicPanel} is a special kind of panel used nearly everywhere in YouScope. It simplifies positioning 
				// of UI elements such that they look "native" for YouScope.
				DynamicPanel contentPane = new DynamicPanel();
		        // Let us add some content...
		        JLabel label = new JLabel("Wheel controller");
		        contentPane.add(label);
		        JTextArea textArea = new JTextArea(
		        		 "This allows you to control the weels in the slim addon\n");
		        // addFill means that the added element can take up extra space when the frame containing it is resized.
		        contentPane.addFill(textArea);
        
        
        
        // Let's add a button to actually do something. In this case, to find out the camera name
        contentPane.add(new JLabel("Servo 1 Control:"));
        JButton button1 = new JButton("Servo 1 pos1");
        contentPane.add(button1);


        button1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					try {
					
						Move1();
					} catch (RemoteException | DeviceException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (MicroscopeException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (MicroscopeLockedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});

        JButton button2 = new JButton("Servo 1 pos2");
        contentPane.add(button2);
        
        button2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					try {
						Move2();
					} catch (RemoteException | DeviceException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
        
        JButton button3 = new JButton("Servo 1 pos0");
        contentPane.add(button3);
        
        button3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					try {
						Move0();
					} catch (RemoteException | DeviceException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
		
        // finally, let us add a listener (ourselves) which gets notified when the frame containing this tool is made visible, or when it is closed.
        getContainingFrame().addFrameListener(this);
        
        return contentPane;
	}
	
	private void Move1() throws RemoteException, DeviceException, MicroscopeException, MicroscopeLockedException, InterruptedException
	{
		/**
		 *  In this function we find out the name of the current standard camera, and display it in
		 *  the provided text field.
		 */

		// First, we get a reference to the YouScope server. The server contains all "real" functionality of YouScope, e.g. the possibility
		// to access devices.
		YouScopeServer server = getServer();
		
		// Now, let's get a reference to the microscope object. This object allows to directly access and manipulate the state of
		// microscope devices.
		Microscope microscope = server.getMicroscope();
		Device arduino = microscope.getDevice("NextGenArduino");
		arduino.getProperty("Pin53").setValue("1");
		/*
		 * try { arduino.getProperty("Pin53").setValue("1"); } catch
		 * (MicroscopeException | MicroscopeLockedException | InterruptedException |
		 * RemoteException | DeviceException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
	}
	
	
	private void Move2() throws RemoteException, DeviceException
	{
		/**
		 *  In this function we find out the name of the current standard camera, and display it in
		 *  the provided text field.
		 */

		// First, we get a reference to the YouScope server. The server contains all "real" functionality of YouScope, e.g. the possibility
		// to access devices.
		YouScopeServer server = getServer();
		
		// Now, let's get a reference to the microscope object. This object allows to directly access and manipulate the state of
		// microscope devices.
		Microscope microscope = server.getMicroscope();
		Device arduino = microscope.getDevice("NextGenArduino");
		
		try {
			arduino.getProperty("Pin53").setValue("2");
			} catch (MicroscopeException | MicroscopeLockedException | 
			 InterruptedException | RemoteException | DeviceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
	}
	
	private void Move0() throws RemoteException, DeviceException
	{
		/**
		 *  In this function we find out the name of the current standard camera, and display it in
		 *  the provided text field.
		 */

		// First, we get a reference to the YouScope server. The server contains all "real" functionality of YouScope, e.g. the possibility
		// to access devices.
		YouScopeServer server = getServer();
		
		// Now, let's get a reference to the microscope object. This object allows to directly access and manipulate the state of
		// microscope devices.
		Microscope microscope = server.getMicroscope();
		Device arduino = microscope.getDevice("NextGenArduino");
		
		try {
			arduino.getProperty("Pin53").setValue("0");
			} catch (MicroscopeException | MicroscopeLockedException | 
			 InterruptedException | RemoteException | DeviceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
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
