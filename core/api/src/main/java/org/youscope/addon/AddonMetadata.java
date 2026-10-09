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
package org.youscope.addon;

import javax.swing.Icon;

/**
 * Provides metadata about a given addon.
 * @author Moritz Lang
 *
 */
public interface AddonMetadata
{
	/**
     * Returns a short human readable name of the addon.
     * @return Human readable name of the addon.
     */
    String getName(); 
    
    /**
     * Returns a description of this addon, or null, if no description is available. The description should be plain text, but might contain line-breaks.
     * @return Short description of the addon, or null if no description is provided.
     */
    String getDescription();
    
    /**
	 * Returns the type identifier of the addon.
	 * 
	 * @return Type identifier of the addon.
	 */
	String getTypeIdentifier();
	
	/**
	 * Returns an icon representative for this addon, or null if no icon is set.
	 * @return Icon representative of this addon.
	 */
	Icon getIcon();
	
	/**
	 * Returns an array of strings (possibly of length 0) specifying the classification of the addon.
	 * This classification can e.g. be used to order addons into a certain folder structure.
	 * @return classification of addon.
	 */
	String[] getClassification();
}
