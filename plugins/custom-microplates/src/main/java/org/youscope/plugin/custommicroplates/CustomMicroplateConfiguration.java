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
package org.youscope.plugin.custommicroplates;

import org.youscope.addon.microplate.MicroplateConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * A customly defined microplate.
 * 
 * @author Moritz Lang
 */
@XStreamAlias("custom-microplate")
public class CustomMicroplateConfiguration extends MicroplateConfiguration
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 7674884664568364109L;
	@XStreamAlias("custom-microplate-name")
	private String customMicroplateName = "unnamed";
	
	@Override
	public String getTypeIdentifier()
	{
		return CustomMicroplateManager.getCustomMicroplateTypeIdentifier(getCustomMicroplateName());
	}

	/**
	 * Sets the name the custom microplate, with which it is identified. Since used for saving, the name must be a valid file name, but without file name extension.
	 * @param customMicroplateName The name of the custom microplate.
	 */
	public void setCustomMicroplateName(String customMicroplateName)
	{
		this.customMicroplateName = customMicroplateName;
	}

	/**
	 * Returns the name of the custom microplate, with which it is identified. The ID is also used as the basename for saving the microplate.
	 * @return Name of the microplate template.
	 */
	public String getCustomMicroplateName()
	{
		return customMicroplateName;
	}
}
