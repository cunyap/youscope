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

/**
 * @author langmo
 */
public class MicroscopeLockedException extends Exception
{

	/**
	 * Serial version UID.
	 */
	private static final long	serialVersionUID	= -7193119368940382198L;

	/**
	 * Default constructor, indicating that the microscope is locked.
	 */
	public MicroscopeLockedException()
	{
		super("Microscope is locked and, thus, its state can not be changed (however, reading its state is allowed).");
	}

	/**
	 * Constructor.
	 * @param message Human readable message.
	 */
	public MicroscopeLockedException(String message)
	{
		super(message);
	}
}
