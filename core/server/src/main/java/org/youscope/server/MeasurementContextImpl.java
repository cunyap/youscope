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
package org.youscope.server;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.UUID;

import org.youscope.common.MeasurementContext;

class MeasurementContextImpl implements MeasurementContext
{
    private final HashMap<String, Serializable> storedProperties = new HashMap<String, Serializable>();

    private final MeasurementImpl measurement;

    MeasurementContextImpl(final MeasurementImpl measurement)
    {
        this.measurement = measurement;
    }

    @Override
    public void setProperty(String identifier, Serializable property)
    {
        synchronized (storedProperties)
        {
            storedProperties.put(identifier, property);
        }
    }

    @Override
    public Serializable getProperty(String identifier)
    {
        synchronized (storedProperties)
        {
            return storedProperties.get(identifier);
        }
    }

    @Override
    public String[] getPropertyIdentifiers()
    {
        synchronized (storedProperties)
        {
            return storedProperties.keySet().toArray(new String[0]);
        }
    }

    void clear()
    {
        synchronized (storedProperties)
        {
            storedProperties.clear();
        }
    }

    @Override
    public <T extends Serializable> T getProperty(String identifier, Class<T> propertyType)
    {
    	Serializable property = getProperty(identifier);
        if (property == null)
            return null;
        if (propertyType.isInstance(property))
            return propertyType.cast(property);
		return null;
    }

    @Override
    public void notifyMeasurementStructureChanged() throws RemoteException
    {
        measurement.notifyMeasurementStructureModified();
    }

	@Override
	public UUID getMeasurementUUID() throws RemoteException {
		return measurement.getUUID();
	}

	@Override
	public long getMeasurementRuntime() throws RemoteException {
		return measurement.getRuntime();
	}

}
