package org.youscope.plugin.youscopeslimcontrol;
import java.rmi.RemoteException;

import javax.swing.JOptionPane;
import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.Microscope;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.serverinterfaces.YouScopeServer;
/**
 * This class provides the tools to retrieve and return the Camera intensity value.
 * @author Elisa Garulli
 *
 */
public class CameraIntensityValue {
	YouScopeServer server;
	Microscope microscope;
	Device cameraLight;
	String intensity;
	
	/**
	 * Constructor, calls the camera and retrieves the property "Intensity"
	 * @param server
	 * @param deviceName
	 * @param property
	 */
	public CameraIntensityValue(YouScopeServer server, String deviceName) {
		this.server = server;
		getIntensity(deviceName, "Intensity");
	}
	
	/**
	 * Retrieves the property "Intensity"
	 * @param deviceName
	 * @param property
	 * @return the intensity value
	 */
	private String getIntensity(String deviceName, String property){
		try {
			Microscope microscope = server.getMicroscope();
			cameraLight = microscope.getDevice(deviceName);
			}
			catch(RemoteException | DeviceException e) { 
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "No Device connected", "Device Error",
												JOptionPane.ERROR_MESSAGE);
				return intensity = "--";
			}
			
			try {
				return intensity = Integer.toString((int)Math.round(Double.parseDouble(cameraLight.getProperty(property).getValue())));
			}
			catch(RemoteException | MicroscopeException | InterruptedException | DeviceException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Could not find Intensity property", "Property Error",
												JOptionPane.ERROR_MESSAGE);
				return intensity = "--";
			}
	}
	
	/**
	 * Returns a string with the value of intensity
	 * @return a string with the value of intensity
	 */
	public String returnIntensity() {
		return intensity;
	}
	
}
