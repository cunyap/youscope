/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - add focus conversion factor handling
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.microscopeaccess;

import mmcorej.CMMCore;

import org.youscope.addon.microscopeaccess.FocusDeviceInternal;
import org.youscope.common.microscope.DeviceType;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;

/**
 * @author langmo
 * 
 */
public class FocusDeviceImpl extends DeviceImpl implements FocusDeviceInternal {
	private double unitMagnifier = 1.0;
	private final static String PROPERTY_POSITION = "Position";

	FocusDeviceImpl(MicroscopeImpl microscope, String deviceName, String libraryID, String driverID,
			HubDeviceImpl hub) {
		super(microscope, deviceName, libraryID, driverID, DeviceType.StageDevice, hub,
				new String[] { PROPERTY_POSITION });
	}

	@Override
	protected void initializeDevice(int accessID) throws MicroscopeException {
		super.initializeDevice(accessID);

		// Add some additional properties...
		properties.put(PROPERTY_POSITION, new FloatPropertyImpl(microscope, getDeviceID(), PROPERTY_POSITION,
				Float.MIN_VALUE, Float.MAX_VALUE, false, this) {
			@Override
			public String getValue() throws MicroscopeException, InterruptedException {
				return Double.toString(getFocusPosition());
			}

			@Override
			protected void setStringValue(String value, int accessID)
					throws MicroscopeException, MicroscopeLockedException, InterruptedException {
				try {
					setFocusPosition(Double.parseDouble(value), accessID);
				} catch (NumberFormatException e) {
					throw new MicroscopeException(
							"Value for stage focus-position \"" + value + "\" is not a float value.", e);
				}
			}
		});
	}

	@Override
	public double getFocusPosition() throws MicroscopeException, InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		try {
			CMMCore core = microscope.startRead();
			double position = core.getPosition(getDeviceID());
			// Transform position from native focus units to program units (micro meters).
			double magnifier = getUnitMagnifier();
			if (magnifier != 1.0)
				position *= magnifier;
			return position;

		} catch (Exception e) {
			throw new MicroscopeException(
					"Could not get position of focus device \"" + getDeviceID() + "\": " + e.getMessage());
		} finally {
			microscope.unlockRead();
		}
	}

	@Override
	public void setFocusPosition(double position, int accessID)
			throws MicroscopeLockedException, MicroscopeException, InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		double positionOrg = position;
		try {
			CMMCore core = microscope.startWrite(accessID);
			// Transform position from program units (micro meters) to native focus units.
			double magnifier = getUnitMagnifier();
			if (magnifier != 1.0)
				position /= magnifier;
			try {
				core.setPosition(getDeviceID(), position);
				core.waitForDevice(getDeviceID());
			} catch (Exception e) {
				throw new MicroscopeException("Could not set focus position.", e);
			}
		} finally {
			deviceStateModified();
			microscope.unlockWrite();
		}
		microscope.stateChanged(
				"Focus of device \"" + getDeviceID() + "\" set to " + Double.toString(positionOrg) + " um.");
	}

	@Override
	public void setRelativeFocusPosition(double offset, int accessID)
			throws MicroscopeLockedException, MicroscopeException, InterruptedException {
		try {
			microscope.lockWrite(accessID);
			setFocusPosition(getFocusPosition() + offset, accessID);
		} finally {
			microscope.unlockWrite();
		}
	}

	@Override
	public double getUnitMagnifier() {
		return unitMagnifier;
	}

	@Override
	public void setUnitMagnifier(double unitMagnifier, int accessID) throws MicroscopeLockedException {
		try {
			microscope.lockWrite(accessID);
			this.unitMagnifier = unitMagnifier;
		} finally {
			microscope.unlockWrite();
		}
		microscope.stateChanged("Set unit magnifier of focus device " + getDeviceID() + " to "
				+ Double.toString(unitMagnifier) + " (microns = " + Double.toString(unitMagnifier) + " * units).");
	}

}
