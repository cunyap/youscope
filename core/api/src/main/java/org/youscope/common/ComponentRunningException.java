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
package org.youscope.common;

/**
 * Exception thrown if trying to change the configuration of a {@link Component} while it is executed.
 * 
 * @author Moritz Lang
 */
public class ComponentRunningException extends ComponentException
{

	/**
	 * Serial version UID.
	 */
	private static final long	serialVersionUID	= -3628890068985604340L;

	/**
	 * Constructor.
	 */
	public ComponentRunningException()
	{
		this("Measurement component cannot be modified while component is executed.");
	}
	/**
	 * Constructor.
	 * @param message Error message.
	 */
	public ComponentRunningException(String message)
	{
		super(message);
	}
}
