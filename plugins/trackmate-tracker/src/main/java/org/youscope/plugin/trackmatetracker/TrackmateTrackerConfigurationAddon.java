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
package org.youscope.plugin.trackmatetracker;

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

/** Configuration UI for the TrackMate tracker: search range and memory. */
class TrackmateTrackerConfigurationAddon extends ComponentAddonUIAdapter<TrackmateTrackerConfiguration>
{
	private final JSpinner linkingField = new JSpinner(new SpinnerNumberModel(15.0, 0.1, 100000.0, 1.0));

	TrackmateTrackerConfigurationAddon(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
	}

	static ComponentMetadataAdapter<TrackmateTrackerConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<TrackmateTrackerConfiguration>(
				TrackmateTrackerConfiguration.TYPE_IDENTIFIER,
				TrackmateTrackerConfiguration.class,
				TrackmateTrackerAddon.class,
				"Trackmate Tracker",
				new String[0],
				"Links cells frame-to-frame with TrackMate's LAP tracker "
						+ "(SparseLAPFrameToFrameTracker) via Fiji, adding cell.track.id.",
				"icons/track-mate-icon.png");
	}

	@Override
	protected Component createUI(TrackmateTrackerConfiguration configuration) throws AddonException
	{
		setTitle("Trackmate tracker");
		setResizable(true);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.fill = GridBagConstraints.HORIZONTAL;

		linkingField.setValue(Double.valueOf(configuration.getLinkingMaxDistance()));

		c.gridx = 0; c.gridy = 0; c.weightx = 0;
		panel.add(new JLabel("Linking max distance (px):"), c);
		c.gridx = 1; c.weightx = 1;
		panel.add(linkingField, c);

		return panel;
	}

	@Override
	protected void initializeDefaultConfiguration(TrackmateTrackerConfiguration configuration) throws AddonException
	{
		// defaults live on the configuration object
	}

	@Override
	protected void commitChanges(TrackmateTrackerConfiguration configuration)
	{
		configuration.setLinkingMaxDistance(((Number) linkingField.getValue()).doubleValue());
	}
}
