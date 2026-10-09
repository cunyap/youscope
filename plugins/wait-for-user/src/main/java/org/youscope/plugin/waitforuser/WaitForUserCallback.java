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
package org.youscope.plugin.waitforuser;

import java.rmi.RemoteException;

import org.youscope.common.callback.Callback;

/**
 * Simple measurement callback to show a message and wait for the user to acknowledge it.
 * @author Moritz Lang
 *
 */
public interface WaitForUserCallback extends Callback
{
	/**
	 * Type identifier of callback.
	 */
	public static final String	TYPE_IDENTIFIER	= "YouScope.WaitForUserJob.Callback";
	/**
	 * Displays the message and returns after user acknowledgment.
	 * @param message Message to display
	 * @throws RemoteException 
	 * @throws InterruptedException 
	 */
	public void waitForUser(String message) throws RemoteException, InterruptedException;
}
