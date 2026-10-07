/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
package org.youscope.plugin.btracktracker;

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

/** Configuration UI for the btrack tracker: max search radius and max lost. */
class BtrackTrackerConfigurationAddon extends ComponentAddonUIAdapter<BtrackTrackerConfiguration>
{
	private final JSpinner searchRangeField = new JSpinner(new SpinnerNumberModel(50.0, 0.1, 100000.0, 1.0));
	private final JSpinner maxLostField = new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));

	BtrackTrackerConfigurationAddon(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
	}

	static ComponentMetadataAdapter<BtrackTrackerConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<BtrackTrackerConfiguration>(
				BtrackTrackerConfiguration.TYPE_IDENTIFIER,
				BtrackTrackerConfiguration.class,
				BtrackTrackerAddon.class,
				"Btrack Tracker",
				new String[0],
				"Links cells frame-to-frame with btrack's Bayesian (Kalman) "
						+ "assignment, adding cell.track.id.",
				"icons/btrack-icon.png");
	}

	@Override
	protected Component createUI(BtrackTrackerConfiguration configuration) throws AddonException
	{
		setTitle("Btrack tracker");
		setResizable(true);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.fill = GridBagConstraints.HORIZONTAL;

		searchRangeField.setValue(Double.valueOf(configuration.getMaxSearchRadius()));
		maxLostField.setValue(Integer.valueOf(configuration.getMaxLost()));

		c.gridx = 0; c.gridy = 0; c.weightx = 0;
		panel.add(new JLabel("Max search radius (px):"), c);
		c.gridx = 1; c.weightx = 1;
		panel.add(searchRangeField, c);

		c.gridx = 0; c.gridy = 1; c.weightx = 0;
		panel.add(new JLabel("Max lost (frames):"), c);
		c.gridx = 1; c.weightx = 1;
		panel.add(maxLostField, c);

		return panel;
	}

	@Override
	protected void initializeDefaultConfiguration(BtrackTrackerConfiguration configuration) throws AddonException
	{
		// defaults live on the configuration object
	}

	@Override
	protected void commitChanges(BtrackTrackerConfiguration configuration)
	{
		configuration.setMaxSearchRadius(((Number) searchRangeField.getValue()).doubleValue());
		configuration.setMaxLost(((Number) maxLostField.getValue()).intValue());
	}
}
