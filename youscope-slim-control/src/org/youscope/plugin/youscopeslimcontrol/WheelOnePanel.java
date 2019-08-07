package org.youscope.plugin.youscopeslimcontrol;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 *  This class deals with the creation of the panel for the control of the Polarizers Wheel.
 * @author localadmin
 *
 */
public class WheelOnePanel extends JPanel implements   ActionListener{
	
	SLIMController slimCTRL = null;
	JRadioButton PolarizerOneButton = new JRadioButton("Polarizer One");
	JRadioButton EmptyButton = new JRadioButton("Empty");
	JRadioButton PolarizerTwoButton = new JRadioButton("Polarizer Two");
	
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

		buttonGroup.add(PolarizerOneButton); 
		buttonGroup.add(EmptyButton); 
		buttonGroup.add(PolarizerTwoButton); 
		
		buttonPanel.add(PolarizerOneButton);
		buttonPanel.add(EmptyButton);
		buttonPanel.add(PolarizerTwoButton);
		
		PolarizerOneButton.addActionListener(this);
		EmptyButton.addActionListener(this);
		PolarizerTwoButton.addActionListener(this);
		
		return buttonPanel;
	}
	
	/**
	 * Programs the listeners to the buttons, allowing the control of the motors from the GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == PolarizerOneButton) {
		slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE_POS);
		}
		else if(e.getSource() == EmptyButton) {
		slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY_POS);
		}
		else if(e.getSource() == PolarizerTwoButton) {
		slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO_POS);
		}
	}
}

