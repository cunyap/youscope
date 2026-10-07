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
package org.youscope.plugin.matlabfocusscores;

import org.youscope.addon.focusscore.FocusScoreConfiguration;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.configuration.YSConfigAlias;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Moritz Lang
 *
 */
@YSConfigAlias("Matlab based score")
@XStreamAlias("matlab-focus-score")
public class MatlabScoresConfiguration extends FocusScoreConfiguration
{

	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= -1111201676160652236L;
	
	@YSConfigAlias("Matlab algorithm")
	private MatlabScoreType scoreAlgorithm = MatlabScoreType.SOBEL3;
	
	/**
	 * Constructor.
	 */
	public MatlabScoresConfiguration()
	{
		// do nothing.
	}

	/**
	 * The identifier for this configuration.
	 */
	public static final String	CONFIGURATION_ID	= "YouScope.MatlabFocusScore";
	
	@Override
	public String getTypeIdentifier()
	{
		return CONFIGURATION_ID;
	}

	/**
	 * Sets the algorithm for the focus score calculation.
	 * @param scoreAlgorithm
	 */
	public void setScoreAlgorithm(MatlabScoreType scoreAlgorithm)
	{
		this.scoreAlgorithm = scoreAlgorithm;
	}

	/**
	 * @return algorithm to calculate the score.
	 */
	public MatlabScoreType getScoreAlgorithm()
	{
		return scoreAlgorithm;
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		// nothing to check.
		
	}
}
