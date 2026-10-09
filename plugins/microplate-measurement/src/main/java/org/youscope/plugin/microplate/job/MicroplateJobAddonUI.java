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
 *     Moritz Lang - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.microplate.job;



import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.job.Job;
import org.youscope.serverinterfaces.YouScopeServer;


/**
 * @author Moritz Lang
 *
 */
class MicroplateJobAddonUI extends JobAddonUIAdapter<MicroplateJobConfiguration>
{
	/**
	 * Constructor.
	 * @param server YouScope server.
	 * @param YouScope client.
	 * @throws AddonException 
	 */
	MicroplateJobAddonUI(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
		
		setTitle("Microplate Job");
		addPage(new MicroplatePage(client));
		addPage(new WellSelectionPage(client, server));  
		addPage(new PathPage(client, server));
		addPage(new ImagingProtocolPage(client, server));
	}
	static ComponentMetadataAdapter<MicroplateJobConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<MicroplateJobConfiguration>(MicroplateJobConfiguration.TYPE_IDENTIFIER, 
				MicroplateJobConfiguration.class, 
				Job.class, 
				"Microplate", 
				new String[]{"containers"},
				"A job realizing most of the functionality of a microplate measurement, but which can be combined with other jobs in a different kind of measurement. Takes images/executes a certain set of child jobs at the activated positions of a microplate or microplate-like object like microfluidic channels.",
				"icons/map.png");
	}
}
