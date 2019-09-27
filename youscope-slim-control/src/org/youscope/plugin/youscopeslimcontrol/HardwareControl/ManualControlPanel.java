package org.youscope.plugin.youscopeslimcontrol.HardwareControl;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;


/**
 * This class represents the panel for the Manual Control.
 * @author Elisa Garulli
 *
 */
public class ManualControlPanel extends JPanel {
	
	/**
	 * Constructor.
	 * Adds all the elements created by the classes Wheel-N-Panel to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public ManualControlPanel(SLIMController slimController) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS)); 
		setTitle();
		add(new WheelOnePanel(slimController));
		add(new WheelTwoPanel(slimController));
		add(new WheelThreePanel(slimController));
	}
	
	/**
	 * Set the title and the border of the panel
	 */
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Manual Control");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}
}
