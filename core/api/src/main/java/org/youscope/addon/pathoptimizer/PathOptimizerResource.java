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
/**
 * 
 */
package org.youscope.addon.pathoptimizer;

import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import org.youscope.common.PositionInformation;
import org.youscope.common.resource.Resource;
import org.youscope.common.resource.ResourceException;

/**
 * A path optimizer is a method to optimize the path of a microplate measurement, such that the distances between the measured
 * wells/ positions is minimal.
 * @author Moritz Lang
 *
 */
public interface PathOptimizerResource extends Resource
{
	/**
	 * Returns the optimized path for a given set of positions.
	 * @param positions Positions to calculate optimal path of.
	 * @return optimized path.
	 * @throws ResourceException 
	 * @throws RemoteException 
	 */
	public List<PositionInformation> getPath(Map<PositionInformation, ? extends Point2D.Double> positions) throws ResourceException, RemoteException;
}
