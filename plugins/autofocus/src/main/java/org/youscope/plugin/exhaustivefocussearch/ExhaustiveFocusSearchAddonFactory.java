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
package org.youscope.plugin.exhaustivefocussearch;

import org.youscope.addon.component.ComponentAddonFactoryAdapter;

/**
 * Factory for the creation of exhaustive search focus search.
 * @author Moritz Lang
 *
 */
public class ExhaustiveFocusSearchAddonFactory extends ComponentAddonFactoryAdapter
{
	/**
	 * Constructor.
	 */
	public ExhaustiveFocusSearchAddonFactory()
	{
		super(ExhaustiveFocusSearchConfiguration.CONFIGURATION_ID, ExhaustiveFocusSearchConfiguration.class, ExhaustiveFocusSearchAddon.class);
	}
}
