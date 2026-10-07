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
package org.youscope.plugin.multicameracontinuousimaging;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.addon.measurement.MeasurementAddonUIAdapter;
import org.youscope.addon.measurement.pages.GeneralSettingsPage;
import org.youscope.addon.measurement.pages.MetadataPage;
import org.youscope.addon.measurement.pages.StartAndEndSettingsPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.measurement.Measurement;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * @author Moritz Lang
 */
class MultiCameraContinousImagingAddonUI extends MeasurementAddonUIAdapter<MultiCameraContinousImagingConfiguration>
{
	/**
	 * Constructor.
	 * @param microscope Interface to the microscope.
	 * @param client Interface to the client.
	 * @throws AddonException 
	 */
	MultiCameraContinousImagingAddonUI(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
		
		setTitle("Multi-Cam Imaging");
		
		addPage(new MetadataPage<>(client));
		addPage(new GeneralSettingsPage<MultiCameraContinousImagingConfiguration>(client, MultiCameraContinousImagingConfiguration.class));
		addPage(new StartAndEndSettingsPage(client, server));
		addPage(new ImagingDefinitionPage(client, server));
	}
	
	static ComponentMetadataAdapter<MultiCameraContinousImagingConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<MultiCameraContinousImagingConfiguration>(MultiCameraContinousImagingConfiguration.TYPE_IDENTIFIER, 
				MultiCameraContinousImagingConfiguration.class, 
				Measurement.class, "Multi-Cam Imaging", new String[]{"misc"});
	}	
}
