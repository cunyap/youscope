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
package org.youscope.client;

/**
 * Type of the table data representation in a plot.
 * @author Moritz Lang
 *
 */
enum TableDataPlotType
{
	/**
	 * Each table data element gets plotted as a scatter element.
	 */
	Scatter,
	/**
	 * If more than one element is transmitted per evaluation, only the median is plotted.
	 */
	LineMedian,
	/**
	 * If more than one element is transmitted per evaluation, only the mean is plotted.
	 */
	LineMean,
	/**
	 * If more than one element is transmitted per evaluation, only the first is plotted.
	 */
	LineFirst,
	/**
	 * If more than one element is transmitted per evaluation, all are plotted. The elements which should be connected are determined by an additional column.
	 */
	LineIdentity;
}
