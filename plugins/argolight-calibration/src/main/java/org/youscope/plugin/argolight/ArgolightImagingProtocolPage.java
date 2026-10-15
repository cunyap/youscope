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

package org.youscope.plugin.argolight;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.youscope.addon.measurement.MeasurementAddonUIPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.Well;
import org.youscope.common.configuration.FocusConfiguration;
import org.youscope.common.job.JobConfiguration;
import org.youscope.common.microplate.WellWithGroup;
import org.youscope.common.microplate.WellWithGroup.WellGroup;
import org.youscope.plugin.imagingjob.ImagingJobConfiguration;
import org.youscope.plugin.zslides.ZSlidesJobConfiguration;
import org.youscope.plugin.composedimaging.PlateScanningJobConfiguration;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.JobsDefinitionPanel;

/**
 * Imaging-protocol page. Shows one tab per used pattern group. Each tab is
 * pre-filled with a sensible default protocol: patterns flagged as z-stacks get
 * a z-slides container (pre-filled with the pattern's dz step and slice count)
 * wrapping an imaging job; all other patterns get a single imaging job. The
 * user
 * can freely edit any of these before starting.
 *
 * @author Andreas P. Cuny
 */
class ArgolightImagingProtocolPage extends MeasurementAddonUIPage<ArgolightCalibrationConfiguration> {
	private static final long serialVersionUID = 1L;

	private final YouScopeClient client;
	private final YouScopeServer server;

	private final HashMap<WellGroup, JobsDefinitionPanel> jobPanels = new HashMap<>();
	private final JTabbedPane tabs = new JTabbedPane();
	private final JPanel tabsPanel = new JPanel(new BorderLayout());
	private YouScopeFrame frame = null;

	ArgolightImagingProtocolPage(YouScopeClient client, YouScopeServer server) {
		this.client = client;
		this.server = server;
	}

	@Override
	public void loadData(ArgolightCalibrationConfiguration configuration) {
		ArgolightPlateDefinition definition;
		try {
			definition = ArgolightPlateManager
					.getDefinition(configuration.getOrCreateArgolightMicroplate().getDefinitionName());
		} catch (ArgolightException e) {
			client.sendError("Could not load Argolight definition for imaging protocol pre-fill.", e);
			definition = new ArgolightPlateDefinition();
		}

		// Which groups are actually used by the current selection?
		final Set<WellGroup> usedGroups = new HashSet<>();
		for (WellWithGroup well : configuration.getSelectedWells())
			usedGroups.add(well.getGroup());
		if (usedGroups.isEmpty())
			usedGroups.add(WellGroup.GROUP0);

		// Load existing jobs into panels; pre-fill empty ones with group defaults
		// derived from the definition (independent of which well happens to be first).
		// Panels are created on demand only for groups that are actually used.
		for (WellGroup group : usedGroups) {
			JobsDefinitionPanel panel = ensurePanel(group);
			JobConfiguration[] existing = configuration.getJobs(group);
			if (existing != null && existing.length > 0)
				panel.setJobs(existing);
			else
				panel.setJobs(ArgolightJobFactory.buildGroupJobs(definition, group.getGroupId(),
						buildFocusConfiguration(configuration.getFocusConfiguration()),
						configuration.getAcquisitionChannelGroup(), configuration.getAcquisitionChannel(),
						configuration.getAcquisitionCamera(), configuration.getAcquisitionExposureMs()));
		}

		final WellGroup[] usedGroupsArray = usedGroups.toArray(new WellGroup[0]);
		Arrays.sort(usedGroupsArray, new Comparator<WellGroup>() {
			@Override
			public int compare(WellGroup a, WellGroup b) {
				return a.getGroupId() - b.getGroupId();
			}
		});

		final ArgolightPlateDefinition def = definition;
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				tabs.removeAll();
				tabsPanel.removeAll();
				if (usedGroupsArray.length == 1) {
					tabsPanel.add(jobPanels.get(usedGroupsArray[0]), BorderLayout.CENTER);
				} else {
					for (WellGroup group : usedGroupsArray)
						tabs.addTab(groupTitle(def, group), group.getIcon(), jobPanels.get(group));
					tabsPanel.add(tabs, BorderLayout.CENTER);
				}
				fireSizeChanged();
			}
		};
		if (SwingUtilities.isEventDispatchThread())
			runner.run();
		else {
			try {
				SwingUtilities.invokeAndWait(runner);
			} catch (InvocationTargetException | InterruptedException e) {
				client.sendError("Could not activate job panels.", e);
			}
		}
	}

	private static String groupTitle(ArgolightPlateDefinition definition, WellGroup group) {
		java.util.List<ArgolightPattern> members = membersOfGroup(definition, group.getGroupId());
		if (members.isEmpty())
			return group.getName();
		if (members.size() == 1)
			return members.get(0).getName();
		// Multiple patterns in one group: show a compact combined label.
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < members.size() && i < 3; i++) {
			if (i > 0)
				sb.append(" / ");
			String n = members.get(i).getName();
			sb.append(n.length() > 16 ? n.substring(0, 16) : n);
		}
		if (members.size() > 3)
			sb.append(" ...");
		return sb.toString();
	}

	private static java.util.List<ArgolightPattern> membersOfGroup(ArgolightPlateDefinition definition, int groupId) {
		return ArgolightJobFactory.membersOfGroup(definition, groupId);
	}

	private FocusConfiguration buildFocusConfiguration(FocusConfiguration measurementFocus) {
		FocusConfiguration focus = new FocusConfiguration();
		if (measurementFocus != null && measurementFocus.getFocusDevice() != null) {
			focus.setFocusDevice(measurementFocus.getFocusDevice());
			focus.setAdjustmentTime(measurementFocus.getAdjustmentTime());
		} else {
			try {
				focus.setFocusDevice(server.getMicroscope().getFocusDevice().getDeviceID());
			} catch (Exception e) {
				// leave device unset; user can pick it in the z-slides job.
			}
			focus.setAdjustmentTime(0);
		}
		return focus;
	}

	@Override
	public boolean saveData(ArgolightCalibrationConfiguration configuration) {
		final Set<WellGroup> usedGroups = new HashSet<>();
		for (WellWithGroup well : configuration.getSelectedWells())
			usedGroups.add(well.getGroup());
		for (WellGroup group : usedGroups) {
			JobsDefinitionPanel panel = jobPanels.get(group);
			if (panel != null)
				configuration.setJobs(group, panel.getJobs());
		}
		return true;
	}

	@Override
	public void setToDefault(ArgolightCalibrationConfiguration configuration) {
		// defaults are generated lazily in loadData.
	}

	@Override
	public String getPageName() {
		return "Imaging Protocol per Pattern";
	}

	@Override
	public void createUI(YouScopeFrame frame) {
		this.frame = frame;
		setLayout(new BorderLayout());
		// Panels are created lazily per used group (there can be many groups now).
		ensurePanel(WellGroup.GROUP0);
		tabs.addTab(WellGroup.GROUP0.getName(), WellGroup.GROUP0.getIcon(), jobPanels.get(WellGroup.GROUP0));
		tabsPanel.add(tabs, BorderLayout.CENTER);
		add(tabsPanel, BorderLayout.CENTER);
		setBorder(new TitledBorder("Imaging Protocol per Pattern"));
	}

	private JobsDefinitionPanel ensurePanel(WellGroup group) {
		JobsDefinitionPanel panel = jobPanels.get(group);
		if (panel == null) {
			panel = new JobsDefinitionPanel(client, server, frame);
			jobPanels.put(group, panel);
		}
		return panel;
	}

}
