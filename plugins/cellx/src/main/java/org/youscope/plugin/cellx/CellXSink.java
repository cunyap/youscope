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
package org.youscope.plugin.cellx;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.youscope.addon.celldetection.CellDetectionException;

/**
 * Interface for helper class with which CellX saves cell detection result in Matlab.
 * @author Moritz Lang
 *
 */
public interface CellXSink extends Remote
{
	/**
	 * Adds a cell to the cell detection table.
	 * @param headers Header of the columns
	 * @param data data of the columns. Must be one row/cell only.
	 * @throws RemoteException
	 * @throws CellDetectionException
	 */
	void addCell(String[] headers, double[] data) throws RemoteException, CellDetectionException;

}
