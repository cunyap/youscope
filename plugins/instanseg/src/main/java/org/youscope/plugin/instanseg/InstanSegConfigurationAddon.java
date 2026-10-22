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
package org.youscope.plugin.instanseg;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonUIAdapter;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;

import javax.swing.*;
import java.awt.*;

/**
 * Swing configuration UI for the InstanSeg cell-detection addon.
 *
 * <p>
 * A straightforward form mirroring the fields of
 * {@link InstanSegConfiguration}.
 * YouScope's generic addon UI framework renders it inside the Cell Detection
 * Job configuration dialog.
 *
 * @author Andreas P. Cuny
 */
class InstanSegConfigurationAddon extends ComponentAddonUIAdapter<InstanSegConfiguration> {
        private static final String CUSTOM_MODEL = "<custom folder>";

        private static final String[] PROCESSING_LABELS = { "Auto (tile large images)", "Whole image", "Tiled" };
        private static final String[] PROCESSING_VALUES = { InstanSegConfiguration.PROCESSING_AUTO,
                        InstanSegConfiguration.PROCESSING_WHOLE, InstanSegConfiguration.PROCESSING_TILED };

        /**
         * Spinner for an optional value where any negative number means "use the model
         * default". Stepping up from the default jumps to 0, stepping down from 0 (or
         * any value that would go negative) returns to the default, so the user never
         * lands on a meaningless value such as -0.95.
         */
        private static final class OptionalDoubleModel extends SpinnerNumberModel {
                private static final long serialVersionUID = 1L;

                OptionalDoubleModel(double max, double step) {
                        super(-1.0, -1.0, max, step);
                }

                private static Double round(double v) {
                        return Double.valueOf(Math.round(v * 1000.0) / 1000.0);
                }

                @Override
                public Object getNextValue() {
                        double v = ((Number) getValue()).doubleValue();
                        if (v < 0)
                                return Double.valueOf(0.0);
                        Object n = super.getNextValue();
                        return n == null ? null : round(((Number) n).doubleValue());
                }

                @Override
                public Object getPreviousValue() {
                        double v = ((Number) getValue()).doubleValue();
                        if (v < 0)
                                return null;
                        Object p = super.getPreviousValue();
                        if (p == null || ((Number) p).doubleValue() < 0)
                                return Double.valueOf(-1.0);
                        return round(((Number) p).doubleValue());
                }
        }

