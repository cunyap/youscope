package org.youscope.plugin.youscopeslimcontrol.HardwareControl;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;


/**
 * This class represents the panel for the Manual Control.
 * @author Elisa Garulli
 *
 */
public class ManualControlPanel extends JPanel {
	
	Font fontTitle = new Font(null, 1, 12);

	/**
	 * Constructor.
	 * Adds all the elements created by the classes Wheel-N-Panel to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public ManualControlPanel(SLIMController slimController) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS)); 
		setBorder(BorderFactory.createTitledBorder(null, "Calibration Two", 0, 0, fontTitle));
		add(new WheelOnePanel(slimController));
		add(new WheelTwoPanel(slimController));
		add(new WheelThreePanel(slimController));
	}
	
}
