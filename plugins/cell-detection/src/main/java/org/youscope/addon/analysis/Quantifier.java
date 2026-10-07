/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
 * The quantification STAGE of the YouScope analysis pipeline.
 *
 * <p>
 * A {@link Quantifier} measures per-cell features from a label mask (produced
 * by
 * ANY segmenter) plus zero or more intensity channels. Crucially, a quantifier
 * <b>declares its own columns</b> ({@link QuantResult#columns}), so the result
 * table is composed dynamically rather than hard-coded. This lets
 * built-in intensity, a CellProfiler-style quantifier, and a CellXMask port all
 * coexist behind one interface.
 *
 * <p>
 * Implementations may run <b>in-process</b> (pure Java, e.g. a CellXMask port;
 * no IPC, ideal for cheap per-frame quantification) or <b>out-of-process</b>
 * (e.g. {@code BackendQuantifier} talking to a Python quantify server). The
 * core
 * composes them identically.
 */
public interface Quantifier {
	/** A declared output column. */
	final class Column {
		public final String name; // e.g. "intensity.mean.ch0"
		public final String dtype; // "double" | "int" | "string"
		public final String description;

		public Column(String name, String dtype, String description) {
			this.name = name;
			this.dtype = dtype;
			this.description = description;
		}
	}

	/** Result: declared columns + per-cell values keyed by cell id. */
	final class QuantResult {
		public final List<Column> columns;
		public final int[] cellIds; // length N
		public final double[][] values; // N x columns.size(); ints/strings encoded by caller convention

		public QuantResult(List<Column> columns, int[] cellIds, double[][] values) {
			this.columns = columns;
			this.cellIds = cellIds;
			this.values = values;
		}
	}

	/**
	 * @param labelMask row-major (height*width) label ids, 0 = background.
	 * @param width     image width.
	 * @param height    image height.
	 * @param channels  intensity channels, each row-major float, same size as the
	 *                  mask.
	 * @param params    quantifier-specific parameters.
	 * @return declared columns + per-cell values.
	 */
	QuantResult quantify(int[] labelMask, int width, int height,
			List<float[]> channels, java.util.Map<String, String> params) throws Exception;

	/** Stable identifier, e.g. "intensity", "cellprofiler", "cellxmask". */
	String name();

}
