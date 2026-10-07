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
package org.youscope.common.table;

/**
 * A table entry containing an integer.
 * @author Moritz Lang
 *
 */
class TableIntegerEntry extends TableEntryAdapter<Integer> 
{

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -5198391925520387931L;

	public TableIntegerEntry(Integer value, boolean nullAllowed) throws NullPointerException, TableException 
	{
		super(value, nullAllowed, Integer.class);
	}

	@Override
	protected Integer cloneValue(Integer value) 
	{
		// Integer's are immutable
		return value;
	}
	
	@Override
	public String getValueAsString(Integer value) 
	{
		return value.toString();
	}
	
}
