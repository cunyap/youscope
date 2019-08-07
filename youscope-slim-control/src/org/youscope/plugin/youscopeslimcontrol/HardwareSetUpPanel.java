package org.youscope.plugin.youscopeslimcontrol;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;


/**
 * This class deals with the creation of the panel for the Hardware SetUp.
 * @author localadmin
 *
 */
public class HardwareSetUpPanel extends JPanel implements ActionListener{
	SLIMController slimCTRL = null;
	JRadioButton slimModeButton = new JRadioButton("SLIM mode");
	JRadioButton fluoModeButton = new JRadioButton("Fluorescence mode");
	JRadioButton calOneModeButton = new JRadioButton("Calibration One");
	JRadioButton calTwoModeButton = new JRadioButton("Calibration Two");
	
	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public HardwareSetUpPanel(SLIMController slimController) {
		slimCTRL = slimController;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS)); 
		setTitle();
		createRadioButton();
		add(Box.createGlue());
	}
	
	/**
	 * Set the title and the border of the panel
	 */
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Hardware Setup");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}
	
	/**
	 * Creates the buttons to drive the motors in the appropriate position for the selected mode.
	 */
	private void createRadioButton() {
		ButtonGroup buttonGroup = new ButtonGroup();
		
		buttonGroup.add(slimModeButton); 
		buttonGroup.add(fluoModeButton); 
		buttonGroup.add(calOneModeButton); 
		buttonGroup.add(calTwoModeButton); 

		add(slimModeButton);
		add(fluoModeButton);
		add(calOneModeButton);
		add(calTwoModeButton);
		
		slimModeButton.addActionListener(this);
		fluoModeButton.addActionListener(this);
		calOneModeButton.addActionListener(this);
		calTwoModeButton.addActionListener(this);
	}
	
	/**
	 * Programs the listeners to the buttons, allowing the control of the motors from the GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == slimModeButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY_POS);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER_POS);
			//slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE_POS);
			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
		}
		if(e.getSource() == fluoModeButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY_POS);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY_POS);
			//slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE_POS);
			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
		}
		if(e.getSource() == calOneModeButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY_POS);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER_POS);
			//slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE_POS);
			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
		}
		if(e.getSource() == calTwoModeButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY_POS);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER_POS);
			//slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE_POS);
			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
