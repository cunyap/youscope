/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Amdreas P. Cuny - bugfixes
 ******************************************************************************/
package org.youscope.plugin.microscopeaccess;

import mmcorej.CMMCore;

import org.youscope.addon.microscopeaccess.PropertyInternal;
import org.youscope.common.microscope.DeviceException;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.common.microscope.MicroscopeLockedException;
import org.youscope.common.microscope.PropertyType;

/**
 * @author langmo
 */
public abstract class PropertyImpl implements PropertyInternal, Comparable<PropertyInternal> {
	protected final MicroscopeImpl microscope;
	private final String device;
	private final String property;
	private final PropertyType type;
	private final boolean editable;
	private final boolean preInit;
	private final PropertyActionListener actionListener;

	/**
	 * Maximum number of attempts when a transient device error is detected.
	 * With RETRY_DELAY_MS = 2000 this gives up to 30 s of warmup time.
	 */
	private static final int MAX_RETRIES = 15;
	/** Wait between retry attempts in milliseconds. */
	private static final long RETRY_DELAY_MS = 2000;

	/**
	 * Error messages (case-insensitive substrings) that indicate a transient
	 * hardware state and should trigger a retry rather than an immediate failure.
	 * Add more device-specific strings here as they are discovered.
	 */
	private static final String[] TRANSIENT_ERROR_MARKERS = {
			"warming up",
			"initializing",
			"not ready",
			"busy",
			"error : 74", // LumenCore Spectra III: lasers warming up
			"error: 74",
			"(74)",
	};

	PropertyImpl(MicroscopeImpl microscope, String deviceID, String propertyID,
			PropertyType type, boolean editable, boolean preInit,
			PropertyActionListener actionListener) {
		this.microscope = microscope;
		this.device = deviceID;
		this.property = propertyID;
		this.type = type;
		this.editable = editable;
		this.preInit = preInit;
		this.actionListener = actionListener;
	}

	protected void deviceStateModified() {
		actionListener.deviceStateModified();
	}

	@Override
	public boolean isPreInitializationProperty() {
		return preInit;
	}

	@Override
	public String getPropertyID() {
		return property;
	}

	@Override
	public String getDeviceID() {
		return device;
	}

	@Override
	public PropertyType getType() {
		return type;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	@Override
	public String getValue() throws MicroscopeException, InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		if (device == null || property == null)
			return null;
		try {
			return microscope.startRead().getProperty(device, property);
		} catch (Exception e) {
			throw new MicroscopeException(
					"Could not get property " + property + " of device " + device + ".", e);
		} finally {
			microscope.unlockRead();
		}
	}

	@Override
	public abstract void setValue(String value, int accessID)
			throws MicroscopeException, MicroscopeLockedException,
			InterruptedException, DeviceException;

	/**
	 * Sets a property value on the hardware, retrying automatically when a
	 * transient device error is detected (e.g. laser engine warming up).
	 *
	 * <p>
	 * On each transient failure the method logs a warning via
	 * {@link ServerSystem#out}, waits {@value #RETRY_DELAY_MS} ms, and
	 * tries again. After {@value #MAX_RETRIES} attempts the last exception
	 * is re-thrown as a {@link MicroscopeException}.
	 *
	 * @throws MicroscopeLockedException thrown by subclasses.
	 */
	protected void setStringValue(String value, int accessID)
			throws MicroscopeException, InterruptedException, MicroscopeLockedException {
		if (Thread.interrupted())
			throw new InterruptedException();

		Exception lastException = null;

		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				CMMCore core = microscope.startWrite(accessID);
				core.setProperty(device, property, value);
				// Success: notify listeners and return normally
				microscope.stateChanged(
						"Property " + device + "." + property + " set to " + value + ".");
				return;
			} catch (InterruptedException ie) {
				// Never swallow interrupts
				throw ie;
			} catch (Exception e) {
				lastException = e;

				if (isTransientError(e)) {
					// Log and wait before retrying
					String msg = "Property " + device + "." + property
							+ ": transient device error on attempt " + attempt
							+ "/" + MAX_RETRIES + " ('" + extractMessage(e)
							+ "'). Retrying in " + RETRY_DELAY_MS + " ms...";
					// Log via standard output and picked up by YouScope's
					// System.out redirect in ClientSystem
					System.out.println("[YouScope] " + msg);

					Thread.sleep(RETRY_DELAY_MS);
					// Continue to next attempt
				} else {
					// Non-transient error therefore fail immediately, no retry
					break;
				}
			} finally {
				// Always release the write lock and notify state change,
				// even on failure, to avoid deadlocks.
				deviceStateModified();
				microscope.unlockWrite();
			}
		}

		// All attempts exhausted or non-transient error encountered
		throw new MicroscopeException(
				"Couldn't set property " + device + "." + property
						+ " to " + value
						+ (isTransientError(lastException)
								? " after " + MAX_RETRIES + " attempts (" + RETRY_DELAY_MS + " ms each)."
								: "."),
				lastException);
	}

	/**
	 * Returns true if the exception looks like a transient hardware state
	 * that is worth retrying (device warming up, busy, initializing, etc.).
	 */
	private static boolean isTransientError(Exception e) {
		if (e == null)
			return false;
		String msg = extractMessage(e).toLowerCase();
		for (String marker : TRANSIENT_ERROR_MARKERS) {
			if (msg.contains(marker.toLowerCase()))
				return true;
		}
		return false;
	}

	/**
	 * Extracts a combined message from the exception chain (up to 3 levels).
	 */
	private static String extractMessage(Exception e) {
		if (e == null)
			return "";
		StringBuilder sb = new StringBuilder();
		Throwable t = e;
		for (int i = 0; i < 3 && t != null; i++) {
			if (t.getMessage() != null) {
				if (sb.length() > 0)
					sb.append(" | ");
				sb.append(t.getMessage());
			}
			t = t.getCause();
		}
		return sb.toString();
	}

	@Override
	public int compareTo(PropertyInternal o) {
		if (o == null)
			return -1;
		return getPropertyID().compareToIgnoreCase(o.getPropertyID());
	}
}