/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.cameditor.detect;

/**
 * A candidate well detected in the backdrop image, in image pixel coordinates.
 * Immutable data holder produced by {@link WellDetector}.
 *
 * @author CAME
 */
public final class DetectedWell
{
	/** Left of the bounding box, in image pixels. */
	public final double x;
	/** Top of the bounding box, in image pixels. */
	public final double y;
	/** Width of the bounding box, in image pixels. */
	public final double width;
	/** Height of the bounding box, in image pixels. */
	public final double height;
	/** Centroid x, in image pixels. */
	public final double centroidX;
	/** Centroid y, in image pixels. */
	public final double centroidY;
	/** Area of the connected component, in pixels. */
	public final int area;

	public DetectedWell(double x, double y, double width, double height, double centroidX, double centroidY, int area)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.centroidX = centroidX;
		this.centroidY = centroidY;
		this.area = area;
	}
}