        // UI fields
        private final JComboBox<String> modelTypeBox = new JComboBox<>(new String[] {
                        "fluorescence_nuclei_and_cells", "brightfield_nuclei", "single_channel_nuclei",
                        CUSTOM_MODEL });
        private final JTextField customModelField = new JTextField(30);
        private final JLabel customModelLabel = new JLabel("Custom model folder:");
        private final JComboBox<String> targetBox = new JComboBox<>(new String[] { "Nuclei", "Cells (whole cell)" });
        private final JSpinner pixelSizeSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1000.0, 0.05));
        private final JComboBox<String> deviceBox = new JComboBox<>(new String[] { "cpu", "cuda", "mps", "auto" });
        private final JComboBox<String> processingBox = new JComboBox<>(PROCESSING_LABELS);
        private final JSpinner tileSizeSpinner = new JSpinner(new SpinnerNumberModel(512, 64, 8192, 64));
        private final JCheckBox normaliseBox = new JCheckBox("Normalise image (percentile; recommended)", true);
        private final JCheckBox invertBox = new JCheckBox("Invert contrast");
        private final JSpinner channelsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 64, 1));
        private final JSpinner seedSpinner = new JSpinner(new OptionalDoubleModel(1.0, 0.05));
        private final JSpinner overlapSpinner = new JSpinner(new OptionalDoubleModel(1.0, 0.05));
        private final JSpinner maskSpinner = new JSpinner(new OptionalDoubleModel(1.0, 0.05));
        private final JSpinner minSizeSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 1000000, 1));
        private final JSpinner peakDistanceSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 1000, 1));
        private final JCheckBox cleanupBox = new JCheckBox("Clean up fragments", true);
        private final JCheckBox resolveBox = new JCheckBox("Resolve cell and nucleus (cells + nuclei models)", true);
        private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(900, 0, 86400, 30));
        private final JSpinner coresSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 256, 1));
        private final JCheckBox labelImageBox = new JCheckBox("Generate label image", true);

        InstanSegConfigurationAddon(YouScopeClient client, YouScopeServer server) throws AddonException {
                super(getMetadata(), client, server);
        }

        static ComponentMetadataAdapter<InstanSegConfiguration> getMetadata() {
                return new ComponentMetadataAdapter<InstanSegConfiguration>(
                                InstanSegConfiguration.TYPE_IDENTIFIER,
                                InstanSegConfiguration.class,
                                InstanSegAddon.class,
                                "InstanSeg Cell-Detection",
                                new String[] { "Cell Detection" },
                                "Uses InstanSeg (embedding-based instance segmentation) to segment nuclei or "
                                                + "whole cells. Requires instanseg-torch in the CPython env referenced "
                                                + "by 'instanseg_cpython_executable' / 'cpython_executable' in "
                                                + "graalpy_config.txt.",
                                "icons/instanseg-icon.png");
        }

        @Override
        protected Component createUI(InstanSegConfiguration configuration) throws AddonException {
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
                        modelTypeBox.setSelectedItem(CUSTOM_MODEL);
                        customModelField.setText(model);
                } else {
                        customModelField.setText("");
                }
                targetBox.setSelectedIndex(
                                InstanSegConfiguration.TARGET_CELLS.equals(configuration.getTarget()) ? 1 : 0);
                pixelSizeSpinner.setValue(configuration.getPixelSizeUm());
                pixelSizeSpinner.setEditor(new JSpinner.NumberEditor(pixelSizeSpinner, "0.0###"));
                deviceBox.setSelectedItem(configuration.getDevice());
                int procIdx = 0;
                for (int i = 0; i < PROCESSING_VALUES.length; i++)
                        if (PROCESSING_VALUES[i].equals(configuration.getProcessingMode()))
                                procIdx = i;
                processingBox.setSelectedIndex(procIdx);
                tileSizeSpinner.setValue(configuration.getTileSize());
                normaliseBox.setSelected(configuration.isNormalise());
                invertBox.setSelected(configuration.isInvert());
                channelsSpinner.setValue(configuration.getInputChannels());
                for (JSpinner s : new JSpinner[] { seedSpinner, overlapSpinner, maskSpinner })
                        s.setEditor(new JSpinner.NumberEditor(s, "0.0##"));
                seedSpinner.setValue(configuration.getSeedThreshold());
                overlapSpinner.setValue(configuration.getOverlapThreshold());
                maskSpinner.setValue(configuration.getMaskThreshold());
                minSizeSpinner.setValue(configuration.getMinSize());
                peakDistanceSpinner.setValue(configuration.getPeakDistance());
                cleanupBox.setSelected(configuration.isCleanupFragments());
                resolveBox.setSelected(configuration.isResolveCellAndNucleus());
                timeoutSpinner.setValue(configuration.getDetectionTimeoutSeconds());
                coresSpinner.setValue(configuration.getCpuCores());
                labelImageBox.setSelected(configuration.isGenerateLabelImage());

                // Show/hide the custom path (label + field together) and the tile size
                // (only meaningful when tiling can happen), repainting the container
                // immediately so the change shows without resizing the window.
                Runnable syncVisibility = new Runnable() {
                        @Override
                        public void run() {
                                boolean isCustom = CUSTOM_MODEL.equals(modelTypeBox.getSelectedItem());
                                customModelLabel.setVisible(isCustom);
                                customModelField.setVisible(isCustom);
                                tileSizeSpinner.setEnabled(processingBox.getSelectedIndex() != 1);
                                Container p = customModelField.getParent();
                                if (p != null) {
                                        p.revalidate();
                                        p.repaint();
                                }
                        }
                };
                syncVisibility.run();
                java.awt.event.ActionListener sync = new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                                syncVisibility.run();
                        }
                };
                modelTypeBox.addActionListener(sync);
                processingBox.addActionListener(sync);

                targetBox.setToolTipText("<html>'Cells' needs a model that segments cells, e.g. "
                                + "<i>fluorescence_nuclei_and_cells</i>.<br>Nuclei-only models fall back to nuclei "
                                + "(a warning is logged).</html>");
                channelsSpinner.setToolTipText("<html>Grayscale detection image is replicated to this many "
                                + "channels.<br>0 = automatic: 3 for 'brightfield' models, otherwise 1.</html>");
                invertBox.setToolTipText("<html>Inverts the intensities before segmentation, e.g. for dark cells on "
                                + "a bright field.</html>");

                // Layout
                JPanel panel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(3, 6, 3, 6);
                gbc.fill = GridBagConstraints.HORIZONTAL;

                int row = 0;
                addRow(panel, gbc, row++, "Model:", modelTypeBox);
                gbc.gridwidth = 1;
                gbc.weightx = 0.0;
                gbc.gridx = 0;
                gbc.gridy = row;
                panel.add(customModelLabel, gbc);
                gbc.gridx = 1;
                gbc.weightx = 1.0;
                panel.add(customModelField, gbc);
                row++;
                addRow(panel, gbc, row++, "Segment:", targetBox);
                addRow(panel, gbc, row++,
                                "<html>Pixel size (&micro;m/px)<br><small>0 = no rescaling; set it so the image "
                                                + "matches the model's resolution</small></html>",
                                pixelSizeSpinner);
                addRow(panel, gbc, row++, "<html>Device<br><small>cuda = NVIDIA GPU, mps = Apple, auto = best "
                                + "available</small></html>", deviceBox);
                addRow(panel, gbc, row++, "Processing:", processingBox);
                addRow(panel, gbc, row++, "Tile size (px):", tileSizeSpinner);

                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                panel.add(normaliseBox, gbc);
                gbc.gridy = row++;
                panel.add(invertBox, gbc);
                gbc.gridwidth = 1;

                addRow(panel, gbc, row++, "<html>Input channels<br><small>0 = automatic</small></html>",
                                channelsSpinner);

                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                panel.add(new JLabel("<html><b>Advanced</b> <small>(-1 = use the model's default)</small></html>"),
                                gbc);
                gbc.gridwidth = 1;
                addRow(panel, gbc, row++, "Seed threshold:", seedSpinner);
                addRow(panel, gbc, row++, "Overlap threshold:", overlapSpinner);
                addRow(panel, gbc, row++, "Mask threshold:", maskSpinner);
                addRow(panel, gbc, row++, "Min. object size (px):", minSizeSpinner);
                addRow(panel, gbc, row++, "Peak distance (px):", peakDistanceSpinner);

                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                panel.add(cleanupBox, gbc);
                gbc.gridy = row++;
                panel.add(resolveBox, gbc);
                gbc.gridwidth = 1;

                addRow(panel, gbc, row++,
                                "<html>Detection timeout (s)<br><small>0 = use graalpy_config.txt; also applies "
                                                + "to the model download at start</small></html>",
                                timeoutSpinner);
                addRow(panel, gbc, row++, "<html>CPU cores<br><small>0 = all cores</small></html>", coresSpinner);

                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                panel.add(labelImageBox, gbc);

                gbc.gridy = row;
                gbc.weighty = 1.0;
                panel.add(new JLabel(
                                "<html><small>InstanSeg runs in the CPython env set by "
                                                + "'instanseg_cpython_executable' (or 'cpython_executable') in "
                                                + "graalpy_config.txt: <i>pip install instanseg-torch</i>. Model weights "
                                                + "are downloaded on first use.</small></html>"),
                                gbc);

                JScrollPane scroll = new JScrollPane(panel);
                scroll.setBorder(BorderFactory.createEmptyBorder());
                return scroll;
        }

        @Override
        protected void commitChanges(InstanSegConfiguration configuration) {
                Object sel = modelTypeBox.getSelectedItem();
                if (CUSTOM_MODEL.equals(sel))
                        configuration.setModelType(customModelField.getText().trim());
                else
                        configuration.setModelType(sel == null ? "fluorescence_nuclei_and_cells" : sel.toString());

                configuration.setTarget(targetBox.getSelectedIndex() == 1 ? InstanSegConfiguration.TARGET_CELLS
                                : InstanSegConfiguration.TARGET_NUCLEI);
                configuration.setPixelSizeUm(((Number) pixelSizeSpinner.getValue()).doubleValue());
                Object dev = deviceBox.getSelectedItem();
                configuration.setDevice(dev == null ? "cpu" : dev.toString());
                configuration.setProcessingMode(PROCESSING_VALUES[Math.max(0, processingBox.getSelectedIndex())]);
                configuration.setTileSize(((Number) tileSizeSpinner.getValue()).intValue());
                configuration.setNormalise(normaliseBox.isSelected());
                configuration.setInvert(invertBox.isSelected());
                configuration.setInputChannels(((Number) channelsSpinner.getValue()).intValue());
                configuration.setSeedThreshold(((Number) seedSpinner.getValue()).doubleValue());
                configuration.setOverlapThreshold(((Number) overlapSpinner.getValue()).doubleValue());
                configuration.setMaskThreshold(((Number) maskSpinner.getValue()).doubleValue());
                configuration.setMinSize(((Number) minSizeSpinner.getValue()).intValue());
                configuration.setPeakDistance(((Number) peakDistanceSpinner.getValue()).intValue());
                configuration.setCleanupFragments(cleanupBox.isSelected());
                configuration.setResolveCellAndNucleus(resolveBox.isSelected());
                configuration.setDetectionTimeoutSeconds(((Number) timeoutSpinner.getValue()).intValue());
                configuration.setCpuCores(((Number) coresSpinner.getValue()).intValue());
                configuration.setGenerateLabelImage(labelImageBox.isSelected());
        }

        @Override
        protected void initializeDefaultConfiguration(InstanSegConfiguration configuration) throws AddonException {
                // Defaults already set in InstanSegConfiguration field initialisers.
        }

        // Helper to add a label and component row
        private static void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
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
