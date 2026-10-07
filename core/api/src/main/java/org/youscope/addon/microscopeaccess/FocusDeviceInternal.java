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
package org.youscope.addon.microscopeaccess;

import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * @author langmo
 * 
 */
public interface FocusDeviceInternal extends DeviceInternal
{
	/**
	 * Returns the position of the focus device.
	 * 
	 * @return Position of the focus device.
	 * @throws MicroscopeException
	 * @throws InterruptedException
	 */
	double getFocusPosition() throws MicroscopeException, InterruptedException;

	/**
	 * Sets the position of the current focus device.
	 * 
	 * @param position The new position.
	 * @param accessID The access ID of the current microscope object. If the microscope is locked with a different accessID, a MicroscopeLockedException is thrown.
	 * @throws MicroscopeLockedException
	 * @throws MicroscopeException
	 * @throws InterruptedException
	 */
	void setFocusPosition(double position, int accessID) throws MicroscopeLockedException, MicroscopeException, InterruptedException;

	/**
	 * Sets the position of the current focus device relative to the current
	 * focus.
	 * 
	 * @param offset The offset.
	 * @param accessID The access ID of the current microscope object. If the microscope is locked with a different accessID, a MicroscopeLockedException is thrown.
	 * @throws MicroscopeLockedException
	 * @throws MicroscopeException
	 * @throws InterruptedException
	 */
	void setRelativeFocusPosition(double offset, int accessID) throws MicroscopeLockedException, MicroscopeException, InterruptedException;

	/**
	 * Returns the calibration factor converting native focus device units to micro meters:
	 * microMeters = units * unitMagnifier. A mechanical step-to-distance calibration, not an optical
	 * magnification; 1.0 for drives that already move in micro meters.
	 * @return Calibration factor for native focus unit to micro meter conversion.
	 */
	double getUnitMagnifier();

	/**
	 * Sets the calibration factor converting native focus device units to micro meters:
	 * microMeters = units * unitMagnifier
	 * @param unitMagnifier Calibration factor for native focus unit to micro meter conversion.
	 * @param accessID The access ID of the current microscope object. If the microscope is locked with a different accessID, a MicroscopeLockedException is thrown.
	 * @throws MicroscopeLockedException
	 */
	void setUnitMagnifier(double unitMagnifier, int accessID) throws MicroscopeLockedException;
}
