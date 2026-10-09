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
package org.youscope.server;

import java.rmi.RemoteException;

import org.youscope.addon.microscopeaccess.ReadOnlyPropertyInternal;
import org.youscope.common.microscope.ReadOnlyProperty;

/**
 * @author Moritz Lang
 * 
 */
class ReadOnlyPropertyRMI extends PropertyRMI implements ReadOnlyProperty
{

	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= 3130755396734654776L;

	/**
	 * Constructor.
	 * @throws RemoteException
	 */
	ReadOnlyPropertyRMI(ReadOnlyPropertyInternal readOnlyProperty, int accessID) throws RemoteException
	{
		super(readOnlyProperty, accessID);
	}
}
