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

public class ManualControlPanel extends JPanel {
	
	public ManualControlPanel(SLIMController slimController) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS)); 
		setTitle();
		add(new WheelOnePanel(slimController));
		add(new WheelTwoPanel(slimController));
		add(new WheelThreePanel(slimController));
	}
	
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Manual Control");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}

}
