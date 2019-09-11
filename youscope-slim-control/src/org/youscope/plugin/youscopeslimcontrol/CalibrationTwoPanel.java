package org.youscope.plugin.youscopeslimcontrol;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.Microscope;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.CameraField;
import org.youscope.uielements.ChannelField;
import org.youscope.uielements.DoubleTextField;
import org.youscope.uielements.DynamicPanel;
import org.youscope.clientinterfaces.StandardProperty;
import org.youscope.clientinterfaces.YouScopeClient;
/**
 * This class represents the panel for the Calibration Two process.
 * @author Elisa Garulli
 *
 */
public class CalibrationTwoPanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	JButton startCalibrationTwo;
	PhaseChart calibrationResults;
	JPanel graphAndOutput;
	YouScopeClient client;
	YouScopeServer server;
	CameraField cameraField;
	ChannelField channelField;
	CameraDevice camera; 
	Device reflector;
	String channel;
	String channelGroup;
	JPanel infoAndButtonPanel;

	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public CalibrationTwoPanel(SLIMController slimController, YouScopeClient client, 
			YouScopeServer server, Device LCPM) {
		slimCTRL = slimController;
		this.client = client;
		this.server = server;
		reflector = LCPM;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setTitle();
		infoAndButtonPanel = createInfoAndButtonPanel();
		graphAndOutput = createGraphAndOutputPanel();
		add(infoAndButtonPanel);
		add(Box.createHorizontalGlue());
		add(graphAndOutput);
	}

	/**
	 * Set the title and the border of the panel
	 */
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Calibration Two");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}

	/**
	 * Creates the panel that contains the description of the computation, the start button and the panel where to select
	 * Camera and Channels.
	 * @return infoAndButtonPanel a panel with description, button an camera and channel option.
	 */
	private JPanel createInfoAndButtonPanel() {
		JPanel infoAndButtonPanel = new JPanel();
		Dimension minSize = new Dimension(100,5);
		Dimension prefSize = new Dimension(100,5);
		Dimension maxSize = new Dimension(100,50);
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.Y_AXIS));
		JTextArea descriptionText = new JTextArea("Calibrate the phase modulation \n"
				+ "values.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		startCalibrationTwo = new JButton("Start Calibration Two");
		startCalibrationTwo.addActionListener(this);
		JPanel channelCameraControl = ChannelControl();
		infoAndButtonPanel.add(descriptionText);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,10)));
		infoAndButtonPanel.add(startCalibrationTwo);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,10)));
		infoAndButtonPanel.add(channelCameraControl);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,200)));


		descriptionText.setAlignmentX(LEFT_ALIGNMENT);
		startCalibrationTwo.setAlignmentX(LEFT_ALIGNMENT);
		channelCameraControl.setAlignmentX(LEFT_ALIGNMENT);
		

		return infoAndButtonPanel;
	}

	/**
	 * Creates the panel that contains the grayscale vs radiants graph.
	 * @return graphAndOutput an empty panel that contains the graph once the Calibration is called.
	 */
	private JPanel createGraphAndOutputPanel() {
		graphAndOutput = new JPanel();
		graphAndOutput.setPreferredSize(new Dimension(450,350));
		graphAndOutput.setBorder(BorderFactory.createLineBorder(Color.black));
		return graphAndOutput; 
	}

	/**
	 * Programs the listeners to the buttons, allowing the starting of the process and the control of the motors from  
	 * the GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == startCalibrationTwo) {
			//			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO_POS);
			//			slimCTRL.setSecondWheelPosition(SecondWheelPosition.LENS_POS);
			//slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_TWO_POS);
			//			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", 
			//											JOptionPane.ERROR_MESSAGE);
			graphAndOutput.removeAll();
			calibrationResults = new PhaseChart(client, server, camera, channel, channelGroup, reflector);
			graphAndOutput.add(calibrationResults);
			graphAndOutput.setVisible(true);
			graphAndOutput.revalidate();
			graphAndOutput.repaint();

		}
	}

	/**
	 * Creates a Panel that contains the dropdown menu to select the Camera and the Channels.
	 * @return
	 */
	public JPanel ChannelControl(){
		JPanel controlPanel = new JPanel();
		JLabel labelCamera;
		JLabel labelChannel;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		controlPanel.setBorder(BorderFactory.createTitledBorder("Camera and Channel Settings"));
		cameraField = new CameraField(client, server);
		channelField = new ChannelField(client, server);
		if((boolean) client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_USE_DEFAULT_SETTINGS)){
			cameraField.setCamera((String) client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_DEFAULT_CAMERA));
			channelField.setChannel((String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_DEFAULT_CHANNEL_GROUP), 
					(String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_DEFAULT_CHANNEL));

			this.camera = getCameraDevice(cameraField.getCameraDevice());
			channel = channelField.getChannel();
			channelGroup = channelField.getChannelGroup();
		}
		else {
			cameraField.setCamera((String) client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_CAMERA));
			channelField.setChannel((String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_CHANNEL_GROUP), 
					(String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_CHANNEL));
			this.camera = getCameraDevice(cameraField.getCameraDevice());
			channel = channelField.getChannel();
			channelGroup = channelField.getChannelGroup();
		}
		labelCamera = new JLabel("Camera:");
		labelCamera.setForeground(Color.black);
		controlPanel.add(labelCamera);
		controlPanel.add(cameraField);
		labelCamera.setAlignmentX(LEFT_ALIGNMENT);
		labelChannel = new JLabel("Channel:");
		labelChannel.setForeground(Color.black);
		controlPanel.add(labelChannel);
		controlPanel.add(channelField);
		labelChannel.setAlignmentX(LEFT_ALIGNMENT);
		return controlPanel;
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






