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

package org.youscope.plugin.fairexport.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for {@code YSConfig_Microscope.cfg}, the Micro-Manager 1.4 / 2.0
 * compatible hardware
 * configuration YouScope stores alongside every measurement.
 * <p>
 * The file is a flat, comma separated list of directives, one per line, with
 * {@code #} comments:
 * </p>
 * 
 * <pre>
 * Configuration entry formats:
 *
 * Device          - name, library, adapter
 * Parent          - name, parent
 * Label           - device, state, label
 * Property        - device, property, value
 * ConfigGroup     - group, preset, device, property, value
 * ConfigPixelSize - preset, device, property, value
 * PixelSize_um    - preset, micrometre
 * </pre>
 * <p>
 * Its main job for the FAIR export is resolving the lateral pixel size, which
 * is not stored directly
 * anywhere. The chain is: the objective device state recorded in
 * {@code information.xml}, through the
 * {@code Label} table to an objective label, through a matching
 * {@code ConfigPixelSize} preset, to a
 * {@code PixelSize_um} value.
 * </p>
 * <p>
 * The parser never guesses. If no preset matches, or if several presets match
 * with conflicting values,
 * it records a warning and returns null so that the exporter omits
 * {@code PhysicalSizeX} entirely. A
 * missing pixel size is recoverable by the user; a wrong one silently corrupts
 * every downstream
 * measurement.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class MicroscopeConfigParser {
	/**
	 * Name of the objective turret device in the standard YouScope configuration.
	 */
	public static final String DEFAULT_OBJECTIVE_DEVICE = "Objective";

	private static final Pattern MAGNIFICATION_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*[xX](?![a-zA-Z])");

	/** Device name to {library, adapter}. */
	private final Map<String, String[]> devices = new LinkedHashMap<String, String[]>();
	/** Device name to state index to label. */
	private final Map<String, Map<Integer, String>> labels = new LinkedHashMap<String, Map<Integer, String>>();
	/** Device name to property name to value, as configured at startup. */
	private final Map<String, Map<String, String>> properties = new LinkedHashMap<String, Map<String, String>>();
	/** Group name to preset name to the device settings that preset applies. */
	private final Map<String, Map<String, List<Setting>>> configGroups = new LinkedHashMap<String, Map<String, List<Setting>>>();
	/** Pixel size preset name to the device settings it is conditional on. */
	private final Map<String, List<Setting>> pixelSizeConditions = new LinkedHashMap<String, List<Setting>>();
	/** Pixel size preset name to micrometre per pixel. */
	private final Map<String, Double> pixelSizes = new LinkedHashMap<String, Double>();

	private final List<String> warnings = new ArrayList<String>();

	/**
	 * One {@code device.property = value} setting.
	 */
	public static class Setting {
		public final String device;
		public final String property;
		public final String value;

		Setting(String device, String property, String value) {
			this.device = device;
			this.property = property;
			this.value = value;
		}

		String getKey() {
			return device + "." + property;
		}
	}

	/**
	 * Parses a microscope configuration file. May be called repeatedly; each call
	 * resets the parser.
	 * 
	 * @param configFile The {@code YSConfig_Microscope.cfg} of the measurement.
	 * @throws IOException Thrown if the file cannot be read.
	 */
	public void parse(File configFile) throws IOException {
		if (configFile == null || !configFile.isFile())
			throw new IOException("Microscope configuration file does not exist.");
		devices.clear();
		labels.clear();
		properties.clear();
		configGroups.clear();
		pixelSizeConditions.clear();
		pixelSizes.clear();
		warnings.clear();

		// The file is written on Windows and may contain non-ASCII characters in device
		// labels.
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(configFile), Charset.forName("ISO-8859-1")));
		try {
			String line;
			int lineNumber = 0;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '#')
					continue;
				parseLine(line, lineNumber);
			}
		} finally {
			reader.close();
		}
	}

	private void parseLine(String line, int lineNumber) {
		// Split with a limit so that commas inside the last field (device labels may
		// contain them)
		// are preserved instead of silently truncating the label.
		if (line.startsWith("Device,")) {
			String[] parts = line.split(",", 4);
			if (parts.length == 4)
				devices.put(parts[1], new String[] { parts[2], parts[3] });
		} else if (line.startsWith("Label,")) {
			String[] parts = line.split(",", 4);
			if (parts.length != 4)
				return;
			Integer state = parseInteger(parts[2]);
			if (state == null) {
				warnings.add(
						"Line " + lineNumber + ": label state \"" + parts[2] + "\" is not a number; line ignored.");
				return;
			}
			Map<Integer, String> deviceLabels = labels.get(parts[1]);
			if (deviceLabels == null) {
				deviceLabels = new HashMap<Integer, String>();
				labels.put(parts[1], deviceLabels);
			}
			deviceLabels.put(state, parts[3]);
		} else if (line.startsWith("Property,")) {
			String[] parts = line.split(",", 4);
			if (parts.length != 4)
				return;
			Map<String, String> deviceProperties = properties.get(parts[1]);
			if (deviceProperties == null) {
				deviceProperties = new LinkedHashMap<String, String>();
				properties.put(parts[1], deviceProperties);
			}
			deviceProperties.put(parts[2], parts[3]);
		} else if (line.startsWith("ConfigGroup,")) {
			String[] parts = line.split(",", 6);
			if (parts.length != 6)
				return;
			Map<String, List<Setting>> group = configGroups.get(parts[1]);
			if (group == null) {
				group = new LinkedHashMap<String, List<Setting>>();
				configGroups.put(parts[1], group);
			}
			List<Setting> preset = group.get(parts[2]);
			if (preset == null) {
				preset = new ArrayList<Setting>();
				group.put(parts[2], preset);
			}
			preset.add(new Setting(parts[3], parts[4], parts[5]));
		} else if (line.startsWith("ConfigPixelSize,")) {
			String[] parts = line.split(",", 5);
			if (parts.length != 5)
				return;
			List<Setting> conditions = pixelSizeConditions.get(parts[1]);
			if (conditions == null) {
				conditions = new ArrayList<Setting>();
				pixelSizeConditions.put(parts[1], conditions);
			}
			conditions.add(new Setting(parts[2], parts[3], parts[4]));
		} else if (line.startsWith("PixelSize_um,")) {
			String[] parts = line.split(",", 3);
			if (parts.length != 3)
				return;
			Double value = parseDouble(parts[2]);
			if (value == null || value.doubleValue() <= 0) {
				warnings.add("Line " + lineNumber + ": pixel size \"" + parts[2]
						+ "\" is not a positive number; preset \"" + parts[1] + "\" ignored.");
				return;
			}
			pixelSizes.put(parts[1], value);
		}
		// Parent and unknown directives carry nothing the FAIR export needs.
	}

	// Resolution

	/**
	 * Resolves the label of a device from the state index recorded in
	 * {@code information.xml}.
	 * <p>
	 * The state index is authoritative. The {@code Label} property in the device
	 * dump may be the raw
	 * label of the device adapter rather than the one assigned in the configuration
	 * file, in which case
	 * it does not appear in the {@code Label} table at all. The dumped label is
	 * therefore only used as a
	 * fallback, and only if it is actually known to the configuration.
	 * </p>
	 * 
	 * @param deviceName  Name of the device, e.g. "Objective".
	 * @param state       State index from the device dump, or null if unknown.
	 * @param dumpedLabel Label from the device dump, or null.
	 * @return Resolved label, or null if it cannot be determined.
	 */
	public String resolveDeviceLabel(String deviceName, Integer state, String dumpedLabel) {
		Map<Integer, String> deviceLabels = labels.get(deviceName);
		if (deviceLabels != null && state != null) {
			String label = deviceLabels.get(state);
			if (label != null) {
				if (dumpedLabel != null && !dumpedLabel.equals(label)) {
					warnings.add(
							"Device \"" + deviceName + "\" reports label \"" + dumpedLabel + "\" but state " + state
									+ " is configured as \"" + label + "\". Using the configured label.");
				}
				return label;
			}
			warnings.add("Device \"" + deviceName + "\" is in state " + state
					+ ", which has no label in the configuration.");
		}
		if (dumpedLabel != null && deviceLabels != null && deviceLabels.containsValue(dumpedLabel))
			return dumpedLabel;
		return null;
	}

	/**
	 * Resolves the name of the objective-changer device in this configuration.
	 * <p>
	 * Micro-Manager does not fix a name for the objective device: the demo turret
	 * adapter calls it
	 * {@code Objective}, Nikon Ti-E / Ti2 call it {@code Nosepiece}, and Zeiss /
	 * Leica / Olympus use
	 * variants such as {@code ObjectiveTurret} or {@code ObjectiveRevolver}. The
	 * name is resolved in
	 * three steps, strongest signal first:
	 * </p>
	 * <ol>
	 * <li>a device that a {@code ConfigPixelSize} preset is conditional on (pixel
	 * size is calibrated
	 * per objective, so the objective changer is among the condition devices) and
	 * whose name looks
	 * like an objective changer; failing a name match, the sole condition device if
	 * there is only one;</li>
	 * <li>any configured device whose label, adapter or library name looks like an
	 * objective changer;</li>
	 * <li>the legacy default {@link #DEFAULT_OBJECTIVE_DEVICE}.</li>
	 * </ol>
	 * 
	 * @return The objective device name, never null.
	 */
	public String resolveObjectiveDeviceName() {
		Set<String> conditionDevices = new LinkedHashSet<String>();
		for (List<Setting> conditions : pixelSizeConditions.values())
			for (Setting condition : conditions)
				conditionDevices.add(condition.device);
		for (String device : conditionDevices) {
			String[] adapter = devices.get(device);
			if (looksLikeObjectiveDevice(device, adapter))
				return device;
		}
		if (conditionDevices.size() == 1)
			return conditionDevices.iterator().next();
		for (Map.Entry<String, String[]> entry : devices.entrySet())
			if (looksLikeObjectiveDevice(entry.getKey(), entry.getValue()))
				return entry.getKey();
		return DEFAULT_OBJECTIVE_DEVICE;
	}

	/**
	 * Tests whether a device is an objective changer, by its configured label and,
	 * if known, its
	 * {@code [library, adapter]} names. Matches {@code objective} and
	 * {@code nosepiece} anywhere, plus
	 * the {@code objturret} / {@code objrevolver} / {@code objchanger} compounds.
	 * Deliberately does not
	 * match a bare {@code turret}, so filter and condenser turrets are not mistaken
	 * for the objective.
	 */
	private static boolean looksLikeObjectiveDevice(String label, String[] adapter) {
		String[] candidates = new String[] {
				label,
				adapter != null && adapter.length > 1 ? adapter[1] : null,
				adapter != null && adapter.length > 0 ? adapter[0] : null };
		for (String candidate : candidates) {
			if (candidate == null)
				continue;
			String normalized = candidate.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
			if (normalized.contains("nosepiece") || normalized.contains("objective")
					|| normalized.contains("objturret") || normalized.contains("objrevolver")
					|| normalized.contains("objchanger"))
				return true;
		}
		return false;
	}

	/**
	 * Resolves the lateral pixel size for a given set of current device settings.
	 * <p>
	 * A pixel size preset applies when every one of its conditions matches the
	 * current device state.
	 * If several presets match, their values must agree; if they do not, the
	 * configuration is ambiguous
	 * and null is returned together with a warning naming the conflicting presets.
	 * </p>
	 * 
	 * @param currentSettings Current device settings as {@code device.property} to
	 *                        value, e.g.
	 *                        {@code {"Objective.Label": "Nikon 10X S Fluor"}}.
	 * @return Micrometre per pixel, or null if it cannot be determined
	 *         unambiguously.
	 */
	public Double resolvePixelSizeMicrons(Map<String, String> currentSettings) {
		if (currentSettings == null)
			currentSettings = Collections.emptyMap();
		List<String> matchingPresets = new ArrayList<String>();
		for (Map.Entry<String, List<Setting>> entry : pixelSizeConditions.entrySet()) {
			if (!pixelSizes.containsKey(entry.getKey())) {
				warnings.add("Pixel size preset \"" + entry.getKey() + "\" has conditions but no PixelSize_um value.");
				continue;
			}
			boolean matches = true;
			for (Setting condition : entry.getValue()) {
				String currentValue = currentSettings.get(condition.getKey());
				// Tolerant match: labels come from free-text config, so ignore surrounding
				// whitespace and
				// case. A wrong pixel size is dangerous, but whitespace/case differences in a
				// device label
				// are never a real distinction and were silently dropping valid presets.
				if (currentValue == null || !equalsNormalized(currentValue, condition.value)) {
					matches = false;
					break;
				}
			}
			if (matches)
				matchingPresets.add(entry.getKey());
		}

		if (matchingPresets.isEmpty()) {
			// Name the objective actually configured and the labels the presets expect, so
			// the mismatch is
			// obvious (usually the measurement used an objective that has no
			// ConfigPixelSize preset in the
			// YSConfig_Microscope.cfg saved with the measurement).
			String objectiveDevice = resolveObjectiveDeviceName();
			String objectiveLabel = currentSettings.get(objectiveDevice + ".Label");
			StringBuilder presetLabels = new StringBuilder();
			for (Map.Entry<String, List<Setting>> entry : pixelSizeConditions.entrySet()) {
				for (Setting condition : entry.getValue()) {
					if (condition.getKey().equals(objectiveDevice + ".Label")) {
						if (presetLabels.length() > 0)
							presetLabels.append(", ");
						presetLabels.append(entry.getKey()).append(" -> \"").append(condition.value).append("\"");
					}
				}
			}
			StringBuilder message = new StringBuilder("No pixel size preset matches the current device settings");
			if (objectiveLabel != null)
				message.append(" (objective in use: \"").append(objectiveLabel).append("\")");
			if (presetLabels.length() > 0)
				message.append("; presets define pixel sizes for: ").append(presetLabels);
			message.append(". PhysicalSizeX/Y will be omitted. Add a ConfigPixelSize/PixelSize_um preset for "
					+ "this objective in the measurement's YSConfig_Microscope.cfg.");
			warnings.add(message.toString());
			return null;
		}
		if (matchingPresets.size() == 1)
			return pixelSizes.get(matchingPresets.get(0));

		// Several presets match. Accept only if they agree.
		Double first = pixelSizes.get(matchingPresets.get(0));
		StringBuilder description = new StringBuilder();
		boolean consistent = true;
		for (String preset : matchingPresets) {
			Double value = pixelSizes.get(preset);
			if (description.length() > 0)
				description.append(", ");
			description.append(preset).append(" = ").append(value).append(" um");
			if (!first.equals(value))
				consistent = false;
		}
		if (consistent)
			return first;
		warnings.add("Several pixel size presets match the current objective with conflicting values (" + description
				+ "). The configuration is ambiguous, so PhysicalSizeX/Y will be omitted. Remove the duplicate preset in YSConfig_Microscope.cfg.");
		return null;
	}

	/**
	 * Compares two config values ignoring surrounding whitespace and case (device
	 * labels are free text).
	 */
	private static boolean equalsNormalized(String a, String b) {
		if (a == null || b == null)
			return a == b;
		return a.trim().equalsIgnoreCase(b.trim());
	}

	/**
	 * Extracts the nominal magnification from an objective label such as "Nikon 10X
	 * S Fluor".
	 * 
	 * @param objectiveLabel Label of the objective, or null.
	 * @return Nominal magnification, or null if the label does not contain one.
	 */
	public static Double parseMagnification(String objectiveLabel) {
		if (objectiveLabel == null)
			return null;
		Matcher matcher = MAGNIFICATION_PATTERN.matcher(objectiveLabel);
		if (!matcher.find())
			return null;
		return parseDouble(matcher.group(1));
	}

	// Accessors

	/**
	 * Returns the device settings a channel preset applies, e.g. the dichroic,
	 * excitation and emission
	 * filter labels of channel "Cit" in group "Channels".
	 * 
	 * @param channelGroup Name of the channel group.
	 * @param channel      Name of the channel.
	 * @return Settings of the preset, or an empty list if the preset is unknown.
	 */
	public List<Setting> getChannelSettings(String channelGroup, String channel) {
		Map<String, List<Setting>> group = configGroups.get(channelGroup);
		if (group == null)
			return Collections.emptyList();
		List<Setting> preset = group.get(channel);
		return preset == null ? Collections.<Setting>emptyList() : Collections.unmodifiableList(preset);
	}

	/**
	 * Returns the device adapter library and adapter name of a device, or null if
	 * the device is unknown.
	 * 
	 * @param deviceName Name of the device.
	 * @return Array of {library, adapter}, or null.
	 */
	public String[] getDeviceAdapter(String deviceName) {
		return devices.get(deviceName);
	}

	/**
	 * Returns all device names defined in the configuration.
	 * 
	 * @return Device names.
	 */
	public List<String> getDeviceNames() {
		return new ArrayList<String>(devices.keySet());
	}

	/**
	 * Returns the pixel size presets defined in the configuration, for diagnostics.
	 * 
	 * @return Preset name to micrometre per pixel.
	 */
	public Map<String, Double> getPixelSizePresets() {
		return Collections.unmodifiableMap(pixelSizes);
	}

	/**
	 * Returns the warnings collected while parsing and resolving.
	 * 
	 * @return Collected warnings.
	 */
	public List<String> getWarnings() {
		return new ArrayList<String>(warnings);
	}

	private static Integer parseInteger(String value) {
		try {
			return Integer.valueOf(value.trim());
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return null;
		}
	}

	private static Double parseDouble(String value) {
		try {
			return Double.valueOf(value.trim().replace(',', '.'));
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return null;
		}
	}
}
