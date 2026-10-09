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
package org.youscope.plugin.travelingsalesman.blossom;

/**
 * Type of primal update performed by the blossom algorithm.
 * @author Moritz Lang
 *
 */
public enum PrimalUpdateType
{
	/**
	 * Current tree was grown by two free nodes.
	 */
	GROW,
	/**
	 * Two trees were joined to a graph. Subsequently, graph was splitted into pairs of free nodes.
	 */
	AUGMENT,
	/**
	 * A cycle was shrinked into a blossom.
	 */
	SHRINK,
	/**
	 * A blossom was expanded.
	 */
	EXPAND,
	/**
	 * The algorithm has finished.
	 */
	FINISHED,
	/**
	 * No update could be performed.
	 */
	NONE;
	
	@Override
	public String toString()
	{
		switch(this)
		{
		case GROW:
			return "grow";
		case AUGMENT:
			return "augment";
		case SHRINK:
			return "shrink";
		case EXPAND:
			return "expand";
		case FINISHED:
			return "finished";
		case NONE:
			return "none";
		default:
			return "unknown: " + super.toString();
		}
	}
}
