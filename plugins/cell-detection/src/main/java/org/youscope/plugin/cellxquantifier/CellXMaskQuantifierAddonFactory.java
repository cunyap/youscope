/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
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
