/*******************************************************************************
 * Copyright © 2026 ETH Zürich
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

import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * @author Moritz Lang
 *
 */
public interface ShutterDeviceInternal extends DeviceInternal
{
	/**
	 * Opens (open == true) or closes (open == false) the shutter.
	 * @param open True if the shutter should be opened, false if it should be closed.
	 * @param accessID The access ID of the current microscope object. If the microscope is locked with a different accessID, a MicroscopeLockedException is thrown.
	 * @throws MicroscopeException
	 * @throws MicroscopeLockedException
	 * @throws InterruptedException 
	 */
	void setOpen(boolean open, int accessID) throws MicroscopeException, MicroscopeLockedException, InterruptedException;
	
	/**
	 * Returns true if the shutter is open, and false if it is closed.
	 * @return True if shutter is open, false otherwise.
	 * @throws MicroscopeException 
	 * @throws InterruptedException 
	 */
	boolean isOpen() throws MicroscopeException, InterruptedException;
}
