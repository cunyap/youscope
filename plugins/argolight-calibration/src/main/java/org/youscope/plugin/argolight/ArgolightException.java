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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/

package org.youscope.plugin.argolight;

/**
 * Exception thrown when an Argolight plate definition cannot be loaded or
 * parsed.
 *
 * @author Andreas P. Cuny
 */
public class ArgolightException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * @param message error message.
	 */
	public ArgolightException(String message) {
		super(message);
	}

	/**
	 * @param message error message.
	 * @param cause   underlying cause.
	 */
	public ArgolightException(String message, Throwable cause) {
		super(message, cause);
	}
}
