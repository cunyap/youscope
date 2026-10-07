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
package org.youscope.client;

import java.util.Hashtable;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * 
 * @author Moritz Lang
 *
 */
class PlotColumnHandler
{
	private final XYSeriesCollection plotsCollection;
	private final String columnName;
	private Hashtable<String, XYSeries> plots = new Hashtable<String, XYSeries>();
	
	private static final String NULL_IDENT_STRING = "theDefault1234";
	PlotColumnHandler(String columnName, XYSeriesCollection plotsCollection)
	{
		this.plotsCollection = plotsCollection;
		this.columnName = columnName;
	}
	
	@Override
	public String toString()
	{
		return columnName;
	}
	
	public String getColumnName()
	{
		return columnName;
	}
	
	public void addDate(double x, double y, String identityString)
	{
		XYSeries series = plots.get(identityString == null ? NULL_IDENT_STRING : identityString);
		if(series == null)
		{
			series = new XYSeries(identityString == null ? columnName : identityString + " - " + columnName);
			plots.put(identityString == null ? NULL_IDENT_STRING : identityString, series);
			plotsCollection.addSeries(series);
		}
		series.add(x, y);
	}
}
