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
 *     Andreas P. Cuny - add calibration factor converting native focus device units to micro meters
 ******************************************************************************/
/**
 * 
 */
package org.youscope.common.microscope;

import java.rmi.RemoteException;

/**
 * @author Moritz Lang
 * 
 */
public interface FocusDevice extends Device {
	/**
	 * Returns the position of the focus device.
	 * 
	 * @return Position of the focus device.
	 * @throws RemoteException
	 * @throws MicroscopeException
	 * @throws InterruptedException
	 */
	double getFocusPosition() throws RemoteException, MicroscopeException, InterruptedException;

	/**
	 * Sets the position of the current focus device.
	 * 
	 * @param position
	 *                 The new position.
	 * @throws MicroscopeLockedException
	 * @throws RemoteException
	 * @throws MicroscopeException
	 * @throws InterruptedException
	 */
	void setFocusPosition(double position)
			throws MicroscopeLockedException, RemoteException, MicroscopeException, InterruptedException;

	/**
	 * Sets the position of the current focus device relative to the current
	 * focus.
	 * 
	 * @param offset
	 *               The offset.
	 * @throws MicroscopeLockedException
	 * @throws RemoteException
	 * @throws MicroscopeException
	 * @throws InterruptedException
	 */
	void setRelativeFocusPosition(double offset)
			throws MicroscopeLockedException, RemoteException, MicroscopeException, InterruptedException;

	/**
	 * Returns the calibration factor converting native focus device units to micro
	 * meters:
	 * microMeters = units * unitMagnifier
	 * <p>
	 * This is a purely mechanical step-to-distance calibration of the focus drive,
	 * <b>not</b> an optical
	 * magnification: it does not involve the objective magnification or numerical
	 * aperture (those govern
	 * focal depth, a different quantity). Most focus/Z drives already move in micro
	 * meters, so the factor
	 * is 1.0. Some do not: a Nikon PFS offset is effectively unit-less, and various
	 * third party focus
	 * motors move an arbitrary physical distance per native unit that must be
	 * calibrated (e.g. against an
	 * Argolight Z staircase or similar). Setting the factor lets YouScope report
	 * and command focus positions in real
	 * micro meters, so a Z stack's step size is a true physical distance and the
	 * FAIR export can record a
	 * correct PhysicalSizeZ.
	 * </p>
	 * 
	 * @return Calibration factor for native focus unit to micro meter conversion.
	 * @throws RemoteException
	 */
	double getUnitMagnifier() throws RemoteException;

	/**
	 * Sets the calibration factor converting native focus device units to micro
	 * meters:
	 * microMeters = units * unitMagnifier. See {@link #getUnitMagnifier()}; 1.0
	 * means the drive already
	 * moves in micro meters.
	 * 
	 * @param unitMagnifier Calibration factor for native focus unit to micro meter
	 *                      conversion.
	 * @throws RemoteException
	 * @throws MicroscopeLockedException
	 */
	void setUnitMagnifier(double unitMagnifier) throws RemoteException, MicroscopeLockedException;
}
