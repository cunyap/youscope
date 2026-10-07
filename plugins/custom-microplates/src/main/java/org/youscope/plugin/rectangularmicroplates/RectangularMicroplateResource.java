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
package org.youscope.plugin.rectangularmicroplates;

import java.rmi.RemoteException;

import org.youscope.addon.microplate.MicroplateResource;
import org.youscope.addon.microplate.RectangularMicroplateLayout;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.resource.ResourceException;

class RectangularMicroplateResource extends ResourceAdapter<RectangularMicroplateConfiguration> implements MicroplateResource
{
	/**
	 * Serial Version UID. 
	 */
	private static final long serialVersionUID = -6734244891085687375L;

	public RectangularMicroplateResource(PositionInformation positionInformation, RectangularMicroplateConfiguration configuration)
					throws ConfigurationException, RemoteException {
		super(positionInformation, configuration, RectangularMicroplateConfiguration.TYPE_IDENTIFIER, RectangularMicroplateConfiguration.class, "Rectangular Microplate");
	}

	@Override
	public MicroplateLayout getMicroplateLayout() throws ResourceException, RemoteException 
	{
		RectangularMicroplateConfiguration configuration = getConfiguration();
		return new RectangularMicroplateLayout(configuration.getNumWellsX(), configuration.getNumWellsY(), configuration.getWellWidth(), configuration.getWellHeight());
	}

}
