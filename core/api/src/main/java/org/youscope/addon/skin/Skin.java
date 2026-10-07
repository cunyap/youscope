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
package org.youscope.addon.skin;

import javax.swing.UIManager;

import org.youscope.addon.AddonException;
import org.youscope.addon.AddonMetadata;

/**
 * Interface providing all needed functionality for a YouScope skin.
 * @author Moritz Lang
 *
 */
public interface Skin 
{
	/**
	 * Returns the metadata (like human readable name) for the look and feel.
	 * @return The metadata of the given look and feel.
	 */
	AddonMetadata getMetadata();
	
	/**
	 * When this function is called, the addon should set its look and feel by calling {@link UIManager#setLookAndFeel(javax.swing.LookAndFeel)} or {@link UIManager#setLookAndFeel(String)}.
	 * @throws AddonException Thrown if look and feel cannot be set.
	 */
	void applySkin() throws AddonException;
}
