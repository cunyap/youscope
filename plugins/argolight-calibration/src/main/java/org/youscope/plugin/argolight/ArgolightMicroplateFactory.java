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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/

package org.youscope.plugin.argolight;

import org.youscope.addon.component.ComponentAddonFactoryAdapter;

/**
 * Factory exposing the Argolight microplate layout as a YouScope microplate
 * component, so it can be consumed by the microplate runtime and the standard
 * well-selection / fine-position UIs. Mirrors the rectangular-microplate
 * factory.
 *
 * @author Andreas P. Cuny
 */
public class ArgolightMicroplateFactory extends ComponentAddonFactoryAdapter {
	/**
	 * Constructor.
	 */
	public ArgolightMicroplateFactory() {
		super(ArgolightMicroplateConfiguration.TYPE_IDENTIFIER, ArgolightMicroplateConfiguration.class,
				ArgolightMicroplateResource.class);
	}
}
