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
package org.youscope.addon.dropletmicrofluidics;

import org.youscope.addon.component.ComponentAddonUI;

/**
 * Interface for addons which allow the configuration of droplet based microfluidic controllers.
 * @author Moritz Lang 
 * @param <C> The configuration class to edit.
 */
public interface DropletControllerConfigurationAddon<C extends DropletControllerConfiguration> extends ComponentAddonUI<C>
{
    /**
     * Sets the IDs (zero based) of the syringes which are connected to the chip controlled by this controller.
     * @param connectedSyringes IDs of connected syringes.
     */
    public void setConnectedSyringes(int[] connectedSyringes);
}
