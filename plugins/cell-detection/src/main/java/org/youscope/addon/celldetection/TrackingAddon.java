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
import org.youscope.common.resource.Resource;

/**
 * A tracking addon links cells across successive frames, assigning a persistent
 * track id to each cell. It is STATEFUL across calls within a measurement (it
 * remembers the previous frame); {@link #resetTracks()} clears that state.
 * It returns an enriched {@link CellDetectionResult} whose table has an added
 * {@code cell.track.id} column.
 */
public interface TrackingAddon extends Resource {
	/** Links the cells of this frame to the previous one; adds cell.track.id. */
	CellDetectionResult track(CellDetectionResult detectionResult) throws TrackingException, RemoteException;

	/**
	 * Clears the remembered previous frame (call at the start of a measurement).
	 */
	void resetTracks() throws RemoteException;
}
