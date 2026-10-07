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
package org.youscope.plugin.usercontrolmeasurement;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.addon.measurement.MeasurementAddonUIAdapter;
import org.youscope.addon.measurement.pages.MetadataPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.measurement.Measurement;
import org.youscope.serverinterfaces.YouScopeServer;


/**
 * @author Moritz Lang
 *
 */
class UserControlMeasurementAddonUI extends MeasurementAddonUIAdapter<UserControlMeasurementConfiguration>
{
	/**
	 * Constructor.
	 * @param server YouScope server.
	 * @param YouScope client.
	 * @throws AddonException 
	 */
	UserControlMeasurementAddonUI(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
		
		setTitle("User Control Measurement");
		
		addPage(new MetadataPage<UserControlMeasurementConfiguration>(client));
		addPage(new GeneralSettingsPage(client));
		addPage(new MonitorPage(client, server));
	}
	
	static ComponentMetadataAdapter<UserControlMeasurementConfiguration> getMetadata()
	{
		String description = "A user control measurement is a measurement in which the current microscope image is displayed to the user. The user can interactively choose the channel and the exposure. By user activation, the currently displayed image is stored on the hard-disk.\n\n" +
				"The principal idea is that the user can interactively manually choose the position of the stage and adjust the focus. It is thus possible to quickly take several images at different positions, without requiring the overhead of configuring a more complex measurement type\n\n" +
				"However, it is recommended not to use this measurement type exhaustively, and to rather use the more precise other measurement types.\n\n" +
				"This measurement type incorporates an stage monitoring possibility to automatically detect if images were taken at the same position or not. This optional feature then automatically adjusts the file names of the stored images, such that they are easier to identify lateron.";
		
		return new ComponentMetadataAdapter<UserControlMeasurementConfiguration>(UserControlMeasurementConfiguration.TYPE_IDENTIFIER, 
				UserControlMeasurementConfiguration.class, 
				Measurement.class, "User Control Measurement", new String[0], 
				description,
				"icons/user-worker-boss.png");
	}
}
