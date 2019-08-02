package org.youscope.plugin.youscopeslimcontrol;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;
import javafx.scene.layout.Border;


public class AttenuationFactorInterface {
	public JPanel calibrationAttenuationFactorCreator(String title, String description, String ButtonName, String RadioButtonName, String ConfirmButton) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		TitledBorder titlePanel = new TitledBorder(title);
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		panel.setBorder(titlePanel);
		
		JPanel infoAndButtonPanel = new JPanel();
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.PAGE_AXIS));
		
		JPanel graphAndOutput = new JPanel();
		graphAndOutput.setLayout(new BoxLayout(graphAndOutput, BoxLayout.PAGE_AXIS));
		
		JTextField descriptionText = new JTextField(description);
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		JButton startProcess = new JButton(ButtonName);
		JRadioButton manualConfirmationButton = new JRadioButton(RadioButtonName);
		
		JPanel placeHolder = new JPanel();
		placeHolder.setPreferredSize(new Dimension(100,100));
		placeHolder.setBackground(Color.darkGray);
		JTextField outputNumbers = new JTextField("Attenuation Factor" + "NN");
		outputNumbers.setEditable(false);
		outputNumbers.setEnabled(false);
		JButton confirmButton = new JButton(ConfirmButton);
		
		infoAndButtonPanel.add(startProcess);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,5)));
		infoAndButtonPanel.add(descriptionText);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,5)));
		infoAndButtonPanel.add(manualConfirmationButton);
		
		graphAndOutput.add(placeHolder);
		graphAndOutput.add(Box.createRigidArea(new Dimension(0,5)));
		graphAndOutput.add(outputNumbers);
		graphAndOutput.add(Box.createRigidArea(new Dimension(0,5)));
		graphAndOutput.add(confirmButton);
		
		panel.add(infoAndButtonPanel, BorderLayout.WEST);
		panel.add(graphAndOutput, BorderLayout.CENTER);
		
		return panel;
	}
}
