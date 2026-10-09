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
public interface AutoFocusDevice extends Device
{
	/**
	 * Returns the last focus score.
	 * @return Last focus score.
	 * @throws MicroscopeException
	 * @throws RemoteException
	 */
	double getLastScore() throws MicroscopeException, RemoteException;

	/**
	 * Returns the current focus score
	 * @return Current focus score
	 * @throws MicroscopeException
	 * @throws RemoteException
	 */
	double getCurrentScore() throws MicroscopeException, RemoteException;

	/**
	 * Enables or disables the auto-focus device.
	 * @param enable True, if focus should be engabled.
	 * @throws MicroscopeException
	 * @throws MicroscopeLockedException
	 * @throws RemoteException
	 */
	void setEnabled(boolean enable) throws MicroscopeException, MicroscopeLockedException, RemoteException;

	/**
	 * Returns true if the autofocus is enabled.
	 * @return True if enabled.
	 * @throws MicroscopeException
	 * @throws RemoteException
	 */
	boolean isEnabled() throws MicroscopeException, RemoteException;

	/**
	 * Returns true if auto-focus is locked.
	 * @return True if locked.
	 * @throws MicroscopeException
	 * @throws RemoteException
	 */
	boolean isLocked() throws MicroscopeException, RemoteException;

	/**
	 * Runs a full focus search.
	 * @throws MicroscopeException
	 * @throws MicroscopeLockedException
	 * @throws RemoteException
	 */
	void runFullFocus() throws MicroscopeException, MicroscopeLockedException, RemoteException;

	/**
	 * Runs an incremental focus search.
	 * @throws MicroscopeException
	 * @throws MicroscopeLockedException
	 * @throws RemoteException
	 */
	void runIncrementalFocus() throws MicroscopeException, MicroscopeLockedException, RemoteException;

	/**
	 * Sets the current offset.
	 * @param offset Offset to set.
	 * @throws MicroscopeException
	 * @throws MicroscopeLockedException
	 * @throws RemoteException
	 */
	void setOffset(double offset) throws MicroscopeException, MicroscopeLockedException, RemoteException;

	/**
	 * Returns the current offset.
	 * @return Current offset.
	 * @throws MicroscopeException
	 * @throws RemoteException
	 */
	double getOffset() throws MicroscopeException, RemoteException;
}
