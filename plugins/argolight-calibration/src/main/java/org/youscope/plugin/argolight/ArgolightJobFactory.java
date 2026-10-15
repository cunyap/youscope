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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.youscope.common.configuration.FocusConfiguration;
import org.youscope.common.job.JobConfiguration;
import org.youscope.plugin.composedimaging.PlateScanningJobConfiguration;
import org.youscope.plugin.imagingjob.ImagingJobConfiguration;
import org.youscope.plugin.zslides.ZSlidesJobConfiguration;

/**
 * Builds the pre-filled imaging protocol for a whole well group from the plate
 * definition. Kept free of any UI/server dependency so the logic is
 * unit-testable.
 *
 * @author Andreas P. Cuny
 */
final class ArgolightJobFactory {
	private ArgolightJobFactory() {
		// utility class.
	}

	/** @return all patterns assigned to the given group, in definition order. */
	static List<ArgolightPattern> membersOfGroup(ArgolightPlateDefinition definition, int groupId) {
		ArrayList<ArgolightPattern> members = new ArrayList<>();
		for (ArgolightPattern p : definition.getPatterns()) {
			if (p.getGroupId() == groupId)
				members.add(p);
		}
		return members;
	}

	/**
	 * Builds the default job list for a group, derived from every pattern assigned
	 * to it (so the protocol does not depend on which well is selected first):
	 * imaging job, wrapped in z-slides if any member is a z-stack (planes = union
	 * of all members' planes), wrapped in a plate-scanning job if any member is a
	 * scan pattern (sized to the largest scan field in the group; always created
	 * for scan groups, even at a single tile).
	 *
	 * @param definition         the plate definition.
	 * @param groupId            the group id (0..6).
	 * @param focusConfiguration focus configuration for the z-slides job (may be
	 *                           null).
	 * @return the job list to pre-fill (never empty).
	 */
	static JobConfiguration[] buildGroupJobs(ArgolightPlateDefinition definition, int groupId,
			FocusConfiguration focusConfiguration) {
		return buildGroupJobs(definition, groupId, focusConfiguration, null, null, null, null);
	}

	/**
	 * As
	 * {@link #buildGroupJobs(ArgolightPlateDefinition, int, FocusConfiguration)},
	 * but overriding the imaging channel / camera / exposure with the values chosen
	 * in the position fine-configuration live view when they are non-null.
	 */
	static JobConfiguration[] buildGroupJobs(ArgolightPlateDefinition definition, int groupId,
			FocusConfiguration focusConfiguration, String overrideChannelGroup, String overrideChannel,
			String overrideCamera, Double overrideExposureMs) {
		List<ArgolightPattern> members = membersOfGroup(definition, groupId);
		ArgolightPattern first = members.isEmpty() ? null : members.get(0);

		// Core acquisition: imaging, optionally wrapped in a z-slides container.
		JobConfiguration core = buildImagingJob(definition, first, overrideChannelGroup, overrideChannel,
				overrideCamera, overrideExposureMs, null);

		double[] zPlanes = unionZPlanes(members);
		if (zPlanes.length > 0) {
			ZSlidesJobConfiguration zJob = new ZSlidesJobConfiguration();
			zJob.setSlideZPositions(zPlanes);
			zJob.setJobs(new JobConfiguration[] { core });
			zJob.setFocusConfiguration(focusConfiguration != null ? focusConfiguration : new FocusConfiguration());
			core = zJob;
		}

		double scanW = 0, scanH = 0;
		for (ArgolightPattern p : members) {
			if (p.isScan()) {
				scanW = Math.max(scanW, p.getScanFieldWidthUm());
				scanH = Math.max(scanH, p.getScanFieldHeightUm());
			}
		}
		if (scanW > 0 || scanH > 0) {
			double stepX = Math.max(1.0, definition.getCameraFovWidthUm() * (1.0 - definition.getTileOverlap()));
			double stepY = Math.max(1.0, definition.getCameraFovHeightUm() * (1.0 - definition.getTileOverlap()));
			PlateScanningJobConfiguration scanJob = new PlateScanningJobConfiguration();
			scanJob.setNumTilesX(tilesForField(scanW, definition.getCameraFovWidthUm(), stepX));
			scanJob.setNumTilesY(tilesForField(scanH, definition.getCameraFovHeightUm(), stepY));
			scanJob.setDeltaX(stepX);
			scanJob.setDeltaY(stepY);
			scanJob.setJobs(new JobConfiguration[] { core });

			// A leading single-frame imaging job precedes the tiling job: at low
			// magnification one frame already covers the whole feature, so the user
			// can keep that and disable the (then redundant) plate-scanning job.
			JobConfiguration overview = buildImagingJob(definition, first, overrideChannelGroup, overrideChannel,
					overrideCamera, overrideExposureMs, "-overview");
			return new JobConfiguration[] { overview, scanJob };
		}

		// A position may request a leading single-frame overview image even when it is
		// not a field scan: the background region takes one in-focus frame (saved as
		// "<id>-overview") in addition to its z-stack (saved as "<id>"), mirroring the
		// overview + plate-scanning pair produced for scan groups above.
		boolean wantsOverview = false;
		for (ArgolightPattern p : members) {
			if (p.isOverview()) {
				wantsOverview = true;
				break;
			}
		}
		if (wantsOverview) {
			JobConfiguration overview = buildImagingJob(definition, first, overrideChannelGroup, overrideChannel,
					overrideCamera, overrideExposureMs, "-overview");
			return new JobConfiguration[] { overview, core };
		}

		return new JobConfiguration[] { core };
	}

