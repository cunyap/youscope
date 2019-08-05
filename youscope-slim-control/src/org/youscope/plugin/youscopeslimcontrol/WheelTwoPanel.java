package org.youscope.plugin.youscopeslimcontrol;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

public class WheelTwoPanel extends JPanel implements ActionListener{
	
	SLIMController slimCTRL = null;
	JRadioButton LensButton = new JRadioButton("Lens");
	JRadioButton EmptyButton = new JRadioButton("Empty");
	JRadioButton PolarizerButton = new JRadioButton("Polarizer");
	
	public WheelTwoPanel(SLIMController slimController) {
		slimCTRL = slimController;	
		setLayout(new BorderLayout());
		JLabel wheelName = new JLabel("Second wheel");
		add(wheelName, BorderLayout.NORTH);
		JPanel buttonPanel = createRadioButton();
		add(buttonPanel, BorderLayout.WEST);
	}
	
	private JPanel createRadioButton() {
		JPanel buttonPanel = new JPanel();
		ButtonGroup buttonGroup = new ButtonGroup();
		
		buttonGroup.add(LensButton); 
		buttonGroup.add(EmptyButton); 
		buttonGroup.add(PolarizerButton); 
		
		buttonPanel.add(LensButton);
		buttonPanel.add(EmptyButton);
		buttonPanel.add(PolarizerButton);
		
		LensButton.addActionListener(this);
		EmptyButton.addActionListener(this);
		PolarizerButton.addActionListener(this);
		
		return buttonPanel;

	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == PolarizerButton) {
		slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER_POS);
		}
		if(e.getSource() == EmptyButton) {
		slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY_POS);
		}
		if(e.getSource() == LensButton) {
		slimCTRL.setSecondWheelPosition(SecondWheelPosition.LENS_POS);
		}
	}
	
}

