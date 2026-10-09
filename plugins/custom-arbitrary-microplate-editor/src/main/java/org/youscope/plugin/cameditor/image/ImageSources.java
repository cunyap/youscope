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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.cameditor.image;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Dispatches a backdrop file to the appropriate loader based on its extension. Raster (tif/tiff/png/
 * jpg/jpeg/bmp/gif), SVG and PDF return a {@link LoadedImage}. DXF is handled separately by
 * {@code org.youscope.plugin.cameditor.image.dxf.DxfImageLoader} because it also yields wells directly.
 *
 * @author CAME
 */
public final class ImageSources
{
	private ImageSources() { }

	public static boolean isRaster(File file) { return matches(file, ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp", ".gif"); }
	public static boolean isSvg(File file) { return matches(file, ".svg"); }
	public static boolean isPdf(File file) { return matches(file, ".pdf"); }
	public static boolean isDxf(File file) { return matches(file, ".dxf"); }

	public static boolean isSupported(File file)
	{
		return isRaster(file) || isSvg(file) || isPdf(file) || isDxf(file);
	}

	/**
	 * Loads raster / SVG / PDF backdrops. Not for DXF (use DxfImageLoader).
	 * @param file file to load.
	 * @return the loaded backdrop.
	 * @throws IOException on decode errors or unsupported types.
	 */
	public static LoadedImage load(File file) throws IOException
	{
		if(isDxf(file))
			throw new IOException("DXF files are imported via the DXF importer, not ImageSources.load().");
		if(isSvg(file))
			return new SvgImageLoader().load(file);
		if(isPdf(file))
			return new PdfImageLoader().load(file);
		if(isRaster(file))
			return new RasterImageLoader().load(file);
		throw new IOException("Unsupported image type: " + file.getName());
	}

	private static boolean matches(File file, String... extensions)
	{
		if(file == null)
			return false;
		String name = file.getName().toLowerCase(Locale.ENGLISH);
		for(String ext : extensions)
		{
			if(name.endsWith(ext))
				return true;
		}
		return false;
	}
}
