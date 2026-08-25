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
package org.youscope.plugin.intensityquantifier;

import org.youscope.addon.component.ComponentAddonFactoryAdapter;

/** Registers the minimal intensity quantifier as a YouScope component. */
public class IntensityQuantifierAddonFactory extends ComponentAddonFactoryAdapter {
	public IntensityQuantifierAddonFactory() {
		super(IntensityQuantifierConfiguration.TYPE_IDENTIFIER, IntensityQuantifierConfiguration.class,
				IntensityQuantifierAddon.class);
	}
}
