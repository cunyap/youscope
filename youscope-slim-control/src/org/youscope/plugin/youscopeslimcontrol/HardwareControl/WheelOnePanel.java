package org.youscope.plugin.youscopeslimcontrol.HardwareControl;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 *  This class represents the panel for the control of the Polarizers Wheel.
 * @author Elisa Garulli
 *
 */
public class WheelOnePanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	public static JRadioButton polarizerOneButton = new JRadioButton("Polarizer One");
	public static JRadioButton emptyButton = new JRadioButton("Empty");
	public static JRadioButton polarizerTwoButton = new JRadioButton("Polarizer Two");
	public static ButtonGroup buttonGroupWheelOne;
	
	static String currentPositionServoOne = "";
	static Image servoOnePolarizerOneToPolarizerTwo = Toolkit.getDefaultToolkit().createImage("./Icons/servoOneP1P2.png");
	static Image servoOneEmptyToPolarizerOne = Toolkit.getDefaultToolkit().createImage("./Icons/servoOneEP1.png");
	static Image servoOnePolarizerOneToEmpty = Toolkit.getDefaultToolkit().createImage("./Icons/servoOneP1E.png");
	static Image servoOnePolarizerTwoToPolarizerOne = Toolkit.getDefaultToolkit().createImage("./Icons/servoOneP2P1.png");
	static Image servoOneEmptyToPolarizerTwo = Toolkit.getDefaultToolkit().createImage("./Icons/servoOneEP2.png");
	static Image servoOnePolarizerTwoToEmpty = Toolkit.getDefaultToolkit().createImage("./Icons/servoOneP2E.png");
	static JButton animationServoOne = new JButton(new ImageIcon(servoOneEmptyToPolarizerOne));

	
	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public WheelOnePanel(SLIMController slimController) {
		slimCTRL = slimController;
		setLayout(new BorderLayout());
		JLabel wheelName = new JLabel("Polarizers wheel");
		add(wheelName, BorderLayout.NORTH);
		JPanel buttonPanel = createRadioButton();
		add(buttonPanel, BorderLayout.WEST);
	}

	/**
	 * Creates the buttons to drive the motor in the selected position and add the listeners.
	 */
	private JPanel createRadioButton() {
		JPanel buttonPanel = new JPanel();
		buttonGroupWheelOne = new ButtonGroup();

		animationServoOne.setOpaque(false);
		animationServoOne.setContentAreaFilled(false);
		animationServoOne.setBorderPainted(true);
		animationServoOne.setFocusPainted(false);
		
		buttonGroupWheelOne.add(polarizerOneButton); 
		buttonGroupWheelOne.add(emptyButton); 
		buttonGroupWheelOne.add(polarizerTwoButton); 

		buttonPanel.add(polarizerOneButton);
		buttonPanel.add(emptyButton);
		buttonPanel.add(polarizerTwoButton);
		buttonPanel.add(animationServoOne);

		polarizerOneButton.addActionListener(this);
		emptyButton.addActionListener(this);
		polarizerTwoButton.addActionListener(this);

		return buttonPanel;
	}

	/**
	 * Programs the listeners to the buttons, allowing the control of the motors from the GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == polarizerOneButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE);
			if (currentPositionServoOne == "polarizerTwo") {
				animationServoOne.setIcon(new ImageIcon(servoOnePolarizerTwoToPolarizerOne));
				servoOnePolarizerTwoToPolarizerOne.flush();
			}
			else if (currentPositionServoOne == "empty") {
				animationServoOne.setIcon(new ImageIcon(servoOneEmptyToPolarizerOne));
				servoOneEmptyToPolarizerOne.flush();
			}
			currentPositionServoOne = "polarizerOne";
		}
		else if(e.getSource() == emptyButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY);
			if (currentPositionServoOne == "polarizerTwo") {
				animationServoOne.setIcon(new ImageIcon(servoOnePolarizerTwoToEmpty));
				servoOnePolarizerTwoToEmpty.flush();
			}
			else if (currentPositionServoOne == "polarizerOne") {
				animationServoOne.setIcon(new ImageIcon(servoOnePolarizerOneToEmpty));
				servoOnePolarizerOneToEmpty.flush();
			}
			currentPositionServoOne = "empty";
		}
		else if(e.getSource() == polarizerTwoButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO);
			if (currentPositionServoOne == "polarizerOne") {
				animationServoOne.setIcon(new ImageIcon(servoOnePolarizerOneToPolarizerTwo));
				servoOnePolarizerOneToPolarizerTwo.flush();
			}
			else if (currentPositionServoOne == "empty") {
				animationServoOne.setIcon(new ImageIcon(servoOneEmptyToPolarizerTwo));
				servoOneEmptyToPolarizerTwo.flush();
			}
			currentPositionServoOne = "polarizerTwo";
		}
	}
}

