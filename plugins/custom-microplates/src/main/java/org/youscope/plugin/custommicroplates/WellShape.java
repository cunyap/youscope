/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
package org.youscope.plugin.custommicroplates;

/**
 * Display-only shape of a well as authored in the Custom Arbitrary Microplate
 * Editor.
 * <p>
 * <b>Important:</b> the shape is purely a visualization/UX aid so that the
 * editor can render a plate
 * that looks like the researcher's original chip design. It never influences
 * image acquisition: the
 * microscope always targets the centre of the well and images the axis-aligned
 * bounding box
 * (x, y, width, height) exactly as before. Camera sensors are rectangular, so
 * acquisition geometry is
 * unaffected by whether a well is drawn as a rectangle or an ellipse.
 *
 * @author Andreas P. Cuny
 */
public enum WellShape {
	/**
	 * Axis-aligned rectangle (the default and the only shape older files know
	 * about).
	 */
	RECTANGLE,
	/**
	 * Ellipse inscribed in the well bounding box (rendered as a circle when width
	 * == height).
	 */
	ELLIPSE;

	/**
	 * Null-safe parse used when reading persisted definitions; unknown / missing
	 * values map to
	 * {@link #RECTANGLE}.
	 * 
	 * @param name persisted name (may be {@code null}).
	 * @return the matching shape, or {@link #RECTANGLE} if none matches.
	 */
	public static WellShape fromNameOrDefault(String name) {
		if (name != null) {
			for (WellShape shape : values()) {
				if (shape.name().equalsIgnoreCase(name))
					return shape;
			}
		}
		return RECTANGLE;
	}
}
