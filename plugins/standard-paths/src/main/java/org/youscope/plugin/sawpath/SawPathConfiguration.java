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
package org.youscope.plugin.sawpath;

import org.youscope.addon.pathoptimizer.PathOptimizerConfiguration;
import org.youscope.common.configuration.YSConfigAlias;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Non-optimized path through microplate
 * @author Moritz Lang
 *
 */
@YSConfigAlias("Saw Path")
@XStreamAlias("saw-path")
public class SawPathConfiguration extends PathOptimizerConfiguration 
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 3041118882205729335L;
	/**
	 * Type identifier.
	 */
	public static final String TYPE_IDENTIFIER = "YouScope.path.SawPath";
	
	/**
	 * Direction of the saw path
	 * @author Moritz Lang
	 *
	 */
	public enum Direction
	{
		/**
		 * Horizontal direction.
		 */
		HORIZONTALLY("Horizontally"),
		/**
		 * Vertical direction.
		 */
		VERTICALLY("Vertically");
		
		private final String description;
		Direction(String description)
		{
			this.description = description;
		}
		@Override
		public String toString()
		{
			return description;
		}
	}
	@XStreamAlias("direction")
	@YSConfigAlias("direction")
	private Direction direction = Direction.HORIZONTALLY;

	/**
	 * Returns if the saw is done vertically or horizontally.
	 * @return direction of saw.
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Sets if the saw is done vertically or horizontally.
	 * @param direction direction of saw.
	 */
	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	

}
