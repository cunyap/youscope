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

package org.youscope.plugin.argolight;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.addon.measurement.MeasurementAddonUIAdapter;
import org.youscope.addon.measurement.pages.MetadataPage;
import org.youscope.addon.measurement.pages.StartAndEndSettingsPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.measurement.Measurement;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * Wizard for the Argolight calibration measurement. Same look and flow as the
 * microplate measurement wizard, specialised for the Argolight pattern map:
 *
 * <ol>
 * <li>Measurement metadata.</li>
 * <li>Measurement properties (executions, iteration, FAIR export, TIFF
 * saving).</li>
 * <li>Argolight pattern selection (icon map, click to select).</li>
 * <li>Pattern position configuration (live-view fine tuning).</li>
 * <li>Start/end device settings.</li>
 * <li>Imaging protocol per pattern (pre-filled z-stack / imaging jobs).</li>
 * </ol>
 *
 * @author Andreas P. Cuny
 */
class ArgolightCalibrationMeasurementAddonUI extends MeasurementAddonUIAdapter<ArgolightCalibrationConfiguration> {
	ArgolightCalibrationMeasurementAddonUI(YouScopeClient client, YouScopeServer server) throws AddonException {
		super(getMetadata(), client, server);

		setTitle("Argolight Calibration");

		addPage(new MetadataPage<ArgolightCalibrationConfiguration>(client));
		addPage(new ArgolightGeneralSettingsPage(client));
		addPage(new ArgolightPatternSelectionPage(client, server));
		addPage(new ArgolightPositionPage(client, server));
		addPage(new StartAndEndSettingsPage(client, server));
		addPage(new ArgolightImagingProtocolPage(client, server));
	}

	static ComponentMetadataAdapter<ArgolightCalibrationConfiguration> getMetadata() {
		return new ComponentMetadataAdapter<ArgolightCalibrationConfiguration>(
				ArgolightCalibrationConfiguration.TYPE_IDENTIFIER,
				ArgolightCalibrationConfiguration.class,
				Measurement.class,
				"Argolight Calibration",
				new String[] { "Calibration" },
				"Automates image acquisition on an Argolight calibration slide. Presents the slide's test patterns (central cross, target, field of rings, intensity gradations, gradually spaced lines, matrix of crosses, meridians of a sphere and the 3D crossing stairs) as selectable positions, pre-fills a z-stack for the 3D/volumetric patterns and a simple imaging job for the rest, then iterates through them acquiring images (TIFF, with optional OME-TIFF FAIR export afterwards).",
				"icons/argolight-icon.png");
	}
}
