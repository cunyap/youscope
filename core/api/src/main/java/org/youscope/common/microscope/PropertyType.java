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
package org.youscope.common.microscope;

/**
 * The type of a property.
 * 
 * @author Moritz Lang
 */
public enum PropertyType
{
	/**
	 * Read only property type.
	 */
	PROPERTY_READ_ONLY,
	/**
	 * String type.
	 */
	PROPERTY_STRING,
	/**
	 * Floating point number.
	 */
	PROPERTY_FLOAT,
	/**
	 * Integer.
	 */
	PROPERTY_INTEGER,
	/**
	 * Property type where value is selectable from a list of allowed values.
	 */
	PROPERTY_SELECTABLE
}
