package org.youscope.plugin.youscopeslimcontrol;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.Microscope;
import org.youscope.common.util.ImageConvertException;
import org.youscope.common.util.ImageTools;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.CameraField;
import org.youscope.uielements.ChannelField;
import org.youscope.uielements.DoubleTextField;
import org.youscope.clientinterfaces.StandardProperty;
import org.youscope.clientinterfaces.YouScopeClient;
/**
 * This class represents the panel for the Calibration Two process.
 * @author Elisa Garulli
 *
 */
public class CalibrationTwoPanel extends JPanel implements ActionListener{

	SLIMController slimController = null;
	JButton startCalibrationTwo;
	JButton snapTestImage;
	PhaseChart calibrationResults;
	JPanel graphAndOutput;
	YouScopeClient client;
	YouScopeServer server;
	CameraField cameraField;
	ChannelField channelField;
	CameraDevice camera; 
	DoubleTextField exposureField;
	Device reflector;
	String channel;
	String channelGroup;
	Font fontTitle = new Font(null, 1, 12);
	Font fontText = new Font(null, 0, 12);
	ImageEvent<?> testImage = null;
	JLabel testImageLabel;
	JLabel labelExposure;
	String intensityValue;

	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public CalibrationTwoPanel(SLIMController slimController, YouScopeClient client, 
			YouScopeServer server, Device reflector) {
		intensityValue = new CameraIntensityValue(server, "TIDiaLamp").returnIntensity();
		this.slimController = slimController;
		this.client = client;
		this.server = server;
		this.reflector = reflector;
		exposureField = new DoubleTextField();
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setTitle();
		JPanel infoAndButtonPanel = createInfoAndButtonPanel();
		graphAndOutput = setGraphAndOutputPanel();
		add(infoAndButtonPanel);
		add(Box.createRigidArea(new Dimension(10,0)));
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
		titlePanel.setTitleFont(new Font(null, 1, 15));
		setBorder(titlePanel);
	}

