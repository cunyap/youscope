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
 *     Jonas Meier - initial API and implementation
 *     Andreas P. Cuny - migration to the current YouScope API
 ******************************************************************************/
package org.youscope.plugin.tiejob;

import org.youscope.common.configuration.CameraConfiguration;
import org.youscope.common.configuration.ChannelConfiguration;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.image.ImageProducerConfiguration;
import org.youscope.common.job.JobConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;

/**
 * Configuration of a four-camera Transport of Intensity Equation (TIE) job.
 *
 * <p>The four-camera concept and reconstruction parameters originate from the
 * historical TIE plugin. The configuration has been migrated to current
 * YouScope CameraConfiguration and ChannelConfiguration objects.</p>
 *
 * @author Jonas Meier - original TIE implementation
 */
@XStreamAlias("tie-job")
public class TieJobConfiguration implements JobConfiguration, ImageProducerConfiguration
{
    private static final long serialVersionUID = 1L;

    public static final String TYPE_IDENTIFIER = "YouScope.TieJob";
    public static final int NUM_CAMERAS = 4;

    @XStreamAlias("camera-configuration-1")
    private CameraConfiguration cameraConfiguration1 = new CameraConfiguration();
    @XStreamAlias("camera-configuration-2")
    private CameraConfiguration cameraConfiguration2 = new CameraConfiguration();
    @XStreamAlias("camera-configuration-3")
    private CameraConfiguration cameraConfiguration3 = new CameraConfiguration();
    @XStreamAlias("camera-configuration-4")
    private CameraConfiguration cameraConfiguration4 = new CameraConfiguration();

    @XStreamAlias("channel-configuration")
    private ChannelConfiguration channelConfiguration = new ChannelConfiguration();

    @XStreamAlias("exposure-ms")
    @XStreamAsAttribute
    private double exposure = 5.0;

    @XStreamAlias("save-images")
    @XStreamConverter(value = BooleanConverter.class, booleans = {false}, strings = {"yes", "no"})
    private boolean saveImages = true;

    @XStreamAlias("image-save-name")
    private String imageSaveName = "tie";

    /* Historical TIE parameters retained for the eventual phase solver. */
    @XStreamAlias("dz-near-m")
    @XStreamAsAttribute
    private double dzNear = 0.5e-6;
    @XStreamAlias("dz-far-m")
    @XStreamAsAttribute
    private double dzFar = 1.0e-6;
    @XStreamAlias("wavelength-m")
    @XStreamAsAttribute
    private double wavelength = 630e-9;
    @XStreamAlias("pixel-size-m")
    @XStreamAsAttribute
    private double pixelSize = 6508.3e-9;
    @XStreamAlias("numerical-aperture")
    @XStreamAsAttribute
    private double numericalAperture = 0.75;
    @XStreamAlias("magnification")
    @XStreamAsAttribute
    private double magnification = 20.0;
    @XStreamAlias("max-radian")
    @XStreamAsAttribute
    private double maxRadian = 120.0;
    @XStreamAlias("min-radian")
    @XStreamAsAttribute
    private double minRadian = -10.0;

    @Override
    public String getTypeIdentifier() { return TYPE_IDENTIFIER; }

    @Override
    public String getDescription()
    {
        return "Four-camera TIE axial intensity derivative preview.";
    }

    public CameraConfiguration getCameraConfiguration1() { return new CameraConfiguration(cameraConfiguration1); }
    public CameraConfiguration getCameraConfiguration2() { return new CameraConfiguration(cameraConfiguration2); }
    public CameraConfiguration getCameraConfiguration3() { return new CameraConfiguration(cameraConfiguration3); }
    public CameraConfiguration getCameraConfiguration4() { return new CameraConfiguration(cameraConfiguration4); }

    public void setCameraConfiguration1(CameraConfiguration c) { cameraConfiguration1 = c == null ? new CameraConfiguration() : new CameraConfiguration(c); }
    public void setCameraConfiguration2(CameraConfiguration c) { cameraConfiguration2 = c == null ? new CameraConfiguration() : new CameraConfiguration(c); }
    public void setCameraConfiguration3(CameraConfiguration c) { cameraConfiguration3 = c == null ? new CameraConfiguration() : new CameraConfiguration(c); }
    public void setCameraConfiguration4(CameraConfiguration c) { cameraConfiguration4 = c == null ? new CameraConfiguration() : new CameraConfiguration(c); }

