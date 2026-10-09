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
 * A table entry containing a float.
 * @author Moritz Lang
 *
 */
class TableFloatEntry extends TableEntryAdapter<Float> 
{

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -381739064279371190L;

	public TableFloatEntry(Float value, boolean nullAllowed) throws NullPointerException, TableException 
	{
		super(value, nullAllowed, Float.class);
	}

	@Override
	protected Float cloneValue(Float value) 
	{
		// Floats are immutable
		return value;
	}
	
	@Override
	public String getValueAsString(Float value) 
	{
		return value.toString();
	}

}
