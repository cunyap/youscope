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
package org.youscope.common.measurement;

import org.youscope.common.ComponentException;

/**
 * Exception thrown by a measurement to indicate that its initialization, execution or deinitialization failed.
 * @author Moritz Lang
 * 
 */
public class MeasurementException extends ComponentException
{
	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= 5219435175494966494L;

	/**
	 * Constructor.
	 * 
	 * @param description Description of the error.
	 */
	public MeasurementException(String description)
	{
		super(description);
	}

	/**
	 * Constructor.
	 * 
	 * @param parent Parent exception.
	 */
	public MeasurementException(Throwable parent)
	{
		super(parent);
	}

	/**
	 * Constructor.
	 * 
	 * @param description Description of the error.
	 * @param parent Parent exception.
	 */
	public MeasurementException(String description, Throwable parent)
	{
		super(description, parent);
	}
}
