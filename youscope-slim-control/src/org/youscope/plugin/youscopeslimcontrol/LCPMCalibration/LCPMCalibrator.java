package org.youscope.plugin.youscopeslimcontrol.LCPMCalibration;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.stream.IntStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

import org.youscope.common.configuration.ChannelConfiguration;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.SettingException;
import org.youscope.common.util.ImageConvertException;
import org.youscope.common.util.ImageTools;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.serverinterfaces.YouScopeServer;

import com.sun.glass.events.WindowEvent;

import org.youscope.plugin.youscopeslimcontrol.BasePopUpPanel;
import org.youscope.plugin.youscopeslimcontrol.LoadingBar;
import org.youscope.plugin.youscopeslimcontrol.Utility;
import org.youscope.plugin.youscopeslimcontrol.External.FourierTransform;
import org.youscope.plugin.youscopeslimcontrol.External.HilbertTransform;

import java.lang.Math;

/**
 * This class represents the tools needed to prepare the gray values and unwrapped intensity values. 
 * The process sets the LCPM reflector to a range (0-255) of values and takes an image of it. 
 * Then the average intensity is calculated from the central region of the image. 
 * Finally the values transformed using a Hilbert transform and stored.
 * @author Elisa Garulli
 *
 */
public class LCPMCalibrator extends JPanel {

	String channelName;
	String channelGroup;
	ImageEvent<?> image;
	Double exposure;
	YouScopeClient client;
	YouScopeServer server;
	CameraDevice camera;
	Device LCPM;

	/**
	 * Constructor. 
	 * @param client
	 * @param server
	 * @param camera the camera to use to snap images of the reflector
	 * @param channelName filter channel Name
	 * @param channelGroup filter channel group
	 * @param LCPM the used reflector
	 */
	public LCPMCalibrator(YouScopeClient client, YouScopeServer server, CameraDevice camera, String exposure,
			String channelName, String channelGroup, Device LCPM) {
		this.channelName = channelName;
		this.channelGroup = channelGroup;
		this.client = client;
		this.server = server;
		this.camera = camera;
		this.LCPM = LCPM;
		this.exposure = Double.valueOf(exposure);
	}
	
	/**
	 * Calls the needed functions to compute the average intensity of each image and unwrap the values.
	 * The computation refers to doi: 10.1364/OE.19.001016 
	 * @return the unwrapped values.
	 */
	public double[] runCalibration(){
		
		LoadingBar loadingBar = new LoadingBar(client,"Calibration Progress");
		YouScopeFrame frame = loadingBar.toFrame();
		frame.setVisible(true);
		Color color = new Color(255,0,0);
		// 1. Extract the average intensity from a selection of each image.
		double[] averageIntensityArray = new double[256];
		double[] normalizedIntensityArray = new double[256];
		for (int grayValue = 0; grayValue < 256; grayValue++) {
			setLCPMImage(grayValue, LCPM);
			BufferedImage rawImage = getImage(client, server, camera);
			BufferedImage croppedImage = cropImage(rawImage);
			double averageIntensity = getAverageIntensity(croppedImage);
			averageIntensityArray[grayValue] = averageIntensity;
			if (grayValue < 125) {
				int colorChange = 4*grayValue*100/255;
				color = new Color(0,255-colorChange,colorChange);
			}
			else {
			int colorChange = 2*grayValue*100/255;
			color = new Color(colorChange,0,255-colorChange);
			}
			loadingBar.setForegroundBar(color);
			loadingBar.setValue(grayValue*100/250);
		}
		loadingBar.close();
		// 2. Normalize the values on the max and min values.
		double minIntensity = Utility.findMin(averageIntensityArray);
		double maxIntensity = Utility.findMax(averageIntensityArray);
		for (int index = 0; index < 256; index++) {
			double intensity = averageIntensityArray[index];
			normalizedIntensityArray[index] = (intensity - minIntensity)/(maxIntensity-minIntensity);
		}
		for (int index = 0; index < 256; index++) {
			normalizedIntensityArray[index] = 2*(normalizedIntensityArray[index]-0.5);
		}
		// 3. Perform Hilbert transform
		double[] realIntensityValues = normalizedIntensityArray.clone();
		HilbertTransform.FHT((normalizedIntensityArray), FourierTransform.Direction.Forward);
		// 4. Perform phase unwrapping
		double[] angles = new double[256];
		for (int y = 0; y < 256; y++) {
			angles[y] = Math.atan2(normalizedIntensityArray[y],realIntensityValues[y]); 
		}
		double[] unwrapped = PhaseUnwrapping.unwrap(angles);

		return unwrapped;
	}

	/**
	 * Retrieves the loaded camera image and tries to snap an image. I it can not it returns an error.
	 * @param client
	 * @param server
	 * @param camera
	 * @return the image.
	 */
	private BufferedImage getImage(YouScopeClient client, YouScopeServer server, CameraDevice camera) {
		try {
			image = server.getMicroscope().getCameraDevice("ImageCamera").makeImage(channelGroup, channelName, exposure );
		} 
		catch (MicroscopeException | DeviceException | InterruptedException | RemoteException | SettingException |
				MicroscopeLockedException e) {
			client.sendError("Could not snap image.", e);
			image = null;
		}
		BufferedImage savedImage = null;
		try {
			savedImage = ImageTools.getMicroscopeImage(image);
		} 
		catch (ImageConvertException e1) {
			client.sendError("Could not parse image.", e1);
		}
		return savedImage;
	}
	
	/** 
	 * Saves a cropped region of an image.
	 * @param rawImage
	 * @return the cropped Image.
	 */
	private BufferedImage cropImage(BufferedImage rawImage) {
		BufferedImage croppedImage = null;
		int upperLeftX = rawImage.getWidth()/4;
		int upperLeftY = rawImage.getHeight()/4;
		int cropWidth = rawImage.getWidth()*3/4;
		int cropHeight = rawImage.getHeight()*3/4;
		croppedImage = rawImage.getSubimage(upperLeftX, upperLeftY, cropWidth, cropHeight);
		return croppedImage;
	}

	/** 
	 * Computes the average intensity of a given gray-scale image.
	 * @param processedImage
	 * @return the average intensity of the input image
	 */
	private double getAverageIntensity(BufferedImage processedImage) {		
		int sumIntensity = 0;
		Raster raster = processedImage.getData();
		for (int i = 0; i < processedImage.getWidth(); i++) {
			for (int j = 0; j < processedImage.getHeight(); j++) {
				sumIntensity += raster.getSample(i, j, 0);
			}
		}
		//double averageIntensity = sumIntensity / (double)(processedImage.getWidth() * processedImage.getHeight());
		double averageIntensity = sumIntensity / (double)(processedImage.getWidth() * processedImage.getHeight());
		return averageIntensity;
	}

	/**
	 * Sets the LCPM reflector to a given gray value (0-255)
	 * @param greyShade
	 * @param LCPM
	 */
	private void setLCPMImage(int greyShade, Device LCPM) {
		String pixelValue = Integer.toString(greyShade);
		try {

			LCPM.getProperty("phaseShiftBackground").setValue(pixelValue);
		} 
		catch (RemoteException | MicroscopeException | MicroscopeLockedException | InterruptedException
				| DeviceException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not set the Device to " + pixelValue, "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	

}
