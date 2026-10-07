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
package org.youscope.addon.microplate;

import java.rmi.RemoteException;

import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.resource.Resource;
import org.youscope.common.resource.ResourceException;

/**
 * Configuration of the tile/well layout defining where images should be taken. Typically, represents a microplate.
 * @author Moritz Lang
 *
 */
public interface MicroplateResource extends Resource 
{    
    /**
     * Returns the layout of the microplate.
     * @return Layout of microplate.
     * @throws ResourceException
     * @throws RemoteException
     */
	MicroplateLayout getMicroplateLayout() throws ResourceException, RemoteException;
}