	private static ImagingJobConfiguration buildImagingJob(ArgolightPlateDefinition definition,
			ArgolightPattern pattern,
			String overrideChannelGroup, String overrideChannel, String overrideCamera, Double overrideExposureMs,
			String saveNameSuffix) {
		ImagingJobConfiguration imaging = new ImagingJobConfiguration();
		String channelGroup = (overrideChannelGroup != null && overrideChannelGroup.length() > 0)
				? overrideChannelGroup
				: definition.getChannelGroup();
		String channel = (overrideChannel != null && overrideChannel.length() > 0)
				? overrideChannel
				: definition.getChannel();
		if (channel != null && channel.length() > 0)
			imaging.setChannel(channelGroup, channel);
		String camera = (overrideCamera != null && overrideCamera.length() > 0)
				? overrideCamera
				: definition.getCamera();
		if (camera != null && camera.length() > 0)
			imaging.setCamera(camera);
		imaging.setExposure(overrideExposureMs != null ? overrideExposureMs.doubleValue() : definition.getExposureMs());
		imaging.setSaveImages(definition.isSaveImages());
		String base = pattern != null ? pattern.getId() : "image";
		if (saveNameSuffix != null && saveNameSuffix.length() > 0)
			base = base + saveNameSuffix;
		imaging.setImageSaveName(sanitize(base));
		return imaging;
	}

	/**
	 * Sorted, de-duplicated union of the z planes of all z-stack patterns in the
	 * group.
	 */
	static double[] unionZPlanes(List<ArgolightPattern> members) {
		TreeSet<Double> set = new TreeSet<>();
		for (ArgolightPattern p : members) {
			if (p.isZStack()) {
				for (double z : p.computeRelativeZPositions())
					set.add(Math.round(z * 1e6) / 1e6);
			}
		}
		double[] result = new double[set.size()];
		int i = 0;
		for (Double z : set)
			result[i++] = z;
		return result;
	}

	static int tilesForField(double field, double fov, double step) {
		if (field <= fov || field <= 0)
			return 1;
		return (int) Math.max(1, Math.ceil((field - fov) / step) + 1);
	}

	private static String sanitize(String name) {
		if (name == null)
			return "image";
		String s = name.replaceAll("[^A-Za-z0-9_-]", "_");
		return s.length() == 0 ? "image" : s;
	}
}
