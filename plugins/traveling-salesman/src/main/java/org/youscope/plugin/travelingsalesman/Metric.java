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
package org.youscope.plugin.travelingsalesman;

import java.awt.geom.Point2D;

/**
 * An implementation of a metric, that is, a definition of the distance between two points on the cartesian plane.
 * @author Moritz Lang
 *
 */
interface Metric 
{
	/**
	 * Returns the distance between the two points. For example, the Euclidean distance would be sqrt((x1-x2)^2+(y1-y2)^2).
	 * @param point1 First point.
	 * @param point2 Second point.
	 * @return distance between points.
	 */
	public double distance(Point2D.Double point1, Point2D.Double point2); 

}
