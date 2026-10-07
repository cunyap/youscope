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
package org.youscope.plugin.scripting;

import java.rmi.RemoteException;

import org.youscope.common.ComponentRunningException;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.image.ImageListener;
import org.youscope.common.saving.MeasurementFileLocations;
import org.youscope.common.saving.MeasurementSaver;
import org.youscope.common.saving.SaveSettings;
import org.youscope.common.table.Table;
import org.youscope.common.table.TableListener;

/**
 * Object pretending to save a measurement. Implemented to be able to debug a measurement.
 * @author Moritz Lang
 *
 */
class MeasurementSaverImpl implements MeasurementSaver, ImageListener, TableListener
{

	@Override
	public ImageListener getSaveImageListener(String imageSaveName)
	{
		return this;
	}

	@Override
	public void setSaveSettings(SaveSettings saveSettings) throws ComponentRunningException
	{
		// Do nothing.
	}

	@Override
	public SaveSettings getSaveSettings()
	{
		return null;
	}

	@Override
	public void imageMade(ImageEvent<?> e)
	{
		// Do nothing.
	}

	@Override
	public TableListener getSaveTableListener(String tableSaveName)
	{
		return this;
	}

	@Override
	public void newTableProduced(Table table) throws RemoteException {
		// do nothing.
		
	}

	@Override
	public MeasurementFileLocations getLastMeasurementFileLocations() throws RemoteException {
		return null;
	}
}
