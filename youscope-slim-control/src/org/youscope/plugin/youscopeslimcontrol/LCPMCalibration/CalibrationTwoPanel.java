package org.youscope.plugin.youscopeslimcontrol.LCPMCalibration;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
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
import org.youscope.uielements.ImagePanel;
import org.youscope.uielements.LiveStreamPanel;

import sun.font.LayoutPathImpl.EmptyPath;

import org.youscope.plugin.slimjob.SlimJobConfiguration;
import org.youscope.plugin.slimjob.SlimProperties;
import org.youscope.plugin.youscopeslimcontrol.BasePopUpPanel;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.PolarizerWheelPosition;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.SLIMController;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.SecondWheelPosition;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.CameraField;
import org.youscope.uielements.ChannelField;
import org.youscope.uielements.DoubleTextField;
import org.youscope.clientinterfaces.StandardProperty;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
/**
 * This class represents the panel for the Calibration Two process.
 * @author Elisa Garulli
 *
 */
public class CalibrationTwoPanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	JButton wizardButton;
	YouScopeClient client;
	YouScopeServer server;
	Device reflector;
	Font fontTitle = new Font(null, 1, 12);
	Font fontText = new Font(null, 0, 12);
	JPanel infoAndButtonPanel;
	
	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public CalibrationTwoPanel(SLIMController slimController, YouScopeClient client, 
			YouScopeServer server, Device reflector) {
		slimCTRL= slimController;
		this.client = client;
		this.server = server;
		this.reflector = reflector;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setBorder(BorderFactory.createTitledBorder(null, "Calibration Two", 0, 0, fontTitle));
		JPanel infoAndButtonPanel = createInfoAndButtonPanel();
		add(infoAndButtonPanel);
		add(Box.createRigidArea(new Dimension(10,0)));
		add(Box.createHorizontalGlue());
		
	}

	/**
	 * Creates the panel that contains the description of the computation, the start button and the panel where to select
	 * Camera and Channels.
	 * @return infoAndButtonPanel a panel with description, button, camera and channel options and exposure settings.
	 */
	private JPanel createInfoAndButtonPanel() {
		infoAndButtonPanel = new JPanel();
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.Y_AXIS));
		JTextArea descriptionText = new JTextArea("Calibrate the phase modulation \n"
				+ "values.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		descriptionText.setFont(fontText);
		wizardButton = new JButton("Open Wizard");		
		wizardButton.addActionListener(this);
		descriptionText.setAlignmentX(LEFT_ALIGNMENT);
		wizardButton.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(descriptionText);
		//infoAndButtonPanel.add(resultsPanel());
		infoAndButtonPanel.add(wizardButton);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,10)));
		return infoAndButtonPanel;
	}

	/**
	 * Programs the listeners to the buttons, allowing the starting of the process and the control of the motors from  
	 * the GUI and the snapping of the test image.
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == wizardButton) {
			LCPMCalibrationWizard calibrationWizard = new LCPMCalibrationWizard(slimCTRL, client, server, reflector);
			YouScopeFrame frame = calibrationWizard.toFrame();
			frame.setVisible(true);
		}
	}
	
}






