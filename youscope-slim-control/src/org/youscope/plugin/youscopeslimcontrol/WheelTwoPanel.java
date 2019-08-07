package org.youscope.plugin.youscopeslimcontrol;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 *  This class deals with the creation of the panel for the control of the second Wheel.
 * @author localadmin
 *
 */
public class WheelTwoPanel extends JPanel implements ActionListener{
	
	SLIMController slimCTRL = null;
	JRadioButton LensButton = new JRadioButton("Lens");
	JRadioButton EmptyButton = new JRadioButton("Empty");
	JRadioButton PolarizerButton = new JRadioButton("Polarizer");
	
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
		ButtonGroup buttonGroup = new ButtonGroup();
		
		buttonGroup.add(LensButton); 
		buttonGroup.add(EmptyButton); 
		buttonGroup.add(PolarizerButton); 
		
		buttonPanel.add(LensButton);
		buttonPanel.add(EmptyButton);
		buttonPanel.add(PolarizerButton);
		
		LensButton.addActionListener(this);
		EmptyButton.addActionListener(this);
		PolarizerButton.addActionListener(this);
		
		return buttonPanel;
	}
	
	/**
	 * Programs the listeners to the buttons, allowing the control of the motors from the GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == PolarizerButton) {
		slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER_POS);
		}
		if(e.getSource() == EmptyButton) {
		slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY_POS);
		}
		if(e.getSource() == LensButton) {
		slimCTRL.setSecondWheelPosition(SecondWheelPosition.LENS_POS);
		}
	}
	
}

