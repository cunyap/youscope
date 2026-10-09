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
package org.youscope.common.saving;

import java.io.Serializable;

/**
 * General information of the measurement which is currently saved, intended to be used by instances of SaveSettings
 * to calculate the names of the folders in which files of the measurement should be saved.
  * <br>
 * The class is immutable.
 * @author mlang
 *
 */
public final class SaveInformation implements Serializable 
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -8822349015437087498L;
	private final String measurementName;
	private final long measurementStartTime;
	
	// TODO: include measurement pause duration, and offer a method to get runtime.
	
	/**
	 * Constructor.
	 * @param measurementName Name of the measurement.
	 * @param measurementStartTime Time in ms when measurement was started. See {@link System#currentTimeMillis()}.
	 */
	public SaveInformation(String measurementName, long measurementStartTime)
	{
		this.measurementName = measurementName;
		this.measurementStartTime = measurementStartTime;
	}

	/**
	 * Returns the name of the measurement.
	 * @return Name of the measurement.
	 */
	public String getMeasurementName() {
		return measurementName;
	}

	/**
	 * Returns the start time of the measurement in ms. See {@link System#currentTimeMillis()}.
	 * @return Start time in ms of the measurement.
	 */
	public long getMeasurementStartTime() {
		return measurementStartTime;
	}
}
