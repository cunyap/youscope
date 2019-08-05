package org.youscope.plugin.youscopeslimcontrol;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;
import javafx.scene.layout.Border;

public class AttenuationFactorPanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	JButton startProcess;
	JRadioButton manualConfirmationButton;
	JButton confirmButton;
	
	
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
	
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Attenuation Factor");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}
	
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
	
	private JPanel createGraphAndOutputPanel() {
		JPanel graphAndOutput = new JPanel();
		graphAndOutput.setLayout(new BoxLayout(graphAndOutput, BoxLayout.PAGE_AXIS));
		
		JPanel placeHolder = new JPanel();
		//placeHolder.setPreferredSize(new Dimension(100,100));
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
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == startProcess) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY_POS);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER_POS);
			//slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE_POS);
			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
}
