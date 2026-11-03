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
import java.awt.Dimension;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.youscope.clientinterfaces.MetadataCategoryProvider;
import org.youscope.common.saving.MeasurementFileLocations;
import org.youscope.plugin.fairexport.model.ExperimentMetadata;
import org.youscope.plugin.fairexport.parser.InformationXmlParser.ChannelDefinition;
import org.youscope.plugin.fairexport.parser.InformationXmlParser.ImageRecord;
import org.youscope.plugin.fairexport.parser.MeasurementConfigParser.ConfiguredPosition;
import org.youscope.plugin.fairexport.parser.MeasurementConfigParser.ImagingJob;

/**
 * Turns a saved measurement folder into the internal {@link ExperimentMetadata}
 * model.
 * <p>
 * One {@code Image} is produced per combination of well and image save name.
 * That grain is not
 * arbitrary: channels acquired inside a Z stack job and channels acquired
 * outside it have different
 * numbers of focal planes, and OME {@code Pixels} is a rectangular CZT block.
 * Merging them into one
 * image would force a sparse block for no gain, whereas the image save name is
 * already YouScope's own
 * grouping concept and yields dense blocks. The channels stay associated
 * through the plate and well
 * hierarchy.
 * </p>
 * <p>
 * Where a value cannot be determined (most importantly the pixel size, when the
 * configuration contains several matching presets) the field is left null and a
 * warning is recorded,
 * so that the exporter omits the corresponding OME element rather than writing
 * a plausible-looking
 * wrong number.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class ExperimentMetadataParser {
	/** Objective turret device name in the standard YouScope configuration. */
	/**
	 * Default file name of the microscope configuration inside a measurement
	 * folder.
	 */
	public static final String MICROSCOPE_CONFIG_FILE_NAME = "YSConfig_Microscope.cfg";

	// Resolved from the microscope configuration after it is parsed; Micro-Manager
	// does not fix a name
	// for the objective changer (demo "Objective", Nikon "Nosepiece", Zeiss/Leica
	// variants, ...).
	private String objectiveDevice = MicroscopeConfigParser.DEFAULT_OBJECTIVE_DEVICE;
	private static final String EXCITATION_DEVICE = "Excitation";
	private static final String EMISSION_DEVICE = "Emission";
	private static final String DICHROIC_DEVICE = "Dichroic";

	private final InformationXmlParser informationParser = new InformationXmlParser();
	private final MeasurementConfigParser configurationParser = new MeasurementConfigParser();
	private final MicroscopeConfigParser microscopeParser = new MicroscopeConfigParser();
	private final MetadataCategoryProvider categoryProvider;
	private final List<String> warnings = new ArrayList<String>();
	private File measurementFolder = null;

	/**
	 * Constructor.
	 * 
	 * @param categoryProvider Provider used to attach a category to every metadata
	 *                         property, or null.
	 *                         Categories become the namespaces of the exported map
	 *                         annotations, so passing null makes all user
	 *                         metadata land in the general namespace.
	 */
	public ExperimentMetadataParser(MetadataCategoryProvider categoryProvider) {
		this.categoryProvider = categoryProvider;
	}

	/**
	 * Parses a measurement folder.
	 * 
	 * @param measurementFolder Folder containing {@code information.xml}.
	 * @return The parsed measurement description.
	 * @throws IOException Thrown if the mandatory files are missing or unreadable.
	 */
	public ExperimentMetadata parse(File measurementFolder) throws IOException {
		if (measurementFolder == null || !measurementFolder.isDirectory())
			throw new IOException("Measurement folder does not exist.");
		return parse(measurementFolder,
				new File(measurementFolder, InformationXmlParser.FILE_NAME),
				new File(measurementFolder, MeasurementConfigParser.FILE_NAME),
				new File(measurementFolder, MICROSCOPE_CONFIG_FILE_NAME));
	}

	/**
	 * Parses a measurement using the exact file paths reported by YouScope.
	 * <p>
	 * Preferred over {@link #parse(File)} whenever the locations are available,
	 * i.e. for automatic
	 * export directly after a measurement: the paths come from the save settings
	 * that actually wrote
	 * the files, so a save settings plugin using different file names still exports
	 * correctly.
	 * </p>
	 * 
	 * @param locations File locations of the measurement, as returned by
	 *                  {@code Measurement.getSaver().getLastMeasurementFileLocations()}.
	 * @return The parsed measurement description.
	 * @throws IOException Thrown if the mandatory files are missing or unreadable.
	 */
	public ExperimentMetadata parse(MeasurementFileLocations locations) throws IOException {
		if (locations == null)
			throw new IOException("No measurement file locations provided.");
		String baseFolder = locations.getMeasurementBaseFolder();
		if (baseFolder == null)
			throw new IOException("The measurement does not have a base folder; it was probably not saved.");
		File measurementFolder = new File(baseFolder);

		File informationFile = locations.getXmlInformationPath() != null
				? new File(locations.getXmlInformationPath())
				: new File(measurementFolder, InformationXmlParser.FILE_NAME);
		File configurationFile = locations.getMeasurementConfigurationFilePath() != null
				? new File(locations.getMeasurementConfigurationFilePath())
				: new File(measurementFolder, MeasurementConfigParser.FILE_NAME);
		File microscopeFile = locations.getMicroscopeConfigurationFilePath() != null
				? new File(locations.getMicroscopeConfigurationFilePath())
				: new File(measurementFolder, MICROSCOPE_CONFIG_FILE_NAME);

		return parse(measurementFolder, informationFile, configurationFile, microscopeFile);
	}

	private ExperimentMetadata parse(File measurementFolder, File informationFile, File configurationFile,
			File microscopeFile) throws IOException {
		warnings.clear();
		this.measurementFolder = measurementFolder;

		if (!informationFile.isFile())
			throw new IOException("The measurement folder contains no " + InformationXmlParser.FILE_NAME
					+ ". Only measurements saved by YouScope 22 or later can be exported.");
		informationParser.parse(informationFile);

		if (configurationFile.isFile()) {
			configurationParser.parse(configurationFile);
			warnings.addAll(configurationParser.getWarnings());
		} else
			warnings.add("No " + MeasurementConfigParser.FILE_NAME
					+ " found; Z step size and exposure times will be missing.");

		boolean microscopeAvailable = microscopeFile.isFile();
		if (microscopeAvailable) {
			microscopeParser.parse(microscopeFile);
			// The objective changer has no fixed device name across microscopes; resolve it
			// from the
			// parsed configuration so state/label lookups and pixel-size presets key off
			// the right device.
			objectiveDevice = microscopeParser.resolveObjectiveDeviceName();
		} else
			warnings.add("No YSConfig_Microscope.cfg found; pixel size and objective cannot be determined.");

		ExperimentMetadata metadata = new ExperimentMetadata();
		metadata.name = informationParser.getMeasurementName() != null
				? informationParser.getMeasurementName()
				: configurationParser.getMeasurementName();
		metadata.description = informationParser.getDescription() != null
				? informationParser.getDescription()
				: configurationParser.getDescription();
		metadata.measurementFolder = measurementFolder.getName();
		// Only meaningful when configuration.csb was present; defaults to false (plain
		// scan) otherwise.
		metadata.feedbackMeasurement = configurationFile.isFile() && configurationParser.isFeedbackMeasurement();
		// Record the running YouScope version, e.g. "YouScope R2026 [build: 2.4.0]",
		// rather than a bare
		// "YouScope". Read reflectively-safe from YouScopeVersion (part of
		// youscope-api); if it is ever
		// unavailable the plain name is kept.
		metadata.softwareName = "YouScope";
		try {
			metadata.softwareVersion = org.youscope.common.YouScopeVersion.getFullVersion();
		} catch (@SuppressWarnings("unused") Throwable e) {
			// keep softwareVersion null; the exporter simply omits the version.
		}

		buildProperties(metadata);
		metadata.acquisitionDate = getMeasurementStart();
		if (microscopeAvailable)
			buildInstrument(metadata);
		buildObjective(metadata);
		buildSeries(metadata);
		buildPlate(metadata);

		// Collect microscope-config warnings last and de-duplicated: some are produced
		// during resolution
		// (pixel size, objective label) in the build steps above rather than at parse
		// time, and resolution
		// runs once per image series, so an identical note would otherwise be repeated
		// for every series.
		if (microscopeAvailable)
			for (String warning : microscopeParser.getWarnings())
				if (!warnings.contains(warning))
					warnings.add(warning);

		return metadata;
	}

	/**
	 * Returns the warnings collected while parsing. These are informational;
	 * parsing itself only fails
	 * when a mandatory file is missing.
	 * 
	 * @return Collected warnings.
	 */
	public List<String> getWarnings() {
		return new ArrayList<String>(warnings);
	}

	// Study level

	private void buildProperties(ExperimentMetadata metadata) {
		for (Map.Entry<String, String> entry : informationParser.getMetadataProperties().entrySet()) {
			String category = categoryProvider == null ? MetadataCategoryProvider.UNCATEGORIZED
					: categoryProvider.getMetadataCategory(entry.getKey());
			metadata.properties.add(new ExperimentMetadata.MetadataEntry(entry.getKey(), entry.getValue(), category));
		}
	}

	private Date getMeasurementStart() {
		long earliest = Long.MAX_VALUE;
		for (ImageRecord image : informationParser.getImages()) {
			if (image.absoluteTimeMs > 0 && image.absoluteTimeMs < earliest)
				earliest = image.absoluteTimeMs;
		}
		return earliest == Long.MAX_VALUE ? null : new Date(earliest);
	}

	private void buildInstrument(ExperimentMetadata metadata) {
		ExperimentMetadata.Instrument instrument = new ExperimentMetadata.Instrument();
		// The device adapter is the only hardware identification YouScope records. It
		// is not a
		// manufacturer and model in the QUAREP sense, but it is better than nothing and
		// is honest
		// about what it is.
		String[] adapter = microscopeParser.getDeviceAdapter("Camera");
		if (adapter != null)
			instrument.model = adapter[0] + " / " + adapter[1];
		instrument.name = informationParser.getDeviceProperty("Camera", "CameraName");
		metadata.instrument = instrument;

		ExperimentMetadata.Detector detector = new ExperimentMetadata.Detector();
		detector.name = informationParser.getDeviceProperty("Camera", "CameraName");
		detector.serialNumber = informationParser.getDeviceProperty("Camera", "CameraID");
		detector.gain = parseDouble(informationParser.getDeviceProperty("Camera", "Gain"));
		detector.offset = parseDouble(informationParser.getDeviceProperty("Camera", "Offset"));
		detector.type = property("Detector type", "Detector Type", "DetectorType");
		metadata.detector = detector;

	}

	/**
	 * Resolves the objective. Preference order: (1) the objective label from the
	 * microscope
	 * configuration file; (2) failing that, an "Objective" metadata property (a
	 * definition the user can
	 * add under configuration/metadata/imaging, e.g. a dropdown of typical
	 * objectives). If neither is
	 * present, a single clear warning is emitted (never crashes on a missing
	 * definition file).
	 */
	private void buildObjective(ExperimentMetadata metadata) {
		String objectiveLabel = resolveObjectiveLabel(null);
		String source = "configuration file";
		if (objectiveLabel == null) {
			// Fallback: the user-provided Objective metadata property (from an XML
			// definition under
			// configuration/metadata/imaging, or typed in the metadata panel). Absent file
			// = absent
			// property here, which is handled gracefully below.
			objectiveLabel = property("Objective", "Objective lens", "Objective Lens");
			source = "metadata property";
		}
		if (objectiveLabel == null) {
			warnings.add("No objective information available: none was found in the microscope "
					+ "configuration file, and no \"Objective\" metadata property was set. Add an "
					+ "\"Objective\" definition under configuration/metadata/imaging (a dropdown of typical "
					+ "objectives) or select the objective in the metadata panel, so pixel calibration and "
					+ "the OME Objective element can be filled.");
			return;
		}

		ExperimentMetadata.Objective objective = new ExperimentMetadata.Objective();
		objective.name = objectiveLabel;
		objective.model = objectiveLabel;
		objective.nominalMagnification = MicroscopeConfigParser.parseMagnification(objectiveLabel);
		objective.immersion = property("Immersion medium", "Immersion name", "Immersion Name");
		objective.lensNA = parseDouble(property("Numerical aperture", "Numerical Aperture", "NA"));
		metadata.objective = objective;
		if (objective.nominalMagnification == null)
			warnings.add("Could not read a magnification from the objective label \"" + objectiveLabel
					+ "\" (from the " + source + ").");
	}

	/**
	 * Resolves the objective label in effect, optionally taking a channel's device
	 * settings into account.
	 * <p>
	 * The state index recorded in the device dump is authoritative; the dumped
	 * {@code Label} may be the
	 * device adapter's own naming rather than the label assigned in the
	 * configuration file.
	 * </p>
	 * 
	 * @param channel Channel whose activation settings override the startup state,
	 *                or null.
	 * @return Objective label, or null if it cannot be determined.
	 */
	private String resolveObjectiveLabel(ChannelDefinition channel) {
		// A channel may explicitly select an objective, in which case it wins over the
		// startup state.
		if (channel != null) {
			String channelObjective = channel.settingsOn.get(objectiveDevice + ".Label");
			if (channelObjective != null && !channelObjective.isEmpty())
				return channelObjective;
		}
		Integer state = parseInteger(informationParser.getDeviceProperty(objectiveDevice, "State"));
		String dumpedLabel = informationParser.getDeviceProperty(objectiveDevice, "Label");
		return microscopeParser.resolveDeviceLabel(objectiveDevice, state, dumpedLabel);
	}

	/**
	 * Builds the device settings in effect for a channel: the state at startup,
	 * overridden by whatever
	 * the channel activates. The objective label is corrected to the configured
	 * label, because the
	 * pixel size presets are expressed in terms of configured labels.
	 */
	private Map<String, String> getEffectiveDeviceSettings(ChannelDefinition channel) {
		Map<String, String> settings = new LinkedHashMap<String, String>(informationParser.getDeviceState());
		String objectiveLabel = resolveObjectiveLabel(channel);
		if (objectiveLabel != null)
			settings.put(objectiveDevice + ".Label", objectiveLabel);
		if (channel != null)
			settings.putAll(channel.settingsOn);
		return settings;
	}

	// Image series

	private void buildSeries(ExperimentMetadata metadata) {
		// Group images by WELL and TILE. Each distinct image save name within a group
		// becomes a CHANNEL
		// of one image -- e.g. a well imaged with "Cit" and "BF" is one image with two
		// channels, not two
		// images. A tile/grid (plate-scanning) acquisition additionally splits each
		// well into one image
		// per tile, so the tiles stay separate images that a stitcher can reassemble;
		// without this split
		// the tiles' lateral index was mis-read as a Z index and made the image look
		// like a ragged Z stack.
		Map<String, List<ImageRecord>> groups = new LinkedHashMap<String, List<ImageRecord>>();
		for (ImageRecord image : informationParser.getImages()) {
			// Key by well (screening) or by position path (well-less), plus the tile
			// indices. Well-less
			// images with no tile collapse into a single image with channels, as before.
			String wellKey = image.well != null ? image.well : "";
			String key = wellKey + "\u0000" + image.getTileIndexX() + "\u0000" + image.getTileIndexY();
			List<ImageRecord> group = groups.get(key);
			if (group == null) {
				group = new ArrayList<ImageRecord>();
				groups.put(key, group);
			}
			group.add(image);
		}
		if (groups.isEmpty()) {
			warnings.add("The measurement contains no images.");
			return;
		}

		Date start = metadata.acquisitionDate;
		long startMs = start == null ? 0 : start.getTime();

		for (Map.Entry<String, List<ImageRecord>> entry : groups.entrySet()) {
			metadata.series.add(buildSeries(entry.getValue(), startMs));
		}
	}

	/**
	 * Returns the distinct image save names in a group, preserving encounter order.
	 * Each becomes a
	 * channel.
	 */
	private List<String> distinctChannelNames(List<ImageRecord> records) {
		List<String> names = new ArrayList<String>();
		for (ImageRecord record : records) {
			String name = record.imageSaveName;
			if (name != null && !names.contains(name))
				names.add(name);
		}
		if (names.isEmpty())
			names.add(null);
		return names;
	}

	/**
	 * Builds the HCS plate description when this is a microplate measurement. A
	 * measurement counts as a
	 * plate if the protocol declares a microplate type OR the images carry well
	 * coordinates. Rows and
	 * columns are taken as the bounding grid of the acquired wells (a sparse
	 * plate); each acquired well
	 * links to the one image built for it.
	 */
	private void buildPlate(ExperimentMetadata metadata) {
		String microplateType = configurationParser.getMicroplateType();
		boolean hasWells = false;
		for (ExperimentMetadata.ImageSeries image : metadata.series) {
			if (image.wellRow != null && image.wellColumn != null) {
				hasWells = true;
				break;
			}
		}
		// Flat (non-plate) measurement: leave metadata.plate null so the exporter
		// writes independent
		// images. This is the single-position / several-random-positions case.
		if (microplateType == null && !hasWells)
			return;
		if (!hasWells)
			return;

		ExperimentMetadata.Plate plate = new ExperimentMetadata.Plate();
		plate.name = plateDisplayName(microplateType, metadata);

		int maxRow = 0;
		int maxColumn = 0;
		for (ExperimentMetadata.ImageSeries image : metadata.series) {
			if (image.wellRow != null)
				maxRow = Math.max(maxRow, image.wellRow.intValue());
			if (image.wellColumn != null)
				maxColumn = Math.max(maxColumn, image.wellColumn.intValue());
		}
		// Physical grid: rows as letters A, B, C ...; columns as 1-based numbers. The
		// grid spans at
		// least the acquired extent. Only acquired wells are added to plate.wells
		// (sparse plate).
		for (int r = 0; r <= maxRow; r++)
			plate.rowNames.add(rowLetter(r));
		for (int c = 0; c <= maxColumn; c++)
			plate.columnNames.add(Integer.toString(c + 1));

		// Group the series of each well together as its fields (a tile scan gives
		// several fields per
		// well; a plain well one). Fields are ordered by tile (y then x) so field
		// 0,1,2,... follow the
		// grid. Series are kept in metadata.series as well, so the OME-XML enumerates
		// every field as an
		// image and the plate simply references them.
		Map<String, ExperimentMetadata.PlateWell> wellsByPath = new LinkedHashMap<String, ExperimentMetadata.PlateWell>();
		List<ExperimentMetadata.PlateWell> orderedWells = new ArrayList<ExperimentMetadata.PlateWell>();
		for (ExperimentMetadata.ImageSeries image : metadata.series) {
			if (image.wellRow == null || image.wellColumn == null)
				continue;
			String path = rowLetter(image.wellRow.intValue()) + "/" + Integer.toString(image.wellColumn.intValue() + 1);
			ExperimentMetadata.PlateWell well = wellsByPath.get(path);
			if (well == null) {
				well = new ExperimentMetadata.PlateWell();
				well.rowIndex = image.wellRow.intValue();
				well.columnIndex = image.wellColumn.intValue();
				well.path = path;
				wellsByPath.put(path, well);
				orderedWells.add(well);
			}
			well.fields.add(image);
		}
		for (ExperimentMetadata.PlateWell well : orderedWells) {
			sortFieldsByTile(well.fields);
			well.image = well.fields.isEmpty() ? null : well.fields.get(0);
			plate.wells.add(well);
		}
		metadata.plate = plate;
	}

	/**
	 * Orders a well's fields by tile (row-major: y then x); non-tiled fields keep
	 * their order.
	 */
	private static void sortFieldsByTile(List<ExperimentMetadata.ImageSeries> fields) {
		java.util.Collections.sort(fields, new java.util.Comparator<ExperimentMetadata.ImageSeries>() {
			@Override
			public int compare(ExperimentMetadata.ImageSeries a, ExperimentMetadata.ImageSeries b) {
				int ay = a.tileIndexY == null ? 0 : a.tileIndexY.intValue();
				int by = b.tileIndexY == null ? 0 : b.tileIndexY.intValue();
				if (ay != by)
					return Integer.compare(ay, by);
				int ax = a.tileIndexX == null ? 0 : a.tileIndexX.intValue();
				int bx = b.tileIndexX == null ? 0 : b.tileIndexX.intValue();
				return Integer.compare(ax, bx);
			}
		});
	}

	private String plateDisplayName(String microplateType, ExperimentMetadata metadata) {
		String title = emptyToNull(informationParser.getMetadataProperties().get("Plate ID"));
		if (title != null)
			return title;
		if (microplateType != null) {
			// Strip the package, keep the simple class name, e.g.
			// org.youscope...BDBioscienceMultiwellTC12MicroplateConfiguration ->
			// BDBioscienceMultiwellTC12
			int dot = microplateType.lastIndexOf('.');
			String simple = dot >= 0 ? microplateType.substring(dot + 1) : microplateType;
			return simple.replaceAll("MicroplateConfiguration$", "").replaceAll("Configuration$", "");
		}
		return metadata.name != null ? metadata.name : "plate";
	}

	/**
	 * Converts a zero based row index to a spreadsheet-style letter (0 -> A, 25 ->
	 * Z, 26 -> AA).
	 */
	private static String rowLetter(int rowIndex) {
		StringBuilder builder = new StringBuilder();
		int value = rowIndex;
		while (value >= 0) {
			builder.insert(0, (char) ('A' + (value % 26)));
			value = value / 26 - 1;
		}
		return builder.toString();
	}

	private ExperimentMetadata.ImageSeries buildSeries(List<ImageRecord> records, long startMs) {
		ImageRecord first = records.get(0);
		ExperimentMetadata.ImageSeries series = new ExperimentMetadata.ImageSeries();

		// Each distinct image save name in this well/position is a channel.
		List<String> channelNames = distinctChannelNames(records);
		String baseName = first.well != null ? first.well
				: (first.imageSaveName != null ? first.imageSaveName : "image");
		series.name = baseName;

		// Tile/grid scan: record which tile this series is, and name it so the tiles
		// stay distinguishable
		// (e.g. "B3 - tile x2 y1"). The stage offset of the tile is set on the planes
		// below.
		if (first.hasTile()) {
			series.tileIndexX = Integer.valueOf(first.getTileIndexX());
			series.tileIndexY = Integer.valueOf(first.getTileIndexY());
			ImagingJob gridJob = firstJobWithTileGrid(records);
			if (gridJob != null) {
				series.tileCountX = gridJob.numTilesX;
				series.tileCountY = gridJob.numTilesY;
			}
			series.name = baseName + " - tile x" + (series.tileIndexX.intValue() + 1)
					+ " y" + (series.tileIndexY.intValue() + 1);
		}

		int[] wellIndices = first.getWellIndices();
		if (wellIndices != null) {
			series.wellRow = Integer.valueOf(wellIndices[0]);
			series.wellColumn = Integer.valueOf(wellIndices[1]);
		}
		series.plateName = emptyToNull(informationParser.getMetadataProperties().get("Plate ID"));

		// SizeC = number of channels. SizeZ = the MAX z-plane count over channels
		// (ragged Z: a stack
		// channel and a single-plane channel coexist; the array is sized to the
		// deepest, and planes a
		// channel never acquired stay at the fill value). SizeT = max time index over
		// all records.
		int maxT = 0;
		int maxZ = 0;
		for (ImageRecord record : records) {
			maxT = Math.max(maxT, record.getTimeIndex());
			maxZ = Math.max(maxZ, record.getFocalPlaneIndex());
		}
		series.sizeC = Integer.valueOf(channelNames.size());
		series.sizeZ = Integer.valueOf(maxZ + 1);
		series.sizeT = Integer.valueOf(maxT + 1);
		if (first.originalBitDepth != null)
			series.pixelType = first.originalBitDepth.intValue() <= 8 ? "uint8" : "uint16";

		Dimension imageSize = readImageSize(first.path);
		if (imageSize != null) {
			series.sizeX = Integer.valueOf(imageSize.width);
			series.sizeY = Integer.valueOf(imageSize.height);
		} else if (first.path != null)
			warnings.add("Could not read the pixel dimensions of image \"" + first.path
					+ "\"; SizeX and SizeY are unknown.");

		// Build one Channel per name, in order, and remember the mapping name ->
		// channel index.
		Map<String, Integer> channelIndex = new LinkedHashMap<String, Integer>();
		for (int i = 0; i < channelNames.size(); i++) {
			String channelName = channelNames.get(i);
			channelIndex.put(channelName, Integer.valueOf(i));
			ImageRecord representative = firstRecordForChannel(records, channelName);
			ImagingJob job = channelName == null ? null : configurationParser.getImagingJobs().get(channelName);
			ChannelDefinition channelDefinition = null;
			if (representative != null && representative.channelGroup != null && representative.channel != null)
				channelDefinition = informationParser.getChannelDefinition(representative.channelGroup,
						representative.channel);
			buildChannel(series, representative != null ? representative : first, job, channelDefinition);
			if (i == 0)
				buildPhysicalSizes(series, job, channelDefinition);
		}

		// Planes: assign each record to its channel index; z and t from the record.
		// Only acquired
		// planes are added, so a channel with fewer z-planes simply contributes fewer
		// planes and the
		// rest of its (c,z) slots remain fill value 0 in the dense writers.
		buildPlanesMultiChannel(series, records, channelIndex, startMs);

		Double temperature = parseDouble(informationParser.getMetadataProperties().get("Temperature"));
		if (temperature != null)
			series.temperatureCelsius = temperature;

		return series;
	}

	private ImageRecord firstRecordForChannel(List<ImageRecord> records, String channelName) {
		for (ImageRecord record : records) {
			if (channelName == null ? record.imageSaveName == null : channelName.equals(record.imageSaveName))
				return record;
		}
		return null;
	}

	/**
	 * Builds planes for a multi-channel series, placing each record at its
	 * (channel, z, t). No fill
	 * planes are fabricated: a missing (c,z) simply has no Plane, and the dense
	 * writers leave it at the
	 * fill value.
	 */
	private void buildPlanesMultiChannel(ExperimentMetadata.ImageSeries series, List<ImageRecord> records,
			Map<String, Integer> channelIndex, long startMs) {
		// Base stage position of this field: the configured well position (if any)
		// plus, for a tile scan,
		// the tile's offset from the grid deltas. Recorded per plane as OME PositionX/Y
		// so the tiles of a
		// grid can be stitched afterwards (10% overlap etc. follow from the deltas vs.
		// the image size).
		ConfiguredPosition well = null;
		if (series.wellRow != null && series.wellColumn != null)
			well = configurationParser.getConfiguredPosition(series.wellRow.intValue(), series.wellColumn.intValue());
		Double baseX = well != null ? Double.valueOf(well.x) : null;
		Double baseY = well != null ? Double.valueOf(well.y) : null;

		ImagingJob gridJob = firstJobWithTileGrid(records);
		if (series.tileIndexX != null && gridJob != null && gridJob.deltaXMicrons != null)
			baseX = Double.valueOf((baseX == null ? 0.0 : baseX.doubleValue())
					+ series.tileIndexX.intValue() * gridJob.deltaXMicrons.doubleValue());
		if (series.tileIndexY != null && gridJob != null && gridJob.deltaYMicrons != null)
			baseY = Double.valueOf((baseY == null ? 0.0 : baseY.doubleValue())
					+ series.tileIndexY.intValue() * gridJob.deltaYMicrons.doubleValue());

		for (ImageRecord record : records) {
			Integer c = channelIndex.get(record.imageSaveName);
			if (c == null)
				c = Integer.valueOf(0);
			ExperimentMetadata.Plane plane = new ExperimentMetadata.Plane();
			plane.channelIndex = c.intValue();
			plane.focalPlaneIndex = record.getFocalPlaneIndex();
			plane.timePointIndex = record.getTimeIndex();
			plane.fileName = record.path;
			if (record.absoluteTimeMs > 0 && startMs > 0)
				plane.deltaTSeconds = Double.valueOf((record.absoluteTimeMs - startMs) / 1000.0);
			ImagingJob job = record.imageSaveName == null ? null
					: configurationParser.getImagingJobs().get(record.imageSaveName);
			if (job != null)
				plane.exposureMs = job.exposureMs;
			if (baseX != null)
				plane.positionXMicrons = baseX;
			if (baseY != null)
				plane.positionYMicrons = baseY;
			if (job != null && job.relativeFocusPositionsMicrons != null
					&& plane.focalPlaneIndex >= 0 && plane.focalPlaneIndex < job.relativeFocusPositionsMicrons.size())
				plane.positionZMicrons = job.relativeFocusPositionsMicrons.get(plane.focalPlaneIndex);
			series.planes.add(plane);
		}
	}

	/**
	 * Returns the first imaging job of these records that carries a tile grid, or
	 * null.
	 */
	private ImagingJob firstJobWithTileGrid(List<ImageRecord> records) {
		for (ImageRecord record : records) {
			if (record.imageSaveName == null)
				continue;
			ImagingJob job = configurationParser.getImagingJobs().get(record.imageSaveName);
			if (job != null && job.hasTileGrid())
				return job;
		}
		return null;
	}

	private void buildChannel(ExperimentMetadata.ImageSeries series, ImageRecord first, ImagingJob job,
			ChannelDefinition definition) {
		ExperimentMetadata.Channel channel = new ExperimentMetadata.Channel();
		channel.name = first.channel != null ? first.channel : first.imageSaveName;
		channel.channelGroup = first.channelGroup;
		channel.samplesPerPixel = Integer.valueOf(1);
		if (job != null)
			channel.exposureMs = job.exposureMs;
		channel.fluor = emptyToNull(informationParser.getMetadataProperties().get("Fluorophore"));
		channel.acquisitionMode = emptyToNull(informationParser.getMetadataProperties().get("Image modality"));

		if (definition != null) {
			// Wavelengths are inferred from the filter labels, e.g. "Chroma-HQ620" -> 620
			// nm. This is a
			// heuristic on a free-text label, so the raw labels are additionally exported
			// as metadata
			// properties: if the heuristic misreads a label, nothing is lost.
			channel.excitationWavelengthNm = parseWavelength(definition.settingsOn.get(EXCITATION_DEVICE + ".Label"));
			channel.emissionWavelengthNm = parseWavelength(definition.settingsOn.get(EMISSION_DEVICE + ".Label"));
		}
		series.channels.add(channel);
	}

	private void buildPhysicalSizes(ExperimentMetadata.ImageSeries series, ImagingJob job,
			ChannelDefinition definition) {
		Double pixelSize = microscopeParser.resolvePixelSizeMicrons(getEffectiveDeviceSettings(definition));
		if (pixelSize != null) {
			series.physicalSizeXMicrons = pixelSize;
			series.physicalSizeYMicrons = pixelSize;
		}
		// microscopeParser already recorded a warning explaining why it could not
		// resolve.

		if (job != null) {
			Double zStep = job.getZStepMicrons();
			if (zStep != null)
				series.physicalSizeZMicrons = zStep;
			else if (series.sizeZ != null && series.sizeZ.intValue() > 1)
				warnings.add("Image series \"" + series.name
						+ "\" is a Z stack, but its focus positions are not evenly spaced; PhysicalSizeZ is omitted.");
		}

		Long period = configurationParser.getPeriodMs();
		if (period != null)
			series.timeIncrementSeconds = Double.valueOf(period.longValue() / 1000.0);
	}

	private void buildPlanes(ExperimentMetadata.ImageSeries series, List<ImageRecord> records, ImagingJob job,
			long startMs) {
		ConfiguredPosition position = null;
		if (series.wellRow != null && series.wellColumn != null)
			position = configurationParser.getConfiguredPosition(series.wellRow.intValue(),
					series.wellColumn.intValue());

		for (ImageRecord record : records) {
			ExperimentMetadata.Plane plane = new ExperimentMetadata.Plane();
			plane.channelIndex = 0;
			plane.focalPlaneIndex = record.getFocalPlaneIndex();
			plane.timePointIndex = record.getTimeIndex();
			plane.fileName = record.path;
			if (record.absoluteTimeMs > 0 && startMs > 0)
				plane.deltaTSeconds = Double.valueOf((record.absoluteTimeMs - startMs) / 1000.0);
			if (job != null)
				plane.exposureMs = job.exposureMs;
			if (position != null) {
				plane.positionXMicrons = Double.valueOf(position.x);
				plane.positionYMicrons = Double.valueOf(position.y);
			}
			if (job != null && job.relativeFocusPositionsMicrons != null
					&& plane.focalPlaneIndex < job.relativeFocusPositionsMicrons.size()) {
				// Focus positions are relative to the focus of the well; without an absolute
				// reference
				// only the relative value is meaningful, which is still what a Z stack needs.
				plane.positionZMicrons = job.relativeFocusPositionsMicrons.get(plane.focalPlaneIndex);
			}
			series.planes.add(plane);
		}
	}

	// Helpers

	/**
	 * Extracts a wavelength in nanometre from a filter label such as "Chroma-HQ620"
	 * or "Chroma-D360".
	 * Only three digit runs in the plausible optical range are accepted, so that
	 * labels containing
	 * other numbers do not produce nonsense.
	 */
	static Double parseWavelength(String filterLabel) {
		if (filterLabel == null)
			return null;
		int index = 0;
		while (index < filterLabel.length()) {
			if (!Character.isDigit(filterLabel.charAt(index))) {
				index++;
				continue;
			}
			int start = index;
			while (index < filterLabel.length() && Character.isDigit(filterLabel.charAt(index)))
				index++;
			if (index - start == 3) {
				int value = Integer.parseInt(filterLabel.substring(start, index));
				if (value >= 300 && value <= 900)
					return Double.valueOf(value);
			}
		}
		return null;
	}

	private static String emptyToNull(String value) {
		return (value == null || value.trim().isEmpty()) ? null : value.trim();
	}

	/**
	 * Case-insensitive lookup of a user metadata property, tolerant of
	 * capitalisation differences such
	 * as "Detector type" vs "Detector Type". Returns the first non-empty match.
	 * 
	 * @param names Candidate property names, tried in order.
	 * @return The value, or null.
	 */
	private String property(String... names) {
		Map<String, String> properties = informationParser.getMetadataProperties();
		for (String name : names) {
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
					String value = emptyToNull(entry.getValue());
					if (value != null)
						return value;
				}
			}
		}
		return null;
	}

	/**
	 * Reads the pixel dimensions of an image from its header, without decoding the
	 * pixel data.
	 * 
	 * @param relativePath Path of the image relative to the measurement folder.
	 * @return The image size, or null if it cannot be determined.
	 */
	private Dimension readImageSize(String relativePath) {
		if (relativePath == null || measurementFolder == null)
			return null;
		File imageFile = new File(measurementFolder, relativePath);
		if (!imageFile.isFile())
			return null;
		ImageInputStream stream = null;
		ImageReader reader = null;
		try {
			stream = ImageIO.createImageInputStream(imageFile);
			if (stream == null)
				return null;
			Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
			if (!readers.hasNext())
				return null;
			reader = readers.next();
			reader.setInput(stream, true, true);
			return new Dimension(reader.getWidth(0), reader.getHeight(0));
		} catch (@SuppressWarnings("unused") Exception e) {
			return null;
		} finally {
			if (reader != null)
				reader.dispose();
			if (stream != null) {
				try {
					stream.close();
				} catch (@SuppressWarnings("unused") Exception e) {
					/* ignore */ }
			}
		}
	}

	private static Integer parseInteger(String value) {
		if (value == null)
			return null;
		try {
			return Integer.valueOf(value.trim());
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Parses a leading number from a free text metadata value such as "30C" or
	 * "10pg/ml".
	 */
	private static Double parseDouble(String value) {
		if (value == null)
			return null;
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			if (Character.isDigit(character) || character == '.' || character == '-' || character == '+')
				builder.append(character);
			else if (character == ',' && builder.indexOf(".") < 0)
				builder.append('.');
			else if (builder.length() > 0)
				break;
		}
		try {
			return builder.length() == 0 ? null : Double.valueOf(builder.toString());
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return null;
		}
	}
}
