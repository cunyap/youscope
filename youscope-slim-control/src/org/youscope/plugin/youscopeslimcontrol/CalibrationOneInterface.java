package org.youscope.plugin.youscopeslimcontrol;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;
import javafx.scene.layout.Border;

public class CalibrationOneInterface {
	public JPanel calibrationOneInterfaceCreator(String title, String description, String ButtonName) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		JTextField descriptionText = new JTextField(description);
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		
		
		TitledBorder titlePanel = new TitledBorder(title);
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		panel.setBorder(titlePanel);
		
		JButton startCalibration = new JButton(ButtonName);
		
		panel.add(startCalibration, BorderLayout.WEST);
		panel.add(descriptionText, BorderLayout.CENTER);
		
		return panel;
	}

}
