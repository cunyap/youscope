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
package org.youscope.plugin.fairexport.ome;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.youscope.plugin.fairexport.model.ExperimentMetadata;
import org.youscope.plugin.fairexport.ome.MetadataCrosswalk.Mapping;
import org.youscope.plugin.fairexport.ome.MetadataCrosswalk.Slot;

import loci.common.services.ServiceFactory;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.quantity.Pressure;
import ome.units.quantity.Temperature;
import ome.units.quantity.Time;
import ome.units.unit.Unit;
import ome.xml.model.MapPair;
import ome.xml.model.enums.AcquisitionMode;
import ome.xml.model.enums.ContrastMethod;
import ome.xml.model.enums.Correction;
import ome.xml.model.enums.DetectorType;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.ExperimentType;
import ome.xml.model.enums.IlluminationType;
import ome.xml.model.enums.Immersion;
import ome.xml.model.enums.PixelType;
import ome.xml.model.enums.handlers.AcquisitionModeEnumHandler;
import ome.xml.model.enums.handlers.ContrastMethodEnumHandler;
import ome.xml.model.enums.handlers.CorrectionEnumHandler;
import ome.xml.model.enums.handlers.DetectorTypeEnumHandler;
import ome.xml.model.enums.handlers.IlluminationTypeEnumHandler;
import ome.xml.model.enums.handlers.ImmersionEnumHandler;
import ome.xml.model.enums.handlers.PixelTypeEnumHandler;
import ome.xml.model.primitives.Color;
import ome.xml.model.primitives.NonNegativeInteger;
import ome.xml.model.primitives.PercentFraction;
import ome.xml.model.primitives.PositiveInteger;
import ome.xml.model.primitives.Timestamp;

