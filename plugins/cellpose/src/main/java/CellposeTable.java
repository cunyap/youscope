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
package org.youscope.plugin.cellpose;

import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.common.table.TableDefinition;

/**
 * Defines the per-cell table produced by the Cellpose addon.
 *
 * <p>
 * Cellpose emits a label mask + <b>core geometry</b>
 * only, using the shared {@link CellDetectionTableColumns} identifiers
 * ({@code cell.index}, {@code cell.center.x}, {@code cell.center.y},
 * {@code cell.area}, {@code cell.perimeter}). Per-cell <i>intensity</i>
 * is the job of the selected {@code QuantificationAddon} (e.g. the Minimal
 * intensity quantifier), which measures features from this mask and the chosen
 * channels.
 *
 * <p>
 * These are also the identifiers the bundled visualizers rely on:
 * {@code LabelVisualizerAddon} looks up
 * {@code cell.center.x}/{@code cell.center.y}
 * to draw the "Cell N" / "Area" labels, so the Cellpose control image stays
 * labelled.
 *
 * @author Andreas P. Cuny
 */
public class CellposeTable {
    private CellposeTable() {
    }

    private static TableDefinition tableDefinition = null;

    /**
     * Returns the singleton {@link TableDefinition} for Cellpose results.
     * Column ORDER here must match the order values are added in
     * {@code CellposeAddon.buildTable(...)}.
     */
    public static synchronized TableDefinition getTableDefinition() {
        if (tableDefinition != null)
            return tableDefinition;

        tableDefinition = new TableDefinition(
                "Cellpose Cell Detection",
                "Segmentation geometry produced by Cellpose (mask + core geometry). "
                        + "Intensity features are added by the selected quantification plugin.",
                // Shared geometry identifiers only (mask and core geometry)
                CellDetectionTableColumns.TABLE_COLUMN_CELL_ID, // cell.index (Integer)
                CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX, // cell.center.x (Double)
                CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX, // cell.center.y (Double)
                CellDetectionTableColumns.TABLE_COLUMN_AREA_PX, // cell.area (Double)
                CellDetectionTableColumns.TABLE_COLUMN_PERIMETER_PX // cell.perimeter (Double)
        );
        return tableDefinition;
    }
}
