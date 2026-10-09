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
package org.youscope.plugin.nemesys;

/**
 * @author Moritz Lang
 *
 */
class SyringeTableRow implements Comparable<SyringeTableRow>
{
	public long time;
	public final double[] flowRates;
	public SyringeTableRow(long time, int numDosingUnits)
	{
		this.time = time;
		this.flowRates = new double[numDosingUnits];
		for(int i=0; i<flowRates.length; i++)
		{
			flowRates[i] = 0;
		}
	}
	@Override
	public int compareTo(SyringeTableRow arg0)
	{
		if(arg0 == null)
			return 1;
		else if(arg0.time > time)
			return -1;
		else if(arg0.time < time)
			return 1;
		else
			return 0;
	}
}
