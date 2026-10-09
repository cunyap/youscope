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
package org.youscope.plugin.travelingsalesman;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * The metric to use in the path optimization.
 * @author Moritz Lang
 *
 */
public enum MetricType 
{
	/**
	 * Manhatten metric or L1 norm.
	 */
	@XStreamAlias("manhatten")
	MANHATTEN("L1 (Manhatten)"),
	/**
	 * Euclidean metric or L2 norm
	 */
	@XStreamAlias("euclidean")
	EUCLIDEAN("L2 (Euclidean)"),
	/**
	 * Maximum metric or Linf norm.
	 */
	@XStreamAlias("maximum")
	MAXIMUM("Linf (Maximum)");
	
	private final String description;
	MetricType(String description)
	{
		this.description = description;
	}
	
	@Override
	public String toString()
	{
		return description;
	}
}
