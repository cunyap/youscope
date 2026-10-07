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
package org.youscope.common.callback;

import org.youscope.common.ComponentException;

/**
 * Exception thrown by measurement callbacks.
 * @author Moritz Lang
 * 
 */
public class CallbackException extends ComponentException
{
	/**
	 * Serial version UID.
	 */
	private static final long	serialVersionUID				= -2814438972668943211L;

	/**
	 * Constructor.
	 * 
	 * @param description Human readable description of the exception.
	 */
	public CallbackException(String description)
	{
		super(description);
	}


	/**
	 * Constructor.
	 * 
	 * @param description Human readable description of the exception.
	 * @param cause the parent exception.
	 */
	public CallbackException(String description, Throwable cause)
	{
		super(description, cause);
	}
}
