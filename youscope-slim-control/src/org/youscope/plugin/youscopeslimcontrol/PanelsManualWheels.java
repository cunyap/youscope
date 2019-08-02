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

import javax.swing.JRadioButton;

public class PanelsManualWheels {

	public JPanel wheelsPanelCreator(String[] namesOfButtons, String label, ButtonGroup buttons, boolean VerticalButtons) {
		JPanel panel = new JPanel();
		JLabel wheelName = new JLabel(label);
		panel.setLayout(new BorderLayout());
		panel.add(wheelName, BorderLayout.NORTH);
		PanelsManualWheels createButtons = new PanelsManualWheels();
		if (VerticalButtons == false) {
		createButtons.radioButtonsHorizontalCreator(namesOfButtons, panel, buttons);
		}
		else {
			createButtons.radioButtonsVerticalCreator(namesOfButtons, panel, buttons);
		}
		return panel; 
	}
	
	public void radioButtonsHorizontalCreator(String[] namesOfButtons, JPanel panelToUse, ButtonGroup buttons) {
		JPanel buttonPanel = new JPanel();
		for (String name : namesOfButtons) {
			JRadioButton button = new JRadioButton(name);
			buttons.add(button); 
			buttonPanel.add(button);
			panelToUse.add(buttonPanel, BorderLayout.WEST);
		}

	}
	
	public void radioButtonsVerticalCreator(String[] namesOfButtons, JPanel panelToUse, ButtonGroup buttons) {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
		for (String name : namesOfButtons) {
			JRadioButton button = new JRadioButton(name);
			buttons.add(button); 
			buttonPanel.add(button);
			panelToUse.add(buttonPanel, BorderLayout.WEST);
		}

	}
}
