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
 * This class represents the tools needed to perform LCPM calibration.
 * @author Elisa Garulli
 *
 */
public class LCPMCal extends JPanel {

	ChannelConfiguration configuration = new ChannelConfiguration("ChannelsTry", "yabba");
	String channelName;
	String channelGroup;
	ImageEvent<?> image;
	JLabel imm = new JLabel("plp");
	ImageIcon img = null;
	public PhaseChart panel;

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
		double[] unwrapped = new PhaseUnwrapping().unwrap(angles);
		int[] range = IntStream.rangeClosed(0, 255).toArray();

		panel = new PhaseChart(range, unwrapped, client);
		add(panel);


	}

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

	private BufferedImage cropImage(BufferedImage rawImage) {
		BufferedImage croppedImage = null;
		int upperLeftX = rawImage.getWidth()/4;
		int upperLeftY = rawImage.getHeight()/4;
		int cropWidth = rawImage.getWidth()*3/4;
		int cropHeight = rawImage.getHeight()*3/4;
		croppedImage = rawImage.getSubimage(upperLeftX, upperLeftY, cropWidth, cropHeight);
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

		return averageIntensity;
	}

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
