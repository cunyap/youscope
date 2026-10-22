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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.instanseg;

import org.youscope.addon.AddonException;
import org.youscope.addon.celldetection.CellDetectionAddon;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.saving.MeasurementSaver;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * YouScope addon factory for the InstanSeg cell-detection plugin.
 *
 * <p>
 * Uses a {@link CustomAddonCreator} (same pattern as the Cellpose plugin) so
 * the
 * addon can be handed the measurement's {@link MeasurementSaver}. That lets it
 * write its reproducibility outputs (environment snapshot, pip requirements)
 * next to the images and {@code cell-table.csv}.
 *
 * <p>
 * Registered under the {@link CellDetectionAddon} interface, so lookup by the
 * cell-detection job/measurement works unchanged.
 *
 * @author Andreas P. Cuny
 */
public class InstanSegAddonFactory extends ComponentAddonFactoryAdapter {
    private static final CustomAddonCreator<InstanSegConfiguration, CellDetectionAddon> CREATOR = new CustomAddonCreator<InstanSegConfiguration, CellDetectionAddon>() {
        @Override
        public CellDetectionAddon createCustom(PositionInformation positionInformation,
                InstanSegConfiguration configuration, ConstructionContext constructionContext)
                throws ConfigurationException, AddonException {
            MeasurementSaver saver = null;
            try {
                // May be null / unavailable on some construction paths; the addon
                // falls back to its working dir when so.
                saver = constructionContext.getMeasurementSaver();
            } catch (@SuppressWarnings("unused") Throwable t) {
                saver = null;
            }
            try {
                return new InstanSegAddon(positionInformation, configuration, saver);
            } catch (Exception e) {
                throw new AddonException("Could not create InstanSeg detection addon.", e);
            }
        }

        @Override
        public Class<CellDetectionAddon> getComponentInterface() {
            return CellDetectionAddon.class;
        }
    };

    /** Called by YouScope's plugin loader at server startup. */
    public InstanSegAddonFactory() {
        super(InstanSegConfigurationAddon.class, CREATOR,
                InstanSegConfigurationAddon.getMetadata());
    }
}
