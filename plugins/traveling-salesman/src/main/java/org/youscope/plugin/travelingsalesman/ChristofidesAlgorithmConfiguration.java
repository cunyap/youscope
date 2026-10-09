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

import org.youscope.addon.pathoptimizer.PathOptimizerConfiguration;
import org.youscope.common.configuration.YSConfigAlias;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Christofide's algorithm configuration
 * @author Moritz Lang
 *
 */
@YSConfigAlias("Christofides's Approximation (1.5-approximation)")
@XStreamAlias("christofides-algorithm")
public class ChristofidesAlgorithmConfiguration extends PathOptimizerConfiguration
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 3031118882205729335L;
	/**
	 * Type identifier.
	 */
	public static final String TYPE_IDENTIFIER = "YouScope.path.ChristofidesAlgorithm";
	
	@XStreamAlias("metric")
	private MetricType metric = MetricType.MANHATTEN;

	/**
	 * Sets the metric to use to calculate distances.
	 * @param metric metric to use.
	 */
	public void setMetric(MetricType metric) {
		this.metric = metric;
	}

	/**
	 * Returns the metric used to calculate distances.
	 * @return used metric.
	 */
	public MetricType getMetric() {
		return metric;
	}

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	

}
