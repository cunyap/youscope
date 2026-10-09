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
package org.youscope.plugin.cellpose;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonUIAdapter;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;

import javax.swing.*;
import java.awt.*;

/**
 * Swing configuration UI for the Cellpose 4 cell-detection addon.
 *
 * <p>
 * Provides a straightforward form mirroring the fields in
 * {@link CellposeConfiguration}. YouScope's generic addon UI framework
 * renders it inside the Cell Detection Job configuration dialog.
 *
 * @author Andreas P. Cuny
 */
class CellposeConfigurationAddon
                extends ComponentAddonUIAdapter<CellposeConfiguration> {
        // UI fields
        private final JComboBox<String> modelTypeBox = new JComboBox<>(new String[] {
                        "cpsam", "cpsam_v2", "cpdino", "cpdino_vitb", "<custom path>" });
        private final JTextField customModelField = new JTextField(30);
        private final JLabel customModelLabel = new JLabel("Custom model path:");
        private final JSpinner diameterSpinner = new JSpinner(
                        new SpinnerNumberModel(0.0, 0.0, 2000.0, 1.0));
        private final JSpinner flowThreshSpinner = new JSpinner(
                        new SpinnerNumberModel(0.4, 0.0, 10.0, 0.05));
        private final JSpinner cellprobSpinner = new JSpinner(
                        new SpinnerNumberModel(0.0, -6.0, 6.0, 0.1));
        private final JSpinner timeoutSpinner = new JSpinner(
                        new SpinnerNumberModel(900, 0, 86400, 30));
        private final JSpinner coresSpinner = new JSpinner(
                        new SpinnerNumberModel(0, 0, 256, 1));
        private final JCheckBox gpuBox = new JCheckBox("Use GPU (requires CUDA)");
        private final JCheckBox fastModeBox = new JCheckBox(
                        "Fast mode (resample off - faster, slightly coarser masks)");
        private final JCheckBox invertBox = new JCheckBox("Invert contrast (often needed for brightfield cells)");
        private final JSpinner downscaleSpinner = new JSpinner(
                        new SpinnerNumberModel(1.0, 0.1, 1.0, 0.05));
        private final JTextField serverHostField = new JTextField(16);
        private final JSpinner serverPortSpinner = new JSpinner(
                        new SpinnerNumberModel(59712, 1, 65535, 1));
        private final JCheckBox labelImageBox = new JCheckBox("Generate label image", true);
        private final JCheckBox flowImageBox = new JCheckBox("Save flow image (debug)");

        CellposeConfigurationAddon(YouScopeClient client, YouScopeServer server)
                        throws AddonException {
                super(getMetadata(), client, server);
        }

        static ComponentMetadataAdapter<CellposeConfiguration> getMetadata() {
                return new ComponentMetadataAdapter<CellposeConfiguration>(
                                CellposeConfiguration.TYPE_IDENTIFIER,
                                CellposeConfiguration.class,
                                CellposeAddon.class,
                                "Cellpose 4 Cell-Detection",
                                new String[] { "Cell Detection" },
                                "Uses Cellpose 4 (CellposeModel/Cellpose-SAM) to segment cells. "
                                                + "Requires cellpose>=4.0 in the CPython env referenced by "
                                                + "'cpython_executable' in graalpy_config.txt.",
                                "icons/cellpose-icon.png");
        }

        @Override
        protected Component createUI(CellposeConfiguration configuration)
                        throws AddonException {
                // Populate from configuration
                String model = configuration.getModelType();
                boolean custom = true;
                for (int i = 0; i < modelTypeBox.getItemCount() - 1; i++) {
                        if (modelTypeBox.getItemAt(i).equals(model)) {
                                modelTypeBox.setSelectedIndex(i);
                                custom = false;
                                break;
                        }
                }
                if (custom) {
                        modelTypeBox.setSelectedItem("<custom path>");
                        customModelField.setText(model);
                } else {
                        customModelField.setText("");
                }

                diameterSpinner.setValue(configuration.getCellDiameter());
                flowThreshSpinner.setValue(configuration.getFlowThreshold());
                cellprobSpinner.setValue(configuration.getCellprobThreshold());
                timeoutSpinner.setValue(configuration.getDetectionTimeoutSeconds());
                coresSpinner.setValue(configuration.getCpuCores());
                gpuBox.setSelected(configuration.isUseGpu());
                fastModeBox.setSelected(configuration.isFastMode());
                invertBox.setSelected(configuration.isInvert());
                downscaleSpinner.setValue(configuration.getDownscaleFactor());
                serverHostField.setText(configuration.getServerHost());
                serverPortSpinner.setValue(configuration.getServerPort());
                labelImageBox.setSelected(configuration.isGenerateLabelImage());
                flowImageBox.setSelected(configuration.isSaveFlowImage());

                // Show/hide custom path field (label and field together), and repaint the
                // container immediately and without revalidate()/repaint() the field only
                // appears after the window is resized.
                Runnable syncCustomVisibility = new Runnable() {
                        @Override
                        public void run() {
                                boolean custom = "<custom path>".equals(modelTypeBox.getSelectedItem());
                                customModelLabel.setVisible(custom);
                                customModelField.setVisible(custom);
                                java.awt.Container p = customModelField.getParent();
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

                // Layout
                JPanel panel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(3, 6, 3, 6);
                gbc.fill = GridBagConstraints.HORIZONTAL;

                int row = 0;

                addRow(panel, gbc, row++, "Model:", modelTypeBox);
                // Custom-path row uses the label FIELD (not an inline label) so it can be
                // shown/hidden together with the text field.
                gbc.gridwidth = 1;
                gbc.weightx = 0.0;
                gbc.gridx = 0;
                gbc.gridy = row;
                panel.add(customModelLabel, gbc);
                gbc.gridx = 1;
                gbc.weightx = 1.0;
                panel.add(customModelField, gbc);
                row++;
                addRow(panel, gbc, row++,
                                "<html>Cell diameter (px)<br><small>0 = auto-estimate</small></html>",
                                diameterSpinner);
                addRow(panel, gbc, row++,
                                "<html>Flow threshold<br><small>default 0.4</small></html>",
                                flowThreshSpinner);
                addRow(panel, gbc, row++,
                                "<html>Cell probability threshold<br><small>default 0.0</small></html>",
                                cellprobSpinner);
                addRow(panel, gbc, row++,
                                "<html>Detection timeout (s)<br><small>0 = use graalpy_config.txt; "
                                                + "cpsam on CPU can need minutes</small></html>",
                                timeoutSpinner);
                addRow(panel, gbc, row++,
                                "<html>CPU cores<br><small>0 = all cores; lower it to leave headroom "
                                                + "or avoid out-of-memory</small></html>",
                                coresSpinner);

                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                panel.add(gpuBox, gbc);

                gbc.gridy = row++;
                panel.add(fastModeBox, gbc);

                gbc.gridy = row++;
                invertBox.setToolTipText("<html>Cellpose (incl. cpsam) expects cells LIGHT on a "
                                + "DARK background (like fluorescence).<br>Brightfield cells are usually the "
                                + "opposite (dark / haloed on a bright field), so inverting<br>typically makes "
                                + "them detectable. Enable this for brightfield.</html>");
                panel.add(invertBox, gbc);

                gbc.gridwidth = 1;
                addRow(panel, gbc, row++,
                                "<html>Downscale factor<br><small>1.0 = full res; 0.5 ~ 4x faster on CPU"
                                                + " (masks upscaled back)</small></html>",
                                downscaleSpinner);
                addRow(panel, gbc, row++,
                                "<html>Server host<br><small>127.0.0.1 = local; or a remote GPU box</small></html>",
                                serverHostField);
                addRow(panel, gbc, row++, "Server port:", serverPortSpinner);

                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                panel.add(labelImageBox, gbc);

                gbc.gridy = row++;
                panel.add(flowImageBox, gbc);

                gbc.gridy = row;
                gbc.gridwidth = 2;
                gbc.weighty = 1.0;
                panel.add(new JLabel(
                                "<html><small>cellpose runs in the CPython env set by 'cpython_executable' "
                                                + "in graalpy_config.txt (pip install \"cellpose&gt;=4\" torch scipy). "
                                                + "No CUDA GPU on this host &rarr; expect minute-scale CPU inference.</small></html>"),
                                gbc);

                JScrollPane scroll = new JScrollPane(panel);
                scroll.setBorder(BorderFactory.createEmptyBorder());
                return scroll;
        }

        @Override
        protected void commitChanges(CellposeConfiguration configuration) {
                Object sel = modelTypeBox.getSelectedItem();
                if ("<custom path>".equals(sel))
                        configuration.setModelType(customModelField.getText().trim());
                else
                        configuration.setModelType(sel == null ? "cpsam" : sel.toString());

                configuration.setCellDiameter(((Number) diameterSpinner.getValue()).doubleValue());
                configuration.setFlowThreshold(((Number) flowThreshSpinner.getValue()).doubleValue());
                configuration.setCellprobThreshold(((Number) cellprobSpinner.getValue()).doubleValue());
                configuration.setDetectionTimeoutSeconds(((Number) timeoutSpinner.getValue()).intValue());
                configuration.setCpuCores(((Number) coresSpinner.getValue()).intValue());
                configuration.setUseGpu(gpuBox.isSelected());
                configuration.setFastMode(fastModeBox.isSelected());
                configuration.setInvert(invertBox.isSelected());
                configuration.setDownscaleFactor(((Number) downscaleSpinner.getValue()).doubleValue());
                configuration.setServerHost(serverHostField.getText());
                configuration.setServerPort(((Number) serverPortSpinner.getValue()).intValue());
                configuration.setGenerateLabelImage(labelImageBox.isSelected());
                configuration.setSaveFlowImage(flowImageBox.isSelected());
        }

        @Override
        protected void initializeDefaultConfiguration(CellposeConfiguration configuration)
                        throws AddonException {
                // Defaults already set in CellposeConfiguration field initialisers.
        }

        // Helper to add a label+component row
        private static void addRow(JPanel panel, GridBagConstraints gbc,
                        int row, String label, JComponent comp) {
                gbc.gridwidth = 1;
                gbc.weightx = 0.0;
                gbc.gridx = 0;
                gbc.gridy = row;
                panel.add(new JLabel(label), gbc);
                gbc.gridx = 1;
                gbc.weightx = 1.0;
                panel.add(comp, gbc);
        }
}