	/**
	 * Creates the panel that contains the description of the computation, the start button and the panel where to select
	 * Camera and Channels.
	 * @return infoAndButtonPanel a panel with description, button, camera and channel options and exposure settings.
	 */
	private JPanel createInfoAndButtonPanel() {
		JPanel infoAndButtonPanel = new JPanel();
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.Y_AXIS));
		JTextArea descriptionText = new JTextArea("Calibrate the phase modulation \n"
				+ "values.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		descriptionText.setFont(fontText);
		startCalibrationTwo = new JButton("Start Calibration Two");		
		startCalibrationTwo.addActionListener(this);
		JPanel channelCameraControlPanel = ChannelControl();
		JPanel exposureIntensityControlPanel = setExposureAndIntensityPanel();
		descriptionText.setAlignmentX(LEFT_ALIGNMENT);
		startCalibrationTwo.setAlignmentX(LEFT_ALIGNMENT);
		channelCameraControlPanel.setAlignmentX(LEFT_ALIGNMENT);
		exposureIntensityControlPanel.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(descriptionText);
		infoAndButtonPanel.add(channelCameraControlPanel);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,10)));
		infoAndButtonPanel.add(exposureIntensityControlPanel);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,10)));
		infoAndButtonPanel.add(startCalibrationTwo);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,20)));
		return infoAndButtonPanel;
	}

	/**
	 * Creates the panel that contains the test image or the gray-scale vs radians graph.
	 * @return graphAndOutput an empty panel that contains the graph once the Calibration is called.
	 */
	private JPanel setGraphAndOutputPanel() {
		graphAndOutput = new JPanel();
		graphAndOutput.setPreferredSize(new Dimension(450,350));
		graphAndOutput.setBorder(BorderFactory.createLineBorder(Color.black));
		return graphAndOutput; 
	}

	/**
	 * Programs the listeners to the buttons, allowing the starting of the process and the control of the motors from  
	 * the GUI and the snapping of the test image.
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == startCalibrationTwo) {

			new Thread (new Runnable() {
				@Override
				public void run() {
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
				new DataCalibrationLCPM(intensityValue, exposureField.getValue(), calibrationResults.returnCalibrationData());
				}
			}).start();
		}
		if (e.getSource() == snapTestImage) {
			graphAndOutput.removeAll();
			BufferedImage testImage = getImage();
			testImage = resize(testImage, 335, 335);
			testImageLabel = new JLabel(new ImageIcon(testImage));
			graphAndOutput.add(testImageLabel);
			graphAndOutput.revalidate();
			graphAndOutput.repaint();
			graphAndOutput.setVisible(true);
		}
	}

	/**
	 * Creates and sets the panel that contains exposure and intensity panel
	 * @return
	 */
	private JPanel setExposureAndIntensityPanel() {
		JPanel exposureIntensityPanel = new JPanel();
		exposureIntensityPanel.setLayout(new BoxLayout(exposureIntensityPanel, BoxLayout.PAGE_AXIS));
		exposureIntensityPanel.setBorder(BorderFactory.createTitledBorder(null, "Exposure and Intensity Settings", 0, 0, fontTitle));
		snapTestImage = new JButton("Snap Test Image");		
		snapTestImage.addActionListener(this);
		labelExposure = new JLabel("Exposure (ms):");
		labelExposure.setForeground(Color.black);
		exposureField.setMinimalValue(0);
		exposureField.setMaximumSize(new Dimension(50, 30));
		JTextArea intensityCurrent = new JTextArea("Current Intensity: " + intensityValue);
		intensityCurrent.setFont(fontText);
		intensityCurrent.setEditable(false);
		intensityCurrent.setEnabled(false);
		intensityCurrent.setOpaque(false);
		labelExposure.setAlignmentX(LEFT_ALIGNMENT);
		exposureField.setAlignmentX(LEFT_ALIGNMENT);
		snapTestImage.setAlignmentX(LEFT_ALIGNMENT);
		intensityCurrent.setAlignmentX(LEFT_ALIGNMENT);
		exposureIntensityPanel.add(labelExposure);
		exposureIntensityPanel.add(exposureField);
		exposureIntensityPanel.add(Box.createRigidArea(new Dimension(0,10)));
		exposureIntensityPanel.add(intensityCurrent);
		exposureIntensityPanel.add(snapTestImage);
		if((boolean) client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_USE_DEFAULT_SETTINGS)){
			exposureField.setValue(client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_DEFAULT_EXPOSURE));
		}
		else {
			exposureField.setValue(client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_EXPOSURE));
		}
		return exposureIntensityPanel; 
	}

	/**
	 * Creates a Panel that contains the dropdown menu to select the Camera and the Channels. It allows the user to choose
	 * between the available options of Camera and channels.
	 * @return the Channel Control panel
	 */
	public JPanel ChannelControl(){
		JPanel controlPanel = new JPanel();
		JLabel labelCamera;
		JLabel labelChannel;

		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.PAGE_AXIS));
		controlPanel.setBorder(BorderFactory.createTitledBorder(null, "Camera and Channel Settings", 0, 0, fontTitle));
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
		labelCamera.setAlignmentX(LEFT_ALIGNMENT);
		cameraField.setAlignmentX(LEFT_ALIGNMENT);
		controlPanel.add(labelCamera);
		controlPanel.add(cameraField);
		labelChannel = new JLabel("Channel:");
		labelChannel.setForeground(Color.black);
		labelChannel.setAlignmentX(LEFT_ALIGNMENT);
		channelField.setAlignmentX(LEFT_ALIGNMENT);
		controlPanel.add(labelChannel);
		controlPanel.add(channelField);

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

	/**
	 * Snaps an image and converts it in a BufferedImage
	 * @return a buffered image
	 */
	private BufferedImage getImage() {
		try {
			testImage = server.getMicroscope().getCameraDevice(cameraField.getCameraDevice()).makeImage(channelGroup, channel, exposureField.getValue());
		} 
		catch (Exception e) {
			client.sendError("Could not snap image.", e);
			testImage = null;
		}
		BufferedImage savedImage = null;
		try {
			savedImage = ImageTools.getMicroscopeImage(testImage);
		} 
		catch (ImageConvertException e1) {
			client.sendError("Could not parse image.", e1);
		}
		return savedImage;
	}

	/**
	 * Resizes and image.
	 * Source: https://stackoverflow.com/questions/9417356/bufferedimage-resize
	 * @param image
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	public static BufferedImage resize(BufferedImage image, int newWidth, int newHeight) { 
		Image tmp = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
		return dimg;
	}  

}






