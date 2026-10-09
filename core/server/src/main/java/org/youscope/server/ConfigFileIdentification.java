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
package org.youscope.server;


/**
 * @author langmo
 * 
 */
class ConfigFileIdentification
{
	public final static String					IDENT_GENERATOR			= "Generator";
	public final static String					IDENT_COMPATIBLE		= "Compatible";
	public final static String					IDENT_CORE_VERSION		= "CoreVersion";
	public final static String					IDENT_CORE_API_VERSION	= "CoreAPIVersion";

	public final static ConfigFileGenerator		THIS_GENERATOR			= new ConfigFileGenerator();

	public final static ConfigFileGenerator[]	COMPATIBLE_GENERATORS	= new ConfigFileGenerator[] {new ConfigFileGenerator("YouScope", "1.0"),new ConfigFileGenerator("MicroManager", "1.3"), new ConfigFileGenerator("MicroManager", "1.4"), new ConfigFileGenerator("MicroManager", "2.0.0")};
}
