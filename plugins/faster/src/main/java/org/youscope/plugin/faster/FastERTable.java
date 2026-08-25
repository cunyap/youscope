/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.faster;

import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.common.table.ColumnDefinition;
import org.youscope.common.table.TableDefinition;

/**
 * Table definition produced by the FastER segmenter: the minimal segmenter core
 * (cell id + centroid + area + perimeter), identical to StarDist/Cellpose. Rich
 * features are added by the selected quantifier.
 */
final class FastERTable
{
	private FastERTable()
	{
	}

	static TableDefinition getTableDefinition()
	{
		ColumnDefinition<?>[] cols = new ColumnDefinition<?>[]
		{
			CellDetectionTableColumns.TABLE_COLUMN_CELL_ID,
			CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX,
			CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX,
			CellDetectionTableColumns.TABLE_COLUMN_AREA_PX,
			ColumnDefinition.createDoubleColumnDefinition("cell.perimeter",
					"Cell perimeter (px)", true)
		};
		return new TableDefinition("FastER Cell-Detection Results",
				"Per-cell core geometry produced by the FastER segmenter.", cols);
	}
}
