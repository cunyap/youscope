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
package org.youscope.plugin.quickdetect;

import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.common.table.TableDefinition;

/**
 * Provides information on the layout of the tables produced by the quick detect algorithm.
 * @author Moritz Lang
 *
 */
public class QuickDetectTable {

	/**
	 * Use static methods.
	 */
	private QuickDetectTable()
	{
		// use static methods.
	}
	
	private static TableDefinition tableDefinition = null;
	
	/**
	 * Returns the table layout for the quick-detect cell-detecktion algorithm.
	 * @return Layout for the cell-detection table.
	 */
	public synchronized static TableDefinition getTableDefinition()
	{
		if(tableDefinition != null)
			return tableDefinition;
		tableDefinition = new TableDefinition("Cell-Detection Results", 
				"Contains information about the cells detected in the images by the Quick-Detect algorithm.",
				CellDetectionTableColumns.TABLE_COLUMN_CELL_ID,
				CellDetectionTableColumns.TABLE_COLUMN_QUANTIFICATION_IMAGE_ID,
				CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX,
				CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX,
				CellDetectionTableColumns.TABLE_COLUMN_AREA_PX,
				CellDetectionTableColumns.TABLE_COLUMN_FLUORESCENCE);
		return tableDefinition;		
	}
}
