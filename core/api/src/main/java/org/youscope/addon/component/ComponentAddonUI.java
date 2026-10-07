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

import org.youscope.addon.AddonException;
import org.youscope.addon.AddonUI;
import org.youscope.common.configuration.Configuration;
import org.youscope.common.configuration.ConfigurationException;

/**
 * An addon providing a user interface for the configuration of a component.
 * @author Moritz Lang
 *
 * @param <C>
 */
public interface ComponentAddonUI<C extends Configuration> extends AddonUI<ComponentMetadata<C>>
{
    /**
     * Initializes the addon to the configuration data.
     * Must not be called after toXXXFrame() or toPanel() was called.
     * 
     * @param configuration The configuration data.
     * @throws AddonException Thrown if error occurred while processing configuration data.
     * @throws ConfigurationException Thrown if configuration is invalid.
     */
    void setConfiguration(Configuration configuration) throws AddonException, ConfigurationException;

    /**
     * Returns the configuration data. If toXXXFrame() or toPanel() was called already, it is expected that the addon
     * commits all current edits to the configuration. Otherwise, a default configuration should be returned. If possible, the configuration
     * should be at a valid state, which is however not necessary (see {@link Configuration#checkConfiguration()}).
     * 
     * @return Configuration data or NULL, if addon was not yet configured.
     */
    C getConfiguration();
    
    /**
     * Adds a listener to this configuration, which should e.g. be informed if the configuration finished.
     * @param listener The listener to add.
     */
    void addUIListener(ComponentAddonUIListener<? super C> listener);
    
    /**
     * Removes a previously added listener.
     * @param listener The listener to remove.
     */
    void removeUIListener(ComponentAddonUIListener<? super C> listener);
	
    @Override
	ComponentMetadata<C> getAddonMetadata();
}
