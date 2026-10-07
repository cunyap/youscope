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
import java.util.ArrayList;

import org.youscope.common.PositionInformation;
import org.youscope.common.Well;

class Vertex extends Point2D.Double implements Comparable<Vertex>
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 8412311532339449645L;
		final PositionInformation positionInformation;
		final ArrayList<Vertex> edgesTo = new ArrayList<>(2);
		public Vertex(Point2D.Double position, PositionInformation positionInformation)
		{
			super(position.getX(), position.getY());
			this.positionInformation = positionInformation;
		}
		
		Vertex(double x, double y, int wellY, int wellX, int tileY, int tileX)
		{
			super(x, y);
			PositionInformation wellInfo = new PositionInformation(new Well(wellY, wellX));
			PositionInformation tileInfo = new PositionInformation(wellInfo, PositionInformation.POSITION_TYPE_YTILE, tileY);
			this.positionInformation = new PositionInformation(tileInfo, PositionInformation.POSITION_TYPE_XTILE, tileX);
		}
		
		@Override
		public String toString()
		{
			return positionInformation.toString();
		}

		@Override
		public int compareTo(Vertex o) {
			return o==null ? -1 : positionInformation.compareTo(o.positionInformation);
		}
}
