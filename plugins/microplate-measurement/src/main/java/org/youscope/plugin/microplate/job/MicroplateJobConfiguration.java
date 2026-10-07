/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
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
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - add sync between selected and configured positions
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.microplate.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.youscope.addon.microplate.MicroplateConfiguration;
import org.youscope.addon.pathoptimizer.PathOptimizerConfiguration;
import org.youscope.common.PositionInformation;
import org.youscope.common.Well;
import org.youscope.common.microplate.WellWithGroup;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.configuration.FocusConfiguration;
import org.youscope.common.job.JobConfiguration;
import org.youscope.common.job.CompositeJobConfiguration;
import org.youscope.plugin.microplate.measurement.FineConfigurationConverter;
import org.youscope.plugin.microplate.measurement.TileConfiguration;
import org.youscope.plugin.microplate.measurement.XYAndFocusPosition;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * This class represents the configuration of a user configurable microplate
 * job.
 * 
 * @author Moritz Lang, Andreas P. Cuny
 */
@XStreamAlias("microplate-job")
public class MicroplateJobConfiguration implements CompositeJobConfiguration {
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 3413810800791783902L;

	/**
	 * The jobs which should be executed in each well/tile, per well group.
	 */
	private HashMap<WellWithGroup.WellGroup, ArrayList<JobConfiguration>> jobs = new HashMap<>();

	/**
	 * Legacy storage for configurations saved before well-group support. Older files
	 * stored a flat <jobs> list which applied to every well. It is migrated to GROUP0
	 * after deserialization. New configurations do not write this field.
	 */
	@XStreamAlias("jobs")
	private ArrayList<JobConfiguration> legacyJobs = null;

	@XStreamAlias("stage")
	private String stageDevice = null;

	@XStreamAlias("path-optimizer")
	private PathOptimizerConfiguration pathOptimizerConfiguration = null;

	@XStreamAlias("microplate-configuration")
	private MicroplateConfiguration microplateConfiguration = null;

	@XStreamAlias("tile-configuration")
	private TileConfiguration tileConfiguration = null;

	@XStreamAlias("configured-positions")
	@XStreamConverter(FineConfigurationConverter.class)
	private HashMap<PositionInformation, XYAndFocusPosition> configuredPositions = new HashMap<>(200);

	@XStreamAlias("selected-well-groups")
	private HashSet<WellWithGroup> selectedWells = new HashSet<>(200);

	/** Legacy storage for plain well selections from pre-group configurations. */
	@XStreamAlias("selected-wells")
	private HashSet<Well> legacySelectedWells = null;
	@XStreamAlias("selected-tiles")
	private HashSet<Well> selectedTiles = new HashSet<>(200);

	/**
	 * Sets the configured imaging positions to the positions in the provided map.
	 * 
	 * @param positions Map of positions where imaging should take place.
	 */
	public void setPositions(Map<PositionInformation, XYAndFocusPosition> positions) {
		configuredPositions.clear();
		configuredPositions.putAll(positions);
	}

	/**
	 * Returns a map of all positions where the measurement images.
	 * 
	 * @return Map of all positions where imaging takes place.
	 */
	public Map<PositionInformation, XYAndFocusPosition> getPositions() {
		return new HashMap<>(configuredPositions);
	}

	/**
	 * Sets the wells which are selected in the microplate.
	 * 
	 * @param selectedWells selected wells.
	 */
	public void setSelectedWells(Set<WellWithGroup> selectedWells) {
		this.selectedWells.clear();
		this.selectedWells.addAll(selectedWells);
	}

	/**
	 * Returns a collection of all wells selected in the microplate.
	 * 
	 * @return selected wells.
	 */
	public Set<WellWithGroup> getSelectedWells() {
		return new HashSet<>(selectedWells);
	}

	/**
	 * Sets the wells which are selected in the microplate.
	 * 
	 * @param selectedTiles selected wells.
	 */
	public void setSelectedTiles(Set<Well> selectedTiles) {
		this.selectedTiles.clear();
		this.selectedTiles.addAll(selectedTiles);
	}

	/**
	 * Returns a collection of all tiles selected in the microplate.
	 * 
	 * @return selected tiles.
	 */
	public Set<Well> getSelectedTiles() {
		return new HashSet<>(selectedTiles);
	}

	/**
	 * Returns the configuration of the microplate layout.
	 * 
	 * @return microplate configuration
	 */
	public MicroplateConfiguration getMicroplateConfiguration() {
		return microplateConfiguration;
	}

	/**
	 * Sets the configuration of the microplate layout.
	 * 
	 * @param microplateConfiguration microplate configuration.
	 */
	public void setMicroplateConfiguration(MicroplateConfiguration microplateConfiguration) {
		this.microplateConfiguration = microplateConfiguration;
	}

	/**
	 * Returns the configuration of the tile layout. Internally, the tile layout is
	 * stored as another microplate layout.
	 * 
	 * @return tile configuration
	 */
	public TileConfiguration getTileConfiguration() {
		return tileConfiguration;
	}

