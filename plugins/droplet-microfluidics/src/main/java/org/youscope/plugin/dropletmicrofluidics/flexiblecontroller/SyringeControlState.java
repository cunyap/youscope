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
package org.youscope.plugin.dropletmicrofluidics.flexiblecontroller;

/**
 * Defines if syringe should have a fixed flow rate, or if its flow rate should be varied either positively or negatively to correct for droplet heights.
 * @author Moritz Lang
 *
 */
public enum SyringeControlState 
{
	/**
	 * fixed flow rate.
	 */
	FIXED("keep fixed"),
	/**
	 * flow rate is kept over zero to correct for droplet height.
	 */
	POSITIVE("keep positive"),
	/**
	 * flow rate is kept below zero to correct for droplet height.
	 */
	NEGATIVE("keep negative");
	
	private final String description;
	SyringeControlState(String description)
	{
		this.description = description;
	}
	@Override
	public String toString() 
	{
		return description;
	}
}
