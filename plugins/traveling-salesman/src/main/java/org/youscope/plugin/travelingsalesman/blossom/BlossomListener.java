/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
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
package org.youscope.plugin.travelingsalesman.blossom;

import java.util.EventListener;

/**
 * A listener which gets informed about the individual steps of the blossom algorithm.
 * @author Moritz Lang
 *
 */
public interface BlossomListener extends EventListener 
{
	/**
	 * Gets invoked if primal update was performed.
	 * @param type Type of primal update performed.
	 */
	public void primalUpdate(PrimalUpdateType type);
	
	/**
	 * Gets invoked if dual update was performed.
	 * @param dualChange Change of the dual variables in current tree.
	 */
	public void dualUpdate(double dualChange);
}
