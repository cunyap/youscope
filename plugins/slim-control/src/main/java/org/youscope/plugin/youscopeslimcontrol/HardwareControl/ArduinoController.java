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
package org.youscope.plugin.youscopeslimcontrol.HardwareControl;
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
