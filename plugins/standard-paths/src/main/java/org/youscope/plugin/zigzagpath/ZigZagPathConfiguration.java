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
package org.youscope.plugin.zigzagpath;

import org.youscope.addon.pathoptimizer.PathOptimizerConfiguration;
import org.youscope.common.configuration.YSConfigAlias;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Zig-Zag path through microplate
 * @author Moritz Lang
 *
 */
@YSConfigAlias("Zig-Zag Path")
@XStreamAlias("zig-zag-path")
public class ZigZagPathConfiguration extends PathOptimizerConfiguration
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 3051118882205729335L;
	/**
	 * Type identifier.
	 */
	public static final String TYPE_IDENTIFIER = "YouScope.path.ZigZagPath";

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	/**
	 * Direction of the zig-zag path
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
	 * Returns if the zig-zag is done vertically or horizontally.
	 * @return direction of zig-zag.
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Sets if the zig-zag is done vertically or horizontally.
	 * @param direction direction of zig-zag.
	 */
	public void setDirection(Direction direction) {
		this.direction = direction;
	}


}
