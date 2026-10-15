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

import org.youscope.addon.AddonException;
import org.youscope.addon.measurement.MeasurementInitializer;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.measurement.Measurement;
import org.youscope.plugin.microplate.measurement.MicroplateMeasurementInitializer;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * Builds the runtime measurement for an Argolight calibration. Because
 * {@link ArgolightCalibrationConfiguration} is a
 * {@link org.youscope.plugin.microplate.measurement.MicroplateMeasurementConfiguration},
 * the whole job is delegated to the standard microplate measurement
 * initializer,
 * so no measurement-execution logic is duplicated.
 *
 * @author Andreas P. Cuny
 */
public class ArgolightCalibrationInitializer implements MeasurementInitializer<ArgolightCalibrationConfiguration> {
	private final MicroplateMeasurementInitializer delegate = new MicroplateMeasurementInitializer();

	@Override
	public void initializeMeasurement(Measurement measurement, ArgolightCalibrationConfiguration configuration,
			ConstructionContext constructionContext) throws ConfigurationException, AddonException {
		delegate.initializeMeasurement(measurement, configuration, constructionContext);
	}
}
