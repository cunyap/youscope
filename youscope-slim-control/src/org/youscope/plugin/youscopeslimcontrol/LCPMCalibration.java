package org.youscope.plugin.youscopeslimcontrol;
//import java.awt.image.BufferedImage;
//import java.awt.image.Raster;
//import java.io.File;
//import java.io.IOException;
//import javax.imageio.ImageIO;
//import javax.swing.ImageIcon;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//
//import org.youscope.clientinterfaces.YouScopeClient;
//import org.youscope.common.configuration.ChannelConfiguration;
//import org.youscope.common.image.ImageEvent;
//import org.youscope.common.measurement.Measurement;
//import org.youscope.common.microscope.CameraDevice;
//import org.youscope.common.util.ImageConvertException;
//import org.youscope.serverinterfaces.YouScopeServer;
//import org.youscope.uielements.HistogramPanel;
//import org.youscope.uielements.LiveStreamPanel.ChannelControl;
//
//import javax.swing.JLabel;
//
public class LCPMCalibration {
//	ChannelConfiguration config;
//	CameraDevice cam;
//	private YouScopeServer server;
//	private YouScopeClient client;
//	private ChannelControl channelControl;
//	private final Object measurementControlLock = new Object();
//	private volatile Measurement measurement = null;
//	private final Object imageLock = new Object();
//    private BufferedImage orgBufferedImage = null;
//    private ImageEvent<?> orgImageEvent = null;
//    protected final HistogramPanel histogramPanel = new HistogramPanel();
//    private BufferedImage renderedImage = null;
//    ImageEvent<?> image;
//    private JLabel imm;
//	
//	public void snapImage()
//	{
//		ImageEvent<?> image;
//		ImageIcon img = null;
//
//		synchronized(measurementControlLock)
//		{
//			if(measurement != null)
//				return;
//			String camera = channelControl.getCamera();
//			String channelGroup = channelControl.getChannelGroup();
//			String channel = channelControl.getChannel();
//			double exposure = channelControl.getExposure();
//			try {
//				image =server.getMicroscope().getCameraDevice(camera).makeImage(channelGroup, channel, exposure);
//			} catch (Exception e) {
//				client.sendError("Could not snap image.", e);
//				image = null;
//			}
//		}
//		if(image != null)
//			setImage(image);
//		img = (ImageIcon) image.getImageData();
//		imm = new JLabel(img);
//		//		return label;
//	}
//
//
//	public synchronized void setImage(ImageEvent<?> imageEvent)
//	{
//		ImageConvertException exception = null;
//		synchronized(imageLock)
//		{
//			this.orgBufferedImage = null;
//			this.orgImageEvent = imageEvent;
//			try
//			{
//				histogramPanel.setImage(imageEvent);
//			}
//			catch(ImageConvertException e)
//			{
//				exception = e;
//			}
//			fireImageChanged();
//		}
//		if(exception != null)
//			client.sendError("Could not calculate histogram of image.", exception);
//	}
//	void fireImageChanged()
//	{
//		synchronized(imageLock)
//		{
//			renderedImage = null;
//		}
//		repaint();
//	}
//
//}
//
}
//
//
//
