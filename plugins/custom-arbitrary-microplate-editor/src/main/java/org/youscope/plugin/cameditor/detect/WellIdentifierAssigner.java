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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.cameditor.detect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.youscope.plugin.cameditor.model.EditableWell;

/**
 * Assigns unique, standard microplate identifiers to a set of wells.
 * <p>
 * Rows are numbered top&rarr;bottom (shown as A, B, C, ... via YouScope's {@code Well}), and columns
 * left->right within a row (shown as 1, 2, 3, ...), so the top-left well becomes A1. This replaces
 * pyCAME's independent x/y ranking, which could assign the same identifier to two physical wells.
 * Rows are found by clustering well centres by their y coordinate using a tolerance derived from the
 * median well height, which tolerates the slight vertical jitter typical of real chip images.
 *
 * @author Andreas P. Cuny
 */
public final class WellIdentifierAssigner
{
	/** Fraction of the median well height used as the row-clustering tolerance. */
	private double rowToleranceFactor = 0.6;

	public void setRowToleranceFactor(double factor)
	{
		if(factor > 0)
			this.rowToleranceFactor = factor;
	}

	/**
	 * Assigns row/column identifiers in place. Wells are mutated (their wellX/wellY set); their order
	 * in the list is not changed.
	 * @param wells wells to label.
	 */
	public void assign(List<EditableWell> wells)
	{
		if(wells == null || wells.isEmpty())
			return;

		double tolerance = rowToleranceFactor * medianHeight(wells);
		if(tolerance <= 0)
			tolerance = 1;

		// Sort a copy by centre-y so we can greedily group rows.
		List<EditableWell> byY = new ArrayList<EditableWell>(wells);
		Collections.sort(byY, new Comparator<EditableWell>()
		{
			@Override
			public int compare(EditableWell a, EditableWell b)
			{
				return Double.compare(a.getCenterYPx(), b.getCenterYPx());
			}
		});

		List<List<EditableWell>> rows = new ArrayList<List<EditableWell>>();
		List<EditableWell> current = new ArrayList<EditableWell>();
		double rowRef = byY.get(0).getCenterYPx();
		for(EditableWell well : byY)
		{
			if(!current.isEmpty() && Math.abs(well.getCenterYPx() - rowRef) > tolerance)
			{
				rows.add(current);
				current = new ArrayList<EditableWell>();
			}
			if(current.isEmpty())
				rowRef = well.getCenterYPx();
			current.add(well);
			// running-average row reference keeps clustering stable across a slightly sloped row
			rowRef = averageCenterY(current);
		}
		if(!current.isEmpty())
			rows.add(current);

		for(int rowIndex = 0; rowIndex < rows.size(); rowIndex++)
		{
			List<EditableWell> row = rows.get(rowIndex);
			Collections.sort(row, new Comparator<EditableWell>()
			{
				@Override
				public int compare(EditableWell a, EditableWell b)
				{
					return Double.compare(a.getCenterXPx(), b.getCenterXPx());
				}
			});
			for(int colIndex = 0; colIndex < row.size(); colIndex++)
			{
				EditableWell well = row.get(colIndex);
				well.setWellY(rowIndex);
				well.setWellX(colIndex);
			}
		}
	}

	private static double averageCenterY(List<EditableWell> wells)
	{
		double sum = 0;
		for(EditableWell w : wells)
			sum += w.getCenterYPx();
		return sum / wells.size();
	}

	private static double medianHeight(List<EditableWell> wells)
	{
		double[] heights = new double[wells.size()];
		for(int i = 0; i < wells.size(); i++)
			heights[i] = wells.get(i).getHeightPx();
		java.util.Arrays.sort(heights);
		int mid = heights.length / 2;
		if(heights.length % 2 == 1)
			return heights[mid];
		return 0.5 * (heights[mid - 1] + heights[mid]);
	}
}
