/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
/**
 * 
 */
package org.youscope.plugin.cellxquantifier;

import org.youscope.addon.component.ComponentAddonFactoryAdapter;

/** Registers the CellXMaskInterface quantifier as a YouScope component. */
public class CellXMaskQuantifierAddonFactory extends ComponentAddonFactoryAdapter {
	public CellXMaskQuantifierAddonFactory() {
		super(CellXMaskQuantifierConfiguration.TYPE_IDENTIFIER, CellXMaskQuantifierConfiguration.class,
				CellXMaskQuantifierAddon.class);
	}
}
