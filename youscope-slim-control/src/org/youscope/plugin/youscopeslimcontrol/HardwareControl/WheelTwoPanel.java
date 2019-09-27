package org.youscope.plugin.youscopeslimcontrol.HardwareControl;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 *  This class represents the panel for the control of the second Wheel.
 * @author Elisa Garulli
 *
 */
public class WheelTwoPanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	public static JRadioButton lensButton = new JRadioButton("Lens");
	public static JRadioButton emptyButton = new JRadioButton("Empty");
	public static JRadioButton polarizerButton = new JRadioButton("Polarizer");
	static String currentPositionServoTwo = "";
	static Image servoTwoLensToPolarizer = Toolkit.getDefaultToolkit().createImage("./Icons/servoTwoLP.png");
	static Image servoTwoEmtyToPolarizer = Toolkit.getDefaultToolkit().createImage("./Icons/servoTwoEP.png");
	static Image servoTwoPolarizerToEmpty = Toolkit.getDefaultToolkit().createImage("./Icons/servoTwoPE.png");
	static Image servoTwoPolarizerToLens = Toolkit.getDefaultToolkit().createImage("./Icons/servoTwoEL.png");
	static Image servoTwoEmptyToLens = Toolkit.getDefaultToolkit().createImage("./Icons/servoTwoEL.png");
	static Image servoTwoLensToEmpty = Toolkit.getDefaultToolkit().createImage("./Icons/servoTwoLE.png");
	static JButton animationServoTwo = new JButton(new ImageIcon(servoTwoEmtyToPolarizer));

	public static ButtonGroup buttonGroupWheelTwo;
	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public WheelTwoPanel(SLIMController slimController) {
		slimCTRL = slimController;	
		setLayout(new BorderLayout());
		JLabel wheelName = new JLabel("Second wheel");
		add(wheelName, BorderLayout.NORTH);
		JPanel buttonPanel = createRadioButton();
		add(buttonPanel, BorderLayout.WEST);
	}

	/**
	 * Creates the buttons to drive the motor in the selected position and adds the listeners.
	 */
	private JPanel createRadioButton() {
		JPanel buttonPanel = new JPanel();
		buttonGroupWheelTwo = new ButtonGroup();

		animationServoTwo.setOpaque(false);
		animationServoTwo.setContentAreaFilled(false);
		animationServoTwo.setBorderPainted(true);
		animationServoTwo.setFocusPainted(false);

		buttonGroupWheelTwo.add(lensButton); 
		buttonGroupWheelTwo.add(emptyButton); 
		buttonGroupWheelTwo.add(polarizerButton); 

		buttonPanel.add(lensButton);
		buttonPanel.add(emptyButton);
		buttonPanel.add(polarizerButton);
		buttonPanel.add(animationServoTwo);

		lensButton.addActionListener(this);
		emptyButton.addActionListener(this);
		polarizerButton.addActionListener(this);

		return buttonPanel;
	}

	/**
	 * Programs the listeners to the buttons, allowing the control of the motors from the GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == polarizerButton) {
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER);
			if (currentPositionServoTwo == "lens") {
				animationServoTwo.setIcon(new ImageIcon(servoTwoLensToPolarizer));
				servoTwoLensToPolarizer.flush();
			}
			else if (currentPositionServoTwo == "empty") {
				animationServoTwo.setIcon(new ImageIcon(servoTwoEmtyToPolarizer));
				servoTwoEmtyToPolarizer.flush();
			}

			currentPositionServoTwo = "polarizer";
		}
		if(e.getSource() == emptyButton) {
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY);
			if (currentPositionServoTwo == "lens") {
				animationServoTwo.setIcon(new ImageIcon(servoTwoLensToEmpty));
				servoTwoLensToEmpty.flush();
			}
			else if (currentPositionServoTwo == "polarizer") {
				animationServoTwo.setIcon(new ImageIcon(servoTwoPolarizerToEmpty));
				servoTwoPolarizerToEmpty.flush();
			}

			currentPositionServoTwo = "empty";
		}
		if(e.getSource() == lensButton) {
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.LENS);
			if (currentPositionServoTwo == "polarizer") {
				animationServoTwo.setIcon(new ImageIcon(servoTwoPolarizerToLens));
				servoTwoPolarizerToLens.flush();
			}
			else if (currentPositionServoTwo == "empty") {
				animationServoTwo.setIcon(new ImageIcon(servoTwoEmptyToLens));
				servoTwoEmptyToLens.flush();
			}
			currentPositionServoTwo = "lens";
		}
	}

}

