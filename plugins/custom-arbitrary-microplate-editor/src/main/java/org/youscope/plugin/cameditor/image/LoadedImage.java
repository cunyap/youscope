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
package org.youscope.plugin.cameditor.image;

import java.awt.image.BufferedImage;

/**
 * A backdrop image loaded from disk, together with an optional known scale (micrometres per pixel) if
 * the source format carried real-world units (e.g. SVG with a physical viewport, or DXF).
 *
 * @author Andreas P. Cuny
 */
public final class LoadedImage
{
	private final BufferedImage image;
	private final double knownMicronsPerPixel; // <= 0 means "unknown"
	private final String sourceDescription;

	public LoadedImage(BufferedImage image, double knownMicronsPerPixel, String sourceDescription)
	{
		this.image = image;
		this.knownMicronsPerPixel = knownMicronsPerPixel;
		this.sourceDescription = sourceDescription;
	}

	public BufferedImage getImage() { return image; }

	/** @return micrometres per pixel if known from the source format, otherwise a value < 0. */
	public double getKnownMicronsPerPixel() { return knownMicronsPerPixel; }

	public boolean hasKnownScale() { return knownMicronsPerPixel > 0; }

	public String getSourceDescription() { return sourceDescription; }
}
