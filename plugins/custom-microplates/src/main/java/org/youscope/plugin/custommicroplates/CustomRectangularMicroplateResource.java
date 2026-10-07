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
package org.youscope.plugin.custommicroplates;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

import org.youscope.addon.microplate.MicroplateResource;
import org.youscope.addon.microplate.RectangularMicroplateLayout;
import org.youscope.common.MeasurementContext;
import org.youscope.common.MessageListener;
import org.youscope.common.PositionInformation;
import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.resource.ResourceException;

class CustomRectangularMicroplateResource extends UnicastRemoteObject implements MicroplateResource
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -7366672098628415823L;
	private CustomMicroplateDefinition microplateDefinition;
	private final PositionInformation positionInformation;
	private final String typeIdentifier;
	private String name = "custom microplate";
	private final UUID uuid = UUID.randomUUID();
	public CustomRectangularMicroplateResource(PositionInformation positionInformation, String typeIdentifier)
					throws RemoteException {
		this.typeIdentifier = typeIdentifier;
		this.positionInformation= positionInformation;
	}

	@Override
	public MicroplateLayout getMicroplateLayout() throws ResourceException, RemoteException 
	{
		if(microplateDefinition == null)
			throw new ResourceException("Using custom microplate resource without initializing it.");
		if(microplateDefinition instanceof CustomRectangularMicroplateDefinition)
		{
			CustomRectangularMicroplateDefinition def = (CustomRectangularMicroplateDefinition) microplateDefinition;
			return new RectangularMicroplateLayout(def.getNumWellsX(), def.getNumWellsY(), def.getWellWidth(), def.getWellHeight());
		}
		else if(microplateDefinition instanceof MicroplateLayout)
			return (MicroplateLayout) microplateDefinition;
		throw new ResourceException("Custom microplate resource is of unknown type "+microplateDefinition.getClass().getName()+".");
	}

	@Override
	public String getTypeIdentifier() throws RemoteException {
		return CustomMicroplateManager.getCustomMicroplateTypeIdentifier(microplateDefinition.getCustomMicroplateName());
	}

	@Override
	public void initialize(MeasurementContext measurementContext) throws ResourceException, RemoteException 
	{
		try {
			microplateDefinition = CustomMicroplateManager.getCustomMicroplate(typeIdentifier);
		} catch (CustomMicroplateException e) {
			throw new ResourceException("Could not get information on layout of custom microplate with type identifer " + typeIdentifier+".", e);
		}
	}

	@Override
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException, RemoteException {
		microplateDefinition = null;
	}

	@Override
	public boolean isInitialized() throws RemoteException {
		return microplateDefinition != null;
	}

	@Override
	public void addMessageListener(MessageListener writer) throws RemoteException {
		// no messages
		
	}

	@Override
	public void removeMessageListener(MessageListener writer) throws RemoteException {
		// no messages
	}

	@Override
	public PositionInformation getPositionInformation() throws RemoteException {
		return positionInformation;
	}

	@Override
	public String getName() throws RemoteException {
		return name;
	}

	@Override
	public void setName(String name) throws RemoteException {
		this.name = name;
	}

	@Override
	public UUID getUUID() throws RemoteException {
		return uuid;
	}

}
