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
package org.youscope.plugin.simplefocusscores;

import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.ResourceException;

class ImageAdapter {

	private final double[][] image;
	private final int height;
	private final int width;
	private double mean = -1;
	private double variance = -1;
	ImageAdapter(ImageEvent<?> imageEvent) throws ResourceException 
	{
		int bands = imageEvent.getBands();
		width = imageEvent.getWidth();
		height = imageEvent.getHeight();
		if(bands > 1)
			throw new ResourceException("Focus score algorithm only supports grayscale images");
		int bytesPerPixel = imageEvent.getBytesPerPixel();
		int bitDepth = imageEvent.getBitDepth();
		double maxExpectValue = Math.pow(2, bitDepth)-1;
		image = new double[height][width];
		if(bytesPerPixel == 1)
		{
			byte[] imageData = (byte[])imageEvent.getImageData();
			for(int j=0;j<height;j++)
			{
				for(int i=0;i<width;i++)
				{
					image[j][i] = (imageData[j*width+i] & 0xff)/maxExpectValue;
				}
			}	
		}
		else if(bytesPerPixel == 2)
		{
			short[] imageData = (short[])imageEvent.getImageData();
			for(int j=0;j<height;j++)
			{
				for(int i=0;i<width;i++)
				{
					image[j][i] = (imageData[j*width+i] & 0xffff)/maxExpectValue;
				}
			}
		}
		else
			throw new ResourceException("Focus score algorithm only supports grayscale images with 1 or 2 bytes per pixel. Current image has " + Integer.toString(bytesPerPixel) + " bytes per pixel.");
	}
	
	double[][] getScaledImage()
	{
		return image;
	}
	int getWidth()
	{
		return width;
	}
	int getHeight()
	{
		return height;
	}
	
	double getMean()
	{
		if(mean >= 0)
			return mean;
		mean = 0;
		for(int j=0;j<height;j++)
		{
			for(int i=0;i<width;i++)
			{
				mean+=image[j][i]/width/height;
			}
		}
		return mean;	
	}
	
	double getVariance()
	{
		if(variance >= 0)
			return variance;
		double mean = getMean();
		variance = 0;
		for(int j=0;j<height;j++)
		{
			for(int i=0;i<width;i++)
			{
				variance += Math.pow(image[j][i]-mean, 2)/width/height;
			}
		}
		return variance;
	}
}
