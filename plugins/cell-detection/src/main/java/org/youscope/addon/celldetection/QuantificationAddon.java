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
package org.youscope.addon.celldetection;

import java.rmi.RemoteException;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.Resource;

/**
 * A quantification addon measures per-cell features from a segmentation label
 * mask
 * (produced by ANY segmenter) plus optional intensity channels, and returns an
 * enriched {@link CellDetectionResult} whose table = core geometry + the
 * quantifier's declared feature columns.
 */
public interface QuantificationAddon extends Resource {
	/**
	 * @param detectionResult      segmentation output (must carry a label image).
	 * @param quantificationImages intensity channels to measure (may be empty).
	 * @return enriched result (geometry + feature columns) with the same label
	 *         image.
	 */
	CellDetectionResult quantify(CellDetectionResult detectionResult, ImageEvent<?>[] quantificationImages)
			throws QuantificationException, RemoteException;
}
