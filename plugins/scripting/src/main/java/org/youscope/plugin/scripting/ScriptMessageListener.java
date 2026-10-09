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
package org.youscope.plugin.scripting;

import java.util.EventListener;

/**
 * @author langmo
 *
 */
interface ScriptMessageListener extends EventListener
{
	/**
	 * Executed if engine produced a text message due to the execution of one or more lines of code.
	 * @param message Message the engine did send.
	 */
	public void outputMessage(String message);
	
	/**
	 * Executed if code was send to the engine to be executed.
	 * @param message Code which will be executed.
	 */
	public void inputMessage(String message);
}
