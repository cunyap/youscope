package org.youscope.plugin.youscopeslimcontrol;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;


/**
 * This class deals with the creation of the panel for the Attenuation Factor computing.
 * @author localadmin
 *
 */
public class AttenuationFactorPanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	JButton startProcess;
	JRadioButton manualConfirmationButton;
	JButton confirmButton;
	
	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public AttenuationFactorPanel(SLIMController slimController) {
		slimCTRL = slimController;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setTitle();
		JPanel infoAndButtonPanel = createInfoAndStartPanel();
		
		add(infoAndButtonPanel);
		infoAndButtonPanel.setAlignmentX(LEFT_ALIGNMENT);
		add(Box.createHorizontalGlue());
		JPanel graphAndOutputPanel = createGraphAndOutputPanel();
		add(graphAndOutputPanel);
		graphAndOutputPanel.setAlignmentX(RIGHT_ALIGNMENT);
		
	}
	
	/**
	 * Set the title and the border of the panel
	 */
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Attenuation Factor");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}
	
	/**
	 * Creates the panel that contains the description of the computation, the start button and the option to ask for
	 * manual confirmation.
	 * @return infoAndButtonPanel a panel with description and button.
	 */
	private JPanel createInfoAndStartPanel() {
		JPanel infoAndButtonPanel = new JPanel();
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.PAGE_AXIS));
		JTextArea descriptionText = new JTextArea("Calculate the Attenuation Factor.\n Mark the Manual Confirmation \n"
				+ "if you want to have a visual \n confirmation of the computation.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		startProcess = new JButton("Calculate Attenuation Factor");
		startProcess.addActionListener(this);
		manualConfirmationButton = new JRadioButton("Manual Confirmation");
		infoAndButtonPanel.add(startProcess);
		startProcess.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(Box.createGlue());
		infoAndButtonPanel.add(descriptionText);
		descriptionText.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(Box.createGlue());
		infoAndButtonPanel.add(manualConfirmationButton);
		manualConfirmationButton.setAlignmentX(LEFT_ALIGNMENT);
		
		return infoAndButtonPanel;
	}
	
	/**
	 * Creates the panel that contains the graph for visual confirmation, the computed Attenuation Factor and the 
	 * confirm button.
	 * @return graphAndOutput a panel with the graph, the numerical parameter and the button
	 */
	private JPanel createGraphAndOutputPanel() {
		JPanel graphAndOutput = new JPanel();
		graphAndOutput.setLayout(new BoxLayout(graphAndOutput, BoxLayout.PAGE_AXIS));
		JPanel placeHolder = new JPanel();
		placeHolder.setBackground(Color.pink);
		JTextArea outputNumbers = new JTextArea("Attenuation Factor: ");
		Font font = new Font("Courier New",Font.BOLD, 15);
		outputNumbers.setFont(font);
		outputNumbers.setForeground(Color.black);
		outputNumbers.append("NN");
		outputNumbers.setOpaque(false);
		outputNumbers.setEditable(false);
		outputNumbers.setEnabled(false);
		confirmButton = new JButton("Confirm");
		
		graphAndOutput.add(placeHolder);
		graphAndOutput.add(Box.createGlue());
		placeHolder.setAlignmentX(RIGHT_ALIGNMENT);
		graphAndOutput.add(outputNumbers);
		outputNumbers.setAlignmentX(RIGHT_ALIGNMENT);
		graphAndOutput.add(Box.createGlue());
		graphAndOutput.add(confirmButton);
		confirmButton.setAlignmentX(RIGHT_ALIGNMENT);
		
		return graphAndOutput;
		
	}
	
	/**
	 * Programs the listeners to the buttons, allowing the starting of the process and the control of the motors from the 
	 * GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == startProcess) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY_POS);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER_POS);
			//slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE_POS);
			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
