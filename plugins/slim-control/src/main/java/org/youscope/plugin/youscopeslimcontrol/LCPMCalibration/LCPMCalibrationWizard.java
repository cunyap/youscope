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
package org.youscope.plugin.youscopeslimcontrol.LCPMCalibration;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.LiveStreamPanel;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.Microscope;
import org.youscope.plugin.youscopeslimcontrol.BasePopUpPanel;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.HardwareSetUpPanel;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.PolarizerWheelPosition;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.SLIMController;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.SecondWheelPosition;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.WheelOnePanel;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.WheelTwoPanel;

/**
 * This class represent the wizard panel dedicated to the LCPM calibration. It allows to set camera, channels, exposure
 * etc. from a live stream panel, and then to call the calibration process that will be performed with those settings.
 * @author Elisa Garulli
 *
 */
public class LCPMCalibrationWizard extends BasePopUpPanel implements ActionListener
{
	private static final long serialVersionUID = 7022131022437265022L;
	Device reflector;
	String channel;
	String channelGroup;
	YouScopeClient client;
	YouScopeServer server;
	CameraDevice camera;
	JPanel graphAndOutput;
	SLIMController slimCTRL = null;
	String intensityValue;
	String exposureValue;
	PhaseChart calibrationResults;
	JButton startCalibrationTwo;
	JButton stopCalibrationTwo;
	LiveStreamPanel liveStream;
	private volatile Thread calibrationThread;

	/**
	 * Constructor.
	 * @param slimCTRL the SLIM Controller, which is the interface with the hardware.
	 * @param client
	 * @param server
	 * @param reflector the LCPM device.
	 */
	public LCPMCalibrationWizard (SLIMController slimCTRL, YouScopeClient client, YouScopeServer server, Device reflector) {
		super(client);
		this.client = client;
		this.server = server;
		this.reflector = reflector;
		this.slimCTRL = slimCTRL;
		setLCPMCalibrationWizard();

	}

	/**
	 * Sets the graphical appearance of the wizard.
	 */
	public void setLCPMCalibrationWizard() {
		setTitle("LCPM Calibration");
		setGraphAndOutputPanel();
		liveStream = new LiveStreamPanel(client, server);
		liveStream.setAutoStartStream(true);
		JSplitPane horizontalSplitPanel = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, liveStream, graphAndOutput);
		setPreferredSize(new Dimension(1275, 600));
		setMaximumSize(new Dimension(1275, 600));
		setMinimumSize(new Dimension(500, 200));
		horizontalSplitPanel.setDividerLocation(0.5);
		horizontalSplitPanel.setResizeWeight(0.5);
		horizontalSplitPanel.setVisible(true);
		super.add(horizontalSplitPanel);

	}

	/**
	 * Creates the panel that contains the test image or the gray-scale vs radians graph.
	 * @return graphAndOutput an empty panel that will contain the graph, once the Calibration is called.
	 */
	private void setGraphAndOutputPanel() {
		graphAndOutput = new JPanel();
		graphAndOutput.setPreferredSize(new Dimension(450,350));
		graphAndOutput.setMinimumSize(new Dimension(400,200));
		startCalibrationTwo = new JButton("Start LCPM Calibration");
		startCalibrationTwo.addActionListener(this);
		graphAndOutput.add(startCalibrationTwo);
		stopCalibrationTwo = new JButton("Stop LCPM Calibration");
		stopCalibrationTwo.addActionListener(this);
		graphAndOutput.add(stopCalibrationTwo);
	}

	/**
	 * Programs the listeners to the buttons, allowing  the movement of the hardware in the corresponding position, the
	 * use of the settings chose by the user (camera, channel, channelGroup, exposure, intensity), the start of the
	 * calibration process and the display of the results.
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == startCalibrationTwo) {
			if (calibrationThread != null && calibrationThread.isAlive())
				return;
			updateChannelControl();
			if (camera == null || channel == null || channelGroup == null) {
				JOptionPane.showMessageDialog(this,
						"Camera, channel group, and channel must be selected before calibration.",
						"Calibration Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			calibrationThread = calibrationProcess();
			calibrationThread.start();
		}
		else if (e.getSource() == stopCalibrationTwo) {
			Thread thread = calibrationThread;
			if (thread != null && thread.isAlive())
				thread.interrupt();
		}
	}

	/**
	 * Captures the current channel, channel group, camera and exposure settings
	 * from the live stream before starting the calibration.
	 */
	private void updateChannelControl() {
		channel = liveStream.getChannel();
		channelGroup = liveStream.getChannelGroup();
		camera = getCameraDevice(liveStream.getCamera());
		exposureValue = Double.toString(liveStream.getExposure());
	}
	/**
	 * Gets the camera device, given the device name.
	 * @param deviceName
	 * @return Camera Device
	 */
	private CameraDevice getCameraDevice(String deviceName) {
		CameraDevice cameraDevice = null;
		try {
			YouScopeServer server = this.server;
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

	/**
	 * Take care of opening a new thread with what is needed to perform and visualize the LCPM calibration process
	 * @return the calibration thread
	 */
	private Thread calibrationProcess() {
		Thread calibration = new Thread (new Runnable() {
			@Override
			public void run() {
				try {
				HardwareSetUpPanel.buttonGroupHardwareSetUp.setSelected(HardwareSetUpPanel.calTwoModeButton.getModel(), true);
				WheelOnePanel.buttonGroupWheelOne.setSelected(WheelOnePanel.polarizerOneButton.getModel(), true);
				WheelTwoPanel.buttonGroupWheelTwo.setSelected(WheelTwoPanel.polarizerButton.getModel(), true);
				slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE);
				slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER);
				intensityValue = CameraPropertyValue.getProperty(server, "TIDiaLamp", "Intensity");
				if (camera == null || channel == null || channelGroup == null)
					throw new IllegalStateException("Camera, channel group, and channel must be selected before calibration.");
				calibrationResults = new PhaseChart(client, server, camera, exposureValue, channel, channelGroup, reflector);
				new CalibrationLCPMData(intensityValue, exposureValue, calibrationResults.returnCalibrationData());
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						graphAndOutput.removeAll();
						graphAndOutput.add(calibrationResults);
						graphAndOutput.add(startCalibrationTwo);
						graphAndOutput.setVisible(true);
						graphAndOutput.revalidate();
						graphAndOutput.repaint();
					}
				});
			}
			catch (Exception e) {
				client.sendError("LCPM calibration failed.", e);
			}
			finally {
				calibrationThread = null;
			}
			}
		});
		return calibration;
	}
}
