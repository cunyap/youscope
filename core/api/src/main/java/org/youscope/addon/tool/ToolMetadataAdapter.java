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
package org.youscope.addon.tool;

import javax.swing.Icon;

import org.youscope.addon.AddonMetadataAdapter;

/**
 * Adapter to simplify {@link ToolMetadata} construction.
 * @author Moritz Lang
 *
 */
public class ToolMetadataAdapter extends AddonMetadataAdapter implements ToolMetadata
{	
	
	/**
	 * Constructor. Sets the icon to the default icon.
	 * @param typeIdentifier Unique type identifier of the tool type. Must not be null.
	 * @param name Human readable name of the tool. Must not be null.
	 * @param classification Classification of the tool. Set to null for unclassified.
	 * @param description Description of the addon. Set to null to not provide any description.
	 */
	public ToolMetadataAdapter(String typeIdentifier, String name, String[] classification, String description)
	{
		super(typeIdentifier, name, classification, description);
	}
	/**
	 * Constructor. 
	 * @param typeIdentifier Unique type identifier of the tool type. Must not be null.
	 * @param name Human readable name of the tool. Must not be null.
	 * @param classification Classification of the tool. Set to null for unclassified.
	 * @param description Description of the addon. Set to null to not provide any description.
	 * @param icon Icon of the tool. Set to null for default icon.
	 */
	public ToolMetadataAdapter(String typeIdentifier, String name, String[] classification, String description, Icon icon)
	{
		super(typeIdentifier, name, classification, description, icon);
	}
	
	/**
	 * Constructor.
	 * @param typeIdentifier Unique type identifier of the tool type. Must not be null.
	 * @param name Human readable name of the tool. Must not be null.
	 * @param classification Classification of the tool. Set to null for unclassified.
	 * @param description Description of the addon. Set to null to not provide any description.
	 * @param iconPath Path of icon of the tool. Set to null for default icon.
	 */
	public ToolMetadataAdapter(String typeIdentifier, String name, String[] classification, String description, String iconPath) 
	{
		super(typeIdentifier, name, classification, description, iconPath);
	}
}