	/**
	 * Sets the configuration of the tile layout. Internally, the tile layout is
	 * stored as another microplate layout.
	 * 
	 * @param tileConfiguration tile configuration.
	 */
	public void setTileConfiguration(TileConfiguration tileConfiguration) {
		this.tileConfiguration = tileConfiguration;
	}

	/**
	 * Returns the configuration of the optimizer which should be used to minimize
	 * the distances between two wells/positions measured.
	 * Returns null if not uses any optimized path.
	 * 
	 * @return The configuration of the path optimizer used, or null
	 */
	public PathOptimizerConfiguration getPathOptimizerConfiguration() {
		return pathOptimizerConfiguration;
	}

	/**
	 * Sets the configuration of the optimizer which should be used to minimize the
	 * distances between two wells/positions measured.
	 * Set to null to not use any optimized path.
	 * 
	 * @param pathOptimizerConfiguration The configuration of the path optimizer
	 *                                   used, or null
	 */
	public void setPathOptimizerConfiguration(PathOptimizerConfiguration pathOptimizerConfiguration) {
		this.pathOptimizerConfiguration = pathOptimizerConfiguration;
	}

	/**
	 * Returns the well groups which currently have an imaging protocol defined,
	 * sorted by group id.
	 * 
	 * @return sorted well groups with jobs.
	 */
	private ArrayList<WellWithGroup.WellGroup> sortedGroups() {
		ArrayList<WellWithGroup.WellGroup> groups = new ArrayList<>(jobs.keySet());
		Collections.sort(groups); // WellGroup is an enum; natural order == group id order.
		return groups;
	}

	// CompositeJobConfiguration contract. A microplate job can define a different
	// imaging protocol
	// per well group; the flat methods below provide the "all child jobs" view
	// expected by generic
	// tooling. getJobs() returns the union across all groups (group-id order); the
	// single-argument
	// mutators operate on the default group (GROUP0). Use the well-group-aware
	// overloads to edit a
	// specific group.

	@Override
	public JobConfiguration[] getJobs() {
		ArrayList<JobConfiguration> all = new ArrayList<>();
		for (WellWithGroup.WellGroup group : sortedGroups())
			all.addAll(jobs.get(group));
		return all.toArray(new JobConfiguration[all.size()]);
	}

	@Override
	public void setJobs(JobConfiguration[] jobs) {
		this.jobs.clear();
		setJobs(WellWithGroup.WellGroup.GROUP0, jobs);
	}

	@Override
	public void addJob(JobConfiguration job) {
		addJob(WellWithGroup.WellGroup.GROUP0, job);
	}

	@Override
	public void clearJobs() {
		jobs.clear();
	}

	/**
	 * Returns the jobs executed in each well/tile belonging to the given group.
	 * 
	 * @param wellGroup The well group for which the jobs should be returned.
	 * @return Jobs of the group (empty array if none).
	 */
	public JobConfiguration[] getJobs(WellWithGroup.WellGroup wellGroup) {
		ArrayList<JobConfiguration> groupJobs = jobs.get(wellGroup);
		if (groupJobs == null)
			return new JobConfiguration[0];
		return groupJobs.toArray(new JobConfiguration[groupJobs.size()]);
	}

	/**
	 * Sets the jobs executed in each well/tile belonging to the given group.
	 * 
	 * @param wellGroup The well group for which the jobs should be set.
	 * @param jobs      New jobs.
	 */
	public void setJobs(WellWithGroup.WellGroup wellGroup, JobConfiguration[] jobs) {
		ArrayList<JobConfiguration> groupJobs = new ArrayList<>(jobs.length);
		for (JobConfiguration job : jobs) {
			groupJobs.add(job);
		}
		this.jobs.put(wellGroup, groupJobs);
	}

	/**
	 * Adds a job to the jobs executed in each well/tile belonging to the given
	 * group.
	 * 
	 * @param wellGroup Well group for the job.
	 * @param job       Job to be added.
	 */
	public void addJob(WellWithGroup.WellGroup wellGroup, JobConfiguration job) {
		ArrayList<JobConfiguration> groupJobs = jobs.get(wellGroup);
		if (groupJobs == null) {
			groupJobs = new ArrayList<>();
			jobs.put(wellGroup, groupJobs);
		}
		groupJobs.add(job);
	}

	/**
	 * Removes all jobs belonging to the given group.
	 * 
	 * @param wellGroup Well group for which all jobs should be cleared.
	 */
	public void clearJobs(WellWithGroup.WellGroup wellGroup) {
		jobs.remove(wellGroup);
	}

	/**
	 * Removes the job at the given index within the given group.
	 * 
	 * @param wellGroup Well group where the job should be removed from.
	 * @param index     Index of the job to be removed.
	 */
	public void removeJob(WellWithGroup.WellGroup wellGroup, int index) {
		ArrayList<JobConfiguration> groupJobs = jobs.get(wellGroup);
		if (groupJobs == null)
			return;
		groupJobs.remove(index);
	}

