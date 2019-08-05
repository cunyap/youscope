package org.youscope.plugin.youscopeslimcontrol;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;

import javafx.scene.layout.Border;


public class HardwareSetUpPanel extends JPanel implements ActionListener{
	SLIMController slimCTRL = null;
	JRadioButton slimModeButton = new JRadioButton("SLIM mode");
	JRadioButton fluoModeButton = new JRadioButton("Fluorescence mode");
	JRadioButton calOneModeButton = new JRadioButton("Calibration One");
	JRadioButton calTwoModeButton = new JRadioButton("Calibration Two");
	
	public HardwareSetUpPanel(SLIMController slimController) {
		slimCTRL = slimController;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS)); 
		setTitle();
		createRadioButton();
		add(Box.createGlue());

	}
	
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Hardware Setup");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}
	
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
