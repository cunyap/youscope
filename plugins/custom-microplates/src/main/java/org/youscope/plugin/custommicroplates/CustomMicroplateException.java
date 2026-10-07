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
package org.youscope.plugin.custommicroplates;

/**
 * Exception thrown to indicate that initializing a custom microplate, creating or deleting it, or similar, failed.
 * @author Moritz Lang
 * 
 */
public class CustomMicroplateException extends Exception
{

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 1834665729033073553L;

	/**
	 * Constructor.
	 * 
	 * @param description
	 */
	public CustomMicroplateException(String description)
	{
		super(description);
	}

	/**
	 * Constructor.
	 * 
	 * @param parent
	 */
	public CustomMicroplateException(Throwable parent)
	{
		super(parent);
	}

	/**
	 * Constructor.
	 * 
	 * @param description
	 * @param parent
	 */
	public CustomMicroplateException(String description, Throwable parent)
	{
		super(description, parent);
	}
}
