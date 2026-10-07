package org.youscope.plugin.youscopeslimcontrol.HardwareControl;

import org.youscope.clientinterfaces.YouScopeClient;

/**
 * This class represents the communication from the GUI to the Arduino device, through the ArduinoController.
 * @author Elisa Garulli
 *
 */
public class SLIMController {
	
	ArduinoController arduino;
	final int PolarizerMotorPin = 52;
	final int SecondMotorPin = 53;
	final int LensMotorPin = 13;
	/**
	 * Constructor
	 * @param arduinoName the Arduino controller to communicate with.
	 */
	public SLIMController(ArduinoController arduino, YouScopeClient client) {
		this.arduino = arduino;
		if (arduino == null) {
			client.sendError("Arduino is not loaded");
		}
	}
	
	/**
	 * Sets the polarizer wheel to the requested position
	 * @param wheelPosition enum element that indicates the position. 
	 */
	public void setPolarizerWheelPosition(PolarizerWheelPosition wheelPosition) {
		if (arduino != null) {
		switch (wheelPosition) {
		case POLARIZER_ONE:	
				arduino.setPinValue(PolarizerMotorPin, 1);
			break;
			
		case EMPTY:
				arduino.setPinValue(PolarizerMotorPin, 0);
			break;
		
		case POLARIZER_TWO:
				arduino.setPinValue(PolarizerMotorPin, 2);
			break;
		}
	}
	}

	/**
	 * Sets the second wheel to the requested position
	 * @param wheelPosition enum element that indicates the position. 
	 */
	public void setSecondWheelPosition(SecondWheelPosition wheelPosition) {
		if (arduino != null) {
		switch (wheelPosition) {
		
		case POLARIZER:	
				arduino.setPinValue(SecondMotorPin, 1);
			break;
			
		case EMPTY:
				arduino.setPinValue(SecondMotorPin, 0);
			break;
		
		case LENS:
				arduino.setPinValue(SecondMotorPin, 2);
			break;
		}	
	}
	}

	/**
	 * Sets the lenses wheel to the requested position
	 * @param wheelPosition enum element that indicates the position. 
	 */
	public void setLensesWheelPosition(LensesWheelPosition wheelPosition) {
		if (arduino != null) {
		switch (wheelPosition) {
		case LENS_ONE:	
				arduino.setPinValue(LensMotorPin, 0);
			break;
			
		case LENS_TWO:
				arduino.setPinValue(LensMotorPin, 1);
			break;
		
		case LENS_THREE:
				arduino.setPinValue(LensMotorPin, 2);
			break;
		
		case LENS_FOUR:	
				arduino.setPinValue(LensMotorPin, 3);
			break;
			
		case LENS_FIVE:
				arduino.setPinValue(LensMotorPin, 4);
			break;
		
		case LENS_SIX:
				arduino.setPinValue(LensMotorPin, 5);
			break;
		}	
	}
	}
}
