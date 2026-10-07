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
package org.youscope.plugin.simplefocusscores;

import org.youscope.addon.focusscore.FocusScoreConfiguration;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.configuration.YSConfigAlias;
import org.youscope.common.configuration.YSConfigIntegerRange;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Moritz Lang
 *
 */
@YSConfigAlias("autocorrelation")
@XStreamAlias("autocorrelation-focus-score")
public class AutocorrelationFocusScoreConfiguration extends FocusScoreConfiguration
{

	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= -1111101676111652236L;
	
	@YSConfigAlias("lag (pixels)")
	@YSConfigIntegerRange(minValue=1)
	private int lag = 1;
	/**
	 * Constructor.
	 */
	public AutocorrelationFocusScoreConfiguration()
	{
		// do nothing.
	}

	/**
	 * The identifier for this configuration.
	 */
	public static final String	CONFIGURATION_ID	= "YouScope.AutocorrelationFocusScore";
	
	@Override
	public String getTypeIdentifier()
	{
		return CONFIGURATION_ID;
	}

	/**
	 * Returns the lag between the two autocorrelations (in pixels).
	 * @return Lag in pixels.
	 */
	public int getLag() {
		return lag;
	}

	/**
	 * Sets the lag between the two autocorrelations (in pixels).
	 * @param lag Lag in pixels.
	 */
	public void setLag(int lag) {
		this.lag = lag;
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		if(lag < 1)
			throw new ConfigurationException("Autocorrelation lag must be at least one.");
		
	}
}
