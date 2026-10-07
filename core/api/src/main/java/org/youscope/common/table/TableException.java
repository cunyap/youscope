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
package org.youscope.common.table;

/**
 * Exception thrown by table entries.
 * @author Moritz Lang
 */
public class TableException extends Exception
{
    /**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -1654663380102330692L;

	/**
     * Constructor.
     * 
     * @param description Description of the exception.
     */
    public TableException(String description)
    {
        super(description);
    }

    /**
     * Constructor.
     * 
     * @param parent Parent exception.
     */
    public TableException(Throwable parent)
    {
        super(parent);
    }

    /**
     * Constructor.
     * 
     * @param description Description of the exception.
     * @param parent Parent exception.
     */
    public TableException(String description, Throwable parent)
    {
        super(description, parent);
    }
}