	/**
	 * Inserts a job at the given index within the given group.
	 * 
	 * @param wellGroup Well group where the job should be added.
	 * @param job       Job to be added.
	 * @param index     position where to insert.
	 */
	public void addJob(WellWithGroup.WellGroup wellGroup, JobConfiguration job, int index) {
		ArrayList<JobConfiguration> groupJobs = jobs.get(wellGroup);
		if (groupJobs == null) {
			groupJobs = new ArrayList<>();
			jobs.put(wellGroup, groupJobs);
		}
		groupJobs.add(index, job);
	}

	/**
	 * The identifier for this measurement type.
	 */
	public static final String TYPE_IDENTIFIER = "YouScope.MicroPlateJob";

	/**
	 * Configuration of the focus device used for focussing the wells. Set to
	 * null to not set focus in wells.
	 */
	@XStreamAlias("focus")
	private FocusConfiguration focusConfiguration = null;

	/**
	 * Migrates configurations saved before well-group support. Legacy jobs and plain
	 * well selections are moved to GROUP0. This is idempotent.
	 */
	private void migrateLegacy() {
		if (jobs == null)
			jobs = new HashMap<>();

		if (legacyJobs != null) {
			if (!legacyJobs.isEmpty()) {
				ArrayList<JobConfiguration> group0 = jobs.get(WellWithGroup.WellGroup.GROUP0);
				if (group0 == null) {
					group0 = new ArrayList<>();
					jobs.put(WellWithGroup.WellGroup.GROUP0, group0);
				}
				group0.addAll(legacyJobs);
			}
			legacyJobs = null;
		}

		if (selectedWells == null)
			selectedWells = new HashSet<>(200);
		if (legacySelectedWells != null) {
			for (Well well : legacySelectedWells)
				selectedWells.add(new WellWithGroup(well, WellWithGroup.WellGroup.GROUP0));
			legacySelectedWells = null;
		}
	}

	/** Called by the deserialization lifecycle to migrate legacy configurations. */
	private Object readResolve() {
		migrateLegacy();
		return this;
	}

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}

	@Override
	public void removeJobAt(int index) {
		// Interpret the index over the flattened (union) job order, matching getJobs().
		int remaining = index;
		for (WellWithGroup.WellGroup group : sortedGroups()) {
			ArrayList<JobConfiguration> groupJobs = jobs.get(group);
			if (remaining < groupJobs.size()) {
				groupJobs.remove(remaining);
				return;
			}
			remaining -= groupJobs.size();
		}
	}

	@Override
	public void addJob(JobConfiguration job, int index) {
		ArrayList<JobConfiguration> groupJobs = jobs.get(WellWithGroup.WellGroup.GROUP0);
		if (groupJobs == null) {
			groupJobs = new ArrayList<>();
			jobs.put(WellWithGroup.WellGroup.GROUP0, groupJobs);
		}
		groupJobs.add(index < 0 ? 0 : (index > groupJobs.size() ? groupJobs.size() : index), job);
	}

	/**
	 * @param focusConfiguration
	 *                           The configuration of the focus used when entering a
	 *                           well. Set
	 *                           to NULL to not set focus.
	 */
	public void setFocusConfiguration(FocusConfiguration focusConfiguration) {
		this.focusConfiguration = focusConfiguration;
	}

	/**
	 * @return The configuration of the focus used when entering a well. NULL if
	 *         focus is not set.
	 */
	public FocusConfiguration getFocusConfiguration() {
		return focusConfiguration;
	}

	/**
	 * Sets the ID of the stage device which should be used to change between wells
	 * and positions. Set to NULL to use default stage device.
	 * 
	 * @param stageDevice The ID of the stage device, or NULL.
	 */
	public void setStageDevice(String stageDevice) {
		this.stageDevice = stageDevice;
	}

	/**
	 * Returns the ID of the stage device which should be used to change between
	 * wells and positions. Returns NULL to use default stage device.
	 * 
	 * @return The ID of the stage device, or NULL.
	 */
	public String getStageDevice() {
		return stageDevice;
	}

	@Override
	public String getDescription() {
		String description = "<p>Microplate Job</p>";
		if (selectedWells != null && selectedWells.size() > 0) {
			description += "<p>for well=1:" + Integer.toString(selectedWells.size()) + "</p>" +
					"<ul style=\"margin-top:0px;margin-bottom:0px;margin-left:12px;list-style-type:none\">";
			for (JobConfiguration job : getJobs()) {
				description += "<li>" + job.getDescription() + "</li>";
			}
			description += "</ul><p>end</p>";
		} else {
			description += "<p>No well specified.</p>";
		}
		return description;
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		for (ArrayList<JobConfiguration> groupJobs : jobs.values()) {
			for (JobConfiguration childJob : groupJobs) {
				childJob.checkConfiguration();
			}
		}
	}
}
