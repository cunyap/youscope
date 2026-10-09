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
package org.youscope.plugin.dropletmicrofluidics.defaultobserver;

import org.youscope.addon.component.ComponentAddonFactoryAdapter;
 
/**
 * Implementation of the an observer based on the discrete Fourier transformation. The observer separately learns the individual and the mean
 * droplet heights. 
 * @author Moritz Lang
 *
 */
public class DefaultObserverFactory extends ComponentAddonFactoryAdapter
{

	/**
	 * Constructor.
	 */
	public DefaultObserverFactory()
	{
		super(DefaultObserverConfiguration.TYPE_IDENTIFIER, DefaultObserverConfiguration.class, DefaultObserver.class);
	}
}
