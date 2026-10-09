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
package org.youscope.plugin.travelingsalesman.blossom;

/**
 * Exception thrown in blossom algorithm.
 * @author Moritz Lang
 *
 */
public class BlossomException extends Exception {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -885010991662839919L;
	/**
	 * Constructor.
	 * @param description error description.
	 */
	public BlossomException(String description) {
		super(description);
	}
	/**
	 * Constructor.
	 * @param description error description.
	 * @param cause Cause of the exception.
	 */
	public BlossomException(String description, Throwable cause) {
		super(description, cause);
	}
}
