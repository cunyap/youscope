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
/**
 * 
 */
package org.youscope.common.table;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Indicates that a given measurement component consumes table data, and allows to obtain information on the layout of the consumed tables.
 * @author Moritz Lang
 * 
 */
public interface TableConsumer extends Remote
{

	/**
	 * Passes the table to the measurement component, such that it can interpret it the next time it is executed.
	 * The component should not immediately take any action except checking if the layout of the table is correct.
	 * The table must have the same number, types and names of the columns as returned by {@link #getConsumedTableDefinition()}.
	 * Columns might have another definition if null entries are allowed. However, if null entries are not allowed for the tables
	 * consumed by this consumer as indicated by {@link #getConsumedTableDefinition()}, the corresponding entries in the column
	 * of the table passed in this function must be non-null.
	 * @param table The table to consume.
	 * 
	 * @throws RemoteException
	 * @throws TableException Thrown if the table has the wrong layout, or if entries of the table are null which should not be null.
	 * @throws NullPointerException Thrown if table is null.
	 */
	public void consumeTable(Table table) throws RemoteException, TableException, NullPointerException;

	/**
	 * Returns the definition of the table layout of the tables consumed by this consumer, e.g. the number and types of its columns.
	 * 
	 * @return Information about of the consumed tables.
	 * @throws RemoteException
	 */
	public TableDefinition getConsumedTableDefinition() throws RemoteException;
}
