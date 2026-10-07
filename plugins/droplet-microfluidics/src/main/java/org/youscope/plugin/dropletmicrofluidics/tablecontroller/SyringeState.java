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
package org.youscope.plugin.dropletmicrofluidics.tablecontroller;

/**
 * State of a syringe, i.e. if it is inactive, or used to set inflow or outflow.
 * @author Moritz Lang
 *
 */
public enum SyringeState 
{
	/**
	 * inactive (flow rate = 0).
	 */
	INACTIVE("inactive"),
	/**
	 * used for inflow.
	 */
	INFLOW("inflow"),
	/**
	 * used for outflow
	 */
	OUTFLOW("outflow");
	
	private final String description;
	SyringeState(String description)
	{
		this.description = description;
	}
	@Override
	public String toString() 
	{
		return description;
	}
}
