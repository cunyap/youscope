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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonUIAdapter;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.configuration.CameraConfiguration;
import org.youscope.common.configuration.ChannelConfiguration;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.CameraField;
import org.youscope.uielements.ChannelField;
import org.youscope.uielements.DoubleTextField;

/**
 * Configuration UI for the four-camera TIE job.
 *
 * <p>The camera controls intentionally reuse the existing CameraField rather
 * than changing the global single-camera UI abstraction.</p>
 *
 * @author Jonas Meier - original TIE plugin; UI migrated to current fields
 */
class TieJobConfigurationAddon extends ComponentAddonUIAdapter<TieJobConfiguration>
{
    private CameraField[] cameraFields;
    private ChannelField channelField;
    private DoubleTextField exposureField;
    private JCheckBox saveImagesField;
    private JTextField imageSaveNameField;
    private DoubleTextField dzNearField;
    private DoubleTextField dzFarField;
    private DoubleTextField wavelengthField;
    private DoubleTextField pixelSizeField;
    private DoubleTextField numericalApertureField;
    private DoubleTextField magnificationField;
    private DoubleTextField minRadianField;
    private DoubleTextField maxRadianField;

    TieJobConfigurationAddon(YouScopeClient client, YouScopeServer server) throws AddonException
    {
        super(getMetadata(), client, server);
    }

    static ComponentMetadataAdapter<TieJobConfiguration> getMetadata()
    {
        return new ComponentMetadataAdapter<TieJobConfiguration>(
                TieJobConfiguration.TYPE_IDENTIFIER,
                TieJobConfiguration.class,
                TieJob.class,
                "TIE (four cameras)",
                new String[] {"Imaging"},
                "Acquires four focal planes in parallel and produces a TIE axial-intensity-derivative preview.",
                (String)null);
    }

    @Override
    protected void initializeDefaultConfiguration(TieJobConfiguration configuration) throws AddonException
    {
        // Defaults are already defined by TieJobConfiguration.
    }

    @Override
    protected Component createUI(TieJobConfiguration configuration) throws AddonException
    {
        JPanel root = new JPanel(new BorderLayout(5, 5));

        JPanel cameras = new JPanel(new GridLayout(TieJobConfiguration.NUM_CAMERAS, 2, 5, 5));
        cameraFields = new CameraField[TieJobConfiguration.NUM_CAMERAS];
        CameraConfiguration[] cameraConfigurations = configuration.getCameraConfigurations();
        for (int i = 0; i < TieJobConfiguration.NUM_CAMERAS; i++)
        {
            cameras.add(new JLabel("Camera " + (i + 1) + ":"));
            cameraFields[i] = new CameraField(cameraConfigurations[i], getClient(), getServer());
            cameras.add(cameraFields[i]);
        }

        JPanel acquisition = new JPanel(new GridLayout(0, 2, 5, 5));
        channelField = new ChannelField(configuration.getChannelConfiguration(), getClient(), getServer());
        acquisition.add(new JLabel("Channel:"));
        acquisition.add(channelField);

        exposureField = new DoubleTextField(configuration.getExposure());
        acquisition.add(new JLabel("Exposure (ms):"));
        acquisition.add(exposureField);

        saveImagesField = new JCheckBox("Save reconstructed preview", configuration.isSaveImages());
        acquisition.add(saveImagesField);
        imageSaveNameField = new JTextField(configuration.getImageSaveName());
        acquisition.add(imageSaveNameField);

        JPanel optics = new JPanel(new GridLayout(0, 2, 5, 5));
        dzNearField = addDouble(optics, "Near dz (m):", configuration.getDzNear());
        dzFarField = addDouble(optics, "Far dz (m):", configuration.getDzFar());
        wavelengthField = addDouble(optics, "Wavelength (m):", configuration.getWavelength());
        pixelSizeField = addDouble(optics, "Pixel size (m):", configuration.getPixelSize());
        numericalApertureField = addDouble(optics, "Numerical aperture:", configuration.getNumericalAperture());
        magnificationField = addDouble(optics, "Magnification:", configuration.getMagnification());
        minRadianField = addDouble(optics, "Minimum radian:", configuration.getMinRadian());
        maxRadianField = addDouble(optics, "Maximum radian:", configuration.getMaxRadian());

        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.add(cameras, BorderLayout.NORTH);
        center.add(acquisition, BorderLayout.CENTER);
        center.add(optics, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        return root;
    }

    private DoubleTextField addDouble(JPanel panel, String label, double value)
    {
        panel.add(new JLabel(label));
        DoubleTextField field = new DoubleTextField(value);
        panel.add(field);
        return field;
    }

    @Override
    protected void commitChanges(TieJobConfiguration configuration)
    {
        CameraConfiguration[] cameras = new CameraConfiguration[TieJobConfiguration.NUM_CAMERAS];
        for (int i = 0; i < cameras.length; i++)
            cameras[i] = cameraFields[i].getCameraConfiguration();
        configuration.setCameraConfigurations(cameras);
        configuration.setChannelConfiguration(channelField.getChannelConfiguration());
        configuration.setExposure(exposureField.getValue());
        configuration.setSaveImages(saveImagesField.isSelected());
        configuration.setImageSaveName(imageSaveNameField.getText());
        configuration.setDzNear(dzNearField.getValue());
        configuration.setDzFar(dzFarField.getValue());
        configuration.setWavelength(wavelengthField.getValue());
        configuration.setPixelSize(pixelSizeField.getValue());
        configuration.setNumericalAperture(numericalApertureField.getValue());
        configuration.setMagnification(magnificationField.getValue());
        configuration.setMinRadian(minRadianField.getValue());
        configuration.setMaxRadian(maxRadianField.getValue());
    }
}
