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
package org.youscope.plugin.trackpytracker;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonUIAdapter;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;

/** Configuration UI for the trackpy tracker: search range and memory. */
class TrackpyTrackerConfigurationAddon extends ComponentAddonUIAdapter<TrackpyTrackerConfiguration>
{
	private final JSpinner searchRangeField = new JSpinner(new SpinnerNumberModel(15.0, 0.1, 100000.0, 1.0));
	private final JSpinner memoryField = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));

	TrackpyTrackerConfigurationAddon(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
	}

	static ComponentMetadataAdapter<TrackpyTrackerConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<TrackpyTrackerConfiguration>(
				TrackpyTrackerConfiguration.TYPE_IDENTIFIER,
				TrackpyTrackerConfiguration.class,
				TrackpyTrackerAddon.class,
				"Trackpy Tracker",
				new String[0],
				"Links cells frame-to-frame with trackpy (nearest-neighbour linking "
						+ "with an optional prediction/memory), adding cell.track.id.",
				"icons/trackpy-icon.png");
	}

	@Override
	protected Component createUI(TrackpyTrackerConfiguration configuration) throws AddonException
	{
		setTitle("Trackpy tracker");
		setResizable(true);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.fill = GridBagConstraints.HORIZONTAL;

		searchRangeField.setValue(Double.valueOf(configuration.getSearchRange()));
		memoryField.setValue(Integer.valueOf(configuration.getMemory()));

		c.gridx = 0; c.gridy = 0; c.weightx = 0;
		panel.add(new JLabel("Search range (px):"), c);
		c.gridx = 1; c.weightx = 1;
		panel.add(searchRangeField, c);

		c.gridx = 0; c.gridy = 1; c.weightx = 0;
		panel.add(new JLabel("Memory (frames):"), c);
		c.gridx = 1; c.weightx = 1;
		panel.add(memoryField, c);

		return panel;
	}

	@Override
	protected void initializeDefaultConfiguration(TrackpyTrackerConfiguration configuration) throws AddonException
	{
		// defaults live on the configuration object
	}

	@Override
	protected void commitChanges(TrackpyTrackerConfiguration configuration)
	{
		configuration.setSearchRange(((Number) searchRangeField.getValue()).doubleValue());
		configuration.setMemory(((Number) memoryField.getValue()).intValue());
	}
}
