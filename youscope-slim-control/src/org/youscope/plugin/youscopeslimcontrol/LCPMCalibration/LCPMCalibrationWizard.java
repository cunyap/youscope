package org.youscope.plugin.youscopeslimcontrol.LCPMCalibration;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.CameraField;
import org.youscope.uielements.ChannelField;
import org.youscope.uielements.LiveStreamPanel;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import javax.swing.JButton;
import javax.swing.JOptionPane;
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
	CameraField cameraField;
	ChannelField channelField;
	LiveStreamPanel liveStream;

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
		Thread calibration = calibrationProcess();
		if (e.getSource() == startCalibrationTwo) {
			calibration.start();
		}
		if (e.getSource() == stopCalibrationTwo) {
			calibration.interrupt();
		}
	}
	/**
	 * Sets the channel, channelGroup and camera as the user selects in the liveStream.
	 */
	public void ChannelControl(){
		channel = liveStream.getChannelInfo();
		channelGroup = liveStream.getChannelGroupInfo();
		camera = getCameraDevice(liveStream.getCameraInfo());
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
		});
		return calibration;
	}
}
