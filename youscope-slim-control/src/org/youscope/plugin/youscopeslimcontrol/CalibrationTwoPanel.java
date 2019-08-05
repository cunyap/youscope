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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;

import com.sun.corba.se.impl.protocol.InfoOnlyServantCacheLocalCRDImpl;

import javafx.scene.layout.Border;

public class CalibrationTwoPanel extends JPanel implements ActionListener{
	
	SLIMController slimCTRL = null;
	JButton startCalibrationTwo;
	
	public CalibrationTwoPanel(SLIMController slimController) {
		
		slimCTRL = slimController;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setTitle();
		JPanel infoAndButtonPanel = createInfoAndButtonPanel();
		JPanel graphAndOutput = createGraphAndOutputPanel();
		add(infoAndButtonPanel);
		add(Box.createHorizontalGlue());
		add(graphAndOutput);
	}
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Calibration Two");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}
	
	private JPanel createInfoAndButtonPanel() {
		JPanel infoAndButtonPanel = new JPanel();
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.PAGE_AXIS));
		startCalibrationTwo = new JButton("Start Calibration Two");
		startCalibrationTwo.addActionListener(this);
		JTextArea descriptionText = new JTextArea("Calibrate the phase modulation \n"
				+ "values.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		infoAndButtonPanel.add(startCalibrationTwo);
		startCalibrationTwo.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(Box.createGlue());
		infoAndButtonPanel.add(descriptionText);
		descriptionText.setAlignmentX(LEFT_ALIGNMENT);
		
		return infoAndButtonPanel;
	}
	
	private JPanel createGraphAndOutputPanel() {
		JPanel graphAndOutput = new JPanel();
		graphAndOutput.setLayout(new BoxLayout(graphAndOutput, BoxLayout.PAGE_AXIS));
		JPanel placeHolder = new JPanel();
		placeHolder.setPreferredSize(new Dimension(100,100));
		placeHolder.setBackground(Color.darkGray);
		JTextArea outputNumbers = new JTextArea("Output number: ");	
		Font font = new Font("Courier New",Font.BOLD, 15);
		outputNumbers.setFont(font);
		outputNumbers.setForeground(Color.black);
		outputNumbers.append("NN, NN, NN, NN");
		outputNumbers.setOpaque(false);
		outputNumbers.setEditable(false);
		outputNumbers.setEnabled(false);
		graphAndOutput.add(placeHolder);
		placeHolder.setAlignmentX(RIGHT_ALIGNMENT);
		graphAndOutput.add(Box.createGlue());
		graphAndOutput.add(outputNumbers);
		outputNumbers.setAlignmentX(RIGHT_ALIGNMENT);
		
		return graphAndOutput;
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == startCalibrationTwo) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_TWO_POS);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.LENS_POS);
			//slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_TWO_POS);
			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
}
