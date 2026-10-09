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
package org.youscope.common.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allowed range of an integer property.
 * @author Moritz Lang
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface YSConfigIntegerRange {
	/**
	 * Minimal value allowed for an integer property.
	 * 
	 * @return minimal allowed value.
	 */
	public int minValue() default Integer.MIN_VALUE;

	/**
	 * Maximal value allowed for an integer property.
	 * 
	 * @return maximal allowed value.
	 */
	public int maxValue() default Integer.MAX_VALUE;

	/**
	 * Null String used to indicate that no min/max value function is given.
	 */
	public static final String NULL = "";

	/**
	 * Name of a function taking no arguments and returning an int. This
	 * function is used during runtime to determine the minimal allowed value.
	 * If set, overwrites anything set in minValue.
	 * 
	 * @return Name of the function to determine minimal value.
	 */
	public String minValueFunction() default NULL;

	/**
	 * Name of a function taking no arguments and returning an int. This
	 * function is used during runtime to determine the maximal allowed value.
	 * If set, overwrites anything set in maxValue.
	 * 
	 * @return Name of the function to determine maximal value.
	 */
	public String maxValueFunction() default NULL;
}
