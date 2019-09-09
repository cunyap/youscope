package org.youscope.plugin.youscopeslimcontrol;
import java.rmi.RemoteException;

import javax.swing.JOptionPane;
import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.Microscope;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.serverinterfaces.YouScopeServer;

public class CameraIntensityValue {
	YouScopeServer server;
	Microscope microscope;
	Device cameraLight;
	String intensity;
	public CameraIntensityValue(YouScopeServer server, String deviceName, String property) {
		this.server = server;
		try {
		Microscope microscope = server.getMicroscope();
		cameraLight = microscope.getDevice(deviceName);
		}
		catch(RemoteException | DeviceException e) { 
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "No Device connected", "Device Error",
											JOptionPane.ERROR_MESSAGE);
		}
		
		try {
			intensity = (String) cameraLight.getProperty(property).getValue();
		}
		catch(RemoteException | MicroscopeException | InterruptedException | DeviceException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not find Intensity property", "Property Error",
											JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public String returnIntensity() {
		return intensity;
	}
	
}
