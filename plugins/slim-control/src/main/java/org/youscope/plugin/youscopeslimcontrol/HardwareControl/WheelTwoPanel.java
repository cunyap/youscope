package org.youscope.plugin.youscopeslimcontrol.HardwareControl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
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
	private static Image loadImage(String name) {
		java.net.URL resource = WheelTwoPanel.class.getResource("/Icons/" + name);
		if (resource == null)
			throw new IllegalStateException("Missing SLIM control resource: " + name);
		return new ImageIcon(resource).getImage();
	}


	SLIMController slimCTRL = null;
	public static JRadioButton lensButton = new JRadioButton("Lens Arm");
	public static JRadioButton emptyButton = new JRadioButton("Empty");
	public static JRadioButton polarizerButton = new JRadioButton("Polarizer Arm");
	static String currentPositionServoTwo = "";
	boolean firstCall = true;
	static Image initialPos = loadImage("LensPolInit.png");
	static Image servoTwoLensToPolarizer = loadImage("servoTwoLPx.gif");
	static Image servoTwoEmtyToPolarizer = loadImage("servoTwoEPx.gif");
	static Image servoTwoPolarizerToEmpty = loadImage("servoTwoPEx.gif");
	static Image servoTwoPolarizerToLens = loadImage("servoTwoPLx.gif");
	static Image servoTwoEmptyToLens = loadImage("servoTwoELx.gif");
	static Image servoTwoLensToEmpty = loadImage("servoTwoLEx.gif");
	static JButton animationServoTwo = new JButton(new ImageIcon(initialPos));

	public static ButtonGroup buttonGroupWheelTwo;
	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public WheelTwoPanel(SLIMController slimController) {
		slimCTRL = slimController;	
		setLayout(new BorderLayout());
		JLabel wheelName = new JLabel("Optical Elements Arm");
		add(wheelName, BorderLayout.NORTH);
		JPanel buttonPanel = createRadioButton();
		JPanel animationPanel = animationPanel();
		add(buttonPanel, BorderLayout.WEST);
		add(animationPanel, BorderLayout.EAST);
	}

	/**
	 * Creates the buttons to drive the motor in the selected position and adds the listeners.
	 */
	private JPanel createRadioButton() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
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
	 * Sets the panel where the animation gets displayed.
	 * @return the animation panel.
	 */
	private JPanel animationPanel() {
		JPanel animationPanel = new JPanel();
		animationPanel.setLayout(new BoxLayout(animationPanel, BoxLayout.PAGE_AXIS));

		animationServoTwo.setOpaque(false);
		animationServoTwo.setContentAreaFilled(false);
		animationServoTwo.setBorderPainted(true);
		animationServoTwo.setFocusPainted(false);
		
		animationPanel.add(animationServoTwo);

		return animationPanel;
	}

	/**
	 * Programs the listeners to the buttons, allowing the control of the motors from the GUI and the displaying of the 
	 * corresponding animation.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == polarizerButton) {
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER);
			if ("lens".equals(currentPositionServoTwo) || firstCall) {
				animationServoTwo.setIcon(new ImageIcon(servoTwoLensToPolarizer));
				servoTwoLensToPolarizer.flush();
			}
			else if ("empty".equals(currentPositionServoTwo)) {
				animationServoTwo.setIcon(new ImageIcon(servoTwoEmtyToPolarizer));
				servoTwoEmtyToPolarizer.flush();
			}

			currentPositionServoTwo = "polarizer";
			firstCall = false;
		}
		if(e.getSource() == emptyButton) {
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY);
			if ("lens".equals(currentPositionServoTwo) || firstCall) {
				animationServoTwo.setIcon(new ImageIcon(servoTwoLensToEmpty));
				servoTwoLensToEmpty.flush();
			}
			else if ("polarizer".equals(currentPositionServoTwo)) {
				animationServoTwo.setIcon(new ImageIcon(servoTwoPolarizerToEmpty));
				servoTwoPolarizerToEmpty.flush();
			}
			currentPositionServoTwo = "empty";
			firstCall = false;
		}
		if(e.getSource() == lensButton) {
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.LENS);
			if ("polarizer".equals(currentPositionServoTwo) || firstCall) {
				animationServoTwo.setIcon(new ImageIcon(servoTwoPolarizerToLens));
				servoTwoPolarizerToLens.flush();
			}
			else if ("empty".equals(currentPositionServoTwo)) {
				animationServoTwo.setIcon(new ImageIcon(servoTwoEmptyToLens));
				servoTwoEmptyToLens.flush();
			}
			currentPositionServoTwo = "lens";
			firstCall = false;
		}
	}

}

