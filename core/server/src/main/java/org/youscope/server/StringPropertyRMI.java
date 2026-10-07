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
/**
 * 
 */
package org.youscope.server;

import java.rmi.RemoteException;

import org.youscope.addon.microscopeaccess.StringPropertyInternal;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.StringProperty;

/**
 * @author Moritz Lang
 * 
 */
class StringPropertyRMI extends PropertyRMI implements StringProperty
{
	/**
	 * Serial Version UID.
	 */
	private static final long				serialVersionUID	= 2756352088850819009L;
	private final StringPropertyInternal	stringProperty;

	/**
	 * Constructor.
	 * @throws RemoteException
	 */
	StringPropertyRMI(StringPropertyInternal stringProperty, int accessID) throws RemoteException
	{
		super(stringProperty, accessID);
		this.stringProperty = stringProperty;
	}

	@Override
	public void setValue(String value) throws RemoteException, MicroscopeException, MicroscopeLockedException, InterruptedException
	{
		stringProperty.setValue(value, accessID);
	}
}
