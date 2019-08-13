package org.youscope.plugin.youscopeslimcontrol;

/**
 * This class represents the communication from the GUI to the Arduino device, through the ArduinoController.
 * @author Elisa Garulli
 *
 */
public class SLIMController {
	
	ArduinoController arduino;
	int PolarizerMotorPin = 53;
	int SecondMotorPin = 53;
	int LensMotorPin = 13;
	/**
	 * Constructor
	 * @param arduinoName the Arduino controller to communicate with.
	 */
	public SLIMController(ArduinoController arduinoName) {
		arduino = arduinoName;
	}
	
	/**
	 * Sets the polarizer wheel to the requested position
	 * @param wheelPosition enum element that indicates the position. 
	 */
	public void setPolarizerWheelPosition(PolarizerWheelPosition wheelPosition) {
		switch (wheelPosition) {
		case POLARIZER_ONE:	
				arduino.setPinValue(PolarizerMotorPin, 0);
			break;
			
		case EMPTY_POS:
				arduino.setPinValue(PolarizerMotorPin, 1);
			break;
		
		case POLARIZER_TWO:
				arduino.setPinValue(PolarizerMotorPin, 2);
			break;
		}	
	}

	/**
	 * Sets the second wheel to the requested position
	 * @param wheelPosition enum element that indicates the position. 
	 */
	public void setSecondWheelPosition(SecondWheelPosition wheelPosition) {
		switch (wheelPosition) {
		
		case POLARIZER_POS:	
				arduino.setPinValue(SecondMotorPin, 0);
			break;
			
		case EMPTY_POS:
				arduino.setPinValue(SecondMotorPin, 1);
			break;
		
		case LENS_POS:
				arduino.setPinValue(SecondMotorPin, 2);
			break;
		}	
	}

	/**
	 * Sets the lenses wheel to the requested position
	 * @param wheelPosition enum element that indicates the position. 
	 */
	public void setLensesWheelPosition(LensesWheelPosition wheelPosition) {
		switch (wheelPosition) {
		case LENS_ONE_POS:	
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
