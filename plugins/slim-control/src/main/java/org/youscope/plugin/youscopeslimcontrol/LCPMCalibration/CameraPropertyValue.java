/*******************************************************************************
 * Copyright (c) 2019 - 2020 Elisa Garulli.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
 * License, Version 2.1 (or at your option any later version).
 *
 * You may choose which license you wish to use when copying, modifying,
 * or distributing this software.
 *
 * Licenses are available at:
 * - Apache 2.0: http://apache.org
 * - LGPL 2.1:   https://gnu.org
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
 *
 * Contributors:
 *     Elisa Garulli - initial API and implementation
 *     Andreas P. Cuny - migration to the current YouScope API
 ******************************************************************************/
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
