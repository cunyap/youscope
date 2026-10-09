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
package org.youscope.addon.component;

/**
 * Exception thrown if creation of a measurement component failed
 * @author Moritz Lang
 *
 */
public class ComponentCreationException extends Exception {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 7512230465385081443L;

	/**
	 * Constructor.
	 * @param message The error message.
	 */
	public ComponentCreationException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * @param message the error message.
	 * @param cause The more detailed cause of the exception
	 */
	public ComponentCreationException(String message, Throwable cause) {
		super(message, cause);
	}

}
