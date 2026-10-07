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

/**
 * @author Moritz Lang
 *
 */
public enum MatlabScoreType
{
	/**
	 * Histogram range algorithm.
	 */
	HISTOGRAM_RANGE("Histogram Range"),
	/**
	 * Sobel edge detection.
	 */
	SOBEL3("Sobel 3x3"),
	/**
	 * Sobel edge detection.
	 */
	SOBEL5("Sobel 5x5"),
	/**
	 * Sobel edge detection.
	 */
	SOBEL7("Sobel 7x7"),
	/**
	 * Normalized variances.
	 */
	NORMALIZED_VARIANCES("Normalized Variances");
	
	
	private final String description;
	MatlabScoreType(String description)
	{
		this.description = description;
	}
	
	/**
	 * Returns a human readable short description of the algorithm.
	 * @return Short algorithm description.
	 */
	public String getDescription()
	{
		return description;
	}
	
	@Override
	public String toString()
	{
		return description;
	}
}