/**
 * Converts an {@link ExperimentMetadata} into an OME metadata store.
 * <p>
 * The resulting {@link IMetadata} is the single source of truth for all
 * exporters: the OME-TIFF writer embeds its serialization into the TIFF header,
 * the OME-Zarr writer
 * derives the multiscale
 * metadata from it, and the RO-Crate builder reads the same object.
 * Fields which are unknown in the input model are simply not written, so that a
 * reader can distinguish
 * "not recorded" from "recorded as zero".
 * </p>
 * <p>
 * Properties which have a native OME representation are written into the
 * corresponding element (see
 * {@link MetadataCrosswalk}); everything else becomes a map annotation, one per
 * metadata category,
 * attached to every image of the measurement.
 * </p>
 * <p>
 * Written against the Bio-Formats 6.x / 7.x API. On Bio-Formats 5.1.x the unit
 * constants are named
 * differently ({@code MICROM}, {@code MS}, {@code S}, {@code NM},
 * {@code DEGREEC}, {@code MBAR});
 * they are collected at the top of this class so that only one block has to be
 * changed.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class OmeMetadataBuilder {
	// Unit constants, collected to isolate the Bio-Formats version dependency.
	private static final Unit<ome.units.quantity.Length> UNIT_MICROMETER = UNITS.MICROMETER;
	private static final Unit<ome.units.quantity.Length> UNIT_NANOMETER = UNITS.NANOMETER;
	private static final Unit<ome.units.quantity.Time> UNIT_SECOND = UNITS.SECOND;
	private static final Unit<ome.units.quantity.Time> UNIT_MILLISECOND = UNITS.MILLISECOND;
	private static final Unit<ome.units.quantity.Temperature> UNIT_CELSIUS = UNITS.CELSIUS;
	private static final Unit<ome.units.quantity.Pressure> UNIT_MILLIBAR = UNITS.MILLIBAR;

	private final MetadataCrosswalk crosswalk;
	private final List<String> warnings = new ArrayList<String>();
	private int annotationIndex = 0;

	/**
	 * Constructor using the default crosswalk.
	 */
	public OmeMetadataBuilder() {
		this(new MetadataCrosswalk());
	}

	/**
	 * Constructor
	 * 
	 * @param crosswalk Crosswalk used to decide where each metadata property is
	 *                  written to.
	 */
	public OmeMetadataBuilder(MetadataCrosswalk crosswalk) {
		this.crosswalk = crosswalk == null ? new MetadataCrosswalk() : crosswalk;
	}

	/**
	 * Returns the warnings collected during the last call to
	 * {@link #build(ExperimentMetadata)}, e.g.
	 * about missing recommended metadata or values which could not be mapped to an
	 * OME enumeration.
	 * The warnings are informational: the export itself always succeeds.
	 * 
	 * @return Collected warnings.
	 */
	public List<String> getWarnings() {
		return new ArrayList<String>(warnings);
	}

	/**
	 * Builds an OME metadata store from the given measurement description.
	 * 
	 * @param metadata Description of the measurement.
	 * @return OME metadata store, ready to be handed to a Bio-Formats writer.
	 * @throws Exception Thrown if the OME-XML service is not available.
	 */
	public IMetadata build(ExperimentMetadata metadata) throws Exception {
		if (metadata == null)
			throw new IllegalArgumentException("Metadata is null.");
		warnings.clear();
		annotationIndex = 0;

		ServiceFactory factory = new ServiceFactory();
		OMEXMLService service = factory.getInstance(OMEXMLService.class);
		IMetadata store = service.createOMEXMLMetadata();

		buildExperimenter(store, metadata);
		buildExperiment(store, metadata);
		buildInstrument(store, metadata);

		for (int seriesIndex = 0; seriesIndex < metadata.series.size(); seriesIndex++) {
			buildImage(store, metadata, metadata.series.get(seriesIndex), seriesIndex);
		}
		buildPlate(store, metadata);
		buildAnnotations(store, metadata);

		return store;
	}

	/**
	 * Serializes an OME metadata store to OME-XML.
	 * 
	 * @param store Store to serialize.
	 * @return OME-XML representation.
	 * @throws Exception Thrown if the OME-XML service is not available.
	 */
	public static String toXML(IMetadata store) throws Exception {
		ServiceFactory factory = new ServiceFactory();
		OMEXMLService service = factory.getInstance(OMEXMLService.class);
		return service.getOMEXML(store);
	}

	// Study level
	private static final String EXPERIMENTER_ID = "Experimenter:0";
	private static final String EXPERIMENT_ID = "Experiment:0";
	private static final String INSTRUMENT_ID = "Instrument:0";
	private static final String OBJECTIVE_ID = "Objective:0:0";
	private static final String DETECTOR_ID = "Detector:0:0";

	private void buildExperimenter(IMetadata store, ExperimentMetadata metadata) {
		String name = getMappedValue(metadata, Slot.EXPERIMENTER_NAME);
		String email = getMappedValue(metadata, Slot.EXPERIMENTER_EMAIL);
		String institution = getMappedValue(metadata, Slot.EXPERIMENTER_INSTITUTION);
		if (name == null && email == null && institution == null) {
			warnings.add(
					"No operator recorded. Add an \"Operator\" metadata property; attribution is required by REMBI and by most repositories.");
			return;
		}
		store.setExperimenterID(EXPERIMENTER_ID, 0);
		if (name != null)
			store.setExperimenterUserName(name, 0);
		if (email != null)
			store.setExperimenterEmail(email, 0);
		if (institution != null)
			store.setExperimenterInstitution(institution, 0);
	}

	private void buildExperiment(IMetadata store, ExperimentMetadata metadata) {
		String experimentDescription = getMappedValue(metadata, Slot.EXPERIMENT_DESCRIPTION);
		if (experimentDescription == null && (metadata.description == null || metadata.description.isEmpty()))
			return;
		store.setExperimentID(EXPERIMENT_ID, 0);
		store.setExperimentType(ExperimentType.OTHER, 0);
		String description = experimentDescription != null ? experimentDescription : metadata.description;
		store.setExperimentDescription(description, 0);
	}

	private void buildInstrument(IMetadata store, ExperimentMetadata metadata) {
		store.setInstrumentID(INSTRUMENT_ID, 0);

		ExperimentMetadata.Instrument instrument = metadata.instrument;
		if (instrument != null) {
			if (instrument.manufacturer != null)
				store.setMicroscopeManufacturer(instrument.manufacturer, 0);
			if (instrument.model != null)
				store.setMicroscopeModel(instrument.model, 0);
			if (instrument.serialNumber != null)
				store.setMicroscopeSerialNumber(instrument.serialNumber, 0);
		} else
			warnings.add("No microscope information available. Check that YSConfig_Microscope.cfg was parsed.");

		ExperimentMetadata.Objective objective = metadata.objective;
		if (objective != null) {
			store.setObjectiveID(OBJECTIVE_ID, 0, 0);
			if (objective.manufacturer != null)
				store.setObjectiveManufacturer(objective.manufacturer, 0, 0);
			if (objective.model != null)
				store.setObjectiveModel(objective.model, 0, 0);
			if (objective.serialNumber != null)
				store.setObjectiveSerialNumber(objective.serialNumber, 0, 0);
			if (objective.nominalMagnification != null)
				store.setObjectiveNominalMagnification(objective.nominalMagnification, 0, 0);
			if (objective.calibratedMagnification != null)
				store.setObjectiveCalibratedMagnification(objective.calibratedMagnification, 0, 0);
			if (objective.lensNA != null)
				store.setObjectiveLensNA(objective.lensNA, 0, 0);
			if (objective.workingDistanceMicrons != null)
				store.setObjectiveWorkingDistance(new Length(objective.workingDistanceMicrons, UNIT_MICROMETER), 0, 0);

			String immersion = objective.immersion != null ? objective.immersion
					: getMappedValue(metadata, Slot.OBJECTIVE_IMMERSION);
			if (immersion != null)
				store.setObjectiveImmersion(toImmersion(immersion), 0, 0);
			String correction = objective.correction != null ? objective.correction
					: getMappedValue(metadata, Slot.OBJECTIVE_CORRECTION);
			if (correction != null)
				store.setObjectiveCorrection(toCorrection(correction), 0, 0);
		} else
			warnings.add(
					"No objective information available. Daybook and most analysis tools need the objective for pixel calibration.");

		ExperimentMetadata.Detector detector = metadata.detector;
		if (detector != null) {
			store.setDetectorID(DETECTOR_ID, 0, 0);
			if (detector.manufacturer != null)
				store.setDetectorManufacturer(detector.manufacturer, 0, 0);
			if (detector.model != null)
				store.setDetectorModel(detector.model, 0, 0);
			if (detector.serialNumber != null)
				store.setDetectorSerialNumber(detector.serialNumber, 0, 0);
			if (detector.type != null)
				store.setDetectorType(toDetectorType(detector.type), 0, 0);
			if (detector.gain != null)
				store.setDetectorGain(detector.gain, 0, 0);
			if (detector.offset != null)
				store.setDetectorOffset(detector.offset, 0, 0);
		}
	}

	// Images
	private void buildImage(IMetadata store, ExperimentMetadata metadata, ExperimentMetadata.ImageSeries series,
			int imageIndex) {
		store.setImageID("Image:" + imageIndex, imageIndex);
		store.setImageName(series.name != null ? series.name : metadata.name, imageIndex);

		String imageDescription = getMappedValue(metadata, Slot.IMAGE_DESCRIPTION);
		if (series.description != null)
			store.setImageDescription(series.description, imageIndex);
		else if (imageDescription != null)
			store.setImageDescription(imageDescription, imageIndex);
		else if (metadata.description != null && !metadata.description.isEmpty())
			store.setImageDescription(metadata.description, imageIndex);

		if (metadata.acquisitionDate != null)
			store.setImageAcquisitionDate(new Timestamp(formatIso8601(metadata.acquisitionDate)), imageIndex);
		else
			warnings.add("No acquisition date available therefore the OME AcquisitionDate element is omitted.");

		store.setImageInstrumentRef(INSTRUMENT_ID, imageIndex);
		if (store.getExperimenterCount() > 0)
			store.setImageExperimenterRef(EXPERIMENTER_ID, imageIndex);
		if (store.getExperimentCount() > 0)
			store.setImageExperimentRef(EXPERIMENT_ID, imageIndex);
		if (metadata.objective != null)
			store.setObjectiveSettingsID(OBJECTIVE_ID, imageIndex);

		buildPixels(store, series, imageIndex);
		buildEnvironment(store, metadata, series, imageIndex);
		buildChannels(store, metadata, series, imageIndex);
		buildPlanes(store, series, imageIndex);
	}

	private void buildPixels(IMetadata store, ExperimentMetadata.ImageSeries series, int imageIndex) {
		store.setPixelsID("Pixels:" + imageIndex, imageIndex);
		// The exporter always assembles planes in XYCZT order, independent of the
		// acquisition order.
		store.setPixelsDimensionOrder(DimensionOrder.XYCZT, imageIndex);
		store.setPixelsBigEndian(Boolean.valueOf(series.bigEndian), imageIndex);
		store.setPixelsInterleaved(Boolean.FALSE, imageIndex);

		store.setPixelsType(toPixelType(series.pixelType), imageIndex);
		setSize(store, series.sizeX, imageIndex, 'X');
		setSize(store, series.sizeY, imageIndex, 'Y');
		setSize(store, series.sizeZ, imageIndex, 'Z');
		setSize(store, series.sizeC, imageIndex, 'C');
		setSize(store, series.sizeT, imageIndex, 'T');

		if (series.physicalSizeXMicrons != null && series.physicalSizeXMicrons.doubleValue() > 0)
			store.setPixelsPhysicalSizeX(new Length(series.physicalSizeXMicrons, UNIT_MICROMETER), imageIndex);
		else
			warnings.add("No lateral pixel size for image " + imageIndex
					+ ". Argolight Daybook cannot perform pixel calibration without PhysicalSizeX.");
		if (series.physicalSizeYMicrons != null && series.physicalSizeYMicrons.doubleValue() > 0)
			store.setPixelsPhysicalSizeY(new Length(series.physicalSizeYMicrons, UNIT_MICROMETER), imageIndex);
		if (series.physicalSizeZMicrons != null && series.physicalSizeZMicrons.doubleValue() > 0)
			store.setPixelsPhysicalSizeZ(new Length(series.physicalSizeZMicrons, UNIT_MICROMETER), imageIndex);
		else if (series.sizeZ != null && series.sizeZ.intValue() > 1)
			warnings.add("Image " + imageIndex
					+ " is a Z stack but has no Z step size. Daybook cannot interpret the axial calibration without PhysicalSizeZ.");

		if (series.timeIncrementSeconds != null)
			store.setPixelsTimeIncrement(new Time(series.timeIncrementSeconds, UNIT_SECOND), imageIndex);

		if (series.isSparse()) {
			// With unified channels this is normal and expected: a single-plane channel
			// (e.g. brightfield)
			// alongside a z-stack channel leaves (c,z) slots unacquired. The writers fill
			// those with 0, so this is not a problem and is not reported as a
			// warning.
		}
	}

	private void setSize(IMetadata store, Integer size, int imageIndex, char dimension) {
		int value = (size == null || size.intValue() < 1) ? 1 : size.intValue();
		if (size == null)
			warnings.add("Size" + dimension + " of image " + imageIndex + " is unknown; assuming 1.");
		PositiveInteger positive = new PositiveInteger(Integer.valueOf(value));
		switch (dimension) {
			case 'X':
				store.setPixelsSizeX(positive, imageIndex);
				break;
			case 'Y':
				store.setPixelsSizeY(positive, imageIndex);
				break;
			case 'Z':
				store.setPixelsSizeZ(positive, imageIndex);
				break;
			case 'C':
				store.setPixelsSizeC(positive, imageIndex);
				break;
			case 'T':
				store.setPixelsSizeT(positive, imageIndex);
				break;
			default:
				break;
		}
	}

	private void buildEnvironment(IMetadata store, ExperimentMetadata metadata, ExperimentMetadata.ImageSeries series,
			int imageIndex) {
		Double temperature = series.temperatureCelsius != null ? series.temperatureCelsius
				: parseDouble(getMappedValue(metadata, Slot.ENVIRONMENT_TEMPERATURE));
		if (temperature != null)
			store.setImagingEnvironmentTemperature(new Temperature(temperature, UNIT_CELSIUS), imageIndex);

		Double co2 = series.co2Percent != null ? series.co2Percent
				: parseDouble(getMappedValue(metadata, Slot.ENVIRONMENT_CO2));
		if (co2 != null)
			store.setImagingEnvironmentCO2Percent(toPercentFraction(co2), imageIndex);

		Double humidity = series.humidityPercent != null ? series.humidityPercent
				: parseDouble(getMappedValue(metadata, Slot.ENVIRONMENT_HUMIDITY));
		if (humidity != null)
			store.setImagingEnvironmentHumidity(toPercentFraction(humidity), imageIndex);

		Double airPressure = parseDouble(getMappedValue(metadata, Slot.ENVIRONMENT_AIR_PRESSURE));
		if (airPressure != null)
			store.setImagingEnvironmentAirPressure(new Pressure(airPressure, UNIT_MILLIBAR), imageIndex);
	}

	private void buildChannels(IMetadata store, ExperimentMetadata metadata, ExperimentMetadata.ImageSeries series,
			int imageIndex) {
		String globalFluor = getMappedValue(metadata, Slot.CHANNEL_FLUOR);
		String globalMode = getMappedValue(metadata, Slot.CHANNEL_ACQUISITION_MODE);
		String globalContrast = getMappedValue(metadata, Slot.CHANNEL_CONTRAST_METHOD);

		for (int channelIndex = 0; channelIndex < series.channels.size(); channelIndex++) {
			ExperimentMetadata.Channel channel = series.channels.get(channelIndex);
			store.setChannelID("Channel:" + imageIndex + ":" + channelIndex, imageIndex, channelIndex);
			if (channel.name != null)
				store.setChannelName(channel.name, imageIndex, channelIndex);
			store.setChannelSamplesPerPixel(new PositiveInteger(
					channel.samplesPerPixel == null ? Integer.valueOf(1) : channel.samplesPerPixel), imageIndex,
					channelIndex);

			String fluor = channel.fluor != null ? channel.fluor : globalFluor;
			if (fluor != null)
				store.setChannelFluor(fluor, imageIndex, channelIndex);

			String acquisitionMode = channel.acquisitionMode != null ? channel.acquisitionMode : globalMode;
			if (acquisitionMode != null)
				store.setChannelAcquisitionMode(toAcquisitionMode(acquisitionMode), imageIndex, channelIndex);

			String contrastMethod = channel.contrastMethod != null ? channel.contrastMethod : globalContrast;
			if (contrastMethod != null)
				store.setChannelContrastMethod(toContrastMethod(contrastMethod), imageIndex, channelIndex);

			if (channel.illuminationType != null)
				store.setChannelIlluminationType(toIlluminationType(channel.illuminationType), imageIndex,
						channelIndex);
			if (channel.excitationWavelengthNm != null)
				store.setChannelExcitationWavelength(new Length(channel.excitationWavelengthNm, UNIT_NANOMETER),
						imageIndex, channelIndex);
			if (channel.emissionWavelengthNm != null)
				store.setChannelEmissionWavelength(new Length(channel.emissionWavelengthNm, UNIT_NANOMETER), imageIndex,
						channelIndex);
			if (channel.color != null)
				store.setChannelColor(new Color(channel.color.intValue()), imageIndex, channelIndex);

			if (metadata.detector != null) {
				store.setDetectorSettingsID(DETECTOR_ID, imageIndex, channelIndex);
				if (metadata.detector.gain != null)
					store.setDetectorSettingsGain(metadata.detector.gain, imageIndex, channelIndex);
			}
		}
		if (series.channels.isEmpty())
			warnings.add("Image " + imageIndex + " has no channel information.");
	}

	private void buildPlanes(IMetadata store, ExperimentMetadata.ImageSeries series, int imageIndex) {
		for (int planeIndex = 0; planeIndex < series.planes.size(); planeIndex++) {
			ExperimentMetadata.Plane plane = series.planes.get(planeIndex);
			store.setPlaneTheC(new NonNegativeInteger(Integer.valueOf(plane.channelIndex)), imageIndex, planeIndex);
			store.setPlaneTheZ(new NonNegativeInteger(Integer.valueOf(plane.focalPlaneIndex)), imageIndex, planeIndex);
			store.setPlaneTheT(new NonNegativeInteger(Integer.valueOf(plane.timePointIndex)), imageIndex, planeIndex);
			if (plane.deltaTSeconds != null)
				store.setPlaneDeltaT(new Time(plane.deltaTSeconds, UNIT_SECOND), imageIndex, planeIndex);
			if (plane.exposureMs != null)
				store.setPlaneExposureTime(new Time(plane.exposureMs, UNIT_MILLISECOND), imageIndex, planeIndex);
			if (plane.positionXMicrons != null)
				store.setPlanePositionX(new Length(plane.positionXMicrons, UNIT_MICROMETER), imageIndex, planeIndex);
			if (plane.positionYMicrons != null)
				store.setPlanePositionY(new Length(plane.positionYMicrons, UNIT_MICROMETER), imageIndex, planeIndex);
			if (plane.positionZMicrons != null)
				store.setPlanePositionZ(new Length(plane.positionZMicrons, UNIT_MICROMETER), imageIndex, planeIndex);
		}
	}

	// Screening
	private void buildPlate(IMetadata store, ExperimentMetadata metadata) {
		String plateName = getMappedValue(metadata, Slot.PLATE_NAME);
		boolean hasWells = false;
		for (ExperimentMetadata.ImageSeries series : metadata.series) {
			if (series.wellRow != null && series.wellColumn != null) {
				hasWells = true;
				break;
			}
		}
		if (plateName == null && !hasWells)
			return;

		store.setPlateID("Plate:0", 0);
		store.setPlateName(plateName != null ? plateName : (metadata.name == null ? "Plate" : metadata.name), 0);

		// One Well per (row,column) with one WellSample per field. A tile scan gives
		// several fields (and
		// hence several images) for the same well; emitting them as separate Wells with
		// equal row/column
		// would be invalid OME, so the images are grouped here into a single well with
		// several samples.
		java.util.Map<String, java.util.List<Integer>> imagesByWell = new java.util.LinkedHashMap<String, java.util.List<Integer>>();
		java.util.Map<String, int[]> wellRowCol = new java.util.HashMap<String, int[]>();
		for (int imageIndex = 0; imageIndex < metadata.series.size(); imageIndex++) {
			ExperimentMetadata.ImageSeries series = metadata.series.get(imageIndex);
			if (series.wellRow == null || series.wellColumn == null)
				continue;
			String key = series.wellRow + ":" + series.wellColumn;
			java.util.List<Integer> list = imagesByWell.get(key);
			if (list == null) {
				list = new java.util.ArrayList<Integer>();
				imagesByWell.put(key, list);
				wellRowCol.put(key, new int[] { series.wellRow.intValue(), series.wellColumn.intValue() });
			}
			list.add(Integer.valueOf(imageIndex));
		}

		int wellIndex = 0;
		for (java.util.Map.Entry<String, java.util.List<Integer>> entry : imagesByWell.entrySet()) {
			int[] rc = wellRowCol.get(entry.getKey());
			store.setWellID("Well:0:" + wellIndex, 0, wellIndex);
			store.setWellRow(new NonNegativeInteger(Integer.valueOf(rc[0])), 0, wellIndex);
			store.setWellColumn(new NonNegativeInteger(Integer.valueOf(rc[1])), 0, wellIndex);
			int sampleIndex = 0;
			for (Integer imageIndex : entry.getValue()) {
				store.setWellSampleID("WellSample:0:" + wellIndex + ":" + sampleIndex, 0, wellIndex, sampleIndex);
				store.setWellSampleIndex(new NonNegativeInteger(Integer.valueOf(wellIndex)), 0, wellIndex, sampleIndex);
				store.setWellSampleImageRef("Image:" + imageIndex.intValue(), 0, wellIndex, sampleIndex);
				sampleIndex++;
			}
			wellIndex++;
		}
	}

	// Annotations

	/**
	 * Writes all metadata properties which have no native OME element as map
	 * annotations, grouped by
	 * their category, and attaches them to every image. The category determines the
	 * namespace, so new
	 * metadata definitions are exported correctly without any change to the
	 * crosswalk.
	 */
	private void buildAnnotations(IMetadata store, ExperimentMetadata metadata) {
		Map<String, List<MapPair>> byCategory = new LinkedHashMap<String, List<MapPair>>();
		for (ExperimentMetadata.MetadataEntry entry : metadata.properties) {
			if (entry.value == null || entry.value.isEmpty())
				continue;
			Mapping mapping = crosswalk.getMapping(entry.name);
			if (mapping.slot != Slot.ANNOTATION)
				continue;
			String namespace = crosswalk.getNamespace(entry.category);
			List<MapPair> pairs = byCategory.get(namespace);
			if (pairs == null) {
				pairs = new ArrayList<MapPair>();
				byCategory.put(namespace, pairs);
			}
			pairs.add(new MapPair(mapping.annotationKey, entry.value));
		}

		// Provenance of the file itself, always written.
		List<MapPair> provenance = new ArrayList<MapPair>();
		String softwareName = metadata.softwareName == null ? "YouScope" : metadata.softwareName;
		String software = metadata.softwareVersion == null ? softwareName
				: softwareName + " " + metadata.softwareVersion;
		provenance.add(new MapPair("Acquisition software", software));
		if (metadata.softwareVersion != null)
			provenance.add(new MapPair("Acquisition software version", metadata.softwareVersion));
		// The metadata/schema version of this FAIR export, distinct from the YouScope
		// build. Bump when
		// the exported structure changes so downstream tools can tell mappings apart.
		provenance.add(new MapPair("FAIR export schema version", "1.0"));
		if (metadata.measurementFolder != null)
			provenance.add(new MapPair("Measurement folder", metadata.measurementFolder));
		List<MapPair> existingProvenance = byCategory.get(MetadataCrosswalk.NAMESPACE_PROVENANCE);
		if (existingProvenance != null)
			existingProvenance.addAll(provenance);
		else
			byCategory.put(MetadataCrosswalk.NAMESPACE_PROVENANCE, provenance);

		for (Map.Entry<String, List<MapPair>> entry : byCategory.entrySet()) {
			String annotationId = "Annotation:" + annotationIndex;
			store.setMapAnnotationID(annotationId, annotationIndex);
			store.setMapAnnotationNamespace(entry.getKey(), annotationIndex);
			store.setMapAnnotationValue(entry.getValue(), annotationIndex);
			for (int imageIndex = 0; imageIndex < metadata.series.size(); imageIndex++) {
				store.setImageAnnotationRef(annotationId, imageIndex, annotationIndex);
			}
			annotationIndex++;
		}
	}

	// Helpers

	/**
	 * Returns the value of the first metadata property which the crosswalk maps to
	 * the given slot.
	 */
	private String getMappedValue(ExperimentMetadata metadata, Slot slot) {
		for (ExperimentMetadata.MetadataEntry entry : metadata.properties) {
			if (entry.value == null || entry.value.isEmpty())
				continue;
			if (crosswalk.getMapping(entry.name).slot == slot)
				return entry.value;
		}
		return null;
	}

	private static String formatIso8601(java.util.Date date) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(date);
	}

	private static Double parseDouble(String value) {
		if (value == null)
			return null;
		// Tolerate values such as "37C", "37 degC" or "5 %" which users type into free
		// text fields.
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

	private static PercentFraction toPercentFraction(double percent) {
		double fraction = percent > 1.0 ? percent / 100.0 : percent;
		if (fraction < 0)
			fraction = 0;
		if (fraction > 1)
			fraction = 1;
		return new PercentFraction(Float.valueOf((float) fraction));
	}

	private PixelType toPixelType(String value) {
		if (value == null) {
			warnings.add("Pixel type unknown; assuming uint16.");
			return PixelType.UINT16;
		}
		try {
			return (PixelType) new PixelTypeEnumHandler().getEnumeration(value);
		} catch (Exception e) {
			warnings.add("Pixel type \"" + value + "\" is not an OME pixel type; assuming uint16.");
			return PixelType.UINT16;
		}
	}

	private Immersion toImmersion(String value) {
		try {
			return (Immersion) new ImmersionEnumHandler().getEnumeration(value);
		} catch (Exception e) {
			warnings.add("Immersion \"" + value + "\" is not an OME immersion type; exported as Other.");
			return Immersion.OTHER;
		}
	}

	private Correction toCorrection(String value) {
		try {
			return (Correction) new CorrectionEnumHandler().getEnumeration(value);
		} catch (Exception e) {
			warnings.add("Correction \"" + value + "\" is not an OME correction type; exported as Other.");
			return Correction.OTHER;
		}
	}

	private DetectorType toDetectorType(String value) {
		try {
			return (DetectorType) new DetectorTypeEnumHandler().getEnumeration(value);
		} catch (Exception e) {
			warnings.add("Detector type \"" + value + "\" is not an OME detector type; exported as Other.");
			return DetectorType.OTHER;
		}
	}

	private AcquisitionMode toAcquisitionMode(String value) {
		try {
			return (AcquisitionMode) new AcquisitionModeEnumHandler().getEnumeration(value);
		} catch (Exception e) {
			warnings.add("Acquisition mode \"" + value + "\" is not an OME acquisition mode; exported as Other.");
			return AcquisitionMode.OTHER;
		}
	}

	private ContrastMethod toContrastMethod(String value) {
		try {
			return (ContrastMethod) new ContrastMethodEnumHandler().getEnumeration(value);
		} catch (Exception e) {
			warnings.add("Contrast method \"" + value + "\" is not an OME contrast method; exported as Other.");
			return ContrastMethod.OTHER;
		}
	}

	private IlluminationType toIlluminationType(String value) {
		try {
			return (IlluminationType) new IlluminationTypeEnumHandler().getEnumeration(value);
		} catch (Exception e) {
			warnings.add("Illumination type \"" + value + "\" is not an OME illumination type; exported as Other.");
			return IlluminationType.OTHER;
		}
	}
}
