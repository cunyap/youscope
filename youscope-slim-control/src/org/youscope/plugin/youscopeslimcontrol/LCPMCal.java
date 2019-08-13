package org.youscope.plugin.youscopeslimcontrol;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.youscope.common.configuration.ChannelConfiguration;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.SettingException;
import org.youscope.common.util.ImageConvertException;
import org.youscope.common.util.ImageTools;
import org.youscope.uielements.LiveStreamPanel;
import org.youscope.uielements.HistogramPanel;
import org.youscope.uielements.ImagePanel;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;

public class LCPMCal extends JPanel {


	ChannelConfiguration configuration = new ChannelConfiguration("ChannelsTry", "yabba");
	String channelName = configuration.getChannel();
	String channelGroup = configuration.getChannelGroup();
	CameraDevice cam;
	ImageEvent<?> image;
	
	JLabel imm = new JLabel("plp");
	ImageIcon img = null;
	
	public LCPMCal(YouScopeClient client, YouScopeServer server) {
		
    	try {
    		image = server.getMicroscope().getCameraDevice("ImageCamera").makeImage(channelGroup, channelName, 50);
		} catch (Exception e) {
			client.sendError("Could not snap image.", e);
			JOptionPane.showMessageDialog(null, "Listen, I honestly don't know what the fuck is going on. "
			+ "I have no clue what this error is, I'm just a poor master student surviving on coffee "
			+ "and existential crisis", "Life Error",
									JOptionPane.ERROR_MESSAGE);
			image = null;
		}
    	
		// get buffered image to save
		BufferedImage saveImage = null;

			try {
				saveImage = ImageTools.getMicroscopeImage(image);
			} catch (ImageConvertException e1) {
				client.sendError("Could not parse image.", e1);
				return;
			}
		
		img = new ImageIcon(saveImage);
		imm = new JLabel(img);
		add(imm);
		//add(getImage());
	}
	
//	private JLabel getImage() {
//		JLabel imm = new JLabel("plp");
//		ImageEvent<?> image = null;
//		ImageIcon img = null;
//		
//		try {
//		image = cam.makeImage(channelGroup, channelName, 50);
//		}
//		catch (RemoteException | MicroscopeException | MicroscopeLockedException 
//				| SettingException | InterruptedException e) { 
//			e.printStackTrace();
//			JOptionPane.showMessageDialog(null, "Listen, I honestly don't know what the fuck is going on. "
//					+ "I have no clue what this error is, I'm just a poor master student surviving on coffee "
//					+ "and existential crisis", "Life Error",
//											JOptionPane.ERROR_MESSAGE);
//		}
//		
//		img = (ImageIcon) image.getImageData();
//		imm = new JLabel(img);
//		return imm;
//	}
}
