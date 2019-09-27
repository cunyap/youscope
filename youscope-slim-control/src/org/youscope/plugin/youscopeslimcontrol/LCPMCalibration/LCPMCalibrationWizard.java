package org.youscope.plugin.youscopeslimcontrol.LCPMCalibration;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.CameraField;
import org.youscope.uielements.ChannelField;
import org.youscope.uielements.LiveStreamPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import org.youscope.clientinterfaces.StandardProperty;
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


public class LCPMCalibrationWizard extends BasePopUpPanel implements ActionListener
{
	/**
	 * 
	 */
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
	CameraField cameraField;
	ChannelField channelField;
	
	public LCPMCalibrationWizard (SLIMController slimCTRL, YouScopeClient client, YouScopeServer server, Device reflector) {
		super(client);
		this.client = client;
		this.server = server;
		this.reflector = reflector;
		this.slimCTRL = slimCTRL;
		setLCPMCalibrationWizard();
	}
	public void setLCPMCalibrationWizard() {
		setTitle("LCPM Calibration");
		ChannelControl();
		setGraphAndOutputPanel();
		LiveStreamPanel liveStream = new LiveStreamPanel(client, server);
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
	 * @return graphAndOutput an empty panel that contains the graph once the Calibration is called.
	 */
	private void setGraphAndOutputPanel() {
		graphAndOutput = new JPanel();
		graphAndOutput.setPreferredSize(new Dimension(450,350));
		graphAndOutput.setMinimumSize(new Dimension(400,200));
		startCalibrationTwo = new JButton("Start Calibration Two");		
		startCalibrationTwo.addActionListener(this);
		graphAndOutput.add(startCalibrationTwo);
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == startCalibrationTwo) {
			new Thread (new Runnable() {
				@Override
				public void run() {
					ChannelControl();
					HardwareSetUpPanel.buttonGroupHardwareSetUp.setSelected(HardwareSetUpPanel.calTwoModeButton.getModel(), true);
					WheelOnePanel.buttonGroupWheelOne.setSelected(WheelOnePanel.polarizerOneButton.getModel(), true);
					WheelTwoPanel.buttonGroupWheelTwo.setSelected(WheelTwoPanel.polarizerButton.getModel(), true);
					slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE);
					slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER);
					intensityValue = CameraPropertyValue.getProperty(server, "TIDiaLamp", "Intensity");
					exposureValue = CameraPropertyValue.getProperty(server, cameraField.getCameraDevice(), "Exposure");
					calibrationResults = new PhaseChart(client, server, camera, exposureValue, channel, channelGroup, reflector);
					graphAndOutput.removeAll();
					graphAndOutput.add(calibrationResults);
					graphAndOutput.add(startCalibrationTwo);
					graphAndOutput.setVisible(true);
					graphAndOutput.revalidate();
					graphAndOutput.repaint();
					new CalibrationLCPMData(intensityValue, exposureValue, calibrationResults.returnCalibrationData());
				}
			}).start();
		}
	}
	
	public void ChannelControl(){
		cameraField = new CameraField(client, server);
		channelField = new ChannelField(client, server);
			cameraField.setCamera((String) client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_CAMERA));
			channelField.setChannel((String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_CHANNEL_GROUP), 
					(String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_CHANNEL));
			this.camera = getCameraDevice(cameraField.getCameraDevice());
			channel = channelField.getChannel();
			channelGroup = channelField.getChannelGroup();
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
	}
