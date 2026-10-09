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
package org.youscope.serverinterfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Moritz Lang
 */
public interface YouScopeLogin extends Remote
{
    /**
     * When the right password is provided, this interface grants access to the microscope.
     * 
     * @param password The password. Should be the same as the one set in the startup of the server.
     * @return Main interface of the microscope.
     * @throws RemoteException
     * @throws SecurityException Thrown if authentification failed.
     */
    public YouScopeServer login(String password) throws RemoteException, SecurityException;

}
