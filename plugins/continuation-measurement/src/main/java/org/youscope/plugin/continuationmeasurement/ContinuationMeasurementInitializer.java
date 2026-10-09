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
package org.youscope.plugin.continuationmeasurement;

import java.rmi.RemoteException;
import java.util.ServiceLoader;

import org.youscope.addon.AddonException;
import org.youscope.addon.measurement.MeasurementAddonFactory;
import org.youscope.addon.measurement.MeasurementInitializer;
import org.youscope.common.ComponentRunningException;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.measurement.Measurement;
import org.youscope.common.measurement.MeasurementConfiguration;
import org.youscope.common.task.Task;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * @author langmo
 * 
 */
class ContinuationMeasurementInitializer implements MeasurementInitializer<ContinuationMeasurementConfiguration>
{

	@Override
	public void initializeMeasurement(Measurement measurement, ContinuationMeasurementConfiguration configuration, ConstructionContext constructionContext) throws ConfigurationException, AddonException
	{
		MeasurementConfiguration encapsulatedConfiguration = configuration.getEncapsulatedConfiguration();
		if(encapsulatedConfiguration == null)
			throw new ConfigurationException("No measurement configuration encapsulated.");
		String encapsulatedTypeIdentifier = encapsulatedConfiguration.getTypeIdentifier();
		
		// Initialize encapsulated configuration.
		MeasurementAddonFactory encapsulatedAddonFactory = null;
		ServiceLoader<MeasurementAddonFactory> addonFactories = ServiceLoader.load(MeasurementAddonFactory.class, ContinuationMeasurementInitializer.class.getClassLoader());
		for(MeasurementAddonFactory addon : addonFactories)
		{
			if(addon.isSupportingTypeIdentifier(encapsulatedTypeIdentifier))
				encapsulatedAddonFactory = addon;
		}
		if (encapsulatedAddonFactory == null)
            throw new AddonException("Type of encapsulated measurement configuration (" + encapsulatedTypeIdentifier + ") unknown.");
		encapsulatedAddonFactory.initializeMeasurement(measurement, encapsulatedConfiguration, constructionContext);
		
		// Change initial frame numbers
		try {
			for(Task task : measurement.getTasks())
			{
				task.setInitialExecutionNumber(configuration.getDeltaEvaluationNumber());
			}
		} 
		catch (RemoteException | ComponentRunningException | IllegalArgumentException e) {
			throw new AddonException("Could not set initial execution number of tasks.", e);
		}
		try {
			measurement.setInitialRuntime(configuration.getPreviousRuntime());
		} catch (RemoteException | ComponentRunningException | IllegalArgumentException e1) {
			throw new AddonException("Could not set initial runtime of measurement.", e1);
		}
		try {
			measurement.getSaver().setSaveSettings(new EncapsulatedSaveSettings(measurement.getSaver().getSaveSettings(), configuration.getMeasurementFolder()));
		} catch (ComponentRunningException | RemoteException e) {
			throw new AddonException("Could not encapsulate save settings.", e);
		}
	}
}
