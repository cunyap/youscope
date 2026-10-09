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
package org.youscope.common.microscope;

import java.rmi.RemoteException;

/**
 * @author Moritz Lang
 * 
 */
public interface ShutterDevice extends Device
{
	/**
	 * Opens (open == true) or closes (open == false) the shutter.
	 * @param open True if the shutter should be opened, false if it should be closed.
	 * @throws MicroscopeException
	 * @throws MicroscopeLockedException
	 * @throws InterruptedException
	 * @throws RemoteException
	 */
	void setOpen(boolean open) throws MicroscopeException, MicroscopeLockedException, InterruptedException, RemoteException;

	/**
	 * Returns true if the shutter is open, and false if it is closed.
	 * @return True if shutter is open, false otherwise.
	 * @throws MicroscopeException
	 * @throws RemoteException
	 * @throws InterruptedException
	 */
	boolean isOpen() throws MicroscopeException, RemoteException, InterruptedException;
}
