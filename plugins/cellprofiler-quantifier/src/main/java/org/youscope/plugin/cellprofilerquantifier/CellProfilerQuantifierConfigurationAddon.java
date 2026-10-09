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
package org.youscope.plugin.cellprofilerquantifier;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonUIAdapter;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;

/** Configuration UI for the CellProfiler quantifier: which measurement families run. */
class CellProfilerQuantifierConfigurationAddon extends ComponentAddonUIAdapter<CellProfilerQuantifierConfiguration>
{
	private final JCheckBox shapeBox = new JCheckBox("Size & shape (MeasureObjectSizeShape)");
	private final JCheckBox intensityBox = new JCheckBox("Intensity (MeasureObjectIntensity)");
	private final JCheckBox textureBox = new JCheckBox("Texture (MeasureTexture)");

	CellProfilerQuantifierConfigurationAddon(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
	}

	static ComponentMetadataAdapter<CellProfilerQuantifierConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<CellProfilerQuantifierConfiguration>(
				CellProfilerQuantifierConfiguration.TYPE_IDENTIFIER,
				CellProfilerQuantifierConfiguration.class,
				CellProfilerQuantifierAddon.class,
				"CellProfiler Quantifier",
				new String[0],
				"Measures per-cell features with CellProfiler's fixed measurement "
						+ "modules (size/shape, intensity, texture) on the segmenter's "
						+ "label mask and the quantification channels.",
				"icons/cellprofiler-icon.png");
	}

	@Override
	protected Component createUI(CellProfilerQuantifierConfiguration configuration) throws AddonException
	{
		setTitle("CellProfiler quantifier");
		setResizable(true);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 6, 4, 6);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;

		shapeBox.setSelected(configuration.isMeasureShape());
		intensityBox.setSelected(configuration.isMeasureIntensity());
		textureBox.setSelected(configuration.isMeasureTexture());

		c.gridy = 0;
		panel.add(new JLabel("CellProfiler measurement families to run:"), c);
		c.gridy = 1; panel.add(shapeBox, c);
		c.gridy = 2; panel.add(intensityBox, c);
		c.gridy = 3; panel.add(textureBox, c);
		c.gridy = 4;
		panel.add(new JLabel("<html><i>Intensity &amp; texture are measured per quantification channel."
				+ "<br>Columns are named cell.cp.&lt;feature&gt; (per-channel features get _&lt;channel&gt;).</i></html>"), c);
		return panel;
	}

	@Override
	protected void initializeDefaultConfiguration(CellProfilerQuantifierConfiguration configuration) throws AddonException
	{
		// defaults live on the configuration object
	}

	@Override
	protected void commitChanges(CellProfilerQuantifierConfiguration configuration)
	{
		configuration.setMeasureShape(shapeBox.isSelected());
		configuration.setMeasureIntensity(intensityBox.isSelected());
		configuration.setMeasureTexture(textureBox.isSelected());
	}
}
