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
