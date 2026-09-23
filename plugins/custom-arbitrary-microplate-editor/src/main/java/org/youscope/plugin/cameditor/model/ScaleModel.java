/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.cameditor.model;

/**
 * The single source of truth for the image scale, expressed as micrometres per pixel.
 * <p>
 * Editor geometry is kept in image/scene pixels; micrometre coordinates (as stored in the YouScope
 * {@code .csb} definition) are derived at export time via this scale. Keeping pixels as the editing
 * unit means that changing the scale never moves wells relative to the backdrop image; it only
 * rescales the exported physical coordinates.
 *
 * @author Andreas P. Cuny
 */
public final class ScaleModel
{
	private double micronsPerPixel = 1.0;

	public ScaleModel()
	{
	}

	public ScaleModel(double micronsPerPixel)
	{
		setMicronsPerPixel(micronsPerPixel);
	}

	public double getMicronsPerPixel()
	{
		return micronsPerPixel;
	}

	public void setMicronsPerPixel(double micronsPerPixel)
	{
		if(micronsPerPixel > 0 && !Double.isInfinite(micronsPerPixel) && !Double.isNaN(micronsPerPixel))
			this.micronsPerPixel = micronsPerPixel;
	}

	/**
	 * Derives the scale from a measured on-screen distance of a known real length.
	 * @param pixelDistance measured distance in image pixels (must be > 0).
	 * @param realMicrons the corresponding real-world distance in micrometres (must be > 0).
	 */
	public void setFromMeasurement(double pixelDistance, double realMicrons)
	{
		if(pixelDistance > 0 && realMicrons > 0)
			setMicronsPerPixel(realMicrons / pixelDistance);
	}

	/** Pixels -> micrometres. */
	public double pxToUm(double px)
	{
		return px * micronsPerPixel;
	}

	/** Micrometres -> pixels. */
	public double umToPx(double um)
	{
		return um / micronsPerPixel;
	}
}
