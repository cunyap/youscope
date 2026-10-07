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
package org.youscope.plugin.ansisbs;

import org.youscope.addon.component.ComponentAddonFactoryAdapter;
/**
 * Factory for ANSI SBS microplates.
 * @author Moritz Lang
 */
public class AnsiSBSMicroplateFactory extends ComponentAddonFactoryAdapter
{
	/**
	 * Constructor.
	 */
	public AnsiSBSMicroplateFactory()
	{
		addAddon(AnsiSBS96MicroplateConfiguration.TYPE_IDENTIFIER, AnsiSBS96MicroplateConfiguration.class, AnsiSBS96MicroplateResource.class);
		addAddon(AnsiSBS384MicroplateConfiguration.TYPE_IDENTIFIER, AnsiSBS384MicroplateConfiguration.class, AnsiSBS384MicroplateResource.class);
		addAddon(AnsiSBS1536MicroplateConfiguration.TYPE_IDENTIFIER, AnsiSBS1536MicroplateConfiguration.class, AnsiSBS1536MicroplateResource.class);
	}
}
