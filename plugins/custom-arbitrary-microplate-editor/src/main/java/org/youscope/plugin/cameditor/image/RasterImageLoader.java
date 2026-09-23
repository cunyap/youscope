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
