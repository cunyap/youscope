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
package org.youscope.plugin.fairexport;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.youscope.addon.postprocessing.ConfigurablePostProcessorFactory.ConfigurationPanel;
import org.youscope.common.measurement.MeasurementConfiguration;

/**
 * Wizard panel letting the user define the FAIR export as part of the
 * measurement protocol.
 * <p>
 * The export options belong with the measurement, chosen up front, so that the
 * full export is
 * documented in the configuration and runs unattended exactly as specified
 * instead of being set in
 * a later manual re-run. Each format is a checkbox; ticking a format reveals
 * its options (progressive
 * disclosure), the same interaction pattern as other YouScope configuration
 * panels. The whole
 * selection is encoded into a single automatic-post-processor identifier via
 * {@link FairExportOptionsCodec}, so it round-trips through the standard
 * measurement configuration with
 * no core change.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class FairExportConfigurationPanel extends JPanel implements ConfigurationPanel {
	private static final long serialVersionUID = 1L;

	private final JCheckBox enableField = new JCheckBox("Export to FAIR / OME formats when the measurement finishes",
			false);

	private final JCheckBox tiffField = new JCheckBox("OME-TIFF (with embedded OME-XML)", true);
	private final JComboBox<String> tiffSplitField = new JComboBox<String>(
			new String[] { "Combined (multi-channel per well)",
					"Per channel (single-channel files, e.g. for Argolight)" });

	private final JCheckBox zarrField = new JCheckBox("OME-Zarr (NGFF; HCS plate for microplates)", false);
	private final JComboBox<String> chunkSizeField = new JComboBox<String>(
			new String[] { "512", "1024", "2048 (full plane)" });
	private final JCheckBox shardingField = new JCheckBox("Use sharding (fewer files for large images)", false);

	private final JCheckBox roCrateField = new JCheckBox("RO-Crate manifest", true);

	private final JCheckBox rootOnlyCrateField = new JCheckBox(
			"RO-Crate only at store root (skip per-well/-image crates)", false);
	private final JCheckBox useqField = new JCheckBox(
			"useq-schema acquisition plan (cell-detection / feedback measurements)", false);

	private final JPanel tiffOptions = new JPanel();
	private final JPanel zarrOptions = new JPanel();

	/**
	 * Constructor.
	 */
	public FairExportConfigurationPanel() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setOpaque(false);

		enableField.setOpaque(false);
		enableField.setAlignmentX(LEFT_ALIGNMENT);
		add(enableField);

		final JPanel formats = new JPanel();
		formats.setLayout(new GridBagLayout());
		formats.setOpaque(false);
		formats.setBorder(BorderFactory.createEmptyBorder(4, 20, 4, 4));
		formats.setAlignmentX(LEFT_ALIGNMENT);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(1, 0, 1, 0);

		tiffField.setOpaque(false);
		zarrField.setOpaque(false);
		roCrateField.setOpaque(false);
		shardingField.setOpaque(false);

		// OME-TIFF with its options revealed underneath.
		formats.add(tiffField, constraints);
		constraints.gridy++;
		tiffOptions.setLayout(new BoxLayout(tiffOptions, BoxLayout.PAGE_AXIS));
		tiffOptions.setOpaque(false);
		tiffOptions.setBorder(BorderFactory.createEmptyBorder(0, 22, 4, 0));
		tiffOptions.setAlignmentX(LEFT_ALIGNMENT);
		JPanel tiffLayoutRow = row("Layout:", tiffSplitField);
		tiffOptions.add(tiffLayoutRow);
		formats.add(tiffOptions, constraints);
		constraints.gridy++;

		// OME-Zarr with its options.
		formats.add(zarrField, constraints);
		constraints.gridy++;
		zarrOptions.setLayout(new BoxLayout(zarrOptions, BoxLayout.PAGE_AXIS));
		zarrOptions.setOpaque(false);
		zarrOptions.setBorder(BorderFactory.createEmptyBorder(0, 22, 4, 0));
		zarrOptions.setAlignmentX(LEFT_ALIGNMENT);
		zarrOptions.add(row("Chunk size (pixels):", chunkSizeField));
		shardingField.setAlignmentX(LEFT_ALIGNMENT);
		zarrOptions.add(shardingField);
		formats.add(zarrOptions, constraints);
		constraints.gridy++;

		// RO-Crate (no options).
		formats.add(roCrateField, constraints);
		constraints.gridy++;

		// useq acquisition plan (no options; gated to feedback measurements at export
		// time).
		useqField.setOpaque(false);
		useqField.setToolTipText("Write sequence.useq.json / .yaml describing the acquisition plan. "
				+ "Only produced for cell-detection / feedback measurements; a plain scan is skipped.");
		formats.add(useqField, constraints);
		constraints.gridy++;

		rootOnlyCrateField.setOpaque(false);
		rootOnlyCrateField.setToolTipText("By default a minimal RO-Crate is also written into each plate "
				+ "well and image sub-group so the OME-NGFF validator is happy when you point it at a "
				+ "sub-group. Tick to write the crate only at the store root (spec-compliant).");
		formats.add(rootOnlyCrateField, constraints);
		constraints.gridy++;

		add(formats);

		chunkSizeField.setSelectedIndex(1);

		// Progressive disclosure: the whole format block is enabled only when export is
		// enabled, and
		// each format's options are enabled only when that format is ticked.
		ActionListener updater = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				updateEnabledState();
			}
		};
		enableField.addActionListener(updater);
		tiffField.addActionListener(updater);
		zarrField.addActionListener(updater);

		updateEnabledState();
	}

	private JPanel row(String label, Component field) {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setOpaque(false);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(0, 0, 0, 6);
		panel.add(new JLabel(label), c);
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(field, c);
		return panel;
	}

	private void updateEnabledState() {
		boolean enabled = enableField.isSelected();
		tiffField.setEnabled(enabled);
		zarrField.setEnabled(enabled);
		roCrateField.setEnabled(enabled);
		useqField.setEnabled(enabled);
		rootOnlyCrateField.setEnabled(enabled);
		setPanelEnabled(tiffOptions, enabled && tiffField.isSelected());
		setPanelEnabled(zarrOptions, enabled && zarrField.isSelected());
	}

	private static void setPanelEnabled(JPanel panel, boolean enabled) {
		panel.setEnabled(enabled);
		for (Component child : panel.getComponents()) {
			child.setEnabled(enabled);
			if (child instanceof JPanel)
				setPanelEnabled((JPanel) child, enabled);
		}
	}

	/**
	 * Builds the options from the current selection.
	 * 
	 * @return The options.
	 */
	private FairExportEngine.Options toOptions() {
		FairExportEngine.Options options = new FairExportEngine.Options();
		options.writeOmeTiff = tiffField.isSelected();
		options.writeOmeZarr = zarrField.isSelected();
		options.writeRoCrate = roCrateField.isSelected();
		options.writeUseq = useqField.isSelected();
		options.rootOnlyStoreCrate = rootOnlyCrateField.isSelected();
		options.writeOmeXml = true;
		options.tiffSplit = tiffSplitField.getSelectedIndex() == 1
				? FairExportEngine.TiffSplit.PER_CHANNEL
				: FairExportEngine.TiffSplit.COMBINED;
		options.zarrChunkSize = chunkSizeField.getSelectedIndex() == 0 ? 512
				: (chunkSizeField.getSelectedIndex() == 2 ? 2048 : 1024);
		options.zarrSharding = shardingField.isSelected();
		return options;
	}

	private void fromOptions(FairExportEngine.Options options) {
		tiffField.setSelected(options.writeOmeTiff);
		zarrField.setSelected(options.writeOmeZarr);
		roCrateField.setSelected(options.writeRoCrate);
		useqField.setSelected(options.writeUseq);
		rootOnlyCrateField.setSelected(options.rootOnlyStoreCrate);
		tiffSplitField.setSelectedIndex(options.tiffSplit == FairExportEngine.TiffSplit.PER_CHANNEL ? 1 : 0);
		chunkSizeField.setSelectedIndex(options.zarrChunkSize == 512 ? 0 : (options.zarrChunkSize == 2048 ? 2 : 1));
		shardingField.setSelected(options.zarrSharding);
	}

	/**
	 * Loads the panel state from a measurement configuration: finds a configured
	 * FAIR identifier among
	 * the automatic post-processors and restores the checkboxes and options from
	 * it.
	 * 
	 * @param configuration Configuration to read.
	 */
	public void loadData(MeasurementConfiguration configuration) {
		if (configuration == null)
			return;
		String configured = null;
		for (String identifier : configuration.getAutomaticPostProcessors()) {
			if (identifier != null && identifier.startsWith(FairExportFactory.TYPE_IDENTIFIER_CONFIGURED_PREFIX)) {
				configured = identifier;
				break;
			}
		}
		if (configured == null) {
			enableField.setSelected(false);
			updateEnabledState();
			return;
		}
		enableField.setSelected(true);
		fromOptions(FairExportOptionsCodec.decode(configured));
		updateEnabledState();
	}

	/**
	 * Writes the panel state into a measurement configuration: replaces any
	 * existing FAIR export
	 * identifiers with the single configured identifier (or none if export is
	 * disabled), preserving all
	 * other post-processors.
	 * 
	 * @param configuration Configuration to write.
	 */
	public void saveData(MeasurementConfiguration configuration) {
		if (configuration == null)
			return;
		java.util.ArrayList<String> processors = new java.util.ArrayList<String>();
		for (String identifier : configuration.getAutomaticPostProcessors()) {
			if (identifier == null)
				continue;
			// Drop any previous FAIR export identifiers (configured or the older per-format
			// ones); keep
			// everything else untouched.
			if (identifier.startsWith(FairExportFactory.TYPE_IDENTIFIER_CONFIGURED_PREFIX)
					|| identifier.equals(FairExportFactory.TYPE_IDENTIFIER)
					|| identifier.equals(FairExportFactory.TYPE_IDENTIFIER_TIFF)
					|| identifier.equals(FairExportFactory.TYPE_IDENTIFIER_ZARR)
					|| identifier.equals(FairExportFactory.TYPE_IDENTIFIER_ROCRATE))
				continue;
			processors.add(identifier);
		}
		if (enableField.isSelected())
			processors.add(FairExportOptionsCodec.encode(toOptions()));
		configuration.setAutomaticPostProcessors(processors.toArray(new String[processors.size()]));
	}

	/**
	 * @return True if export is enabled in this panel.
	 */
	public boolean isExportEnabled() {
		return enableField.isSelected();
	}

	@Override
	public Component getComponent() {
		return this;
	}
}
