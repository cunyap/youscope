/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.addon.celldetection;

/** Thrown when quantification fails. */
public class QuantificationException extends Exception {
	private static final long serialVersionUID = 7710134131654146648L;

	public QuantificationException(String description) {
		super(description);
	}

	public QuantificationException(String description, Throwable cause) {
		super(description, cause);
	}

	public QuantificationException(Throwable cause) {
		super(cause);
	}
}
