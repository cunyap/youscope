package org.youscope.plugin.youscopeslimcontrol;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.stream.IntStream;
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
 * This class represents the tools needed to prepare the gray values and unwrapepd intensity values. 
 * The process sets the LCPM reflector to a range (0-255) of values and takes an image of it. 
 * Then the average intensity is calculated from the central region of the image. 
 * Finally the values tranformed using a Hilbert transform and stored.
 * @author Elisa Garulli
 *
 */
public class LCPMCal extends JPanel {

	ChannelConfiguration configuration = new ChannelConfiguration("ChannelsTry", "yabba");
	String channelName;
	String channelGroup;
	ImageEvent<?> image;
	private double[] unwrapped;

	/**
	 * Constructor. Calls the needed functions to compute the average intensity of each image and unwrap the values.
	 * @param client
	 * @param server
	 * @param camera the camera to use to snap images of the reflector
	 * @param channelName filter channel Name
	 * @param channelGroup filter channel group
	 * @param LCPM the used reflector
	 */
	public LCPMCal(YouScopeClient client, YouScopeServer server, CameraDevice camera, String channelName, String channelGroup,
			Device LCPM) {
		this.channelName = channelName;
		this.channelGroup = channelGroup;
		double[] averageIntensityArray = new double[256];
		double[] normalizedIntensityArray = new double[256];
		BufferedImage croppedImage = null;
		for (int grayValue = 0; grayValue < 256; grayValue++) {
			setLCPMImage(grayValue, LCPM);
			BufferedImage rawImage = getImage(client, server, camera);
			croppedImage = cropImage(rawImage);
			double averageIntensity = getAverageIntensity(croppedImage);
			averageIntensityArray[grayValue] = averageIntensity;
		}
		double minIntensity = findMin(averageIntensityArray);
		double maxIntensity = findMax(averageIntensityArray);
		for (int index = 0; index < 256; index++) {
			double intensity = averageIntensityArray[index];
			normalizedIntensityArray[index] = (intensity - minIntensity)/(maxIntensity-minIntensity);
		}
		for (int index = 0; index < 256; index++) {
			normalizedIntensityArray[index] = 2*(normalizedIntensityArray[index]-0.5);
		}
		double[] realIntensityValues = normalizedIntensityArray.clone();
		HilbertTransform.FHT((normalizedIntensityArray), FourierTransform.Direction.Forward);

		double[] angles = new double[256];
		for (int y = 0; y < 256; y++) {
			angles[y] = Math.atan2(normalizedIntensityArray[y],realIntensityValues[y]); 
		}
		unwrapped = new PhaseUnwrapping().unwrap(angles);
		
		returnRadians();
	}

	/**
	 * Snaps an image.
	 * @param client
	 * @param server
	 * @param camera
	 * @return the image.
	 */
	private BufferedImage getImage(YouScopeClient client, YouScopeServer server, CameraDevice camera) {
		try {
			image = server.getMicroscope().getCameraDevice("ImageCamera").makeImage(channelGroup, channelName, 50);
		} 
		catch (Exception e) {
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

	/**
	 * Finds the element with minimum value in a double array.
	 * @param doubleArray
	 * @return the minimum value.
	 */
	private double findMin(double[] doubleArray) {
		double[] dummyArray = doubleArray.clone();
		Arrays.sort(dummyArray);
		return dummyArray[0];
	}

	/**
	 * Finds the element with maximum value in a double array.
	 * @param doubleArray
	 * @return the maximum value.
	 */
	private double findMax(double[] doubleArray) {
		double[] dummyArray = doubleArray.clone();
		Arrays.sort(dummyArray);
		return dummyArray[dummyArray.length - 1];
	}
	
	/**
	 * Returns the computed radians
	 */
	public double[] returnRadians(){
		return unwrapped;
	}

}
