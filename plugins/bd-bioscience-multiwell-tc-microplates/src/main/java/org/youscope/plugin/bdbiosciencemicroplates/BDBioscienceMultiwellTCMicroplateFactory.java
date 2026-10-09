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
package org.youscope.plugin.bdbiosciencemicroplates;

import org.youscope.addon.component.ComponentAddonFactoryAdapter;
/**
 * Factory for ANSI SBS microplates.
 * @author Moritz Lang
 */
public class BDBioscienceMultiwellTCMicroplateFactory extends ComponentAddonFactoryAdapter
{
	/**
	 * Constructor.
	 */
	public BDBioscienceMultiwellTCMicroplateFactory()
	{
		addAddon(BDBioscienceMultiwellTC6MicroplateConfiguration.TYPE_IDENTIFIER, BDBioscienceMultiwellTC6MicroplateConfiguration.class, BDBioscienceMultiwellTC6MicroplateResource.class);
		addAddon(BDBioscienceMultiwellTC24MicroplateConfiguration.TYPE_IDENTIFIER, BDBioscienceMultiwellTC24MicroplateConfiguration.class, BDBioscienceMultiwellTC24MicroplateResource.class);
		addAddon(BDBioscienceMultiwellTC12MicroplateConfiguration.TYPE_IDENTIFIER, BDBioscienceMultiwellTC12MicroplateConfiguration.class, BDBioscienceMultiwellTC12MicroplateResource.class);
	}
}
