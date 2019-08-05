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


public class ArduinoController {
	Device arduino;
	public ArduinoController(Device arduinoDevice){
		arduino = arduinoDevice;
	}
	public void setPinValue(int pinNumber, int pinValue) throws RemoteException, DeviceException, MicroscopeException, MicroscopeLockedException, InterruptedException{
		  try { arduino.getProperty("Pin"+ pinNumber).setValue(Integer.toString(pinValue)); } catch
		  (MicroscopeException | MicroscopeLockedException | InterruptedException |
		  RemoteException | DeviceException e) { 
		  e.printStackTrace(); }
		 
	}
}
