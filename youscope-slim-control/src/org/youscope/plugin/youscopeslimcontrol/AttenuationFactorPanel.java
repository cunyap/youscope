package org.youscope.plugin.youscopeslimcontrol;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.plugin.slimidentification.AttenuationFactorWizard;
import org.youscope.plugin.slimjob.SlimProperties;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DoubleTextField;




/**
 * This class deals with the creation of the panel for the Attenuation Factor computing.
 * @author localadmin
 *
 */
public class AttenuationFactorPanel extends JPanel implements ActionListener{

	SLIMController slimCTRL = null;
	YouScopeServer server;
	YouScopeClient client;
	JButton wizzardButton;
	private final DoubleTextField attenuationFactorField = new DoubleTextField(); 
	
	/**
	 * Constructor.
	 * Adds all the elements created to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public AttenuationFactorPanel(SLIMController slimController, YouScopeClient client, YouScopeServer server) {
		
		this.server = server;
		this.client = client;
		slimCTRL = slimController;
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setTitle();
		JPanel infoAndButtonPanel = createInfoAndStartPanel();
		
		add(infoAndButtonPanel);
		infoAndButtonPanel.setAlignmentX(LEFT_ALIGNMENT);
		add(Box.createHorizontalGlue());
		
		wizzardButton = new JButton("Attenuation Factor Wizard");
		wizzardButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				AttenuationFactorWizard wizard = new AttenuationFactorWizard(client, server);
				wizard.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) 
					{
						actualizeValues();
					}
				});
				YouScopeFrame frame = wizard.toFrame();
				frame.setVisible(true);
			}
		});
		infoAndButtonPanel.add(wizzardButton);
		actualizeValues();
		
	}
	
	/**
	 * Set the title and the border of the panel
	 */
	private void setTitle() {
		TitledBorder titlePanel = new TitledBorder("Attenuation Factor");
		titlePanel.setTitleJustification(TitledBorder.LEFT);
		titlePanel.setTitlePosition(TitledBorder.TOP);
		setBorder(titlePanel);
	}
	
	/**
	 * Creates the panel that contains the description of the computation, the start button and the option to ask for
	 * manual confirmation.
	 * @return infoAndButtonPanel a panel with description and button.
	 */
	private JPanel createInfoAndStartPanel() {
		JPanel infoAndButtonPanel = new JPanel();
		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.PAGE_AXIS));
		JTextArea descriptionText = new JTextArea("Calculate the Attenuation Factor.\nMark the Manual Confirmation \n"
				+ "if you want to have a visual \nconfirmation of the computation.");
		descriptionText.setEditable(false);
		descriptionText.setEnabled(false);
		descriptionText.setOpaque(false);
		infoAndButtonPanel.add(Box.createGlue());
		infoAndButtonPanel.add(descriptionText);
		descriptionText.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(Box.createGlue());
		attenuationFactorField.setMaximumSize(new Dimension(100, 30));
		attenuationFactorField.setAlignmentX(LEFT_ALIGNMENT);
		infoAndButtonPanel.add(attenuationFactorField);
		
		return infoAndButtonPanel;
	}
	

	/**
	 * Programs the listeners to the buttons, allowing the starting of the process and the control of the motors from the 
	 * GUI.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == wizzardButton) {
//			slimCTRL.setPolarizerWheelPosition(PolarizerWheelPosition.EMPTY_POS);
//			slimCTRL.setSecondWheelPosition(SecondWheelPosition.POLARIZER_POS);
			//slimCTRL.setLensesWheelPosition(LensesWheelPosition.LENS_THREE_POS);
			JOptionPane.showMessageDialog(null, "Stepper controller not functional yet", "Stepper Error", JOptionPane.ERROR_MESSAGE);
			
			
		}
	}

	private void actualizeValues()
	{
		SlimProperties properties = new SlimProperties(client);
		attenuationFactorField.setValue(properties.getAttenuationFactor());
	}
}
