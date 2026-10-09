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
package org.youscope.addon.measurement;

import org.youscope.addon.AddonException;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.measurement.Measurement;
import org.youscope.common.measurement.MeasurementConfiguration;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * Interface which has to be implemented if the creation of a component, given its configuration, should be done
 * differently from the default mechanism.
 * @author Moritz Lang
 * @param <C> The configuration class consumed for the creation.
 *
 */
public interface MeasurementInitializer<C extends MeasurementConfiguration>
{
	/**
	 * Called when this addon should initialize the the measurement according to its configuration.
	 * @param measurement The measurement which should be initialized.
	 * @param configuration The configuration according to which the measurement should be initialized.
	 * @param constructionContext An interface to an object allowing to initialize the various measurement components.
	 * @throws ConfigurationException Thrown if the configuration is invalid.
	 * @throws AddonException Thrown if an error occurred during the initialization.
	 */
	void initializeMeasurement(Measurement measurement, C configuration, ConstructionContext constructionContext) throws ConfigurationException, AddonException;
}
