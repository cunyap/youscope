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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;
import javafx.scene.layout.Border;

public class CalibrationOnePanel extends JPanel implements ActionListener{
	
	SLIMController slimCTRL = null;
	JButton startCalibrationOne;
	
	public CalibrationOnePanel(SLIMController slimController) {
	slimCTRL = slimController;
	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
	setTitle();
	JPanel textPanel = createInfoBox();
	startCalibrationOne = new JButton("Start Calibration One");
	add(startCalibrationOne);
	startCalibrationOne.setAlignmentX(LEFT_ALIGNMENT);
	startCalibrationOne.addActionListener(this);
	add(Box.createHorizontalGlue());
	add(textPanel);
	textPanel.setAlignmentX(RIGHT_ALIGNMENT);
	}
	
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Calibration One");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}
	private JPanel createInfoBox() {
		JPanel textPanel = new JPanel();
		JTextArea descriptionText = new JTextArea("Match the ring displayed on the LCPM \n"
				+ " with the phase plate ring.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		textPanel.add(descriptionText);
		return textPanel;	
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == startCalibrationOne) {
			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.POLARIZER_ONE_POS);
			slimCTRL.setSecondWheelPosition(SecondWheelPosition.EMPTY_POS);
			slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_ONE_POS);
		}
	}
	
}
