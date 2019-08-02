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
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;
import javafx.scene.layout.Border;

public class CalibrationTwoInterface {
	public JPanel calibrationTwoInterfaceCreator(String title, String description, String ButtonName) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JPanel infoAndButtonPanel = new JPanel();
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.PAGE_AXIS));
		JPanel graphAndOutput = new JPanel();
		graphAndOutput.setLayout(new BoxLayout(graphAndOutput, BoxLayout.PAGE_AXIS));
		JPanel placeHolder = new JPanel();
		placeHolder.setPreferredSize(new Dimension(100,100));
		placeHolder.setBackground(Color.darkGray);
		
		JTextField descriptionText = new JTextField(description);
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		JTextField outputNumbers = new JTextField("Output number" + "NN, NN, NN, NN");
		outputNumbers.setEditable(false);
		outputNumbers.setEnabled(false);
		
		
		TitledBorder titlePanel = new TitledBorder(title);
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		panel.setBorder(titlePanel);
		
		JButton startCalibration = new JButton(ButtonName);
		
		infoAndButtonPanel.add(startCalibration);
		infoAndButtonPanel.add(Box.createRigidArea(new Dimension(0,5)));
		infoAndButtonPanel.add(descriptionText);
		
		graphAndOutput.add(placeHolder);
		graphAndOutput.add(Box.createRigidArea(new Dimension(0,5)));
		graphAndOutput.add(outputNumbers);
		
		panel.add(infoAndButtonPanel, BorderLayout.WEST);
		panel.add(graphAndOutput, BorderLayout.CENTER);
		
		return panel;
	}
}
