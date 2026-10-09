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
package org.youscope.addon.dropletmicrofluidics;

import java.rmi.RemoteException;

import org.youscope.common.ExecutionInformation;
import org.youscope.common.MeasurementContext;
import org.youscope.common.resource.Resource;
import org.youscope.common.resource.ResourceException;

/**
 * Abstract class all controllers for droplet based microfluidics should implement. 
 * @author Moritz Lang
 *
 */
public interface DropletControllerResource  extends Resource 
{
	/**
	 * Runs the controller and returns the flow rates of the flow units (i.e. the controller's output).
	 * @param executionInformation Current execution information.
	 * @param meanDropletOffset Mean height offset of the droplet (as obtained by observer).
	 * @param measurementContext The Measurement context.
	 * @param microfluidicChipID The id of the microfluidic chip whose droplets are controlled by this controller. Controllers for different chips have to have independent states, i.e. not influence one another.
	 * @return Flow rate of the flow units.
	 * @throws ResourceException
	 * @throws RemoteException
	 */
	public DropletControllerResult runController(ExecutionInformation executionInformation, MeasurementContext measurementContext, double meanDropletOffset, int microfluidicChipID) throws ResourceException, RemoteException;	
}
