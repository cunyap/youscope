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
package org.youscope.common.microplate;

import java.io.Serializable;

import org.youscope.common.Well;

/**
 * The layout of a microplate. A microplate is defined by the location of its wells.
 * Important: a microplate layout must be serializable.
 * @author Moritz Lang
 *
 */
public interface MicroplateLayout extends Serializable, Iterable<WellLayout>
{
	/**
	 * Returns the number of wells in this layout.
	 * @return Number of wells.
	 */
	public int getNumWells();
	
	/**
	 * Returns the well with the given index.
	 * @param index Index of the well.
	 * @return Well with the given index.
	 */
	public WellLayout getWell(int index);
	
	/**
	 * Returns the well layout for the given well, or null if no well layout for the given well exists.
	 * @param well Well to return the well layout of.
	 * @return Well layout of the well, or null if no such layout exists.
	 */
	public WellLayout getWell(Well well);
}
