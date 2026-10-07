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
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Loads raster images (PNG, JPEG, BMP, GIF and TIFF, including CMYK TIFFs) via {@link ImageIO}.
 * <p>
 * TIFF/CMYK support is provided by the TwelveMonkeys ImageIO plugins, which register themselves
 * through the ServiceLoader mechanism simply by being on the class-path (see build.gradle). The
 * decoder converts to a standard sRGB {@link BufferedImage} regardless of the source colour model.
 *
 * @author Andreas P. Cuny
 */
public final class RasterImageLoader
{
	public LoadedImage load(File file) throws IOException
	{
		BufferedImage image = ImageIO.read(file);
		if(image == null)
			throw new IOException("No registered ImageIO reader could decode \"" + file.getName()
					+ "\". For TIFF/CMYK ensure the TwelveMonkeys imageio-tiff plugin is on the class-path.");
		return new LoadedImage(toStandardRgb(image), -1, file.getName());
	}

	/** Ensures a predictable sRGB image the rest of the pipeline can threshold safely. */
	static BufferedImage toStandardRgb(BufferedImage source)
	{
		if(source.getType() == BufferedImage.TYPE_INT_RGB || source.getType() == BufferedImage.TYPE_INT_ARGB)
			return source;
		BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = rgb.createGraphics();
		try
		{
			g.drawImage(source, 0, 0, null);
		}
		finally
		{
			g.dispose();
		}
		return rgb;
	}
}
