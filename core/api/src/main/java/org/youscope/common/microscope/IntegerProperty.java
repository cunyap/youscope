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
package org.youscope.common.microscope;

import java.rmi.RemoteException;

/**
 * @author Moritz Lang
 * 
 */
public interface IntegerProperty extends Property
{
	/**
	 * Returns the current value of the device property as an integer.
	 * @return Value of property.
	 * @throws MicroscopeException
	 * @throws NumberFormatException Thrown when value is not an integer.
	 * @throws InterruptedException
	 * @throws RemoteException
	 */
	public int getIntegerValue() throws MicroscopeException, NumberFormatException, InterruptedException, RemoteException;

	/**
	 * Sets the current value of the property.
	 * @param value Value to set the property to.
	 * @throws MicroscopeException
	 * @throws MicroscopeLockedException
	 * @throws InterruptedException
	 * @throws RemoteException
	 */
	public void setValue(int value) throws MicroscopeException, MicroscopeLockedException, InterruptedException, RemoteException;

	/**
	 * Sets the value of the microscope relative to its current value. Throws a NumberFormatException if the current value does not correspond to an integer value.
	 * @param offset Amount for which the current value should be changed.
	 * @throws MicroscopeException
	 * @throws MicroscopeLockedException
	 * @throws NumberFormatException Thrown when current value is not an integer.
	 * @throws InterruptedException
	 * @throws RemoteException
	 */
	public void setValueRelative(int offset) throws MicroscopeException, MicroscopeLockedException, NumberFormatException, InterruptedException, RemoteException;

	/**
	 * Returns the lower limit of this value.
	 * Returns Integer.MIN_VALUE if this property does not have limits.
	 * @return Minimal value this property can have.
	 * @throws RemoteException
	 */
	int getLowerLimit() throws RemoteException;

	/**
	 * Returns the upper limit of this value.
	 * Returns Integer.MAX_VALUE if this property does not have limits.
	 * @return Maximal value this property can have.
	 * @throws RemoteException
	 */
	int getUpperLimit() throws RemoteException;
}
