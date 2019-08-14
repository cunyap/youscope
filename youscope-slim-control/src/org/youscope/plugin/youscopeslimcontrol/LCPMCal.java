package org.youscope.plugin.youscopeslimcontrol;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.youscope.common.configuration.ChannelConfiguration;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.util.ImageConvertException;
import org.youscope.common.util.ImageTools;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.plugin.youscopeslimcontrol.HilbertTransform;
import java.lang.Math;

/**
 * This class represents the tools needed to perform LCPM calibration.
 * @author Elisa Garulli
 *
 */
public class LCPMCal extends JPanel {

	ChannelConfiguration configuration = new ChannelConfiguration("ChannelsTry", "yabba");
	String channelName = configuration.getChannel();
	String channelGroup = configuration.getChannelGroup();
	ImageEvent<?> image;
//	FourierTransform.Direction forward;
	JLabel imm = new JLabel("plp");
	ImageIcon img = null;

	public LCPMCal(YouScopeClient client, YouScopeServer server, CameraDevice camera, Device LCPM) {

		double[] averageIntensityArray = new double[255];
		double[] normalizedIntensityArray = new double[255];
		BufferedImage croppedImage = null;
		for (int grayValue = 0; grayValue < 255; grayValue++) {
			setLCPMImage(grayValue, LCPM);
			BufferedImage rawImage = getImage(client, server, camera);
			croppedImage = cropImage(rawImage);
			double averageIntensity = getAverageIntensity(croppedImage);
			averageIntensityArray[grayValue] = averageIntensity;
		}
		double minIntensity = findMin(averageIntensityArray);
		double maxIntensity = findMax(averageIntensityArray);
		for (int index = 0; index < 255; index++) {
			double intensity = averageIntensityArray[index];
			normalizedIntensityArray[index] = (intensity - minIntensity)/(maxIntensity-minIntensity);
			
		}
		for (int index = 0; index < 256; index++) {
			normalizedIntensityArray[index] = 2*normalizedIntensityArray[index]-0.5;
		}
		HilbertTransform.FHT((normalizedIntensityArray), FourierTransform.Direction.Forward);
		double[] angles = new double[255];
		for (int y = 0; y < 256; y++) {
			angles[y] = Math.atan(normalizedIntensityArray[y]); 
		}
		//double[] angles = Math.atan2(normalizedIntesityArray);
		
		int[] range = IntStream.rangeClosed(0, 255).toArray();
		
		JOptionPane.showMessageDialog(null, angles, "Life Error",
				JOptionPane.ERROR_MESSAGE);

		img = new ImageIcon(croppedImage);
		
		imm = new JLabel(Double.toString(averageIntensityArray[23]));
		add(imm);

	}

	private BufferedImage getImage(YouScopeClient client, YouScopeServer server, CameraDevice camera) {
		try {
			//image = camera.makeImage(channelGroup, channelName, 50);
			image = server.getMicroscope().getCameraDevice("ImageCamera").makeImage(channelGroup, channelName, 50);
		} 
		catch (Exception e) {
			client.sendError("Could not snap image.", e);
			JOptionPane.showMessageDialog(null, "Listen, I honestly don't know what the fuck is going on. \n" 
					+ "I think there's a problem with snapping the image, "
					+ "but what do I know? I'm just a poor master student surviving on coffee "
					+ "and existential crisis'", "Life Error",
					JOptionPane.ERROR_MESSAGE);
			image = null;
		}
		BufferedImage savedImage = null;
		try {
			savedImage = ImageTools.getMicroscopeImage(image);
		} 
		catch (ImageConvertException e1) {
			client.sendError("Could not parse image.", e1);
		}
		if (savedImage == null) {
			JOptionPane.showMessageDialog(null, "Errr... something with getImage", "\\_('.')_/- Error",
					JOptionPane.ERROR_MESSAGE);
		
	}
		return savedImage;
	}

	private BufferedImage cropImage(BufferedImage rawImage) {
		BufferedImage croppedImage = null;
		int upperLeftX = rawImage.getWidth()/4;
		int upperLeftY = rawImage.getHeight()/4;
		int cropWidth = rawImage.getWidth()*3/4;
		int cropHeight = rawImage.getHeight()*3/4;
		croppedImage = rawImage.getSubimage(upperLeftX, upperLeftY, cropWidth, cropHeight);
		if (croppedImage == null) {
			JOptionPane.showMessageDialog(null, "Errr... something with cropImage(?)", "\\_('.')_/- Error",
					JOptionPane.ERROR_MESSAGE);
		}
		return croppedImage;
	}



	private int getAverageIntensity(BufferedImage processedImage) {		

		int averageIntensity = 0;

		int sumIntensity = 0;
		Raster raster = processedImage.getData();
		for (int i = 0; i < processedImage.getWidth(); i++) {
			for (int j = 0; j < processedImage.getHeight(); j++) {
				sumIntensity += raster.getSample(i, j, 0);
			}
		}
		averageIntensity = sumIntensity / (processedImage.getWidth() * processedImage.getHeight());
		if (sumIntensity == 0) {
			JOptionPane.showMessageDialog(null, "Errr... something with the AverageIntensity(?)", "\\_('.')_/- Error",
					JOptionPane.ERROR_MESSAGE);
		}

		return averageIntensity;
	}

	private void setLCPMImage(int greyShade, Device LCPM) {
		String pixelValue = Integer.toString(greyShade);
		try {

			LCPM.getProperty("phaseShiftBackground").setValue(pixelValue);

		} catch (RemoteException | MicroscopeException | MicroscopeLockedException | InterruptedException
				| DeviceException e) {

			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not set the Device to " + pixelValue, "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private double findMin(double[] doubleArray) {
		double[] dummyArray = doubleArray.clone();
		Arrays.sort(dummyArray);
		return dummyArray[0];
	}
	
	private double findMax(double[] doubleArray) {
		double[] dummyArray = doubleArray.clone();
		Arrays.sort(dummyArray);
		return dummyArray[dummyArray.length - 1];
	}


}
