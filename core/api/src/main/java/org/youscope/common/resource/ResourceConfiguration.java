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
package org.youscope.common.resource;

import org.youscope.common.configuration.Configuration;
import org.youscope.common.configuration.ConfigurationException;

/**
 * Base class of all configurations of measurement resources.
 * @author Moritz Lang
 *
 */
public abstract class ResourceConfiguration implements Configuration 
{

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -7383728961240600655L;

	@Override
	public void checkConfiguration() throws ConfigurationException {
		// do nothing.
		
	}
}
