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

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

/**
 * Rasterizes an SVG chip drawing to a {@link BufferedImage} using Apache Batik.
 * <p>
 * <b>Dependency isolation:</b> this is the only class that references Batik (pinned to
 * batik-transcoder 1.17). A custom {@link ImageTranscoder} captures the rendered image rather than
 * writing it to a file. The target raster width controls the working resolution.
 *
 * @author Andreas P. Cuny
 */
public final class SvgImageLoader
{
	private float targetWidthPixels = 2000f;

	public void setTargetWidthPixels(float targetWidthPixels)
	{
		if(targetWidthPixels > 0)
			this.targetWidthPixels = targetWidthPixels;
	}

	public LoadedImage load(File file) throws IOException
	{
		final BufferedImage[] holder = new BufferedImage[1];
		ImageTranscoder transcoder = new ImageTranscoder()
		{
			@Override
			public BufferedImage createImage(int width, int height)
			{
				return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			}

			@Override
			public void writeImage(BufferedImage image, TranscoderOutput output)
			{
				holder[0] = image;
			}
		};
		transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, Float.valueOf(targetWidthPixels));
		try
		{
			transcoder.transcode(new TranscoderInput(file.toURI().toString()), new TranscoderOutput());
		}
		catch(TranscoderException e)
		{
			throw new IOException("Could not rasterize SVG \"" + file.getName() + "\": " + e.getMessage(), e);
		}
		if(holder[0] == null)
			throw new IOException("SVG rasterization produced no image for \"" + file.getName() + "\".");
		// Flatten onto white so thresholding sees a light background.
		BufferedImage flattened = new BufferedImage(holder[0].getWidth(), holder[0].getHeight(), BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = flattened.createGraphics();
		try
		{
			g.setColor(java.awt.Color.WHITE);
			g.fillRect(0, 0, flattened.getWidth(), flattened.getHeight());
			g.drawImage(holder[0], 0, 0, null);
		}
		finally
		{
			g.dispose();
		}
		return new LoadedImage(flattened, -1, file.getName());
	}
}
