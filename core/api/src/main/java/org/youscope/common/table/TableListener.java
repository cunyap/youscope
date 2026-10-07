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
import java.util.EventListener;

import org.youscope.common.job.Job;

/**
 * Listener which can be registered at a table producing measurement component to get informed when new table data was produced.
 * See {@link Table} for more information about the produced table.
 * @author Moritz Lang
 * 
 */
public interface TableListener extends Remote, EventListener
{
	/**
	 * Gets called when a new table was produced by the table producer. For measurement components (e.g. {@link Job}), this function
	 * should be called for every listener exactly once per evaluation.
	 * This includes calling this function even with a Table containing no rows.
	 * @param table The produced table.
	 * @throws RemoteException
	 */
	public void newTableProduced(Table table) throws RemoteException;
}
