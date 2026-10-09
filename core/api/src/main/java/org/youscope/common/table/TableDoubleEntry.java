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
package org.youscope.common.table;

/**
 * A table entry containing a double.
 * @author Moritz Lang
 *
 */
class TableDoubleEntry extends TableEntryAdapter<Double>  
{

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -5118391925520387931L;
	
	public TableDoubleEntry(Double value, boolean nullAllowed) throws NullPointerException, TableException 
	{
		super(value, nullAllowed, Double.class);
	}

	@Override
	String getValueAsString(Double value) 
	{
		return value.toString();
	}

	@Override
	protected Double cloneValue(Double value) 
	{
		// Double's are immutable
		return value;
	}
	
}
