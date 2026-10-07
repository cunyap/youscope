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
package org.youscope.plugin.scripting;

import java.util.EventListener;
import java.util.Set;
import java.util.Map.Entry;

/**
 * @author langmo
 *
 */
interface ScriptVariablesListener extends EventListener
{
	/**
	 * Event executed if the variables defined in the script engine might have changed, e.g. due to the execution of one or
	 * more lines of script.
	 * @param variables The variables defined in the script engine.
	 */
	public void variablesChanged(Set<Entry<String, Object>> variables);
}
