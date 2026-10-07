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
package org.youscope.serverinterfaces;


import javax.script.ScriptEngineManager;

import org.youscope.common.MessageListener;
import org.youscope.common.callback.CallbackProvider;
import org.youscope.common.saving.MeasurementSaver;

/**
 * Interface handed over to addons during the construction/initialization of a measurement and its components. With this interface it is possible for
 * an addon to create sub-components by only knowing their type identifiers ({@link #getComponentProvider()}), to configure how data (e.g. images) produced
 * by the component should be saved ({@link #getMeasurementSaver()}, load script engines ({@link #getScriptEngineManager()}, or to initialize callbacks with the
 * client {@link #getCallbackProvider()}. 
 * @author Moritz Lang
 * 
 */
public interface ConstructionContext
{
	/**
	 * Returns functionality to save images, tables and similar produced by a measurement component to hard disk.
	 * @return Provider for saving measurement data.
	 */
	MeasurementSaver getMeasurementSaver();
	
	/**
	 * Returns a provider with which measurement components, such as jobs and resources, can be created given the corresponding configurations.
	 * @return Provider to create components.
	 */
	ComponentProvider getComponentProvider(); 
	
	/**
	 * Returns a provider with which script engines can be created.
	 * @return The script engine provider of the server.
	 */
	ScriptEngineManager getScriptEngineManager();
	
	/**
	 * Returns a message listener which can be used for logging/displaying short status messages or errors similar to System.out and System.err.
	 * @return message listener for logging.
	 */
	MessageListener getLogger();
	
	/**
	 * Returns a provider with which callbacks to the client can be obtained.
	 * @return callback provider.
	 */
	CallbackProvider getCallbackProvider();
}
