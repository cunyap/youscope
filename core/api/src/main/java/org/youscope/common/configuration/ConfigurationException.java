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
package org.youscope.common.configuration;

/**
 * Thrown by a {@link Configuration} to indicate that its current setting is invalid.
 * @author Moritz Lang
 */
public class ConfigurationException extends Exception
{

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -6409165571340277838L;

    /**
     * Constructor.
     * 
     * @param description
     */
    public ConfigurationException(String description)
    {
        super(description);
    }

    /**
     * Constructor.
     * 
     * @param parent
     */
    public ConfigurationException(Throwable parent)
    {
        super(parent);
    }

    /**
     * Constructor.
     * 
     * @param description
     * @param parent
     */
    public ConfigurationException(String description, Throwable parent)
    {
        super(description, parent);
    }
}
