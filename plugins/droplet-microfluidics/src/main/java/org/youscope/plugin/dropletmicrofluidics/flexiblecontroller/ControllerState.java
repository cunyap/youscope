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
package org.youscope.plugin.dropletmicrofluidics.flexiblecontroller;

import java.io.Serializable;

/**
 * State of the observer.
 * @author Moritz Lang
 *
 */
class ControllerState implements Serializable
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -5670488844311377421L;

	private long lastExecutionTime = -1;
	private double integralError = 0;
	public long getLastExecutionTime() {
		return lastExecutionTime;
	}
	public void setLastExecutionTime(long lastExecutionTime) {
		this.lastExecutionTime = lastExecutionTime;
	}
	public double getIntegralError() {
		return integralError;
	}
	public void setIntegralError(double integralError) {
		this.integralError = integralError;
	}
}
