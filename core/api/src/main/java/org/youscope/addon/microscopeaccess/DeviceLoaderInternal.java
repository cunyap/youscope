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
package org.youscope.addon.microscopeaccess;


import org.youscope.common.microscope.MicroscopeDriverException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * @author langmo
 *
 */
public interface DeviceLoaderInternal
{
	/**
	 * Returns a list of all available device drivers.
	 * @return List of all available device drivers.
	 * @throws MicroscopeDriverException 
	 */
	public AvailableDeviceDriverInternal[] getAvailableDeviceDrivers() throws MicroscopeDriverException;
	
	/**
	 * Returns the device driver with the given library and driver ID, or null, if driver could not be found.
	 * @param libraryID The ID of the library the driver belongs to.
	 * @param driverID The ID of the device driver.
	 * @return the device driver with the given IDs.
	 * @throws MicroscopeDriverException Thrown if error occurred while trying to load drivers.
	 */
	public AvailableDeviceDriverInternal getAvailableDeviceDriver(String libraryID, String driverID) throws MicroscopeDriverException;
	
	/**
	 * Removes a previously added device.
	 * @param accessID The access ID of the current microscope object. If the microscope is locked with a different accessID, a MicroscopeLockedException is thrown.
	 * @param name Name of the device.
	 * @throws MicroscopeDriverException
	 * @throws MicroscopeLockedException 
	 */
	public void removeDevice(String name, int accessID) throws MicroscopeDriverException, MicroscopeLockedException;	
}
