package org.youscope.plugin.youscopeslimcontrol;

/**
 * This class deals with the communication from the GUI to the Arduino device, through the ArduinoController.
 * @author localadmin
 *
 */
public class SLIMController {
	
	ArduinoController arduino;
	
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
		case POLARIZER_ONE_POS:	
				arduino.setPinValue(53, 0);
			break;
			
		case EMPTY_POS:
				arduino.setPinValue(53, 1);
			break;
		
		case POLARIZER_TWO_POS:
				arduino.setPinValue(53, 2);
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
				arduino.setPinValue(53, 0);
			break;
			
		case EMPTY_POS:
				arduino.setPinValue(53, 1);
			break;
		
		case LENS_POS:
				arduino.setPinValue(53, 2);
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
				arduino.setPinValue(13, 0);
			break;
			
		case LENS_TWO_POS:
				arduino.setPinValue(13, 1);
			break;
		
		case LENS_THREE_POS:
				arduino.setPinValue(13, 2);
			break;
		
		case LENS_FOUR_POS:	
				arduino.setPinValue(13, 3);
			break;
			
		case LENS_FIVE_POS:
				arduino.setPinValue(13, 4);
			break;
		
		case LENS_SIX_POS:
				arduino.setPinValue(13, 5);
			break;
		}	
	}
}
