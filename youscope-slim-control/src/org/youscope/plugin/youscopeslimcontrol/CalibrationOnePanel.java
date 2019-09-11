package org.youscope.plugin.youscopeslimcontrol;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

/**
 *  This class represents the panel for the Calibration One.
 * @author Elisa Garulli
 *
 */
public class CalibrationOnePanel extends JPanel implements ActionListener{
	SLIMController slimCTRL = null;
	JButton startCalibrationOneButton;

	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel and creates the start button.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public CalibrationOnePanel(SLIMController slimController) {
		slimCTRL = slimController;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setTitle();
		JPanel textPanel = createInfoBox();
		startCalibrationOneButton = new JButton("Start Calibration One");
		add(startCalibrationOneButton);
		startCalibrationOneButton.setAlignmentX(LEFT_ALIGNMENT);
		startCalibrationOneButton.addActionListener(this);
		add(Box.createHorizontalGlue());
		add(textPanel);
		textPanel.setAlignmentX(RIGHT_ALIGNMENT);
	}

	/**
	 * Set the title and the border of the panel
	 */
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Calibration One");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}
	
	/**
	 * Creates the panel that contains the description of the computation
	 * @return textPanel a panel containing the description.
	 */
	private JPanel createInfoBox() {
		JPanel textPanel = new JPanel();
		JTextArea descriptionText = new JTextArea("Match the ring displayed on the LCPM \n"
				+ " with the phase plate ring.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		textPanel.add(descriptionText);
		return textPanel;	
	}
	
	/**
	 * Programs the listeners to the buttons, allowing the starting of the process and the control of the motors from the 
	 * GUI.
	 */
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == startCalibrationOneButton) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY);
			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
