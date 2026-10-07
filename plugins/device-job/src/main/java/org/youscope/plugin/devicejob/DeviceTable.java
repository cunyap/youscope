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
package org.youscope.plugin.devicejob;

import org.youscope.common.table.ColumnDefinition;
import org.youscope.common.table.TableDefinition;

/**
 * Class to provide information about the layout of the table consumed by the device job.
 * @author Moritz Lang
 *
 */
public class DeviceTable
{
	static final ColumnDefinition<String> COLUMN_DEVICE = ColumnDefinition.createStringColumnDefinition("Device", "Identifier of device whose property should be changed.",false);
	static final ColumnDefinition<String> COLUMN_PROPERTY = ColumnDefinition.createStringColumnDefinition("Property", "Name of the property of the device whose value should be changed.", false);
	static final ColumnDefinition<String> COLUMN_VALUE = ColumnDefinition.createStringColumnDefinition("Value", "New value of property. Which values are allowed depends on the device property.", false);
	
	/**
	 * Use static methods.
	 */
	private DeviceTable()
	{
		// use static methods.
	}
	
	private static TableDefinition tableDefinition = null;
	
	/**
	 * Returns the table layout for the input table for a device job.
	 * @return Layout for the device job's input table.
	 */
	public synchronized static TableDefinition getTableDefinition()
	{
		if(tableDefinition != null)
			return tableDefinition;
		tableDefinition = new TableDefinition("Device job input table", 
				"Table containing several rows specifying each a new value of a device's property.\nThe corresponding device properties are set to these values the next time the job executes.",
				COLUMN_DEVICE,
				COLUMN_PROPERTY,
				COLUMN_VALUE
				);
		return tableDefinition;
	}
}
