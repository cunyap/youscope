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
package org.youscope.plugin.custommicroplates;

class CustomRectangularMicroplateDefinition implements CustomMicroplateDefinition
{
	private int numWellsX = 12;
	private int numWellsY = 8;
	private double wellWidth = 9000.;
	private double wellHeight = 9000.;
	private String customMicroplateName = "unnamed";
	public int getNumWellsX()
	{
		return numWellsX;
	}
	
	public void setNumWellsX(int numWellsX)
	{
		this.numWellsX = numWellsX;
	}

	public int getNumWellsY()
	{
		return numWellsY;
	}
	
	public void setNumWellsY(int numWellsY)
	{
		this.numWellsY = numWellsY;
	}

	public double getWellWidth()
	{
		return wellWidth;
	}
	
	public void setWellWidth(double wellWidth)
	{
		this.wellWidth =  wellWidth;
	}

	public double getWellHeight()
	{
		return wellHeight;
	}
	
	public void setWellHeight(double wellHeight)
	{
		this.wellHeight =  wellHeight;
	}

	@Override
	public void setCustomMicroplateName(String customMicroplateName)
	{
		this.customMicroplateName = customMicroplateName;
	}

	@Override
	public String getCustomMicroplateName()
	{
		return customMicroplateName;
	}

}
