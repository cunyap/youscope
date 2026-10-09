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
package org.youscope.addon.analysis;

import java.util.ArrayList;
import java.util.List;

import org.youscope.common.table.ColumnDefinition;
import org.youscope.common.table.TableDefinition;

/**
 * Utility for building table definitions from quantifier columns.
 */
public final class QuantifierTables {

    private QuantifierTables() {
        // utility class
    }

    /**
     * Builds a {@link TableDefinition} from a set of fixed geometry columns
     * (contributed by the segmenter) followed by a quantifier's declared columns.
     *
     * @param tableId          table type id (e.g. "YouScope.CellDetection").
     * @param tableDescription human-readable description.
     * @param geometry         fixed leading columns (cell id, x, y, area, ...); may
     *                         be null.
     * @param quantColumns     the quantifier's declared columns; may be null/empty.
     */
    public static TableDefinition composeTable(String tableId, String tableDescription,
            ColumnDefinition<?>[] geometry, List<Quantifier.Column> quantColumns) {
        List<ColumnDefinition<?>> cols = new ArrayList<ColumnDefinition<?>>();
        if (geometry != null) {
            for (ColumnDefinition<?> g : geometry) {
                cols.add(g);
            }
        }
        if (quantColumns != null) {
            for (Quantifier.Column c : quantColumns) {
                if ("int".equals(c.dtype)) {
                    cols.add(ColumnDefinition.createIntegerColumnDefinition(c.name, c.description, true));
                } else if ("string".equals(c.dtype)) {
                    cols.add(ColumnDefinition.createStringColumnDefinition(c.name, c.description, true));
                } else {
                    cols.add(ColumnDefinition.createDoubleColumnDefinition(c.name, c.description, true));
                }
            }
        }
        return new TableDefinition(tableId, tableDescription, cols.toArray(new ColumnDefinition<?>[0]));
    }
}