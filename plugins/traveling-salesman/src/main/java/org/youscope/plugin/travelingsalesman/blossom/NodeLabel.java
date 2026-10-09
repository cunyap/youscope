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
 * Label of a pseudonode. If node is in tree, labels alternate from root (PLUS) to leaves (PLUS) between PLUS and MINUS.
 * If pseudonode is free, label must be EMPTY}. 
 * @author mlang
 *
 */
enum NodeLabel 
{
	PLUS,
	MINUS,
	EMPTY;
	@Override
	public String toString()
	{
		if(this==PLUS)
			return "+";
		else if(this==MINUS)
			return "-";
		else
			return "o";
	}
}
