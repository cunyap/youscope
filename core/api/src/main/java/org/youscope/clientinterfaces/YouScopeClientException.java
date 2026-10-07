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
package org.youscope.clientinterfaces;

/**
 * Exception thrown by the {@link YouScopeClient} to indicate that a requested operation failed due to an exception on the client side.
 * @author Moritz Lang
 */
public class YouScopeClientException extends Exception { 

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -6285854028085158122L;

	/**
	 * Constructor.
	 * @param message The error message.
	 */
	public YouScopeClientException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * @param message the error message.
	 * @param cause The more detailed cause of the exception
	 */
	public YouScopeClientException(String message, Throwable cause) {
		super(message, cause);
	}
}
