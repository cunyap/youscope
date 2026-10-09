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
package org.youscope.addon.dropletmicrofluidics;

import org.youscope.common.resource.ResourceConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration of an observer for droplet based microfluidics.
 * @author Moritz Lang
 *
 */
@XStreamAlias("droplet-observer-configuration")
public abstract class DropletObserverConfiguration  extends ResourceConfiguration
{

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 6268977101312936654L;

	

}
