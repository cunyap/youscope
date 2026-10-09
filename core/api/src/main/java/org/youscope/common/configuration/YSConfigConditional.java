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
 * Marks a conditional configuration property. Conditional properties are properties only "making sense" (=having any effect), if
 * certain conditions are fulfilled. 
 * There should exists a no-args function in the respective configuration class returning true if these conditions are fulfilled,
 * and otherwise false. This annotation should return the name of this function.
 * @author Moritz Lang
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface YSConfigConditional 
{
	/**
	 * Name of the function (no args) returning a boolean value indicating if the property has any effect.
	 * 
	 * @return Name of function.
	 */
	String value();
}
