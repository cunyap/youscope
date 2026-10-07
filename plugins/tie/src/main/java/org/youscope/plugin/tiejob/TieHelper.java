/*******************************************************************************
 * Copyright (c) 2023 - 2024 Jonas Meier.
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
 *     Jonas Meier - initial API and implementation
 *     Andreas P. Cuny - migration to the current YouScope API
 ******************************************************************************/
package org.youscope.plugin.tiejob;

import org.youscope.common.image.ImageEvent;

/**
 * TIE calculation helpers.
 *
 * <p>The historical helper returned an FFT of one input image. That was useful
 * during development but was not a validated TIE phase reconstruction. The
 * migrated helper therefore implements the first physically meaningful,
 * testable step: estimating dI/dz from the four focal planes.</p>
 *
 * <p>Camera order is fixed as: -dzFar, -dzNear, +dzNear, +dzFar. The returned
 * 16-bit preview is signed derivative data encoded around 32768.</p>
 *
 * @author Jonas Meier - original TIE implementation; calculation modernized
 */
final class TieHelper
{
    private TieHelper() { }

    static ImageEvent<short[]> calculateTieDerivativePreview(ImageEvent<?>[] images,
            double dzNear, double dzFar) throws Exception
    {
        if (images == null || images.length != TieJobConfiguration.NUM_CAMERAS)
            throw new Exception("Expected exactly four TIE images.");
        if (dzNear <= 0 || dzFar <= dzNear)
            throw new Exception("Invalid TIE defocus distances.");

        int width = images[0].getWidth();
        int height = images[0].getHeight();
        int bands = images[0].getBands();
        int bytesPerPixel = images[0].getBytesPerPixel();

        if (bands != 1)
            throw new Exception("TIE preview currently supports grayscale images only.");
        if (bytesPerPixel != 1 && bytesPerPixel != 2 && bytesPerPixel != 4)
            throw new Exception("Unsupported image pixel size: " + bytesPerPixel + " bytes.");

        for (int i = 1; i < images.length; i++)
        {
            if (images[i].getWidth() != width || images[i].getHeight() != height)
                throw new Exception("All four TIE images must have identical dimensions.");
            if (images[i].getBands() != bands || images[i].getBytesPerPixel() != bytesPerPixel)
                throw new Exception("All four TIE images must have identical grayscale pixel formats.");
        }

        double[] z = new double[] {-dzFar, -dzNear, dzNear, dzFar};
        double denominator = 0;
        double[] mean = new double[1];
        for (int i = 0; i < z.length; i++)
            denominator += z[i] * z[i];

        double[] derivative = new double[width * height];
        double maxAbs = 0;
        double[] values = new double[4];

        for (int p = 0; p < derivative.length; p++)
        {
            double sum = 0;
            for (int i = 0; i < 4; i++)
            {
                values[i] = pixel(images[i], p);
                sum += values[i];
            }
            mean[0] = sum / 4.0;

            double slope = 0;
            for (int i = 0; i < 4; i++)
                slope += z[i] * (values[i] - mean[0]);
            slope /= denominator;
            derivative[p] = slope;
            maxAbs = Math.max(maxAbs, Math.abs(slope));
        }

        short[] result = new short[derivative.length];
        double scale = maxAbs > 0 ? 32767.0 / maxAbs : 0;
        for (int p = 0; p < result.length; p++)
        {
            double encoded = 32768.0 + derivative[p] * scale;
            if (encoded < 0) encoded = 0;
            if (encoded > 65535) encoded = 65535;
            result[p] = (short)Math.round(encoded);
        }

        return ImageEvent.createImage(result, width, height, 16);
    }

    private static double pixel(ImageEvent<?> image, int index) throws Exception
    {
        Object data = image.getImageData();
        if (data instanceof byte[])
            return ((byte[])data)[index] & 0xff;
        if (data instanceof short[])
            return ((short[])data)[index] & 0xffff;
        if (data instanceof int[])
            return ((int[])data)[index] & 0xffffffffL;
        throw new Exception("Unsupported image data type: " + data.getClass().getName());
    }
}
