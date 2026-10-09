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
package org.youscope.common.callback;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * A provider for {@link Callback} implementations.
 * 
 * @author Moritz Lang
 * 
 */
public interface CallbackProvider extends Remote
{
	/**
	 * Returns a callback with the given type identifier.
	 * Throws a {@link CallbackCreationException} if no callback with the given identifier is available.
	 * @param typeIdentifier Type identifier of callback.
	 * @return A callback with the given type identifier.
	 * @throws RemoteException
	 * @throws CallbackCreationException Thrown if no callback with the given name is available, or if creation of callback failed.
	 */
	public Callback createCallback(String typeIdentifier) throws RemoteException, CallbackCreationException;
	
	/**
	 * Returns a callback with the given type identifier implementing a given interface.
	 * Throws a {@link CallbackCreationException} if no callback with the given identifier is available, or if a callback with the identifier is available but does not implement the given interface.
	 * @param typeIdentifier Type identifier of callback.
	 * @param callbackInterface The interface of the callback.
	 * @return A callback with the given type identifier implementing the given interface.
	 * @throws RemoteException
	 * @throws CallbackCreationException Thrown if no callback with the given name is available, if creation of callback failed, or if callback with given identifier does not implement given interface.
	 */
	public <T extends Callback> T createCallback(String typeIdentifier, Class<T> callbackInterface) throws RemoteException, CallbackCreationException;
}
