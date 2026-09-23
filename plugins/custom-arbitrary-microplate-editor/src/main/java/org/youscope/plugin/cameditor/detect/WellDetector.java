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
package org.youscope.plugin.cameditor.detect;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

/**
 * Headless, UI-free well detector. This is the Java/BoofCV port of pyCAME's scikit-image pipeline:
 * grayscale -> threshold -> morphological closing -> connected components -> bounding
 * boxes filtered by area. It works entirely in image pixel coordinates; scaling to micrometres is the
 * caller's responsibility (via {@code ScaleModel}), applied exactly once.
 * <p>
 * <b>Dependency isolation:</b> this is the only class that references BoofCV. It targets the long-stable
 * BoofCV API ({@code ConvertBufferedImage}, {@code ThresholdImageOps}, {@code BinaryImageOps},
 * {@code GrayU8}/{@code GrayS32}, {@code ConnectRule}) and is pinned to BoofCV 1.1.x in build.gradle.
 * If a future BoofCV changes a signature, only this file needs adjusting.
 *
 * @author Andreas P. Cuny
 */
public final class WellDetector
{
	/**
	 * Parameters mirroring pyCAME's three controls.
	 */
	public static final class Parameters
	{
		/** Intensity threshold (0..255). Structures darker than this are treated as foreground. */
		public int intensityThreshold = 100;
		/** Minimum connected-component area in pixels; smaller blobs are discarded. */
		public int minAreaPixels = 100;
		/**
		 * If {@code true}, foreground = pixels strictly below the threshold (dark structures on a light
		 * background, as in pyCAME). If {@code false}, foreground = pixels above the threshold.
		 */
		public boolean foregroundIsDark = true;
		/** Number of morphological closing iterations (dilate then erode). 0 disables closing. */
		public int closingIterations = 1;

		public Parameters()
		{
		}

		public Parameters(int intensityThreshold, int minAreaPixels, boolean foregroundIsDark, int closingIterations)
		{
			this.intensityThreshold = intensityThreshold;
			this.minAreaPixels = minAreaPixels;
			this.foregroundIsDark = foregroundIsDark;
			this.closingIterations = closingIterations;
		}
	}

	/**
	 * Detects wells in the given (already single-channel or convertible) image.
	 * @param image backdrop image (any type; converted to 8-bit grayscale internally).
	 * @param parameters detection parameters.
	 * @return detected wells in image pixel coordinates, never {@code null}.
	 */
	public List<DetectedWell> detect(BufferedImage image, Parameters parameters)
	{
		final List<DetectedWell> result = new ArrayList<DetectedWell>();
		if(image == null)
			return result;
		if(parameters == null)
			parameters = new Parameters();

		// 1) to 8-bit grayscale
		GrayU8 gray = ConvertBufferedImage.convertFrom(image, (GrayU8) null);

		// 2) threshold -> binary (1 = foreground)
		GrayU8 binary = new GrayU8(gray.width, gray.height);
		ThresholdImageOps.threshold(gray, binary, parameters.intensityThreshold, parameters.foregroundIsDark);

		// 3) morphological closing (dilate then erode) to bridge small gaps, like pyCAME's 3x3 closing
		if(parameters.closingIterations > 0)
		{
			GrayU8 tmp = new GrayU8(gray.width, gray.height);
			GrayU8 closed = new GrayU8(gray.width, gray.height);
			BinaryImageOps.dilate8(binary, parameters.closingIterations, tmp);
			BinaryImageOps.erode8(tmp, parameters.closingIterations, closed);
			binary = closed;
		}

		// 4) connected components. We ignore the returned contour list (its package moved across
		//    BoofCV versions) and instead read blob ids from the labeled image directly.
		GrayS32 labeled = new GrayS32(gray.width, gray.height);
		List<?> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, labeled);
		int numLabels = contours == null ? 0 : contours.size();
		if(numLabels <= 0)
			return result;

		// 5) accumulate per-label bounding box, area and centroid from the labeled image
		int[] minX = new int[numLabels + 1];
		int[] minY = new int[numLabels + 1];
		int[] maxX = new int[numLabels + 1];
		int[] maxY = new int[numLabels + 1];
		long[] sumX = new long[numLabels + 1];
		long[] sumY = new long[numLabels + 1];
		int[] area = new int[numLabels + 1];
		for(int i = 0; i <= numLabels; i++)
		{
			minX[i] = Integer.MAX_VALUE;
			minY[i] = Integer.MAX_VALUE;
			maxX[i] = Integer.MIN_VALUE;
			maxY[i] = Integer.MIN_VALUE;
		}
		for(int yy = 0; yy < labeled.height; yy++)
		{
			int rowStart = labeled.startIndex + yy * labeled.stride;
			for(int xx = 0; xx < labeled.width; xx++)
			{
				int label = labeled.data[rowStart + xx];
				if(label <= 0 || label > numLabels)
					continue;
				if(xx < minX[label]) minX[label] = xx;
				if(yy < minY[label]) minY[label] = yy;
				if(xx > maxX[label]) maxX[label] = xx;
				if(yy > maxY[label]) maxY[label] = yy;
				sumX[label] += xx;
				sumY[label] += yy;
				area[label]++;
			}
		}

		// 6) emit blobs passing the area filter
		for(int label = 1; label <= numLabels; label++)
		{
			if(area[label] < parameters.minAreaPixels || area[label] <= 0)
				continue;
			double w = (maxX[label] - minX[label]) + 1;
			double h = (maxY[label] - minY[label]) + 1;
			double cx = (double) sumX[label] / area[label];
			double cy = (double) sumY[label] / area[label];
			result.add(new DetectedWell(minX[label], minY[label], w, h, cx, cy, area[label]));
		}
		return result;
	}
}
