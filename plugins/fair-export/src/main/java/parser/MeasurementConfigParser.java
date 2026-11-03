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
 * Parser for {@code configuration.csb}, the measurement protocol.
 * <p>
 * The file is XStream XML with readable aliases. Only a small part of it is
 * relevant to the FAIR
 * export, but that part is the only source for three things: the Z step size,
 * the exposure configured
 * per image save name, and the plate geometry.
 * </p>
 * <p>
 * The imaging jobs are walked recursively rather than at fixed depth, because
 * an imaging job may sit
 * directly in the job list or nested inside a Z stack job. That nesting is
 * exactly what
 * determines whether the resulting series has a Z dimension. An imaging job
 * inside a
 * {@code z-slides-job} inherits that job's focus positions; one outside it has
 * a single focal plane.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class MeasurementConfigParser {
	/** File name of the measurement configuration. */
	public static final String FILE_NAME = "configuration.csb";

	private String measurementName = null;
	private String description = null;
	private Integer numExecutions = null;
	private Long periodMs = null;
	private String microplateType = null;
	private boolean feedbackMeasurement = false;
	private final Map<String, ImagingJob> imagingJobs = new LinkedHashMap<String, ImagingJob>();
	private final List<ConfiguredPosition> configuredPositions = new ArrayList<ConfiguredPosition>();
	private final List<int[]> selectedWells = new ArrayList<int[]>();
	private final List<String> warnings = new ArrayList<String>();

	/**
	 * Lower-case markers whose presence in the raw configuration identifies an
	 * adaptive
	 * (cell-detection / feedback / online-analysis) measurement. Scanned as
	 * substrings of the raw XML
	 * because the job elements are contributed by other plugins and their exact
	 * element names /
	 * XStream aliases are not known here; matching the plugin package fragments and
	 * job aliases is
	 * robust to those names changing. Kept deliberately broad and a false positive
	 * only means an extra
	 * (harmless) useq plan is offered, never a wrong scan.
	 */
	private static final String[] FEEDBACK_MARKERS = new String[] {
			"celldetection", "cell-detection", "cell_detection",
			"feedback", "onlineanalysis", "online-analysis", "online_analysis",
			"adaptive", "analysis-job", "analysisjob", "microplate-adaptive"
	};

	/**
	 * An imaging job, i.e. one image save name and everything configured for it.
	 */
	public static class ImagingJob {
		/**
		 * Image save name; this is the key under which images appear in the image
		 * table.
		 */
		public String imageSaveName = null;
		public String channel = null;
		public String channelGroup = null;
		public String cameraDevice = null;
		public Double exposureMs = null;
		/**
		 * Relative focus positions in micrometre if this job sits inside a Z stack job,
		 * otherwise null.
		 * A null value means a single focal plane, not an unknown one.
		 */
		public List<Double> relativeFocusPositionsMicrons = null;
		/** Focus device of the enclosing Z stack job, or null. */
		public String focusDevice = null;

		/**
		 * Tile-grid parameters if this job sits inside a {@code plate-scanning-job} (a
		 * tile/grid scan),
		 * otherwise null. When set, the acquisition covers {@code numTilesX} ×
		 * {@code numTilesY} tiles
		 * stepped by {@code deltaXMicrons} / {@code deltaYMicrons} on the stage; the
		 * FAIR export splits
		 * these into one image per tile and records each tile's stage offset so the
		 * tiles can be stitched.
		 */
		public Integer numTilesX = null;
		public Integer numTilesY = null;
		public Double deltaXMicrons = null;
		public Double deltaYMicrons = null;

		/** True if this job is part of a tile/grid scan. */
		public boolean hasTileGrid() {
			return (numTilesX != null && numTilesX.intValue() > 1) || (numTilesY != null && numTilesY.intValue() > 1);
		}

		/**
		 * Returns the number of focal planes this job acquires.
		 * 
		 * @return Number of focal planes, at least 1.
		 */
		public int getFocalPlaneCount() {
			return relativeFocusPositionsMicrons == null ? 1 : Math.max(1, relativeFocusPositionsMicrons.size());
		}

		/**
		 * Returns the Z step size, i.e. the spacing of the configured focus positions.
		 * <p>
		 * Returns null unless the positions are evenly spaced: OME
		 * {@code PhysicalSizeZ} describes a
		 * uniform spacing, and reporting a single number for an irregular stack would
		 * be wrong.
		 * </p>
		 * 
		 * @return Z step in micrometre, or null.
		 */
		public Double getZStepMicrons() {
			if (relativeFocusPositionsMicrons == null || relativeFocusPositionsMicrons.size() < 2)
				return null;
			double first = relativeFocusPositionsMicrons.get(1).doubleValue()
					- relativeFocusPositionsMicrons.get(0).doubleValue();
			for (int i = 2; i < relativeFocusPositionsMicrons.size(); i++) {
				double step = relativeFocusPositionsMicrons.get(i).doubleValue()
						- relativeFocusPositionsMicrons.get(i - 1).doubleValue();
				if (Math.abs(step - first) > 1e-6)
					return null;
			}
			return Double.valueOf(Math.abs(first));
		}
	}

	/**
	 * A configured stage position for one well.
	 */
	public static class ConfiguredPosition {
		public double x;
		public double y;
		public double focus;
		/** Zero based well column. */
		public int wellX;
		/** Zero based well row. */
		public int wellY;
	}

	/**
	 * Parses a measurement configuration.
	 * 
	 * @param configurationFile The {@code configuration.csb} of the measurement.
	 * @throws IOException Thrown if the file cannot be read or is not valid XML.
	 */
	public void parse(File configurationFile) throws IOException {
		if (configurationFile == null || !configurationFile.isFile())
			throw new IOException("Measurement configuration file does not exist.");
		measurementName = null;
		description = null;
		numExecutions = null;
		periodMs = null;
		microplateType = null;
		feedbackMeasurement = false;
		imagingJobs.clear();
		configuredPositions.clear();
		selectedWells.clear();
		warnings.clear();

		Document document;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(configurationFile);
		} catch (SAXException e) {
			throw new IOException("Measurement configuration file is not valid XML.", e);
		} catch (Exception e) {
			throw new IOException("Could not parse measurement configuration file.", e);
		}

		Element root = document.getDocumentElement();
		measurementName = getChildText(root, "n");
		description = getChildText(root, "description");

		Element period = getFirstChild(root, "period");
		if (period != null) {
			numExecutions = parseIntObject(period.getAttribute("num-executions"));
			Long parsedPeriod = parseLongObject(period.getAttribute("period"));
			// A period of 0 means "as fast as possible": there is no nominal time
			// increment, and the
			// actual spacing has to come from the image table.
			periodMs = (parsedPeriod != null && parsedPeriod.longValue() > 0) ? parsedPeriod : null;
		}

		Element microplate = getFirstChild(root, "microplate-configuration");
		if (microplate != null)
			microplateType = emptyToNull(microplate.getAttribute("type"));

		// Walk the whole job tree; imaging jobs may be nested at any depth.
		Element jobs = getFirstChild(root, "jobs");
		if (jobs != null)
			collectImagingJobs(jobs, null, null, null);
		if (imagingJobs.isEmpty())
			warnings.add(
					"No imaging jobs found in the measurement configuration; channel and exposure metadata will be missing.");

		parseSelectedWells(root);
		parseConfiguredPositions(root);
		scanForFeedbackMarkers(configurationFile);
	}

	/**
	 * Sets {@link #feedbackMeasurement} by scanning the raw configuration text for
	 * any
	 * {@link #FEEDBACK_MARKERS}. Reading the file as text (rather than walking the
	 * DOM for specific
	 * element names) keeps the detection independent of which plugin contributed
	 * the adaptive job and
	 * of its XStream alias. A read failure leaves the flag false and a plain
	 * measurement is the safe
	 * default (no useq plan is forced).
	 */
	private void scanForFeedbackMarkers(File configurationFile) {
		try {
			byte[] bytes = java.nio.file.Files.readAllBytes(configurationFile.toPath());
			String text = new String(bytes, java.nio.charset.Charset.forName("UTF-8")).toLowerCase();
			for (String marker : FEEDBACK_MARKERS) {
				if (text.contains(marker)) {
					feedbackMeasurement = true;
					return;
				}
			}
		} catch (@SuppressWarnings("unused") Exception ignored) {
			// leave feedbackMeasurement false
		}
	}

	/**
	 * Tile-grid parameters read from a {@code plate-scanning-job}, inherited by the
	 * jobs nested in it.
	 */
	private static final class TileGrid {
		final Integer numTilesX;
		final Integer numTilesY;
		final Double deltaXMicrons;
		final Double deltaYMicrons;

		TileGrid(Integer numTilesX, Integer numTilesY, Double deltaXMicrons, Double deltaYMicrons) {
			this.numTilesX = numTilesX;
			this.numTilesY = numTilesY;
			this.deltaXMicrons = deltaXMicrons;
			this.deltaYMicrons = deltaYMicrons;
		}
	}

	/**
	 * Recursively collects imaging jobs, carrying the focus positions of any
	 * enclosing Z stack job and
	 * the tile grid of any enclosing plate-scanning (tile) job.
	 */
	private void collectImagingJobs(Element element, List<Double> inheritedFocusPositions, String inheritedFocusDevice,
			TileGrid inheritedTileGrid) {
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element child = (Element) node;
			String name = child.getNodeName();

			if ("z-slides-job".equals(name)) {
				List<Double> positions = new ArrayList<Double>();
				for (Element position : getChildren(child, "relative-focus-position-um")) {
					Double value = parseDoubleObject(position.getTextContent());
					if (value != null)
						positions.add(value);
				}
				String focusDevice = inheritedFocusDevice;
				Element focusConfiguration = getFirstChild(child, "focus-configuration");
				if (focusConfiguration != null) {
					String device = emptyToNull(focusConfiguration.getAttribute("focus-device"));
					if (device != null)
						focusDevice = device;
				}
				if (positions.isEmpty())
					warnings.add(
							"A Z stack job defines no focus positions; its images are treated as a single focal plane.");
				collectImagingJobs(child, positions.isEmpty() ? inheritedFocusPositions : positions, focusDevice,
						inheritedTileGrid);
				continue;
			}

			// A plate-scanning-job performs a lateral tile/grid scan; its num-tiles-x/y and
			// delta-x/y-um
			// describe the grid and stage step. Nested imaging jobs inherit it so each
			// acquired image knows
			// which tile it is and where on the stage that tile sits.
			if ("plate-scanning-job".equals(name)) {
				TileGrid grid = new TileGrid(
						parseIntObject(child.getAttribute("num-tiles-x")),
						parseIntObject(child.getAttribute("num-tiles-y")),
						parseDoubleObject(child.getAttribute("delta-x-um")),
						parseDoubleObject(child.getAttribute("delta-y-um")));
				collectImagingJobs(child, inheritedFocusPositions, inheritedFocusDevice, grid);
				continue;
			}

			// An acquisition is either an <imaging-job> element or any element flagged
			// type="imaging-job"
			// (e.g. a cell-detection job's <detection-image type="imaging-job">, which
			// acquires the
			// detection channel: e.g. BF in a typical feedback protocol). Recognising the
			// latter is what gives
			// that channel its channel-group and exposure; without it the channel would
			// look derived.
			if ("imaging-job".equals(name) || "imaging-job".equals(child.getAttribute("type"))) {
				ImagingJob job = new ImagingJob();
				job.imageSaveName = emptyToNull(child.getAttribute("image-save-name"));
				job.exposureMs = parseDoubleObject(child.getAttribute("exposure-ms"));
				job.relativeFocusPositionsMicrons = inheritedFocusPositions;
				job.focusDevice = inheritedFocusDevice;
				if (inheritedTileGrid != null) {
					job.numTilesX = inheritedTileGrid.numTilesX;
					job.numTilesY = inheritedTileGrid.numTilesY;
					job.deltaXMicrons = inheritedTileGrid.deltaXMicrons;
					job.deltaYMicrons = inheritedTileGrid.deltaYMicrons;
				}

				Element channelConfiguration = getFirstChild(child, "channel-configuration");
				if (channelConfiguration != null) {
					job.channel = emptyToNull(channelConfiguration.getAttribute("channel"));
					job.channelGroup = emptyToNull(channelConfiguration.getAttribute("channel-group"));
				}
				Element cameraConfiguration = getFirstChild(child, "camera-configuration");
				if (cameraConfiguration != null)
					job.cameraDevice = emptyToNull(cameraConfiguration.getAttribute("camera-device"));

				if (job.imageSaveName == null) {
					// Jobs which do not save images are irrelevant to the export.
					continue;
				}
				if (imagingJobs.containsKey(job.imageSaveName)) {
					warnings.add("Several imaging jobs save under the name \"" + job.imageSaveName
							+ "\". Their images cannot be told apart in the image table; using the first definition.");
					continue;
				}
				imagingJobs.put(job.imageSaveName, job);
				// An imaging job has no nested imaging jobs; no need to recurse further.
				continue;
			}

			collectImagingJobs(child, inheritedFocusPositions, inheritedFocusDevice, inheritedTileGrid);
		}
	}

	private void parseSelectedWells(Element root) {
		Element container = getFirstChild(root, "selected-wells");
		if (container == null)
			return;
		for (Element wellWithGroup : getChildren(container, "well-with-group")) {
			Element well = getFirstChild(wellWithGroup, "well");
			if (well == null)
				continue;
			Integer wellX = parseIntObject(well.getAttribute("well-x"));
			Integer wellY = parseIntObject(well.getAttribute("well-y"));
			if (wellX == null || wellY == null)
				continue;
			// These indices are already zero based and therefore match the OME convention
			// directly.
			selectedWells.add(new int[] { wellY.intValue(), wellX.intValue() });
		}
	}

	private void parseConfiguredPositions(Element root) {
		Element container = getFirstChild(root, "configured-positions");
		if (container == null)
			return;
		for (Element position : getChildren(container, "position")) {
			Double x = parseDoubleObject(position.getAttribute("x"));
			Double y = parseDoubleObject(position.getAttribute("y"));
			Integer wellX = parseIntObject(position.getAttribute("well-x"));
			Integer wellY = parseIntObject(position.getAttribute("well-y"));
			if (x == null || y == null || wellX == null || wellY == null)
				continue;
			ConfiguredPosition result = new ConfiguredPosition();
			result.x = x.doubleValue();
			result.y = y.doubleValue();
			Double focus = parseDoubleObject(position.getAttribute("focus"));
			result.focus = focus == null ? 0 : focus.doubleValue();
			result.wellX = wellX.intValue();
			result.wellY = wellY.intValue();
			configuredPositions.add(result);
		}
	}

	// Accessors

	public String getMeasurementName() {
		return measurementName;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Returns the configured number of executions, i.e. the intended number of time
	 * points. The actual
	 * number may be lower if the measurement was stopped early, so the image table
	 * wins over this value.
	 * 
	 * @return Configured number of executions, or null.
	 */
	public Integer getNumExecutions() {
		return numExecutions;
	}

	/**
	 * Returns the configured period between executions, or null if the measurement
	 * ran as fast as
	 * possible (in which case there is no nominal time increment to export).
	 * 
	 * @return Period in milliseconds, or null.
	 */
	public Long getPeriodMs() {
		return periodMs;
	}

	/**
	 * Returns the configuration class name of the microplate, e.g.
	 * {@code org.youscope.plugin.ansisbs.AnsiSBS96MicroplateConfiguration}.
	 * 
	 * @return Microplate configuration type, or null.
	 */
	public String getMicroplateType() {
		return microplateType;
	}

	/**
	 * @return true if the configuration contains cell-detection / feedback /
	 *         online-analysis jobs, i.e.
	 *         an adaptive acquisition for which a useq-schema plan is worth
	 *         exporting.
	 */
	public boolean isFeedbackMeasurement() {
		return feedbackMeasurement;
	}

	/**
	 * Returns the imaging jobs keyed by image save name.
	 * 
	 * @return Imaging jobs.
	 */
	public Map<String, ImagingJob> getImagingJobs() {
		return Collections.unmodifiableMap(imagingJobs);
	}

	/**
	 * Returns the selected wells as zero based {row, column} pairs.
	 * 
	 * @return Selected wells.
	 */
	public List<int[]> getSelectedWells() {
		return Collections.unmodifiableList(selectedWells);
	}

	/**
	 * Returns the configured stage position of a well, or null if none was
	 * configured.
	 * 
	 * @param row    Zero based well row.
	 * @param column Zero based well column.
	 * @return Configured position, or null.
	 */
	public ConfiguredPosition getConfiguredPosition(int row, int column) {
		for (ConfiguredPosition position : configuredPositions) {
			if (position.wellY == row && position.wellX == column)
				return position;
		}
		return null;
	}

	public List<String> getWarnings() {
		return new ArrayList<String>(warnings);
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

	private static Integer parseIntObject(String value) {
		try {
			return Integer.valueOf(value.trim());
		} catch (@SuppressWarnings("unused") Exception e) {
			return null;
		}
	}

	private static Long parseLongObject(String value) {
		try {
			return Long.valueOf(value.trim());
		} catch (@SuppressWarnings("unused") Exception e) {
			return null;
		}
	}

	private static Double parseDoubleObject(String value) {
		try {
			return Double.valueOf(value.trim());
		} catch (@SuppressWarnings("unused") Exception e) {
			return null;
		}
	}
}
