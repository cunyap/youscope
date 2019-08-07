package org.youscope.plugin.youscopeslimcontrol;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


/**
 *  This class deals with the creation of the panel for the control of the lenses Wheel.
 * @author localadmin
 *
 */
public class WheelThreePanel extends JPanel implements ActionListener{
	SLIMController slimCTRL = null;
	JRadioButton LensOneButton = new JRadioButton("Lens One");
	JRadioButton LensTwoButton = new JRadioButton("Lens Two");
	JRadioButton LensThreeButton = new JRadioButton("Lens Three");
	JRadioButton LensFourButton = new JRadioButton("Lens Four");
	JRadioButton LensFiveButton = new JRadioButton("Lens Five");
	JRadioButton LensSixButton = new JRadioButton("Lens Six");
	
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
		add(buttonPanel, BorderLayout.WEST);
	}
	
	/**
	 * Creates the buttons to drive the motor in the selected position and adds the listeners.
	 */
	private JPanel createRadioButton() {
		JPanel buttonPanel = new JPanel();
		ButtonGroup buttonGroup = new ButtonGroup();
		
		buttonGroup.add(LensOneButton); 
		buttonGroup.add(LensTwoButton); 
		buttonGroup.add(LensThreeButton); 
		buttonGroup.add(LensFourButton); 
		buttonGroup.add(LensFiveButton); 
		buttonGroup.add(LensSixButton); 
		
		buttonPanel.add(LensOneButton);
		buttonPanel.add(LensTwoButton);
		buttonPanel.add(LensThreeButton);
		buttonPanel.add(LensFourButton);
		buttonPanel.add(LensFiveButton);
		buttonPanel.add(LensSixButton);
		
		LensOneButton.addActionListener(this);
		LensTwoButton.addActionListener(this);
		LensThreeButton.addActionListener(this);
		LensFourButton.addActionListener(this);
		LensFiveButton.addActionListener(this);
		LensSixButton.addActionListener(this);
		
		return buttonPanel;
	}
	
	/**
	 * Programs the listeners to the buttons, allowing the control of the motors from the GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
//		if(e.getSource() == LensOneButton) {
//		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_ONE_POS);
//		}
//		if(e.getSource() == LensTwoButton) {
//		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_TWO_POS);
//		}
//		if(e.getSource() == LensThreeButton) {
//		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE_POS);
//		}
//		if(e.getSource() == LensFourButton) {
//		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_FOUR_POS);
//		}
//		if(e.getSource() == LensFiveButton) {
//		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_FIVE_POS);
//		}
//		if(e.getSource() == LensSixButton) {
//		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_SIX_POS);
//		}
	}
}

