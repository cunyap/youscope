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
import org.youscope.common.microscope.CameraDevice;
import java.rmi.RemoteException;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import org.youscope.addon.AddonException;
import org.youscope.addon.tool.ToolAddonUIAdapter;
import org.youscope.addon.tool.ToolMetadata;
import org.youscope.addon.tool.ToolMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrameListener;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.Microscope;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DynamicPanel;


/**
 * A plug-in to automate the calibration process in the SLIM system. 
 * @author Elisa Garulli
 *
 */
public class YouscopeSLIMControl extends ToolAddonUIAdapter implements YouScopeFrameListener
{
	/**
	 * Define the identifier of the tool. 
	 */
	public final static String TYPE_IDENTIFIER = "YouScope.SLIM.Control";

	/**
	 * 
	 * @return Metadata of the tool.
	 */
	public static ToolMetadata getMetadata()
	{
		return new ToolMetadataAdapter(TYPE_IDENTIFIER, "SLIM Control", new String[]{}, 
				"SLIM Control and Calibration",
				"icons/ghost.png");
	}

	/**
	 * 
	 * @param client Interface to the YouScope client, e.g. allowing to open or close new windows, or permanently save settings.
	 * @param server Interface to the YouScope server, e.g. allowing access to the microscope.
	 * @throws AddonException 
	 */
	
	public YouscopeSLIMControl(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server); 
	}
	@Override
	
	/*
	 * This method defines the user interface which is exposed to the user.
	 */
	public java.awt.Component createUI()
	{
		SLIMController slimController = setSLIMController("NextGenArduino");
		Device LCPM = getLCPMDevice("Reflector");

		// General Settings regarding the main frame.
		setMaximizable(false);
		setResizable(true);
		setTitle("SLIM Control and Calibration Panel");

		// Create the content panel.
		DynamicPanel contentPane = new DynamicPanel();

		// Adding the Manual Control Panel and the Hardware SetUp Panel to the left side of the window.
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
		JPanel leftUpPanel = new ManualControlPanel(slimController);
		JPanel leftDownPanel = new HardwareSetUpPanel(slimController);
		leftPanel.add(leftUpPanel);
		leftUpPanel.setAlignmentX(0);
		leftPanel.add(leftDownPanel);
		leftDownPanel.setAlignmentX(0);

		// Adding the Calibration One, Calibration Two and Attenuation Factor panels to the right side of the window,
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
		rightPanel.add(new CalibrationOnePanel(slimController));
		rightPanel.add(new CalibrationTwoPanel(slimController, getClient(), getServer(), LCPM));
		rightPanel.add(new AttenuationFactorPanel(slimController));

		// Adding all the elements to the content panel.
		JSplitPane horizontalSplitPanel = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
		horizontalSplitPanel.setDividerLocation(0.5);
		horizontalSplitPanel.setResizeWeight(0.5);
		contentPane.add(horizontalSplitPanel);

		getContainingFrame().addFrameListener(this);

		return contentPane;
	}

	/**
	 * 
	 * @param deviceName Name of the device to be loaded. In this case an Arduino.
	 * @return	nextGenArduino, a device which is properly set up.
	 * @throws RemoteException
	 * @throws DeviceException
	 */
	private Device getArduinoDevice(String deviceName) throws RemoteException, DeviceException{
		YouScopeServer server = getServer();
		Microscope microscope = server.getMicroscope();
		Device nextGenArduino = microscope.getDevice(deviceName);
		return nextGenArduino;
	}
	
	private CameraDevice getCameraDevice(String deviceName) {
		CameraDevice cameraDevice = null;
		try {
		YouScopeServer server = getServer();
		Microscope microscope = server.getMicroscope();
		cameraDevice = microscope.getCameraDevice(deviceName);
		}
		catch(RemoteException | DeviceException e) { 
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "No Camera Device connected", "Camera Error",
											JOptionPane.ERROR_MESSAGE);
		}
		return cameraDevice;
	}
	
	private Device getLCPMDevice(String deviceName) {
		Device LCPMDevice = null;
		try {
		YouScopeServer server = getServer();
		Microscope microscope = server.getMicroscope();
		LCPMDevice = microscope.getDevice(deviceName);
		}
		catch(RemoteException | DeviceException e) { 
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "No LCPM connected", "LCPM Error",
											JOptionPane.ERROR_MESSAGE);
		}
		return LCPMDevice;
	}
	
	/**
	 * 
	 * @param deviceName Name of the device to be loaded. In this case an Arduino.
	 * @return slimController an instance of SLIMController which allows to control the loaded device.
	 */
	private SLIMController setSLIMController(String deviceName) {
		ArduinoController nextGenArduino = null;
		try {
			nextGenArduino = new ArduinoController(getArduinoDevice(deviceName));
		}
		catch (RemoteException | DeviceException e) { 
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "No Arduino Device connected", "Arduino Error",
											JOptionPane.ERROR_MESSAGE);
		}
		SLIMController slimController = new SLIMController(nextGenArduino);
		return slimController;
	}

	@Override
	public void frameClosed()
	{
		
	}

	@Override
	public void frameOpened()
	{

	}
}
