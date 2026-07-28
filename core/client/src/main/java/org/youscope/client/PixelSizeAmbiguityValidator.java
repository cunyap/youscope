/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.youscope.common.microscope.DeviceSetting;
import org.youscope.common.microscope.PixelSize;

/**
 * Detects pixel size settings which are not mutually exclusive, i.e. which can
 * be simultaneously
 * active for the same microscope state.
 * <p>
 * A pixel size setting is a rule: a set of device settings that must be active,
 * and the resulting
 * pixel size. For the rule set as a whole to define a function of the
 * microscope state, at most one
 * setting may match any reachable state. If two settings can match at once,
 * YouScope cannot decide
 * which pixel size applies, and any choice it makes is arbitrary.
 * </p>
 * <p>
 * Two settings A and B can both be active exactly when they agree on every
 * device property that both
 * of them constrain. They are mutually exclusive only if there is at least one
 * commonly constrained
 * property on which they specify different values.
 * </p>
 * <p>
 * The subtle case is a property constrained by A but not by B: this does
 * <b>not</b> separate them.
 * B is then the more permissive rule and still matches whenever A does. A rule
 * that constrains only
 * the objective conflicts with one that constrains the objective and the
 * camera, whenever their
 * objectives agree.
 * </p>
 * <p>
 * Worked example from a real configuration: {@code 40x} requires
 * {@code Objective.Label = Nikon 40X Plan Fluor ELWD} and yields 0.1625 µm;
 * {@code Res40x} requires
 * the same and yields 0.25 µm. They share exactly one property and agree on it,
 * so both match
 * whenever that objective is in place. Neither value is wrong in itself but
 * they
 * describe cameras with
 * 6.5 µm and 10 µm pixel pitch and neither setting says which camera it
 * applies to. The fix is to
 * add the camera to the conditions of both, which makes them exclusive and both
 * correct.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class PixelSizeAmbiguityValidator {
	/**
	 * A pair of pixel size settings which can be active at the same time.
	 */
	public static class Conflict {
		/** ID of the first pixel size setting. */
		public final String firstId;
		/** ID of the second pixel size setting. */
		public final String secondId;
		/** Pixel size of the first setting, in micrometre. */
		public final double firstPixelSize;
		/** Pixel size of the second setting, in micrometre. */
		public final double secondPixelSize;
		/** Device properties constrained by both settings, with identical values. */
		public final List<String> sharedProperties;

		Conflict(String firstId, String secondId, double firstPixelSize, double secondPixelSize,
				List<String> sharedProperties) {
			this.firstId = firstId;
			this.secondId = secondId;
			this.firstPixelSize = firstPixelSize;
			this.secondPixelSize = secondPixelSize;
			this.sharedProperties = sharedProperties;
		}

		/**
		 * Returns true if the two settings would at least yield the same pixel size.
		 * Such a conflict is
		 * harmless for the resulting value, but still indicates a redundant
		 * configuration.
		 * 
		 * @return True if both settings define the same pixel size.
		 */
		public boolean isHarmless() {
			return Math.abs(firstPixelSize - secondPixelSize) < 1e-9;
		}

		/**
		 * Returns a message suitable for display in the configuration panel.
		 * 
		 * @return Human readable description of the conflict.
		 */
		public String getMessage() {
			StringBuilder builder = new StringBuilder();
			builder.append("Pixel size settings \"").append(firstId).append("\" and \"").append(secondId)
					.append("\" can be active at the same time");
			if (sharedProperties.isEmpty())
				builder.append(", because neither restricts any device setting the other also restricts");
			else {
				builder.append(", because they agree on every device setting they both restrict (");
				for (int i = 0; i < sharedProperties.size(); i++) {
					if (i > 0)
						builder.append(", ");
					builder.append(sharedProperties.get(i));
				}
				builder.append(')');
			}
			builder.append(". ");
			if (isHarmless()) {
				builder.append("Both define ").append(firstPixelSize)
						.append(" um, so the resulting pixel size is unaffected, but one of them is redundant.");
			} else {
				builder.append("They define ").append(firstPixelSize).append(" um and ").append(secondPixelSize)
						.append(" um. YouScope cannot determine which applies. Add a device setting that distinguishes them")
						.append(" e.g. typically the camera, if the settings describe cameras with different pixel pitch.");
			}
			return builder.toString();
		}
	}

	/**
	 * Checks all pixel size settings of the microscope for pairs which are not
	 * mutually exclusive.
	 * 
	 * @param pixelSizes All configured pixel size settings, as returned by
	 *                   {@code PixelSizeManager.getPixelSizes()}.
	 * @return All conflicting pairs, empty if the configuration is unambiguous.
	 * @throws Exception Thrown if a pixel size setting cannot be read.
	 */
	public static List<Conflict> validate(PixelSize[] pixelSizes) throws Exception {
		return validate(pixelSizes == null ? Collections.<PixelSize>emptyList() : Arrays.asList(pixelSizes));
	}

	/**
	 * Checks all pixel size settings of the microscope for pairs which are not
	 * mutually exclusive.
	 * 
	 * @param pixelSizes All configured pixel size settings.
	 * @return All conflicting pairs, empty if the configuration is unambiguous.
	 * @throws Exception Thrown if a pixel size setting cannot be read.
	 */
	public static List<Conflict> validate(Iterable<PixelSize> pixelSizes) throws Exception {
		List<String> ids = new ArrayList<String>();
		List<Double> sizes = new ArrayList<Double>();
		List<Map<String, String>> conditions = new ArrayList<Map<String, String>>();

		if (pixelSizes == null)
			return new ArrayList<Conflict>();
		for (PixelSize pixelSize : pixelSizes) {
			ids.add(pixelSize.getPixelSizeID());
			sizes.add(Double.valueOf(pixelSize.getPixelSize()));
			conditions.add(toConditionMap(pixelSize.getPixelSizeSettings()));
		}

		List<Conflict> conflicts = new ArrayList<Conflict>();
		for (int i = 0; i < ids.size(); i++) {
			for (int j = i + 1; j < ids.size(); j++) {
				List<String> shared = getSharedAgreeingProperties(conditions.get(i), conditions.get(j));
				if (shared == null)
					continue; // they disagree somewhere: mutually exclusive, nothing to report.
				conflicts.add(new Conflict(ids.get(i), ids.get(j),
						sizes.get(i).doubleValue(), sizes.get(j).doubleValue(), shared));
			}
		}
		return conflicts;
	}

	/**
	 * Returns the properties both condition sets constrain and agree on, or null if
	 * they disagree on at
	 * least one commonly constrained property in which case they can never both
	 * be active.
	 */
	private static List<String> getSharedAgreeingProperties(Map<String, String> first, Map<String, String> second) {
		Set<String> sharedKeys = new HashSet<String>(first.keySet());
		sharedKeys.retainAll(second.keySet());

		List<String> agreeing = new ArrayList<String>();
		for (String key : sharedKeys) {
			String firstValue = first.get(key);
			String secondValue = second.get(key);
			if (firstValue == null ? secondValue != null : !firstValue.equals(secondValue))
				return null;
			agreeing.add(key + " = " + firstValue);
		}
		// No shared key at all, or all shared keys agree: both rules match
		// simultaneously.
		return agreeing;
	}

	private static Map<String, String> toConditionMap(DeviceSetting[] settings) {
		Map<String, String> result = new HashMap<String, String>();
		if (settings == null)
			return result;
		for (DeviceSetting setting : settings) {
			if (setting == null)
				continue;
			result.put(setting.getDevice() + "." + setting.getProperty(), setting.getStringValue());
		}
		return result;
	}
}
