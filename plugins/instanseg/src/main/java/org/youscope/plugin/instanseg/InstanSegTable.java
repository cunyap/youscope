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
/**
 * 
 */
package org.youscope.plugin.instanseg;

import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.common.table.TableDefinition;

/**
 * Defines the per-cell table produced by the InstanSeg addon.
 *
 * <p>
 * InstanSeg emits a label mask and <b>core geometry</b> only, using the shared
 * {@link CellDetectionTableColumns} identifiers ({@code cell.index},
 * {@code cell.center.x}, {@code cell.center.y}, {@code cell.area},
 * {@code cell.perimeter}). Per-cell intensity is the job of the selected
 * quantification addon, which measures it from this mask.
 *
 * <p>
 * Column order must match the order values are added in
 * {@code InstanSegAddon.buildTable(...)}, and must equal what the
 * configuration declares (golden rule 1 of the extension guide).
 *
 * @author Andreas P. Cuny
 */
public class InstanSegTable {
    private InstanSegTable() {
    }

    private static TableDefinition tableDefinition = null;

    /** Returns the singleton {@link TableDefinition} for InstanSeg results. */
    public static synchronized TableDefinition getTableDefinition() {
        if (tableDefinition != null)
            return tableDefinition;

        tableDefinition = new TableDefinition(
                "InstanSeg Cell Detection",
                "Segmentation geometry produced by InstanSeg (mask + core geometry). "
                        + "Intensity features are added by the selected quantification plugin.",
                CellDetectionTableColumns.TABLE_COLUMN_CELL_ID, // cell.index (Integer)
                CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX, // cell.center.x (Double)
                CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX, // cell.center.y (Double)
                CellDetectionTableColumns.TABLE_COLUMN_AREA_PX, // cell.area (Double)
                CellDetectionTableColumns.TABLE_COLUMN_PERIMETER_PX // cell.perimeter (Double)
        );
        return tableDefinition;
    }
}
