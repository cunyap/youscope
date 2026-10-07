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
 * A table entry containing a long.
 * @author Moritz Lang
 *
 */
class TableLongEntry extends TableEntryAdapter<Long> 
{

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -311739064279371190L;

	public TableLongEntry(Long value, boolean nullAllowed) throws NullPointerException, TableException 
	{
		super(value, nullAllowed, Long.class);
	}

	@Override
	protected Long cloneValue(Long value) 
	{
		// Longs are immutable
		return value;
	}
	
	@Override
	public String getValueAsString(Long value) 
	{
		return value.toString();
	}

}
