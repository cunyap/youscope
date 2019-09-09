package org.youscope.plugin.youscopeslimcontrol;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;
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
	JRadioButton polarizerOneButton = new JRadioButton("Polarizer One");
	JRadioButton emptyButton = new JRadioButton("Empty");
	JRadioButton polarizerTwoButton = new JRadioButton("Polarizer Two");

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
		ButtonGroup buttonGroup = new ButtonGroup();

		buttonGroup.add(polarizerOneButton); 
		buttonGroup.add(emptyButton); 
		buttonGroup.add(polarizerTwoButton); 

		buttonPanel.add(polarizerOneButton);
		buttonPanel.add(emptyButton);
		buttonPanel.add(polarizerTwoButton);

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
		}
		else if(e.getSource() == emptyButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY);
		}
		else if(e.getSource() == polarizerTwoButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO);
		}
	}
}

