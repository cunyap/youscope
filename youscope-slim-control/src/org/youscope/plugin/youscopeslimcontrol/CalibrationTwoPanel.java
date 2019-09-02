package org.youscope.plugin.youscopeslimcontrol;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.awt.event.ActionEvent;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.ImageIcon;

import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.common.configuration.ChannelConfiguration;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.measurement.Measurement;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.Microscope;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.SettingException;
import org.youscope.common.util.ImageConvertException;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;
//import com.sun.corba.se.impl.protocol.InfoOnlyServantCacheLocalCRDImpl;
//import javafx.scene.layout.Border;
//import org.youscope.common.microscope.CameraDevice;
import org.youscope.uielements.CameraField;
import org.youscope.uielements.HistogramPanel;
import org.youscope.uielements.LiveStreamPanel.ChannelControl;
import org.youscope.addon.component.ComponentAddonUI;
/**
 * This class deals with the creation of the panel for the Calibration Two process.
 * @author Elisa Garulli
 *
 */
public class CalibrationTwoPanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	JButton startCalibrationTwo;
	LCPMCal calibrationResults;
	JPanel graphAndOutput;
	YouScopeClient clt;
	YouScopeServer srv;
	CameraDevice cam; 
	Device reflector;

	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */

	public CalibrationTwoPanel(SLIMController slimController) {
		slimCTRL = slimController;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setTitle();
		JPanel infoAndButtonPanel = createInfoAndButtonPanel();
		//		JPanel graphAndOutput = createGraphAndOutputPanel();
		add(infoAndButtonPanel);
		add(Box.createHorizontalGlue());
		//	add(graphAndOutput);
	}
	public CalibrationTwoPanel(SLIMController slimController, CameraDevice camera, YouScopeClient client, 
			YouScopeServer server, Device LCPM) {
		slimCTRL = slimController;
		clt = client;
		cam = camera;
		srv = server;
		reflector = LCPM;
		//String img = camera.makeImage(channelGroupID, channelID, exposure);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setTitle();
		JPanel infoAndButtonPanel = createInfoAndButtonPanel();
		JPanel graphAndOutput = createGraphAndOutputPanel(camera, client, server, LCPM);
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
	 * Creates the panel that contains the description of the computation and the start button 
	 * @return infoAndButtonPanel a panel with description and button.
	 */
	private JPanel createInfoAndButtonPanel() {
		JPanel infoAndButtonPanel = new JPanel();
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.PAGE_AXIS));
		startCalibrationTwo = new JButton("Start Calibration Two");
		startCalibrationTwo.addActionListener(this);
		JTextArea descriptionText = new JTextArea("Calibrate the phase modulation \n"
				+ "values.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		infoAndButtonPanel.add(startCalibrationTwo);
		startCalibrationTwo.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(Box.createGlue());
		infoAndButtonPanel.add(descriptionText);
		descriptionText.setAlignmentX(LEFT_ALIGNMENT);

		return infoAndButtonPanel;
	}

	/**
	 * Creates the panel that contains the graph for visual confirmation, the computed Attenuation Factor and the 
	 * confirm button.
	 * @return graphAndOutput a panel with the graph, the numerical parameter and the button
	 */
	private JPanel createGraphAndOutputPanel(CameraDevice camera, YouScopeClient client, 
			YouScopeServer server, Device LCPM) {
		graphAndOutput = new JPanel();
		graphAndOutput.setPreferredSize(new Dimension(400,300));

////		outputNumbers.append("NN, NN, NN, NN");
////		outputNumbers.setOpaque(false);
////		outputNumbers.setEditable(false);
////		outputNumbers.setEnabled(false);
//
//		graphAndOutput.add(calibrationResults);


		return graphAndOutput;
	}

	/**
	 * Programs the listeners to the buttons, allowing the starting of the process and the control of the motors from the 
	 * GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == startCalibrationTwo) {
//			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO_POS);
//			slimCTRL.setSecondWheelPosition(SecondWheelPosition.LENS_POS);
//			slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_TWO_POS);
//			
//			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", 
//					JOptionPane.ERROR_MESSAGE);
			calibrationResults = new LCPMCal(clt, srv, cam, reflector);
			calibrationResults.setPreferredSize(new Dimension(400,300));
			graphAndOutput.add(calibrationResults);
			graphAndOutput.setVisible(true);
			revalidate();
			
		}
	}


}




