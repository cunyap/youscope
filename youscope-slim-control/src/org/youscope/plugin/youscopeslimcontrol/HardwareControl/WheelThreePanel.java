package org.youscope.plugin.youscopeslimcontrol.HardwareControl;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


/**
 *  This class represents the panel for the control of the lenses Wheel.
 * @author Elisa Garulli
 *
 */
public class WheelThreePanel extends JPanel implements ActionListener{
	SLIMController slimCTRL = null;
	static Image wheelPos1 = Toolkit.getDefaultToolkit().createImage("Icons/try1.gif");
	static Image initialPos = Toolkit.getDefaultToolkit().createImage("Icons/LensWheelInit.png");
	static JLabel animationLensWheel = new JLabel(new ImageIcon(initialPos));
	JRadioButton LensOneButton = new JRadioButton("Lens One");
	JRadioButton LensTwoButton = new JRadioButton("Lens Two");
	JRadioButton LensThreeButton = new JRadioButton("Lens Three");
	JRadioButton LensFourButton = new JRadioButton("Lens Four");
	JRadioButton LensFiveButton = new JRadioButton("Lens Five");
	JRadioButton LensSixButton = new JRadioButton("Lens Six");
	JRadioButton[] buttonsArray = { LensOneButton, LensTwoButton,
			LensThreeButton, LensFourButton, LensFiveButton, LensSixButton
	};

	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public WheelThreePanel(SLIMController slimController) {
		slimCTRL = slimController;	
		setLayout(new BorderLayout());
		JLabel wheelName = new JLabel("Lens wheel");
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
		ButtonGroup buttonGroup = new ButtonGroup();

		for (JRadioButton button : buttonsArray) {
			buttonGroup.add(button);
		}

		for (JRadioButton button : buttonsArray) {
			buttonPanel.add(button);
		}

		for (JRadioButton button : buttonsArray) {
			button.addActionListener(this);;
		}
		
		buttonPanel.add(animationLensWheel);
		return buttonPanel;
	}
	
	/**
	 * Sets the panel where the animation gets displayed.
	 * @return the animation panel.
	 */
	private JPanel animationPanel() {
		JPanel animationPanel = new JPanel();
		animationPanel.setLayout(new BoxLayout(animationPanel, BoxLayout.PAGE_AXIS));
		animationPanel.setSize(new Dimension(100,100));
		animationLensWheel.setSize(new Dimension(100,100));
		animationPanel.add(animationLensWheel);

		return animationPanel;
	}

	/**
	 * Programs the listeners to the buttons, allowing the control of the motors from the GUI and display the animation.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == LensOneButton) {
			slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_ONE);
			animationLensWheel.setIcon(new ImageIcon(wheelPos1));
			wheelPos1.flush();
		}
		if(e.getSource() == LensTwoButton) {
			slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_TWO);
			animationLensWheel.setIcon(new ImageIcon(wheelPos1));
			wheelPos1.flush();
		}
		if(e.getSource() == LensThreeButton) {
			slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE);
			animationLensWheel.setIcon(new ImageIcon(wheelPos1));
			wheelPos1.flush();
		}
		if(e.getSource() == LensFourButton) {
			slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_FOUR);
			animationLensWheel.setIcon(new ImageIcon(wheelPos1));
			wheelPos1.flush();
		}
		if(e.getSource() == LensFiveButton) {
			slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_FIVE);
			animationLensWheel.setIcon(new ImageIcon(wheelPos1));
			wheelPos1.flush();
		}
		if(e.getSource() == LensSixButton) {
			slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_SIX);
			animationLensWheel.setIcon(new ImageIcon(wheelPos1));
			wheelPos1.flush();
		}
	}
}

