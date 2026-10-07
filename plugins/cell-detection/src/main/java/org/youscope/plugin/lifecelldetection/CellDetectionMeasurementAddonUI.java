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
package org.youscope.plugin.lifecelldetection;


import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.addon.measurement.MeasurementAddonUIAdapter;
import org.youscope.addon.measurement.pages.GeneralSettingsPage;
import org.youscope.addon.measurement.pages.MetadataPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.common.measurement.Measurement;

/**
 * @author Moritz Lang
 */
class CellDetectionMeasurementAddonUI extends MeasurementAddonUIAdapter<CellDetectionMeasurementConfiguration>
{
	/**
	 * Constructor.
	 * @param microscope Interface to the microscope.
	 * @param client Interface to the client.
	 * @throws AddonException 
	 */
	CellDetectionMeasurementAddonUI(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
		
		setTitle("Life Cell Detection Measurement");
		addPage(new MetadataPage<>(client));
		addPage(new GeneralSettingsPage<CellDetectionMeasurementConfiguration>(client, CellDetectionMeasurementConfiguration.class));
		addPage(new StartAndEndConfigurationPage(client, server));
		addPage(new ImagingConfigurationPage(client, server));
	}
	
	static ComponentMetadataAdapter<CellDetectionMeasurementConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<CellDetectionMeasurementConfiguration>(CellDetectionMeasurementConfiguration.TYPE_IDENTIFIER, 
				CellDetectionMeasurementConfiguration.class, 
				Measurement.class, "Cell-Detection", new String[]{"misc"},
				"A measurement providing several algorithms to segment microscopy images, and to extract information on the detected cells.", "icons/smiley-mr-green.png");
	}
}
