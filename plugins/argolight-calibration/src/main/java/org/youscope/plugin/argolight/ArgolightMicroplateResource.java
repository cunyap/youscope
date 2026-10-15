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

import java.rmi.RemoteException;

import org.youscope.addon.microplate.MicroplateResource;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.resource.ResourceException;

/**
 * Resource producing the {@link MicroplateLayout} for an Argolight plate.
 *
 * @author Andreas P. Cuny
 */
class ArgolightMicroplateResource extends ResourceAdapter<ArgolightMicroplateConfiguration>
		implements MicroplateResource {
	private static final long serialVersionUID = 1L;

	ArgolightMicroplateResource(PositionInformation positionInformation, ArgolightMicroplateConfiguration configuration)
			throws ConfigurationException, RemoteException {
		super(positionInformation, configuration, ArgolightMicroplateConfiguration.TYPE_IDENTIFIER,
				ArgolightMicroplateConfiguration.class, "Argolight Plate");
	}

	@Override
	public MicroplateLayout getMicroplateLayout() throws ResourceException, RemoteException {
		ArgolightMicroplateConfiguration configuration = getConfiguration();
		try {
			ArgolightPlateDefinition definition = ArgolightPlateManager
					.getDefinition(configuration.getDefinitionName());
			return definition.createMicroplateLayout();
		} catch (ArgolightException e) {
			throw new ResourceException(
					"Could not load Argolight plate definition '" + configuration.getDefinitionName() + "'.", e);
		}
	}
}
