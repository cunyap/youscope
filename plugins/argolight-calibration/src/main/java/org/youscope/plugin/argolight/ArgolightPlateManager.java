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

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Loads {@link ArgolightPlateDefinition}s.
 *
 * <p>
 * Definitions are XML files. A built-in default ("Argolight LM (default)")
 * ships inside the plugin jar. Additional user definitions can be dropped into
 * the {@code configuration/argolight} folder next to the YouScope executable;
 * every {@code *.xml} there is offered in the wizard. Adding a new Argolight
 * (or other manufacturer) layout is therefore a matter of adding an XML file,
 * no recompilation required.
 * </p>
 *
 * @author Andreas P. Cuny
 */
public final class ArgolightPlateManager {
	/** Name of the bundled default definition (also its identifier). */
	public static final String DEFAULT_DEFINITION_NAME = "Argolight LM (default)";

	private static final String DEFAULT_RESOURCE = "argolight-default.xml";
	private static final String USER_FOLDER = "configuration/argolight";

	private ArgolightPlateManager() {
		// utility class.
	}

	/**
	 * Returns all available definitions keyed by name, insertion-ordered with the
	 * bundled default first. Malformed user files are skipped (their name is
	 * simply absent); callers may log a warning if desired.
	 * 
	 * @return map of definition name -> definition.
	 */
	public static synchronized Map<String, ArgolightPlateDefinition> getAvailableDefinitions() {
		LinkedHashMap<String, ArgolightPlateDefinition> result = new LinkedHashMap<>();
		try {
			ArgolightPlateDefinition def = loadDefault();
			result.put(def.getName(), def);
		} catch (Exception e) {
			// Should never happen for the bundled resource; ignore.
		}
		File folder = new File(USER_FOLDER);
		if (folder.exists() && folder.isDirectory()) {
			File[] files = folder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String fileName) {
					return fileName.toLowerCase().endsWith(".xml");
				}
			});
			if (files != null) {
				for (File file : files) {
					try {
						ArgolightPlateDefinition def = parse(readDocument(file));
						result.put(def.getName(), def);
					} catch (Exception e) {
						// Skip malformed file.
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns the definition with the given name, falling back to the bundled
	 * default if the name is null or unknown.
	 * 
	 * @param name definition name, or null.
	 * @return a definition (never null).
	 * @throws ArgolightException if even the default cannot be loaded.
	 */
	public static synchronized ArgolightPlateDefinition getDefinition(String name) throws ArgolightException {
		if (name != null) {
			ArgolightPlateDefinition def = getAvailableDefinitions().get(name);
			if (def != null)
				return def;
		}
		return loadDefault();
	}

	/**
	 * Loads the bundled default definition from the plugin jar.
	 * 
	 * @return the default definition.
	 * @throws ArgolightException if the bundled resource cannot be read.
	 */
	public static ArgolightPlateDefinition loadDefault() throws ArgolightException {
		try (InputStream in = ArgolightPlateManager.class.getResourceAsStream(DEFAULT_RESOURCE)) {
			if (in == null)
				throw new ArgolightException(
						"Bundled default Argolight definition '" + DEFAULT_RESOURCE + "' not found on classpath.");
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(in);
			return parse(document);
		} catch (ArgolightException e) {
			throw e;
		} catch (Exception e) {
			throw new ArgolightException("Could not load bundled default Argolight definition.", e);
		}
	}

	private static Document readDocument(File file) throws ArgolightException {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			return builder.parse(file);
		} catch (Exception e) {
			throw new ArgolightException("Could not parse Argolight definition file '" + file.getAbsolutePath() + "'.",
					e);
		}
	}

	private static ArgolightPlateDefinition parse(Document document) throws ArgolightException {
		Element root = document.getDocumentElement();
		if (root == null || !"argolight-plate".equals(root.getNodeName()))
			throw new ArgolightException("Root element must be <argolight-plate>.");

		ArgolightPlateDefinition definition = new ArgolightPlateDefinition();

		String name = getChildText(root, "name");
		if (name != null && name.trim().length() > 0)
			definition.setName(name.trim());

		Element defaults = getChildElement(root, "imaging-defaults");
		if (defaults != null) {
			String cg = getChildText(defaults, "channel-group");
			if (cg != null)
				definition.setChannelGroup(cg.trim());
			String ch = getChildText(defaults, "channel");
			if (ch != null)
				definition.setChannel(ch.trim());
			String cam = getChildText(defaults, "camera");
			if (cam != null)
				definition.setCamera(cam.trim());
			String exp = getChildText(defaults, "exposure-ms");
			if (exp != null && exp.trim().length() > 0)
				definition.setExposureMs(parseDouble(exp, 5.0));
			String save = getChildText(defaults, "save-images");
			if (save != null)
				definition.setSaveImages(parseBoolean(save, true));
			String cpx = getChildText(defaults, "camera-pixels-x");
			if (cpx != null && cpx.trim().length() > 0)
				definition.setCameraPixelsX((int) parseDouble(cpx, 2048));
			String cpy = getChildText(defaults, "camera-pixels-y");
			if (cpy != null && cpy.trim().length() > 0)
				definition.setCameraPixelsY((int) parseDouble(cpy, 2048));
			String ps = getChildText(defaults, "pixel-size-um");
			if (ps != null && ps.trim().length() > 0)
				definition.setPixelSizeUm(parseDouble(ps, 0.1625));
			String ov = getChildText(defaults, "tile-overlap");
			if (ov != null && ov.trim().length() > 0)
				definition.setTileOverlap(parseDouble(ov, 0.1));
		}

		// Optional stage orientation (affects the physical stage layout only).
		Element stage = getChildElement(root, "stage");
		if (stage != null) {
			definition.setStageMirrorX(parseBoolean(attr(stage, "mirror-x", "false"), false));
			definition.setStageMirrorY(parseBoolean(attr(stage, "mirror-y", "false"), false));
			definition.setStageTranspose(parseBoolean(attr(stage, "transpose", "false"), false));
		}

		Element patternsNode = getChildElement(root, "patterns");
		if (patternsNode == null)
			throw new ArgolightException("Missing <patterns> element.");
		NodeList patternList = patternsNode.getElementsByTagName("pattern");
		if (patternList.getLength() == 0)
			throw new ArgolightException("No <pattern> elements found.");

		for (int i = 0; i < patternList.getLength(); i++) {
			Element p = (Element) patternList.item(i);
			definition.addPattern(parsePattern(p, i));
		}

		// Expand any <cross-grid> elements into individual cross patterns.
		NodeList gridList = patternsNode.getElementsByTagName("cross-grid");
		for (int i = 0; i < gridList.getLength(); i++)
			expandCrossGrid(definition, (Element) gridList.item(i));

		// Assign well labels by position (rows A.. top-to-bottom, columns 1..
		// left-to-right).
		definition.reindexWellsByPosition();

		return definition;
	}

	private static ArgolightPattern parsePattern(Element p, int i) {
		String id = attr(p, "id", "pattern-" + i);
		String pname = attr(p, "name", id);
		String icon = attr(p, "icon", "generic");
		int wellRow = (int) parseDouble(attr(p, "well-row", "0"), 0);
		int wellCol = (int) parseDouble(attr(p, "well-col", "0"), 0);
		double x = parseDouble(attr(p, "relative-x-um", "0"), 0);
		double y = parseDouble(attr(p, "relative-y-um", "0"), 0);
		double fovW = parseDouble(attr(p, "fov-width-um", "100"), 100);
		double fovH = parseDouble(attr(p, "fov-height-um", "100"), 100);
		int groupId = (int) parseDouble(attr(p, "group-id", "0"), 0);
		if (groupId < 0)
			groupId = 0;
		int maxGroup = org.youscope.common.microplate.WellWithGroup.WellGroup.values().length - 1;
		if (groupId > maxGroup)
			groupId = maxGroup;

		boolean zStack = parseBoolean(attr(p, "z-stack", "false"), false);
		ArgolightPattern.ZMode zMode = "range".equalsIgnoreCase(attr(p, "z-mode", "symmetric"))
				? ArgolightPattern.ZMode.RANGE
				: ArgolightPattern.ZMode.SYMMETRIC;
		double zStep = parseDouble(attr(p, "z-step-um", "1.0"), 1.0);
		int below = (int) parseDouble(attr(p, "z-slices-below", "3"), 3);
		int above = (int) parseDouble(attr(p, "z-slices-above", "3"), 3);
		double zStart = parseDouble(attr(p, "z-start-um", "0"), 0);
		double zEnd = parseDouble(attr(p, "z-end-um", "0"), 0);

		boolean scan = parseBoolean(attr(p, "scan", "false"), false);
		double scanW = parseDouble(attr(p, "scan-field-width-um", "0"), 0);
		double scanH = parseDouble(attr(p, "scan-field-height-um", "0"), 0);
		boolean overview = parseBoolean(attr(p, "overview", "false"), false);

		double iconScale = parseDouble(attr(p, "icon-scale", "1.0"), 1.0);
		double iconDetail = parseDouble(attr(p, "icon-detail", "0"), 0);
		double markerScale = parseDouble(attr(p, "marker-scale", "1.0"), 1.0);

		return new ArgolightPattern(id, pname, icon, wellRow, wellCol, x, y, fovW, fovH, groupId,
				zStack, zMode, zStep, below, above, zStart, zEnd, scan, scanW, scanH, overview, iconScale, iconDetail,
				markerScale);
	}

	/**
	 * Expands a <cross-grid> element into individual small-cross patterns
	 * arranged on a regular grid centred on (center-x-um, center-y-um). Grid nodes
	 * that coincide (within 1 um) with an already-defined pattern are skipped
	 * (so the central cross, target and rings are not duplicated). Well labels are
	 * auto-assigned on fresh rows after the existing ones.
	 */
	private static void expandCrossGrid(ArgolightPlateDefinition definition, Element grid) {
		int rows = (int) parseDouble(attr(grid, "rows", "5"), 5);
		int cols = (int) parseDouble(attr(grid, "cols", "5"), 5);
		double pitchX = parseDouble(attr(grid, "pitch-x-um", "500"), 500);
		double pitchY = parseDouble(attr(grid, "pitch-y-um", "500"), 500);
		double centerX = parseDouble(attr(grid, "center-x-um", "0"), 0);
		double centerY = parseDouble(attr(grid, "center-y-um", "0"), 0);
		double length = parseDouble(attr(grid, "length-um", "20"), 20);
		String icon = attr(grid, "icon", "cross");
		int groupId = (int) parseDouble(attr(grid, "group-id", "0"), 0);
		String idPrefix = attr(grid, "id-prefix", "grid-cross");
		boolean skipOccupied = parseBoolean(attr(grid, "skip-occupied", "true"), true);
		double markerScale = parseDouble(attr(grid, "marker-scale", "0.6"), 0.6);
		double iconScale = parseDouble(attr(grid, "icon-scale", "0.9"), 0.9);
		int labelCols = (int) parseDouble(attr(grid, "label-cols", "5"), 5);

		int startRow = 0;
		for (ArgolightPattern existing : definition.getPatterns())
			startRow = Math.max(startRow, existing.getWellRow() + 1);

		int added = 0;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				double x = centerX + (c - (cols - 1) / 2.0) * pitchX;
				double y = centerY + ((rows - 1) / 2.0 - r) * pitchY;
				if (skipOccupied && isOccupied(definition, x, y))
					continue;
				int wellRow = startRow + added / labelCols;
				int wellCol = added % labelCols;
				String id = idPrefix + "-r" + r + "-c" + c;
				String name = "Grid cross (" + formatUm(x) + ";" + formatUm(y) + " um)";
				definition.addPattern(new ArgolightPattern(id, name, icon, wellRow, wellCol, x, y,
						length, length, groupId, false, ArgolightPattern.ZMode.SYMMETRIC, 1.0, 0, 0, 0, 0,
						false, 0, 0, false, iconScale, 0, markerScale));
				added++;
			}
		}
	}

	private static boolean isOccupied(ArgolightPlateDefinition definition, double x, double y) {
		for (ArgolightPattern p : definition.getPatterns()) {
			if (Math.abs(p.getRelativeXUm() - x) < 1.0 && Math.abs(p.getRelativeYUm() - y) < 1.0)
				return true;
		}
		return false;
	}

	private static String formatUm(double v) {
		if (v == Math.rint(v))
			return Integer.toString((int) v);
		return Double.toString(v);
	}

	private static String attr(Element element, String name, String fallback) {
		String value = element.getAttribute(name);
		return (value == null || value.length() == 0) ? fallback : value;
	}

	private static Element getChildElement(Element parent, String tag) {
		NodeList list = parent.getElementsByTagName(tag);
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node instanceof Element && node.getParentNode() == parent)
				return (Element) node;
		}
		// fall back to first match regardless of depth.
		if (list.getLength() > 0 && list.item(0) instanceof Element)
			return (Element) list.item(0);
		return null;
	}

	private static String getChildText(Element parent, String tag) {
		Element child = getChildElement(parent, tag);
		return child == null ? null : child.getTextContent();
	}

	private static double parseDouble(String value, double fallback) {
		try {
			return Double.parseDouble(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	private static boolean parseBoolean(String value, boolean fallback) {
		if (value == null)
			return fallback;
		String v = value.trim().toLowerCase();
		if (v.equals("true") || v.equals("yes") || v.equals("1"))
			return true;
		if (v.equals("false") || v.equals("no") || v.equals("0"))
			return false;
		return fallback;
	}

	/**
	 * Convenience: names of all available definitions (default first).
	 * 
	 * @return array of definition names.
	 */
	public static String[] getAvailableDefinitionNames() {
		ArrayList<String> names = new ArrayList<>(getAvailableDefinitions().keySet());
		return names.toArray(new String[names.size()]);
	}
}
