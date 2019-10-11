package org.youscope.plugin.youscopeslimcontrol;
import java.awt.event.ActionListener;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.HardwareSetUpPanel;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.PolarizerWheelPosition;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.SLIMController;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.SecondWheelPosition;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.WheelOnePanel;
import org.youscope.plugin.youscopeslimcontrol.HardwareControl.WheelTwoPanel;

/**
 *  This class represents the panel for the Calibration One.
 * @author Elisa Garulli
 *
 */
public class CalibrationOnePanel extends JPanel implements ActionListener{
	SLIMController slimCTRL = null;
	JButton startCalibrationOneButton;
	Font fontTitle = new Font(null, 1, 12);
	Font fontText = new Font(null, 0, 12);

	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel and creates the start button.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public CalibrationOnePanel(SLIMController slimController) {
		slimCTRL = slimController;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS)); 
		setBorder(BorderFactory.createTitledBorder(null, "Ring Calibration", 0, 0, fontTitle));
		JPanel textPanel = createInfoBox();
		startCalibrationOneButton = new JButton("Start Ring Calibration");
		add(startCalibrationOneButton);
		startCalibrationOneButton.setAlignmentX(LEFT_ALIGNMENT);
		startCalibrationOneButton.addActionListener(this);
		add(Box.createHorizontalGlue());
		add(textPanel);
		textPanel.setAlignmentX(RIGHT_ALIGNMENT);
	}

	
	/**
	 * Creates the panel that contains the description of the computation
	 * @return textPanel a panel containing the description.
	 */
	private JPanel createInfoBox() {
		JPanel textPanel = new JPanel();
		JTextArea descriptionText = new JTextArea("Match the ring displayed on the LCPM \n"
				+ " with the phase plate ring.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		descriptionText.setFont(fontText);
		textPanel.add(descriptionText);
		return textPanel;	
	}
	
	/**
	 * Programs the listeners to the buttons, allowing the starting of the process and the control of the motors from the 
	 * GUI.
	 */
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == startCalibrationOneButton) {
			HardwareSetUpPanel.buttonGroupHardwareSetUp.setSelected(HardwareSetUpPanel.calOneModeButton.getModel(), true);
			WheelOnePanel.buttonGroupWheelOne.setSelected(WheelOnePanel.polarizerOneButton.getModel(), true);
			WheelTwoPanel.buttonGroupWheelTwo.setSelected(WheelTwoPanel.lensButton.getModel(), true);
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.LENS);
			}
		}
	}

