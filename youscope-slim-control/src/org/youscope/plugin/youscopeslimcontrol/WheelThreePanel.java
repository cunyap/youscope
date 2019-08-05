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


public class WheelThreePanel extends JPanel implements ActionListener{
	
	SLIMController slimCTRL = null;
	JRadioButton LensOneButton = new JRadioButton("Lens One");
	JRadioButton LensTwoButton = new JRadioButton("Lens Two");
	JRadioButton LensThreeButton = new JRadioButton("Lens Three");
	JRadioButton LensFourButton = new JRadioButton("Lens Four");
	JRadioButton LensFiveButton = new JRadioButton("Lens Five");
	JRadioButton LensSixButton = new JRadioButton("Lens Six");
	
	public WheelThreePanel(SLIMController slimController) {
		slimCTRL = slimController;	
		setLayout(new BorderLayout());
		JLabel wheelName = new JLabel("Lens wheel");
		add(wheelName, BorderLayout.NORTH);
		JPanel buttonPanel = createRadioButton();
		add(buttonPanel, BorderLayout.WEST);
	}
	
	private JPanel createRadioButton() {
		JPanel buttonPanel = new JPanel();
		ButtonGroup buttonGroup = new ButtonGroup();
		
		buttonGroup.add(LensOneButton); 
		buttonGroup.add(LensTwoButton); 
		buttonGroup.add(LensThreeButton); 
		buttonGroup.add(LensFourButton); 
		buttonGroup.add(LensFiveButton); 
		buttonGroup.add(LensSixButton); 
		
		buttonPanel.add(LensOneButton);
		buttonPanel.add(LensTwoButton);
		buttonPanel.add(LensThreeButton);
		buttonPanel.add(LensFourButton);
		buttonPanel.add(LensFiveButton);
		buttonPanel.add(LensSixButton);
		
		LensOneButton.addActionListener(this);
		LensTwoButton.addActionListener(this);
		LensThreeButton.addActionListener(this);
		LensFourButton.addActionListener(this);
		LensFiveButton.addActionListener(this);
		LensSixButton.addActionListener(this);
		
		return buttonPanel;
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == LensOneButton) {
		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_ONE_POS);
		}
		if(e.getSource() == LensTwoButton) {
		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_TWO_POS);
		}
		if(e.getSource() == LensThreeButton) {
		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE_POS);
		}
		if(e.getSource() == LensFourButton) {
		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_FOUR_POS);
		}
		if(e.getSource() == LensFiveButton) {
		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_FIVE_POS);
		}
		if(e.getSource() == LensSixButton) {
		slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_SIX_POS);
		}
	}
	
}

