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
package org.youscope.common.microscope;

/**
 * Types of devices of a microscope.
 * 
 * @author Moritz Lang
 */
public enum DeviceType
{

	/**
	 * Camera Device.
	 */
	CameraDevice("Camera"),
	/**
	 * Shutter.
	 */
	ShutterDevice("Shutter"),
	/**
	 * Filter, Turrets, ...
	 */
	StateDevice("Filter, Turrets, ..."),
	/**
	 * 1D Stage, Focus, ...
	 */
	StageDevice("1D Stage, Focus, ..."),
	/**
	 * 2D Stage
	 */
	XYStageDevice("2D Stage"),
	/**
	 * Serial Port
	 */
	SerialDevice("Serial Port"),
	/**
	 * Auto-Focus
	 */
	AutoFocusDevice("Auto-Focus"),
	/**
	 * Signal IO
	 */
	SignalIODevice("Signal IO"),
	/**
	 * Programmable IO
	 */
	ProgrammableIODevice("Programmable IO"),
	/**
	 * Image Processor
	 */
	ImageProcessorDevice("Image Processor"),
	/**
	 * Image Streamer
	 */
	ImageStreamerDevice("Image Streamer"),
	/**
	 * Magnifier
	 */
	MagnifierDevice("Magnifier"),
	/**
	 * Generic Device
	 */
	GenericDevice("Generic"),
	/**
	 * A hub, containing other "child" peripheral devices
	 */
	HubDevice("Hub"),
	/**
	 * Special Device.
	 */
	AnyType("Special"),
	/**
	 * Unknown Device.
	 */
	UnknownType("Unknown");

	private final String	description;

	private DeviceType(String description)
	{
		this.description = description;
	}

	/**
	 * Returns a human readable name/description of this device type.
	 * @return Human readable name/description.
	 */
	public String getDescription()
	{
		return description;
	}

	@Override
	public String toString()
	{
		return description;
	}
}