    public CameraConfiguration[] getCameraConfigurations()
    {
        return new CameraConfiguration[] {
            getCameraConfiguration1(), getCameraConfiguration2(),
            getCameraConfiguration3(), getCameraConfiguration4()
        };
    }

    public void setCameraConfigurations(CameraConfiguration[] cameras)
    {
        if (cameras == null || cameras.length != NUM_CAMERAS)
            throw new IllegalArgumentException("TIE requires exactly four camera configurations.");
        setCameraConfiguration1(cameras[0]);
        setCameraConfiguration2(cameras[1]);
        setCameraConfiguration3(cameras[2]);
        setCameraConfiguration4(cameras[3]);
    }

    public String[] getCameras()
    {
        String[] result = new String[NUM_CAMERAS];
        CameraConfiguration[] configurations = getCameraConfigurations();
        for (int i = 0; i < NUM_CAMERAS; i++)
            result[i] = configurations[i].getCameraDevice();
        return result;
    }

    public ChannelConfiguration getChannelConfiguration()
    {
        return new ChannelConfiguration(channelConfiguration);
    }

    public void setChannelConfiguration(ChannelConfiguration configuration)
    {
        channelConfiguration = configuration == null ? new ChannelConfiguration() : new ChannelConfiguration(configuration);
    }

    public double getExposure() { return exposure; }
    public void setExposure(double exposure) { this.exposure = exposure; }
    public boolean isSaveImages() { return saveImages; }
    public void setSaveImages(boolean saveImages) { this.saveImages = saveImages; }
    public String getImageSaveName() { return imageSaveName; }
    public void setImageSaveName(String imageSaveName) { this.imageSaveName = imageSaveName; }
    public double getDzNear() { return dzNear; }
    public void setDzNear(double value) { dzNear = value; }
    public double getDzFar() { return dzFar; }
    public void setDzFar(double value) { dzFar = value; }
    public double getWavelength() { return wavelength; }
    public void setWavelength(double value) { wavelength = value; }
    public double getPixelSize() { return pixelSize; }
    public void setPixelSize(double value) { pixelSize = value; }
    public double getNumericalAperture() { return numericalAperture; }
    public void setNumericalAperture(double value) { numericalAperture = value; }
    public double getMagnification() { return magnification; }
    public void setMagnification(double value) { magnification = value; }
    public double getMaxRadian() { return maxRadian; }
    public void setMaxRadian(double value) { maxRadian = value; }
    public double getMinRadian() { return minRadian; }
    public void setMinRadian(double value) { minRadian = value; }

    @Override
    public String[] getImageSaveNames()
    {
        return saveImages ? new String[] {imageSaveName} : null;
    }

    @Override
    public int getNumberOfImages() { return 1; }

    @Override
    public void checkConfiguration() throws ConfigurationException
    {
        CameraConfiguration[] cameras = getCameraConfigurations();
        String[] ids = new String[NUM_CAMERAS];
        for (int i = 0; i < NUM_CAMERAS; i++)
        {
            cameras[i].checkConfiguration();
            ids[i] = cameras[i].getCameraDevice();
            if (ids[i] == null || ids[i].trim().length() == 0)
                throw new ConfigurationException("TIE camera " + (i + 1) + " is not selected.");
            for (int j = 0; j < i; j++)
                if (ids[i].equals(ids[j]))
                    throw new ConfigurationException("TIE camera " + (i + 1) + " is already selected as camera " + (j + 1) + ".");
        }

        if (channelConfiguration != null)
            channelConfiguration.checkConfiguration();
        if (exposure <= 0)
            throw new ConfigurationException("Exposure must be greater than zero.");
        if (dzNear <= 0 || dzFar <= 0)
            throw new ConfigurationException("TIE defocus distances must be greater than zero.");
        if (dzFar <= dzNear)
            throw new ConfigurationException("Far defocus distance must be larger than near defocus distance.");
        if (wavelength <= 0 || pixelSize <= 0 || numericalAperture <= 0 || magnification <= 0)
            throw new ConfigurationException("Optical TIE parameters must be greater than zero.");
        if (minRadian > maxRadian)
            throw new ConfigurationException("Minimum radian value must not exceed maximum radian value.");
        if (imageSaveName == null || imageSaveName.trim().length() == 0)
            throw new ConfigurationException("Image save name must not be empty.");
    }
}
