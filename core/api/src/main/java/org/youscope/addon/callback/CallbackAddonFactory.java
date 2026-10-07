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
package org.youscope.addon.callback;

import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.callback.Callback;
import org.youscope.common.callback.CallbackCreationException;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * A factory to create measurement callbacks.
 * Per measurement, there exist only one instance of each implementation of CallbackAddonFactory. Thus, singletons (per measurement) can be realized by
 * always returning the same callback object.
 * @author Moritz Lang
 */
public interface CallbackAddonFactory
{
	/**
     * Returns a new measurement callback for the given type identifier.
     * @param typeIdentifier The ID for which a callback should be created.
     * @param measurement Measurement which creates the callback.
     * @param client Interface to allow the addon to communicate with the client.
     * @param server Interface to the server.
     * @return The created addon for a given type identifier.
     * @throws CallbackCreationException Thrown if callback type identifier is not supported by this factory, or if any general error occurred during the callback creation.
     */
    Callback createCallback(String typeIdentifier, YouScopeClient client, YouScopeServer server) throws CallbackCreationException;

    /**
	 * Returns a list of all callback type identifiers supported by this factory.
	 * 
	 * @return List of supported callback types.
	 */
	String[] getSupportedTypeIdentifiers();

	/**
	 * Returns true if this factory supports callback with the given type identifier, false otherwise.
	 * @param typeIdentifier The ID of the callback type for which it should be queried if this factory supports its construction.
	 * @return True if this factory supports creating callback with the given ID, false otherwise.
	 */
	boolean isSupportingTypeIdentifier(String typeIdentifier);
}
