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
package org.youscope.plugin.microplate.measurement;



import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.addon.measurement.MeasurementAddonUIAdapter;
import org.youscope.addon.measurement.pages.MetadataPage;
import org.youscope.addon.measurement.pages.StartAndEndSettingsPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.measurement.Measurement;
import org.youscope.serverinterfaces.YouScopeServer;


/**
 * @author Moritz Lang
 *
 */
class MicroplateMeasurementAddonUI extends MeasurementAddonUIAdapter<MicroplateMeasurementConfiguration>
{
	/**
	 * Constructor.
	 * @param server YouScope server.
	 * @param YouScope client.
	 * @throws AddonException 
	 */
	MicroplateMeasurementAddonUI(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
		
		setTitle("Microplate Measurement");
		
		addPage(new MetadataPage<>(client));
		addPage(new GeneralSettingsPage(client));
		addPage(new MicroplatePage(client));
		addPage(new WellSelectionPage(client, server));  
		addPage(new PathPage(client, server));
		addPage(new StartAndEndSettingsPage(client, server));
		addPage(new ImagingProtocolPage(client, server));
		addPage(new MiscPage(client, server)); 
	}
	static ComponentMetadataAdapter<MicroplateMeasurementConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<MicroplateMeasurementConfiguration>(MicroplateMeasurementConfiguration.TYPE_IDENTIFIER, 
				MicroplateMeasurementConfiguration.class, 
				Measurement.class, "Microplate Measurement", new String[0], 
				"Takes images/executes a certain set of jobs at the activated well/tile of a microplate or microplate-like object like microfluidic channels. From a list of pre-defined microplate layouts, an appropriate layout can be chosen such that not all imaging positions have to be manually defined. Alternatively, a custom microplate layout can be defined.",
				"icons/map.png");
	}
}
