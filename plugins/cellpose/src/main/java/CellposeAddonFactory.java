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
package org.youscope.plugin.cellpose;

import org.youscope.addon.AddonException;
import org.youscope.addon.celldetection.CellDetectionAddon;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.saving.MeasurementSaver;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * YouScope addon factory for the Cellpose 4 cell-detection plugin.
 *
 * <p>
 * Uses a {@link CustomAddonCreator} (same pattern as
 * {@code CellDetectionJobAddonFactory}) so the addon can be handed the
 * measurement's {@link MeasurementSaver} from the {@link ConstructionContext}.
 * That saver is the reliable route to the measurement results folder, letting
 * the addon write its reproducibility outputs (environment snapshot,
 * requirements,
 * {@code cellpose_last_input.tif}, {@code cellpose_server.log}) next to the
 * images
 * and {@code cell-table.csv} instead of into the YouScope program directory.
 *
 * <p>
 * The component is registered under the {@link CellDetectionAddon} interface,
 * so lookup by the cell-detection job/measurement is unchanged.
 *
 * @author Andreas P. Cuny
 */
public class CellposeAddonFactory extends ComponentAddonFactoryAdapter {
    private static final CustomAddonCreator<CellposeConfiguration, CellDetectionAddon> CREATOR = new CustomAddonCreator<CellposeConfiguration, CellDetectionAddon>() {
        @Override
        public CellDetectionAddon createCustom(PositionInformation positionInformation,
                CellposeConfiguration configuration, ConstructionContext constructionContext)
                throws ConfigurationException, AddonException {
            MeasurementSaver saver = null;
            try {
                // May be null / unavailable on some construction paths; the addon
                // falls back to its working dir when so.
                saver = constructionContext.getMeasurementSaver();
            } catch (Throwable t) {
                saver = null;
            }
            try {
                return new CellposeAddon(positionInformation, configuration, saver);
            } catch (Exception e) {
                throw new AddonException("Could not create Cellpose detection addon.", e);
            }
        }

        @Override
        public Class<CellDetectionAddon> getComponentInterface() {
            return CellDetectionAddon.class;
        }
    };

    /**
     * Constructor: Called by YouScope's plugin loader at server startup.
     */
    public CellposeAddonFactory() {
        super(CellposeConfigurationAddon.class, CREATOR,
                CellposeConfigurationAddon.getMetadata());
    }
}
