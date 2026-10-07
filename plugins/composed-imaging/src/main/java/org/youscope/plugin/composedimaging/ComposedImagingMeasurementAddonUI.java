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
package org.youscope.plugin.composedimaging;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.addon.measurement.MeasurementAddonUIAdapter;
import org.youscope.addon.measurement.pages.GeneralSettingsPage;
import org.youscope.addon.measurement.pages.MetadataPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.measurement.Measurement;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * @author Moritz Lang
 */
class ComposedImagingMeasurementAddonUI extends MeasurementAddonUIAdapter<ComposedImagingMeasurementConfiguration>
{
	/**
	 * Constructor.
	 * @param client YouScope client.
	 * @param server YouScope server.
	 * @throws AddonException 
	 */
	ComposedImagingMeasurementAddonUI(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
		
		setTitle("Composed Imaging Measurement");
		addPage(new MetadataPage<>(client));
		addPage(new GeneralSettingsPage<ComposedImagingMeasurementConfiguration>(client, ComposedImagingMeasurementConfiguration.class));
		addPage(new StartAndEndConfigurationPage(client, server));
		addPage(new ImagingConfigurationPage(client, server));
		addPage(new AreaConfigurationPage(client, server));
	}
	
	static ComponentMetadataAdapter<ComposedImagingMeasurementConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<ComposedImagingMeasurementConfiguration>(ComposedImagingMeasurementConfiguration.TYPE_IDENTIFIER, 
				ComposedImagingMeasurementConfiguration.class, 
				Measurement.class, "Composed Imaging Measurement", new String[]{"misc"},
				"Takes partly overlapping images on a rectangular grid, such that the resulting images can be stitched together using a 3rd party algorithm (stitching is not part of the measurement).",
				"icons/layers-group.png");
	}
}
