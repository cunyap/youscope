package org.youscope.plugin.youscopeslimcontrol;
import java.rmi.RemoteException;
import org.youscope.addon.AddonException;
import org.youscope.addon.tool.ToolAddonUI;
import org.youscope.addon.tool.ToolAddonUIAdapter;
import org.youscope.addon.tool.ToolMetadata;
import org.youscope.addon.tool.ToolMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrameListener;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.common.microscope.Device;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.Microscope;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DynamicPanel;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;

public class SLIMController {
	
	ArduinoController arduino;
	
	public SLIMController(ArduinoController arduinoName) {
		arduino = arduinoName;
	}
	
	public void setPolarizerWheelPosition(PolarizerWheelPosition wheelPosition) {
		switch (wheelPosition) {
		case POLARIZER_ONE_POS:	
			
			try {
				arduino.setPinValue(53, 0);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
			
		case EMPTY_POS:
			
			try {
				arduino.setPinValue(53, 1);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
		
		case POLARIZER_TWO_POS:
			
			try {
				arduino.setPinValue(53, 2);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
		
		}	
	}

	public void setSecondWheelPosition(SecondWheelPosition wheelPosition) {
		switch (wheelPosition) {
		case POLARIZER_POS:	
			
			try {
				arduino.setPinValue(53, 0);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
			
		case EMPTY_POS:
			
			try {
				arduino.setPinValue(53, 1);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
		
		case LENS_POS:
			
			try {
				arduino.setPinValue(53, 2);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
		
		}	
	}

	public void setLensesWheelPosition(LensesWheelPosition wheelPosition) {
		switch (wheelPosition) {
		case LENS_ONE_POS:	
			
			try {
				arduino.setPinValue(13, 0);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
			
		case LENS_TWO_POS:
			
			try {
				arduino.setPinValue(13, 1);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
		
		case LENS_THREE_POS:
			
			try {
				arduino.setPinValue(13, 2);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
		
		case LENS_FOUR_POS:	
			
			try {
				arduino.setPinValue(13, 3);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
			
		case LENS_FIVE_POS:
			
			try {
				arduino.setPinValue(13, 4);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
		
		case LENS_SIX_POS:
			
			try {
				arduino.setPinValue(13, 5);
			} 
			
			catch (RemoteException | DeviceException | MicroscopeException | InterruptedException | MicroscopeLockedException e1) {
				e1.printStackTrace();
			}
			break;
		//hiii
		}	
	}
}
