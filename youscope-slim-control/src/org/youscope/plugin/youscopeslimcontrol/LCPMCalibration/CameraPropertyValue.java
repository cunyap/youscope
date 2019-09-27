package org.youscope.plugin.youscopeslimcontrol.LCPMCalibration;
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
public class CameraPropertyValue {
	static Microscope microscope;
	static Device cameraLight;
	static String propertyValue;


	/**
	 * Retrieves the given property
	 * @param deviceName
	 * @param property
	 * @return the property value
	 */
	public static String getProperty(YouScopeServer server, String deviceName, String property){
		try {
			Microscope microscope = server.getMicroscope();
			cameraLight = microscope.getDevice(deviceName);
		}
		catch(RemoteException | DeviceException e) { 
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "No Device connected", "Device Error",
					JOptionPane.ERROR_MESSAGE);
			propertyValue = "--";
			return propertyValue;
		}

		try {
			propertyValue = Integer.toString((int)Math.round(Double.parseDouble(cameraLight.getProperty(property).getValue())));
			return propertyValue;
		}
		catch(RemoteException | MicroscopeException | InterruptedException | DeviceException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not find property", "Property Error",
					JOptionPane.ERROR_MESSAGE);
			propertyValue = "--";
			return propertyValue;
		}
	}

}
