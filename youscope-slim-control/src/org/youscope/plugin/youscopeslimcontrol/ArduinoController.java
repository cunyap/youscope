package org.youscope.plugin.youscopeslimcontrol;
import java.rmi.RemoteException;

import javax.swing.JOptionPane;

import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * This class represents the direct communication with the Arduino device.
 * @author Elisa Garulli
 *
 */
public class ArduinoController {
	Device arduino;

	/**
	 * Constructor
	 * @param arduinoDevice the loaded device to communicate with.
	 */
	public ArduinoController(Device arduinoDevice){
		arduino = arduinoDevice;
	}

	/**
	 * Sets the given pin to the given pinValue.
	 * @param pinNumber is the pin on the Arduino board that controls a device.
	 * @param pinValue is the state of the pin one desires to set it to.
	 */
	public void setPinValue(int pinNumber, int pinValue) {
		try { 
			arduino.getProperty("Pin"+ pinNumber).setValue(Integer.toString(pinValue)); 
		} 
		catch (MicroscopeException | MicroscopeLockedException | InterruptedException |
				RemoteException | DeviceException | NullPointerException e) {
			e.printStackTrace(); 

		}

	}
}
