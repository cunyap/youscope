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
package org.youscope.addon.component;

import org.youscope.addon.AddonMetadata;
import org.youscope.common.Component;
import org.youscope.common.configuration.Configuration;

/**
 * Provides metadata about a given measurement component.
 * @author Moritz Lang
 * @param <C> The class of configuration of this measurement component.
 *
 */
public interface ComponentMetadata<C extends Configuration>  extends AddonMetadata
{
    /**
     * Returns the class of the configuration of the measurement component.
     * The class should have a public no-argument constructor, such that newInstance() can be called. 
     * @return Configuration class of component.
     */
    Class<C> getConfigurationClass();
	
	/**
     * Returns the interface of the measurement component created when the configuration is parsed. 
     * This should not be the implementation of the component, but rather
     * a public interface implementing Remote.
     * @return Component interface.
     */
    Class<? extends Component> getComponentInterface();
}
