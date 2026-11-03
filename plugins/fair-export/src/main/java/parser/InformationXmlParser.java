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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parser for {@code information.xml}, the consolidated record YouScope writes
 * alongside every
 * measurement.
 * <p>
 * This file is the primary metadata source for the FAIR export: it already
 * merges the user entered
 * metadata, the state of every device at startup, the channel definitions with
 * their filter settings,
 * and the complete image table. {@code configuration.csb} and
 * {@code YSConfig_Microscope.cfg} are
 * consulted only for what it does not contain namely the Z step size and the
 * pixel
 * size presets.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class InformationXmlParser {
	/** File name of the consolidated measurement information. */
	public static final String FILE_NAME = "information.xml";

	private String measurementName = null;
	private String description = null;
	private final Map<String, String> metadataProperties = new LinkedHashMap<String, String>();
	/** Device state at startup, keyed as {@code Device.Property}. */
	private final Map<String, String> deviceState = new LinkedHashMap<String, String>();
	private final Map<String, ChannelDefinition> channelDefinitions = new LinkedHashMap<String, ChannelDefinition>();
	private final List<ImageRecord> images = new ArrayList<ImageRecord>();

	/**
	 * A channel definition with the device settings applied when the channel is
	 * switched on.
	 */
	public static class ChannelDefinition {
		public final String name;
		public final String group;
		/** Device settings applied on activation, keyed as {@code Device.Property}. */
		public final Map<String, String> settingsOn = new LinkedHashMap<String, String>();

		ChannelDefinition(String name, String group) {
			this.name = name;
			this.group = group;
		}

		/**
		 * Returns the key of this channel within its group, as used in the measurement
		 * configuration.
		 * 
		 * @return {@code group.name}.
		 */
		public String getKey() {
			return group + "." + name;
		}
	}

	/**
	 * One acquired image, as recorded in the {@code <images>} section.
	 */
	public static class ImageRecord {
		/** One based execution index of the measurement, i.e. the time point. */
		public int evaluation = 1;
		/** Milliseconds since the beginning of the measurement. */
		public long runtimeMs = 0;
		/** Absolute acquisition time in milliseconds since the epoch. */
		public long absoluteTimeMs = 0;
		/** Well as written in the file, e.g. "B2", or null. */
		public String well = null;
		/** Raw position field, e.g. "z-stack: 1, x-tile: 2, y-tile: 3", or empty. */
		public String position = null;
		/** File name of the image relative to the measurement folder. */
		public String path = null;
		/** Image save name, i.e. the series this image belongs to, e.g. "Cit". */
		public String imageSaveName = null;
		public String camera = null;
		public String channelGroup = null;
		public String channel = null;
		public Integer originalBitDepth = null;

		/** Zero based time point index. */
		public int getTimeIndex() {
			return evaluation - 1;
		}

		/**
		 * Zero based focal plane index, derived from the {@code z-stack} token of the
		 * position field.
		 */
		public int getFocalPlaneIndex() {
			return parseFocalPlaneIndex(position);
		}

		/**
		 * Zero based X tile index, or 0 if the position field carries no {@code x-tile}
		 * token.
		 */
		public int getTileIndexX() {
			return zeroBased(parsePositionToken(position, "x-tile"));
		}

		/**
		 * Zero based Y tile index, or 0 if the position field carries no {@code y-tile}
		 * token.
		 */
		public int getTileIndexY() {
			return zeroBased(parsePositionToken(position, "y-tile"));
		}

		/**
		 * True if this image belongs to a tile/grid scan (the position field has an
		 * {@code x-} or {@code y-tile} token).
		 */
		public boolean hasTile() {
			return parsePositionToken(position, "x-tile") != null || parsePositionToken(position, "y-tile") != null;
		}

		/** Zero based well row and column, or null if the well cannot be parsed. */
		public int[] getWellIndices() {
			return parseWell(well);
		}
	}

	/**
	 * Parses a measurement information file.
	 * 
	 * @param informationFile The {@code information.xml} of the measurement.
	 * @throws IOException Thrown if the file cannot be read or is not valid XML.
	 */
	public void parse(File informationFile) throws IOException {
		if (informationFile == null || !informationFile.isFile())
			throw new IOException("Measurement information file does not exist.");
		measurementName = null;
		description = null;
		metadataProperties.clear();
		deviceState.clear();
		channelDefinitions.clear();
		images.clear();

		Document document;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// The file is written by YouScope itself, but it is still user-influenced
			// input; disabling
			// external entities costs nothing and removes an XXE vector.
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(informationFile);
		} catch (SAXException e) {
			throw new IOException("Measurement information file is not valid XML.", e);
		} catch (Exception e) {
			throw new IOException("Could not parse measurement information file.", e);
		}

		Element root = document.getDocumentElement();
		measurementName = getChildText(root, "n");
		Element descriptionElement = getFirstChild(root, "description");
		if (descriptionElement != null) {
			// The description may contain simple HTML markup such as <p>; take the text
			// content.
			String text = descriptionElement.getTextContent();
			description = text == null ? null : text.trim();
			if (description != null && description.isEmpty())
				description = null;
		}

		parseMetadataProperties(root);
		parseDeviceState(root);
		parseChannelDefinitions(root);
		parseImages(root);
	}

	private void parseMetadataProperties(Element root) {
		Element container = getFirstChild(root, "metadata-properties");
		if (container == null)
			return;
		for (Element property : getChildren(container, "metadata-property")) {
			String name = property.getAttribute("name");
			if (name == null || name.isEmpty())
				continue;
			// Empty values are kept out of the model: an empty metadata field is not
			// information, and
			// exporting it would produce noise annotations.
			String value = property.getAttribute("value");
			if (value == null || value.trim().isEmpty())
				continue;
			metadataProperties.put(name, value.trim());
		}
	}

	private void parseDeviceState(Element root) {
		Element container = getFirstChild(root, "device-settings-at-startup");
		if (container == null)
			return;
		for (Element device : getChildren(container, "device")) {
			String deviceName = device.getAttribute("name");
			if (deviceName == null || deviceName.isEmpty())
				continue;
			for (Element property : getChildren(device, "property")) {
				String propertyName = property.getAttribute("name");
				if (propertyName == null || propertyName.isEmpty())
					continue;
				deviceState.put(deviceName + "." + propertyName, property.getAttribute("value"));
			}
		}
	}

	private void parseChannelDefinitions(Element root) {
		Element container = getFirstChild(root, "channel-definitions");
		if (container == null)
			return;
		for (Element channel : getChildren(container, "channel-definition")) {
			String name = channel.getAttribute("name");
			String group = channel.getAttribute("group");
			if (name == null || name.isEmpty())
				continue;
			ChannelDefinition definition = new ChannelDefinition(name, group == null ? "" : group);
			Element settingsOn = getFirstChild(channel, "device-settings-on");
			if (settingsOn != null) {
				for (Element setting : getChildren(settingsOn, "device-setting")) {
					String device = setting.getAttribute("device");
					String property = setting.getAttribute("property");
					if (device == null || device.isEmpty() || property == null || property.isEmpty())
						continue;
					definition.settingsOn.put(device + "." + property, setting.getAttribute("value"));
				}
			}
			channelDefinitions.put(definition.getKey(), definition);
		}
	}

	private void parseImages(Element root) {
		Element container = getFirstChild(root, "images");
		if (container == null)
			return;
		for (Element image : getChildren(container, "image")) {
			ImageRecord record = new ImageRecord();
			record.evaluation = parseInt(image.getAttribute("evaluation"), 1);
			record.runtimeMs = parseLong(image.getAttribute("runtime"), 0);
			record.absoluteTimeMs = parseLong(image.getAttribute("time"), 0);
			record.well = emptyToNull(image.getAttribute("well"));
			record.position = image.getAttribute("position");
			record.path = emptyToNull(image.getAttribute("path"));
			record.imageSaveName = emptyToNull(image.getAttribute("name"));
			record.camera = emptyToNull(image.getAttribute("camera"));
			record.channelGroup = emptyToNull(image.getAttribute("channel-group"));
			record.channel = emptyToNull(image.getAttribute("channel"));
			String bitDepth = image.getAttribute("original-bit-depth");
			record.originalBitDepth = (bitDepth == null || bitDepth.isEmpty()) ? null
					: Integer.valueOf(parseInt(bitDepth, 0));
			if (record.path != null)
				images.add(record);
		}
	}

	// Static helpers for the conventions used inside the file

	/**
	 * Converts a well name such as "B2" or "AA12" into zero based row and column
	 * indices, matching the
	 * OME convention and the {@code well-y} / {@code well-x} attributes of the
	 * measurement configuration.
	 * <p>
	 * Note that {@code images.csv} and {@code information.xml} use letter plus one
	 * based column, while
	 * the configuration uses zero based indices for both. OME agrees with the
	 * configuration.
	 * </p>
	 * 
	 * @param well Well name, e.g. "B2".
	 * @return Array of {row, column}, zero based, or null if the name cannot be
	 *         parsed.
	 */
	public static int[] parseWell(String well) {
		if (well == null)
			return null;
		String trimmed = well.trim();
		int index = 0;
		int row = 0;
		while (index < trimmed.length() && Character.isLetter(trimmed.charAt(index))) {
			row = row * 26 + (Character.toUpperCase(trimmed.charAt(index)) - 'A' + 1);
			index++;
		}
		if (index == 0 || index >= trimmed.length())
			return null;
		try {
			int column = Integer.parseInt(trimmed.substring(index).trim());
			if (row < 1 || column < 1)
				return null;
			return new int[] { row - 1, column - 1 };
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Derives the zero based focal plane index from the {@code position} field.
	 * <p>
	 * YouScope writes the position as a comma-separated list of labelled tokens,
	 * e.g.
	 * {@code "z-stack: 1, x-tile: 2, y-tile: 3"} for an image that is slice 1 of a
	 * Z stack and tile
	 * (2,3) of a grid scan; a plain Z stack is {@code "z-stack: 3"}; and the field
	 * is empty for a single
	 * image at the current focus. Only the {@code z-stack} token denotes the axial
	 * (Z) position — the
	 * {@code x-tile} / {@code y-tile} tokens are lateral positions and must
	 * <b>not</b> be read as focal
	 * planes (doing so is what made tile scans look like ragged Z stacks).
	 * </p>
	 * <p>
	 * For backward compatibility with any older file that stored a bare number with
	 * no label and no tile
	 * tokens, a lone trailing number is still accepted as the focal plane.
	 * </p>
	 * 
	 * @param position Raw position field.
	 * @return Zero based focal plane index; 0 if none is indicated.
	 */
	public static int parseFocalPlaneIndex(String position) {
		if (position == null)
			return 0;
		String trimmed = position.trim();
		if (trimmed.isEmpty())
			return 0;
		Integer z = parsePositionToken(trimmed, "z-stack");
		if (z != null)
			return zeroBased(z);
		// No z-stack label. If there are also no tile labels, fall back to a lone
		// trailing number
		// (legacy). If tile labels are present, this image's Z is 0 (it is a lateral
		// position only).
		if (parsePositionToken(trimmed, "x-tile") != null || parsePositionToken(trimmed, "y-tile") != null)
			return 0;
		return zeroBased(parseTrailingNumber(trimmed));
	}

	/**
	 * Reads a labelled, one based integer token such as {@code z-stack},
	 * {@code x-tile} or {@code y-tile}
	 * from a position field like {@code "z-stack: 1, x-tile: 2, y-tile: 3"}.
	 * 
	 * @param position The raw position field.
	 * @param token    The label to read (without the colon), case-insensitive.
	 * @return The one based value, or null if the token is absent or unparseable.
	 */
	static Integer parsePositionToken(String position, String token) {
		if (position == null || token == null)
			return null;
		String haystack = position.toLowerCase(java.util.Locale.ROOT);
		String needle = token.toLowerCase(java.util.Locale.ROOT);
		int at = haystack.indexOf(needle);
		while (at >= 0) {
			int after = at + needle.length();
			// require the label to be followed by a separator/colon so "tile" doesn't match
			// inside a word
			int i = after;
			while (i < position.length()
					&& (position.charAt(i) == ':' || position.charAt(i) == ' ' || position.charAt(i) == '='))
				i++;
			int start = i;
			while (i < position.length() && Character.isDigit(position.charAt(i)))
				i++;
			if (i > start) {
				try {
					return Integer.valueOf(Integer.parseInt(position.substring(start, i)));
				} catch (@SuppressWarnings("unused") NumberFormatException e) {
					return null;
				}
			}
			at = haystack.indexOf(needle, after);
		}
		return null;
	}

	/** Trailing integer of a string, or null. */
	private static Integer parseTrailingNumber(String value) {
		int end = value.length();
		int start = end;
		while (start > 0 && Character.isDigit(value.charAt(start - 1)))
			start--;
		if (start == end)
			return null;
		try {
			return Integer.valueOf(Integer.parseInt(value.substring(start, end)));
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Converts a one based token value (or null) to a zero based index (0 for null
	 * / non-positive).
	 */
	private static int zeroBased(Integer oneBased) {
		if (oneBased == null)
			return 0;
		int value = oneBased.intValue();
		return value > 0 ? value - 1 : 0;
	}

	// Accessors

	public String getMeasurementName() {
		return measurementName;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Returns the non-empty user entered metadata properties, in file order.
	 * 
	 * @return Property name to value.
	 */
	public Map<String, String> getMetadataProperties() {
		return Collections.unmodifiableMap(metadataProperties);
	}

	/**
	 * Returns the state of all devices at measurement start, keyed as
	 * {@code Device.Property}.
	 * 
	 * @return Device state.
	 */
	public Map<String, String> getDeviceState() {
		return Collections.unmodifiableMap(deviceState);
	}

	/**
	 * Returns the value of a single device property at measurement start.
	 * 
	 * @param device   Device name.
	 * @param property Property name.
	 * @return Value, or null if unknown.
	 */
	public String getDeviceProperty(String device, String property) {
		return deviceState.get(device + "." + property);
	}

	/**
	 * Returns a channel definition.
	 * 
	 * @param channelGroup Channel group.
	 * @param channel      Channel name.
	 * @return Definition, or null if unknown.
	 */
	public ChannelDefinition getChannelDefinition(String channelGroup, String channel) {
		return channelDefinitions.get(channelGroup + "." + channel);
	}

	/**
	 * Returns all image records in file order.
	 * 
	 * @return Image records.
	 */
	public List<ImageRecord> getImages() {
		return Collections.unmodifiableList(images);
	}

	// DOM helpers

	private static Element getFirstChild(Element parent, String tagName) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName()))
				return (Element) child;
		}
		return null;
	}

	private static List<Element> getChildren(Element parent, String tagName) {
		List<Element> result = new ArrayList<Element>();
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName()))
				result.add((Element) child);
		}
		return result;
	}

	private static String getChildText(Element parent, String tagName) {
		Element child = getFirstChild(parent, tagName);
		if (child == null)
			return null;
		String text = child.getTextContent();
		return (text == null || text.trim().isEmpty()) ? null : text.trim();
	}

	private static String emptyToNull(String value) {
		return (value == null || value.trim().isEmpty()) ? null : value.trim();
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (@SuppressWarnings("unused") Exception e) {
			return fallback;
		}
	}

	private static long parseLong(String value, long fallback) {
		try {
			return Long.parseLong(value.trim());
		} catch (@SuppressWarnings("unused") Exception e) {
			return fallback;
		}
	}
}
