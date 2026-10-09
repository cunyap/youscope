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
/**
 * 
 */
package org.youscope.plugin.stardist;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonUIAdapter;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;

import javax.swing.*;
import java.awt.*;

/**
 * Swing configuration UI for the StarDist cell-detection addon.
 *
 * <p>The form exposes exactly the parameters StarDist needs to run a pretrained
 * or custom model successfully: the model (a built-in name or a custom model
 * path), the two detection thresholds ({@code prob_thresh} / {@code nms_thresh}),
 * tiling for large frames, and the intensity-normalization percentiles. 
 *
 * @author Andreas P. Cuny
 */
class StarDistConfigurationAddon
        extends ComponentAddonUIAdapter<StarDistConfiguration>
{
    // Model selection
    private final JComboBox<String> modelTypeBox  = new JComboBox<>(new String[]{
            "2D_versatile_fluo", "2D_versatile_he", "2D_paper_dsb2018", "<custom path>"});
    private final JTextField customModelField      = new JTextField(30);
    private final JLabel      customModelLabel      = new JLabel("Custom model path:");

    // Detection thresholds
    private final JCheckBox  modelDefaultThreshBox = new JCheckBox(
            "Use model's optimized thresholds (recommended)", true);
    private final JSpinner   probThreshSpinner     = new JSpinner(
            new SpinnerNumberModel(0.50, 0.0, 1.0, 0.05));
    private final JSpinner   nmsThreshSpinner      = new JSpinner(
            new SpinnerNumberModel(0.40, 0.0, 1.0, 0.05));

    // Normalization + tiling
    private final JSpinner   percentileLowSpinner  = new JSpinner(
            new SpinnerNumberModel(1.0, 0.0, 50.0, 0.5));
    private final JSpinner   percentileHighSpinner = new JSpinner(
            new SpinnerNumberModel(99.8, 50.0, 100.0, 0.1));
    private final JSpinner   nTilesSpinner         = new JSpinner(
            new SpinnerNumberModel(1, 1, 32, 1));

    // Pre-processing / execution
    private final JCheckBox  invertBox             = new JCheckBox("Invert contrast (often needed for brightfield cells)");
    private final JSpinner   downscaleSpinner      = new JSpinner(
            new SpinnerNumberModel(1.0, 0.1, 1.0, 0.05));
    private final JCheckBox  gpuBox                = new JCheckBox("Use GPU (requires CUDA)");
    private final JSpinner   coresSpinner          = new JSpinner(
            new SpinnerNumberModel(0, 0, 256, 1));
    private final JSpinner   timeoutSpinner        = new JSpinner(
            new SpinnerNumberModel(900, 0, 86400, 30));

    // Server
    private final JTextField serverHostField       = new JTextField(16);
    private final JSpinner   serverPortSpinner     = new JSpinner(
            new SpinnerNumberModel(59712, 1, 65535, 1));
    private final JCheckBox  persistentServerBox   = new JCheckBox("Keep model loaded (persistent server)", true);

    // Output
    private final JCheckBox  labelImageBox         = new JCheckBox("Generate label image", true);

    StarDistConfigurationAddon(YouScopeClient client, YouScopeServer server)
            throws AddonException
    {
        super(getMetadata(), client, server);
    }

    static ComponentMetadataAdapter<StarDistConfiguration> getMetadata()
    {
        return new ComponentMetadataAdapter<StarDistConfiguration>(
                StarDistConfiguration.TYPE_IDENTIFIER,
                StarDistConfiguration.class,
                StarDistAddon.class,
                "StarDist Cell-Detection",
                new String[]{"Cell Detection"},
                "Uses StarDist to segment cells with a pretrained or custom model. "
                + "Requires the 'stardist' package in the CPython env referenced by "
                + "'cpython_executable' in graalpy_config.txt.",
                "icons/stardist-icon.png"
        );
    }

    @Override
    protected Component createUI(StarDistConfiguration configuration)
            throws AddonException
    {
        // Populate model selection
        String model = configuration.getModelType();
        boolean custom = true;
        for (int i = 0; i < modelTypeBox.getItemCount() - 1; i++)
        {
            if (modelTypeBox.getItemAt(i).equals(model))
            {
                modelTypeBox.setSelectedIndex(i);
                custom = false;
                break;
            }
        }
        if (custom)
        {
            modelTypeBox.setSelectedItem("<custom path>");
            customModelField.setText(model);
        }
        else
        {
            customModelField.setText("");
        }

        // Populate thresholds
        double prob = configuration.getProbThreshold();
        double nms  = configuration.getNmsThreshold();
        boolean useModelDefaults =
                prob < 0 || nms < 0;
        modelDefaultThreshBox.setSelected(useModelDefaults);
        probThreshSpinner.setValue(prob >= 0 ? prob : 0.50);
        nmsThreshSpinner.setValue(nms  >= 0 ? nms  : 0.40);

        // Populate the rest
        percentileLowSpinner.setValue(configuration.getPercentileLow());
        percentileHighSpinner.setValue(configuration.getPercentileHigh());
        nTilesSpinner.setValue(Math.max(1, configuration.getNTiles()));
        invertBox.setSelected(configuration.isInvert());
        downscaleSpinner.setValue(configuration.getDownscaleFactor());
        gpuBox.setSelected(configuration.isUseGpu());
        coresSpinner.setValue(configuration.getCpuCores());
        timeoutSpinner.setValue(configuration.getDetectionTimeoutSeconds());
        serverHostField.setText(configuration.getServerHost());
        serverPortSpinner.setValue(configuration.getServerPort());
        persistentServerBox.setSelected(configuration.isUsePersistentServer());
        labelImageBox.setSelected(configuration.isGenerateLabelImage());

        // Show/hide custom-path field with the model dropdown.
        Runnable syncCustomVisibility = new Runnable() {
            @Override
            public void run() {
                boolean isCustom = "<custom path>".equals(modelTypeBox.getSelectedItem());
                customModelLabel.setVisible(isCustom);
                customModelField.setVisible(isCustom);
                Container p = customModelField.getParent();
                if (p != null) {
                    p.revalidate();
                    p.repaint();
                }
            }
        };
        syncCustomVisibility.run();
        modelTypeBox.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                syncCustomVisibility.run();
            }
        });

        // Enable/disable the manual threshold spinners with the checkbox.
        Runnable syncThreshEnabled = new Runnable() {
            @Override
            public void run() {
                boolean manual = !modelDefaultThreshBox.isSelected();
                probThreshSpinner.setEnabled(manual);
                nmsThreshSpinner.setEnabled(manual);
            }
        };
        syncThreshEnabled.run();
        modelDefaultThreshBox.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                syncThreshEnabled.run();
            }
        });

        // Layout
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        int row = 0;

        addRow(panel, gbc, row++, "Model:", modelTypeBox);
        // Custom-path row uses the label FIELD so it can be shown/hidden with the field.
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(customModelLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(customModelField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        modelDefaultThreshBox.setToolTipText("<html>Pretrained and well-trained custom models ship with "
                + "probability and overlap thresholds tuned on their validation data.<br>Leave this ticked "
                + "to use them. Untick only if you want to override the thresholds manually below.</html>");
        panel.add(modelDefaultThreshBox, gbc);
        gbc.gridwidth = 1;

        addRow(panel, gbc, row++,
               "<html>Probability threshold<br><small>0..1 - higher = fewer, more confident cells</small></html>",
               probThreshSpinner);
        addRow(panel, gbc, row++,
               "<html>Overlap (NMS) threshold<br><small>0..1 - higher = allow more touching cells</small></html>",
               nmsThreshSpinner);
        addRow(panel, gbc, row++,
               "<html>Normalize percentile (low)<br><small>default 1.0</small></html>",
               percentileLowSpinner);
        addRow(panel, gbc, row++,
               "<html>Normalize percentile (high)<br><small>default 99.8</small></html>",
               percentileHighSpinner);
        addRow(panel, gbc, row++,
               "<html>Tiles per axis<br><small>1 = whole image; raise for very large frames "
               + "to bound memory</small></html>",
               nTilesSpinner);

        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        invertBox.setToolTipText("<html>StarDist expects cells LIGHT on a DARK background (like fluorescence)."
                + "<br>Brightfield cells are usually the opposite (dark / haloed on a bright field), so "
                + "inverting<br>typically makes them detectable. Enable this for brightfield.</html>");
        panel.add(invertBox, gbc);
        gbc.gridwidth = 1;

        addRow(panel, gbc, row++,
               "<html>Downscale factor<br><small>1.0 = full res; 0.5 ~ 4x faster on CPU "
               + "(masks upscaled back)</small></html>",
               downscaleSpinner);

        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(gpuBox, gbc);
        gbc.gridwidth = 1;

        addRow(panel, gbc, row++,
               "<html>CPU cores<br><small>0 = all cores; lower it to leave headroom "
               + "or avoid out-of-memory</small></html>",
               coresSpinner);
        addRow(panel, gbc, row++,
               "<html>Detection timeout (s)<br><small>0 = use graalpy_config.txt; "
               + "large frames on CPU can need minutes</small></html>",
               timeoutSpinner);

        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        persistentServerBox.setToolTipText("<html>Keeps the StarDist model loaded in a background process "
                + "between detections.<br>Strongly recommended for live-cell / feedback microscopy with "
                + "repeated detection.</html>");
        panel.add(persistentServerBox, gbc);
        gbc.gridwidth = 1;

        addRow(panel, gbc, row++,
               "<html>Server host<br><small>127.0.0.1 = local; or a remote GPU box</small></html>",
               serverHostField);
        addRow(panel, gbc, row++, "Server port:", serverPortSpinner);

        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(labelImageBox, gbc);

        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(new JLabel(
            "<html><small>stardist runs in the CPython env set by 'cpython_executable' "
            + "in graalpy_config.txt (pip install \"stardist\" plus a deep-learning backend). "
            + "Without a CUDA GPU, expect minute-scale CPU inference on large frames.</small></html>"),
            gbc);

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    @Override
    protected void commitChanges(StarDistConfiguration configuration)
    {
        Object sel = modelTypeBox.getSelectedItem();
        if ("<custom path>".equals(sel))
            configuration.setModelType(customModelField.getText().trim());
        else
            configuration.setModelType(sel == null ? "2D_versatile_fluo" : sel.toString());

        if (modelDefaultThreshBox.isSelected()) {
            configuration.setProbThreshold(StarDistConfiguration.USE_MODEL_DEFAULT);
            configuration.setNmsThreshold(StarDistConfiguration.USE_MODEL_DEFAULT);
        } else {
            configuration.setProbThreshold(((Number) probThreshSpinner.getValue()).doubleValue());
            configuration.setNmsThreshold(((Number) nmsThreshSpinner.getValue()).doubleValue());
        }

        configuration.setPercentileLow(((Number) percentileLowSpinner.getValue()).doubleValue());
        configuration.setPercentileHigh(((Number) percentileHighSpinner.getValue()).doubleValue());
        configuration.setNTiles(((Number) nTilesSpinner.getValue()).intValue());
        configuration.setInvert(invertBox.isSelected());
        configuration.setDownscaleFactor(((Number) downscaleSpinner.getValue()).doubleValue());
        configuration.setUseGpu(gpuBox.isSelected());
        configuration.setCpuCores(((Number) coresSpinner.getValue()).intValue());
        configuration.setDetectionTimeoutSeconds(((Number) timeoutSpinner.getValue()).intValue());
        configuration.setServerHost(serverHostField.getText());
        configuration.setServerPort(((Number) serverPortSpinner.getValue()).intValue());
        configuration.setUsePersistentServer(persistentServerBox.isSelected());
        configuration.setGenerateLabelImage(labelImageBox.isSelected());
    }

    @Override
    protected void initializeDefaultConfiguration(StarDistConfiguration configuration)
            throws AddonException
    {
        // Defaults already set in StarDistConfiguration field initialisers.
    }

    // Helper to add a label and component row
    private static void addRow(JPanel panel, GridBagConstraints gbc,
                                int row, String label, JComponent comp)
    {
        gbc.gridwidth = 1;
        gbc.weightx   = 0.0;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(comp, gbc);
    }
}
