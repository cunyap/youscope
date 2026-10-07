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
package org.youscope.plugin.dropletmicrofluidics;

import org.youscope.addon.callback.CallbackAddonFactory;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.callback.Callback;
import org.youscope.common.callback.CallbackCreationException;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * Callback providing an UI for droplet based microfluidic measurements.
 * @author Moritz Lang
 */
public class DropletMicrofluidicJobCallbackFactory implements CallbackAddonFactory
{
    @Override
	public synchronized Callback createCallback(String typeIdentifier, YouScopeClient client,
			YouScopeServer server) throws CallbackCreationException {
		if(!DropletMicrofluidicJobCallback.TYPE_IDENTIFIER.equals(typeIdentifier))
		{
			throw new CallbackCreationException("Construction of callbacks with type identifiers "+typeIdentifier+" not supported by this factory.");
		}
		return new DropletMicrofluidicJobCallbackImpl(client);
	}

	@Override
	public String[] getSupportedTypeIdentifiers() {
		return new String[]{DropletMicrofluidicJobCallback.TYPE_IDENTIFIER};
	}

	@Override
	public boolean isSupportingTypeIdentifier(String typeIdentifier) {
		return DropletMicrofluidicJobCallback.TYPE_IDENTIFIER.equals(typeIdentifier);
	}
}
