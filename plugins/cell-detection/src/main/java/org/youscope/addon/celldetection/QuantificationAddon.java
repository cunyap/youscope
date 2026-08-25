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
