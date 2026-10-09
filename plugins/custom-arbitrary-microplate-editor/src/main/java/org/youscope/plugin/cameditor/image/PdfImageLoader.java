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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Rasterizes the first page of a PDF chip drawing using Apache PDFBox.
 * <p>
 * <b>Dependency isolation &amp; version note:</b> this is the only class that references PDFBox. It
 * targets the <b>PDFBox 2.x</b> API ({@code PDDocument.load(File)} + {@code PDFRenderer.renderImageWithDPI})
 * and is pinned to pdfbox 2.0.x in build.gradle. PDFBox 3.x renamed the loader to
 * {@code org.apache.pdfbox.Loader.loadPDF(File)}; if you upgrade to 3.x, change only the load call here.
 *
 * @author Andreas P. Cuny
 */
public final class PdfImageLoader
{
	private float dpi = 300f;

	public void setDpi(float dpi)
	{
		if(dpi > 0)
			this.dpi = dpi;
	}

	public LoadedImage load(File file) throws IOException
	{
		PDDocument document = PDDocument.load(file); // PDFBox 2.x
		try
		{
			if(document.getNumberOfPages() < 1)
				throw new IOException("PDF \"" + file.getName() + "\" has no pages.");
			PDFRenderer renderer = new PDFRenderer(document);
			BufferedImage image = renderer.renderImageWithDPI(0, dpi);
			return new LoadedImage(RasterImageLoader.toStandardRgb(image), -1, file.getName());
		}
		finally
		{
			document.close();
		}
	}
}
