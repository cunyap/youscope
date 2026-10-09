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
/**
 * 
 */
package org.youscope.common.table;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Indicates that a given measurement component produces table data, and allows to add a listener to obtain produced data.
 * @author Moritz Lang
 * 
 */
public interface TableProducer extends Remote
{
	/**
	 * Removes a previously added listener.
	 * @param listener Listener to be removed.
	 * @throws RemoteException
	 */
	public void removeTableListener(TableListener listener) throws RemoteException;

	/**
	 * Adds a listener which gets informed of newly created statistical data.
	 * @param listener Listener to be added.
	 * @throws RemoteException
	 */
	public void addTableListener(TableListener listener) throws RemoteException;

	/**
	 * Returns the definition of the table layout of the tables produced by this producer, e.g. the number and types of its columns.
	 * 
	 * @return Information about of the produced tables.
	 * @throws RemoteException
	 */
	public TableDefinition getProducedTableDefinition() throws RemoteException;
}
