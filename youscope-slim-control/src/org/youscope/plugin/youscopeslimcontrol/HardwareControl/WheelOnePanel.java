package org.youscope.plugin.youscopeslimcontrol.HardwareControl;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 *  This class represents the panel for the control of the Polarizers Wheel.
 * @author Elisa Garullistatic Image initialPos = Toolkit.getDefaultToolkit().createImage("Icons/LensPolInit.png");
 *
 */
public class WheelOnePanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	public static JRadioButton polarizerOneButton = new JRadioButton("Polarizer One");
	public static JRadioButton emptyButton = new JRadioButton("Empty");
	public static JRadioButton polarizerTwoButton = new JRadioButton("Polarizer Two");
	public static ButtonGroup buttonGroupWheelOne;
	boolean firstCall = true;
	static String currentPositionServoOne = "empty";
	static Image initialPos = Toolkit.getDefaultToolkit().createImage("Icons/servoOneInit.png");
	static Image servoOnePolarizerOneToPolarizerTwo = Toolkit.getDefaultToolkit().createImage("Icons/servoOneP1P2x.gif");
	static Image servoOneEmptyToPolarizerOne = Toolkit.getDefaultToolkit().createImage("Icons/servoOneEP1x.gif");
	static Image servoOnePolarizerOneToEmpty = Toolkit.getDefaultToolkit().createImage("Icons/servoOneP1Ex.gif");
	static Image servoOnePolarizerTwoToPolarizerOne = Toolkit.getDefaultToolkit().createImage("Icons/servoOneP2P1x.gif");
	static Image servoOneEmptyToPolarizerTwo = Toolkit.getDefaultToolkit().createImage("Icons/servoOneEP2x.gif");
	static Image servoOnePolarizerTwoToEmpty = Toolkit.getDefaultToolkit().createImage("Icons/servoOneP2Ex.gif");
	static JButton animationServoOne = new JButton(new ImageIcon(initialPos));

	
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
		JPanel animationPanel = animationPanel();
		add(buttonPanel, BorderLayout.WEST);
		add(animationPanel, BorderLayout.EAST);
	}

	/**
	 * Creates the buttons to drive the motor in the selected position and add the listeners.
	 */
	private JPanel createRadioButton() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
		buttonGroupWheelOne = new ButtonGroup();

		animationServoOne.setOpaque(false);
		animationServoOne.setContentAreaFilled(false);
		animationServoOne.setBorderPainted(true);
		animationServoOne.setFocusPainted(false);
		
		buttonGroupWheelOne.add(polarizerOneButton); 
		buttonGroupWheelOne.add(emptyButton); 
		buttonGroupWheelOne.add(polarizerTwoButton); 
		
		//buttonPanel.add(Box.createRigidArea(new Dimension(0,30)));
		buttonPanel.add(polarizerOneButton);
		buttonPanel.add(emptyButton);
		buttonPanel.add(polarizerTwoButton);
		buttonPanel.add(animationServoOne);

		polarizerOneButton.addActionListener(this);
		emptyButton.addActionListener(this);
		polarizerTwoButton.addActionListener(this);

		return buttonPanel;
	}
	
	private JPanel animationPanel() {
		JPanel animationPanel = new JPanel();
		animationPanel.setLayout(new BoxLayout(animationPanel, BoxLayout.PAGE_AXIS));

		animationServoOne.setOpaque(false);
		animationServoOne.setContentAreaFilled(false);
		animationServoOne.setBorderPainted(true);
		animationServoOne.setFocusPainted(false);
		
		animationPanel.add(animationServoOne);

		return animationPanel;
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
			else if (currentPositionServoOne == "empty" | firstCall) {
				animationServoOne.setIcon(new ImageIcon(servoOneEmptyToPolarizerOne));
				servoOneEmptyToPolarizerOne.flush();
			}
			currentPositionServoOne = "polarizerOne";
			firstCall = false;
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
			firstCall = false;
		}
		else if(e.getSource() == polarizerTwoButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO);
			if (currentPositionServoOne == "polarizerOne") {
				animationServoOne.setIcon(new ImageIcon(servoOnePolarizerOneToPolarizerTwo));
				servoOnePolarizerOneToPolarizerTwo.flush();
			}
			else if (currentPositionServoOne == "empty" | firstCall) {
				animationServoOne.setIcon(new ImageIcon(servoOneEmptyToPolarizerTwo));
				servoOneEmptyToPolarizerTwo.flush();
			}
			currentPositionServoOne = "polarizerTwo";
			firstCall = false;
		}
	}
}

