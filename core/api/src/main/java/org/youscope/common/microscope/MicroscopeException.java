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
package org.youscope.common.microscope;

/**
 * Class representing an exception which occurred in the execution of some microscope tasks.
 * 
 * @author Moritz Lang
 */
public class MicroscopeException extends Exception
{
    /**
     * Version UID.
     */
    private static final long serialVersionUID = 4055279582579151582L;

    /**
     * Constructor.
     * 
     * @param description Human readable description of the exception.
     */
    public MicroscopeException(String description)
    {
        super(description);
    }

    /**
     * Constructor.
     * 
     * @param description Human readable description of the exception.
     * @param cause The cause of the exception.
     */
    public MicroscopeException(String description, Exception cause)
    {
        super(description, cause);
    }
}
