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
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;

import javafx.scene.layout.Border;


public class HardwareSetUpPanel extends JPanel{

	public HardwareSetUpPanel(SLIMController slimController) {
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
		//JPanel buttonPanel = new JPanel();
		ButtonGroup buttonGroup = new ButtonGroup();
		//buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
		
		JRadioButton LensOneButton = new JRadioButton("SLIM mode");
		JRadioButton LensTwoButton = new JRadioButton("Fluorescence mode");
		JRadioButton LensThreeButton = new JRadioButton("Calibration One");
		JRadioButton LensFourButton = new JRadioButton("Calibration Two");

		buttonGroup.add(LensOneButton); 
		buttonGroup.add(LensTwoButton); 
		buttonGroup.add(LensThreeButton); 
		buttonGroup.add(LensFourButton); 

		add(LensOneButton);
		add(LensTwoButton);
		add(LensThreeButton);
		add(LensFourButton);


	}
}
