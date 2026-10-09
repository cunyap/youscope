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
package org.youscope.common.task;

import org.youscope.common.configuration.ConfigurationException;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * This class represents a varying period length. The actual period length for a given iteration is a member of an array of predefined period lengths.
 * @author Moritz Lang
 */
@XStreamAlias("varying-period")
public class VaryingPeriodConfiguration extends PeriodConfiguration
{
	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= 5382800416891354802L;

	/**
	 * Times between single executions (in milliseconds).
	 */
	@XStreamImplicit(itemFieldName = "period")
	private long[]				periods				= new long[0];
	
	@Override
	public String getTypeIdentifier() 
	{
		return "YouScope.VaryingPeriod";
	}

	/**
	 * Sets the periods. Setting it to null has no effect.
	 * @param periods the periods to set (in ms).
	 */
	public void setPeriods(long[] periods)
	{
		if(periods == null)
			return;
		this.periods = new long[periods.length];
		System.arraycopy(periods, 0, this.periods, 0, periods.length);
	}

	/**
	 * Returns the periods.
	 * @return the periods to set (in ms).
	 */
	public long[] getPeriods()
	{
		long[] periods = new long[this.periods.length];
		System.arraycopy(this.periods, 0, periods, 0, this.periods.length);
		return periods;
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		super.checkConfiguration();
		if(periods == null || periods.length == 0)
			throw new ConfigurationException("There must be at least one period defined.");
	}
}
