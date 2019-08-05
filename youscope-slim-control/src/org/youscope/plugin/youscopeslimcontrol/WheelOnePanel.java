package org.youscope.plugin.youscopeslimcontrol;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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

public class WheelOnePanel extends JPanel implements ActionListener{
	
	SLIMController slimCTRL = null;
	JRadioButton PolarizerOneButton = new JRadioButton("Polarizer One");
	JRadioButton EmptyButton = new JRadioButton("Empty");
	JRadioButton PolarizerTwoButton = new JRadioButton("Polarizer Two");
	
	public WheelOnePanel(SLIMController slimController) {
		slimCTRL = slimController;
		setLayout(new BorderLayout());
		JLabel wheelName = new JLabel("Polarizers wheel");
		add(wheelName, BorderLayout.NORTH);
		JPanel buttonPanel = createRadioButton();
		add(buttonPanel, BorderLayout.WEST);
	}
	
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
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == PolarizerOneButton) {
		slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE_POS);
		}
		if(e.getSource() == EmptyButton) {
		slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY_POS);
		}
		if(e.getSource() == PolarizerTwoButton) {
		slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO_POS);
		}
	}
}

