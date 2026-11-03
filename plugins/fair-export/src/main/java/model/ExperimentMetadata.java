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
package org.youscope.plugin.fairexport.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Internal, format independent description of one acquired measurement.
 * <p>
 * This is the single hand-over point of the FAIR export pipeline: the parsers
 * of
 * {@code YSConfig_Microscope.cfg}, {@code configuration.csb} and
 * {@code images.csb} fill this model,
 * and the OME-TIFF, OME-Zarr and RO-Crate exporters consume it. The model
 * deliberately does not depend
 * on Bio-Formats, so that it can also be serialized to JSON for RO-Crate
 * without pulling in the OME
 * libraries.
 * </p>
 * <p>
 * All physical quantities use fixed units to avoid unit confusion inside the
 * pipeline:
 * lengths and pixel sizes in micrometre, times in seconds, exposures in
 * milliseconds, wavelengths in
 * nanometre, temperatures in degree Celsius. Every field may be null which
 * means meaning "unknown"; the exporters
 * omit unknown values rather than guessing them.
 * </p>
 * <p>
 * The value classes below use public fields rather than accessors: this is a
 * plain data transfer model
 * with no invariants to protect, and the parsers fill it field by field.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class ExperimentMetadata {
	// Study level

	/** Name of the measurement, as displayed in YouScope. */
	public String name = null;
	/**
	 * Free text description, taken from the {@code <description>} element of the
	 * configuration.
	 */
	public String description = null;
	/** Begin of the acquisition. */
	public Date acquisitionDate = null;
	/** Name of the acquisition software, i.e. "YouScope". */
	public String softwareName = "YouScope";
	/** Version of the acquisition software. */
	public String softwareVersion = null;
	/** Folder the measurement was saved to. */
	public String measurementFolder = null;

	/**
	 * True if this measurement uses cell-detection / feedback / online-analysis
	 * jobs, i.e. an adaptive
	 * acquisition rather than a plain scan. The useq-schema export is written only
	 * for these,
	 * because a fixed grid/plate scan is already fully described by the OME
	 * metadata, whereas a feedback
	 * acquisition's <em>plan</em> is the interesting, reusable artefact. Detected
	 * from the measurement
	 * configuration; defaults to false (treat as plain) when it cannot be
	 * determined.
	 */
	public boolean feedbackMeasurement = false;

	/**
	 * User entered metadata as shown in the metadata panel, including the category
	 * each property
	 * belongs to. Properties which have no native OME representation are exported
	 * as map annotations
	 * grouped by their category.
	 */
	public final List<MetadataEntry> properties = new ArrayList<MetadataEntry>();

	// Hardware

	public Instrument instrument = null;
	public Objective objective = null;
	public Detector detector = null;

	// Image data

	/**
	 * The image series of this measurement. A simple measurement produces exactly
	 * one series; a
	 * measurement with several positions, wells or image save names produces one
	 * series per position.
	 */
	public final List<ImageSeries> series = new ArrayList<ImageSeries>();

	/**
	 * Plate description for microplate measurements, or null for flat (non-plate)
	 * measurements. When
	 * present, the OME-Zarr exporter writes an HCS plate hierarchy instead of
	 * independent images.
	 */
	public Plate plate = null;

	/**
	 * A user entered metadata property together with the category (metadata
	 * sub-folder) it belongs to.
	 */
	public static class MetadataEntry {
		public String name;
		public String value;
		/**
		 * Category of the underlying metadata definition, or empty for uncategorized
		 * properties.
		 */
		public String category;

		public MetadataEntry(String name, String value, String category) {
			this.name = name;
			this.value = value;
			this.category = category == null ? "" : category;
		}
	}

	/**
	 * The microscope as a whole.
	 */
	public static class Instrument {
		public String name = null;
		public String manufacturer = null;
		public String model = null;
		public String serialNumber = null;
	}

	/**
	 * The objective used for the acquisition.
	 */
	public static class Objective {
		public String name = null;
		public String manufacturer = null;
		public String model = null;
		public String serialNumber = null;
		/** Nominal magnification, e.g. 60.0. */
		public Double nominalMagnification = null;
		/** Calibrated magnification, if known. */
		public Double calibratedMagnification = null;
		public Double lensNA = null;
		/**
		 * Immersion medium as free text, e.g. "Oil", "Water", "Air". Mapped to the OME
		 * enumeration.
		 */
		public String immersion = null;
		/** Correction as free text, e.g. "PlanApo". Mapped to the OME enumeration. */
		public String correction = null;
		/** Working distance in micrometre. */
		public Double workingDistanceMicrons = null;
	}

	/**
	 * The camera or other detector used for the acquisition.
	 */
	public static class Detector {
		public String name = null;
		public String manufacturer = null;
		public String model = null;
		public String serialNumber = null;
		/**
		 * Detector type as free text, e.g. "CMOS", "CCD", "EMCCD". Mapped to the OME
		 * enumeration.
		 */
		public String type = null;
		public Double gain = null;
		public Double offset = null;
		/** Physical size of one sensor pixel in micrometre. */
		public Double pixelSizeMicrons = null;
	}

	/**
	 * One image series, i.e. one OME {@code Image} element with its own pixel
	 * block.
	 */
	public static class ImageSeries {
		/** Name of the series, e.g. the image save name or the position name. */
		public String name = null;
		public String description = null;

		public Integer sizeX = null;
		public Integer sizeY = null;
		public Integer sizeZ = null;
		public Integer sizeC = null;
		public Integer sizeT = null;
		/** Pixel type as free text, e.g. "uint16". Mapped to the OME enumeration. */
		public String pixelType = null;
		/** True if the image data is stored most significant byte first. */
		public boolean bigEndian = false;

		public Double physicalSizeXMicrons = null;
		public Double physicalSizeYMicrons = null;
		public Double physicalSizeZMicrons = null;
		/** Nominal time between two time points in seconds. */
		public Double timeIncrementSeconds = null;

		/** Acquisition temperature in degree Celsius, if recorded by the incubator. */
		public Double temperatureCelsius = null;
		public Double co2Percent = null;
		public Double humidityPercent = null;

		/** Plate name for screening acquisitions, or null. */
		public String plateName = null;
		/** Zero based well row, or null. */
		public Integer wellRow = null;
		/** Zero based well column, or null. */
		public Integer wellColumn = null;

		/**
		 * Zero based tile indices within a tile/grid (plate-scanning) acquisition, or
		 * null when the
		 * image is not part of a tile scan. Each tile is exported as its own image; the
		 * indices identify
		 * the tile's place in the grid and the stage offset is carried on the planes'
		 * XY positions so the
		 * tiles can be stitched afterwards.
		 */
		public Integer tileIndexX = null;
		public Integer tileIndexY = null;
		/**
		 * Grid size of the tile scan this image belongs to (for montage metadata), or
		 * null.
		 */
		public Integer tileCountX = null;
		public Integer tileCountY = null;

		public final List<Channel> channels = new ArrayList<Channel>();
		public final List<Plane> planes = new ArrayList<Plane>();

		/**
		 * Returns the index of a plane within this series for the OME dimension order
		 * XYCZT.
		 * 
		 * @param c Channel index.
		 * @param z Focal plane index.
		 * @param t Time point index.
		 * @return Index of the plane, or -1 if the series dimensions are unknown.
		 */
		public int getPlaneIndex(int c, int z, int t) {
			if (sizeC == null || sizeZ == null)
				return -1;
			return c + sizeC.intValue() * (z + sizeZ.intValue() * t);
		}

		/**
		 * Returns the number of planes a dense dataset of this series would contain.
		 * 
		 * @return Expected number of planes, or -1 if the dimensions are unknown.
		 */
		public int getExpectedPlaneCount() {
			if (sizeC == null || sizeZ == null || sizeT == null)
				return -1;
			return sizeC.intValue() * sizeZ.intValue() * sizeT.intValue();
		}

		/**
		 * Returns true if planes are missing, i.e. if the acquisition was sparse.
		 * OME-TIFF
		 * requires a dense plane block, whereas OME-Zarr can keep the sparse layout.
		 * 
		 * @return True if at least one plane of the dense block is missing.
		 */
		public boolean isSparse() {
			int expected = getExpectedPlaneCount();
			return expected >= 0 && planes.size() < expected;
		}
	}

	/**
	 * One channel of an image series.
	 */
	public static class Channel {
		/** Name of the channel as configured in YouScope, e.g. "BF" or "GFP". */
		public String name = null;
		/** Channel group the channel belongs to, e.g. "Channels". */
		public String channelGroup = null;
		/** Fluorophore, taken from the labelling metadata if available. */
		public String fluor = null;
		/**
		 * Acquisition mode as free text, e.g. "WideField". Mapped to the OME
		 * enumeration.
		 */
		public String acquisitionMode = null;
		/**
		 * Contrast method as free text, e.g. "Brightfield", "Fluorescence". Mapped to
		 * the OME enumeration.
		 */
		public String contrastMethod = null;
		/**
		 * Illumination type as free text, e.g. "Transmitted", "Epifluorescence". Mapped
		 * to the OME enumeration.
		 */
		public String illuminationType = null;
		public Double excitationWavelengthNm = null;
		public Double emissionWavelengthNm = null;
		/** Default exposure time in milliseconds. Per plane exposures override this. */
		public Double exposureMs = null;
		/** Number of samples per pixel, i.e. 1 for greyscale and 3 for RGB. */
		public Integer samplesPerPixel = null;
		/** Display colour as 0xRRGGBBAA, or null. */
		public Integer color = null;
	}

	/**
	 * One acquired image plane.
	 */
	public static class Plane {
		public int channelIndex = 0;
		public int focalPlaneIndex = 0;
		public int timePointIndex = 0;

		/** Time elapsed since the beginning of the acquisition, in seconds. */
		public Double deltaTSeconds = null;
		/** Exposure of this plane in milliseconds. */
		public Double exposureMs = null;

		public Double positionXMicrons = null;
		public Double positionYMicrons = null;
		public Double positionZMicrons = null;

		/** File name of the plane relative to the measurement folder. */
		public String fileName = null;
	}

	/**
	 * Returns the value of a user entered metadata property, ignoring case.
	 * 
	 * @param propertyName Name of the property.
	 * @return Value of the property, or null if the property is not present or
	 *         empty.
	 */
	public String getProperty(String propertyName) {
		if (propertyName == null)
			return null;
		for (MetadataEntry entry : properties) {
			if (propertyName.equalsIgnoreCase(entry.name))
				return (entry.value == null || entry.value.isEmpty()) ? null : entry.value;
		}
		return null;
	}

	/**
	 * High-content-screening plate description. Rows and columns are the full
	 * physical grid; wells are
	 * only those actually acquired (a sparse plate, per the NGFF spec).
	 */
	public static class Plate {
		/** Plate name, e.g. the microplate configuration type or a user title. */
		public String name = null;
		/** Row names in physical order, e.g. ["A","B",...]. */
		public final List<String> rowNames = new ArrayList<String>();
		/** Column names in physical order, e.g. ["1","2",...]. */
		public final List<String> columnNames = new ArrayList<String>();
		/** Acquired wells. */
		public final List<PlateWell> wells = new ArrayList<PlateWell>();
	}

	/**
	 * One acquired well of a plate, linking a row/column to the image (field) that
	 * holds its pixels.
	 */
	public static class PlateWell {
		public int rowIndex = 0;
		public int columnIndex = 0;
		/** Path of the well group in the plate, e.g. "B/2". */
		public String path = null;
		/**
		 * The image series that provides this well's first field of view. Kept for
		 * callers that assume a
		 * single field; equal to {@code fields.get(0)} when fields are present.
		 */
		public ImageSeries image = null;
		/**
		 * All fields of view acquired in this well, in field order. A plain well has
		 * one field; a well
		 * imaged with a tile/grid scan has one field per tile. The OME/NGFF exporters
		 * write these as
		 * fields {@code 0, 1, 2, ...} under the well group.
		 */
		public final java.util.List<ImageSeries> fields = new java.util.ArrayList<ImageSeries>();
	}
}
