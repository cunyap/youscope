/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
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
 *     Moritz Lang - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.common.microscope;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;


/**
 * Listener which gets notified if a device of the microscope was removed or its configuration modified.
 * @author Moritz Lang
 *
 */
public interface MicroscopeConfigurationListener extends EventListener, Remote
{
	
	/**
	 * Called when the microscope is uninitialized. All stored values about the microscope configuration should be unloaded.
	 * @throws RemoteException 
	 */
	public void microscopeUninitialized() throws RemoteException;
	/**
	 * Called when a previously defined device was removed.
	 * @param deviceID The ID of the removed device.
	 * @throws RemoteException 
	 */
	public void deviceRemoved(String deviceID) throws RemoteException;
	/**
	 * Called when a device was newly added.
	 * @param deviceID The ID of the added device.
	 * @throws RemoteException 
	 */
	public void deviceAdded(String deviceID) throws RemoteException;
	/**
	 * Called when a label of a state device changed its name.
	 * @param oldLabel Setting containing the old label.
	 * @param newLabel Setting containing the new label.
	 * @throws RemoteException 
	 */
	public void labelChanged(DeviceSetting oldLabel, DeviceSetting newLabel) throws RemoteException;
}
