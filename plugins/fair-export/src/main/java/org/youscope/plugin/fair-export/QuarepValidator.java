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

package org.youscope.plugin.fairexport;

import java.util.ArrayList;
import java.util.List;

import org.youscope.plugin.fairexport.model.ExperimentMetadata;

/**
 * Checks a measurement against the QUAREP-LiMi metadata recommendations.
 * <p>
 * QUAREP-LiMi working group 7 endorsed the tiered 4DN-BINA-OME specification,
 * so "QUAREP compatible"
 * in practice means Tier 1 completeness plus the calibration provenance the
 * quality control working
 * groups rely on. This class implements that checklist against the parsed
 * measurement.
 * </p>
 * <p>
 * The result is advisory. A missing field never blocks an export (with the data
 * already acquired), and
 * refusing to write standards-compliant metadata because it is incomplete would
 * help nobody. What the
 * check does is make the gaps explicit at the moment they can still be closed,
 * and distinguish gaps
 * the user can fix by filling in a metadata field from gaps that need a
 * configuration change.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class QuarepValidator {
	/**
	 * Tier of the 4DN-BINA-OME specification a requirement belongs to.
	 */
	public enum Tier {
		/** Minimum information required for the data to be interpretable at all. */
		TIER_1,
		/** Recommended additions for quantitative comparison between instruments. */
		TIER_2
	}

	/**
	 * One checked requirement.
	 */
	public static class Requirement {
		public final String name;
		public final Tier tier;
		public final boolean satisfied;
		/**
		 * How to obtain the value if it is missing, or the value found if it is
		 * present.
		 */
		public final String detail;

		Requirement(String name, Tier tier, boolean satisfied, String detail) {
			this.name = name;
			this.tier = tier;
			this.satisfied = satisfied;
			this.detail = detail;
		}
	}

	/**
	 * Runs the full checklist.
	 * 
	 * @param metadata Parsed measurement.
	 * @return All requirements with their status, in checklist order.
	 */
	public static List<Requirement> check(ExperimentMetadata metadata) {
		List<Requirement> requirements = new ArrayList<Requirement>();
		if (metadata == null)
			return requirements;

		// Study and provenance
		add(requirements, "Acquisition date", Tier.TIER_1, metadata.acquisitionDate != null,
				"Derived from the image table; missing only if no image carries a timestamp.");
		add(requirements, "Experimenter", Tier.TIER_1,
				metadata.getProperty("Operator") != null || metadata.getProperty("User") != null,
				"Fill in the \"Operator\" metadata property.");
		add(requirements, "Institution", Tier.TIER_2, metadata.getProperty("Institution") != null,
				"Fill in the \"Institution\" metadata property.");

		// Instrument
		boolean hasInstrument = metadata.instrument != null
				&& (metadata.instrument.manufacturer != null || metadata.instrument.model != null);
		add(requirements, "Microscope make and model", Tier.TIER_1, hasInstrument,
				"YouScope records only the device adapter, not the manufacturer. Add \"Microscope model\" as a metadata definition under imaging/.");

		boolean hasObjective = metadata.objective != null;
		add(requirements, "Objective magnification", Tier.TIER_1,
				hasObjective && metadata.objective.nominalMagnification != null,
				"Read from the objective label in YSConfig_Microscope.cfg; requires a label containing e.g. \"40X\".");
		boolean hasNA = (hasObjective && metadata.objective.lensNA != null)
				|| metadata.getProperty("Numerical aperture") != null || metadata.getProperty("NA") != null;
		add(requirements, "Objective numerical aperture", Tier.TIER_1, hasNA,
				"Not present in any YouScope or Micro-Manager configuration file. Add \"Numerical aperture\" as a metadata definition under imaging/, with a default value per objective.");
		add(requirements, "Objective immersion", Tier.TIER_1,
				hasObjective && metadata.objective.immersion != null,
				"Fill in the \"Immersion medium\" metadata property.");
		add(requirements, "Objective refractive index", Tier.TIER_2,
				metadata.getProperty("Refractive index") != null,
				"Add \"Refractive index\" as a metadata definition under imaging/.");

		boolean hasDetector = metadata.detector != null;
		add(requirements, "Detector make and model", Tier.TIER_1,
				hasDetector && (metadata.detector.model != null || metadata.detector.name != null),
				"Taken from the camera device properties recorded at startup.");
		boolean hasDetectorType = (hasDetector && metadata.detector.type != null)
				|| metadata.getProperty("Detector type") != null;
		add(requirements, "Detector type", Tier.TIER_2, hasDetectorType,
				"Add \"Detector type\" as a metadata definition under imaging/, e.g. CMOS or EMCCD.");

		// Calibration
		boolean anyMissingPixelSize = false;
		boolean anyMissingZStep = false;
		boolean anyMissingExposure = false;
		boolean anyMissingWavelength = false;
		boolean anyMissingBitDepth = false;
		for (ExperimentMetadata.ImageSeries series : metadata.series) {
			if (series.physicalSizeXMicrons == null)
				anyMissingPixelSize = true;
			if (series.sizeZ != null && series.sizeZ.intValue() > 1 && series.physicalSizeZMicrons == null)
				anyMissingZStep = true;
			if (series.pixelType == null)
				anyMissingBitDepth = true;
			for (ExperimentMetadata.Channel channel : series.channels) {
				if (channel.exposureMs == null)
					anyMissingExposure = true;
				if (channel.excitationWavelengthNm == null && channel.emissionWavelengthNm == null)
					anyMissingWavelength = true;
			}
		}
		boolean hasSeries = !metadata.series.isEmpty();

		add(requirements, "Lateral pixel size", Tier.TIER_1, hasSeries && !anyMissingPixelSize,
				"Resolved from the pixel size settings. If several settings match the microscope state, the value is omitted on purpose. Add a device setting distinguishing them, typically the camera.");
		add(requirements, "Axial step size", Tier.TIER_1, !anyMissingZStep,
				"Derived from the focus positions of the Z stack job; requires evenly spaced positions.");
		add(requirements, "Exposure time", Tier.TIER_1, hasSeries && !anyMissingExposure,
				"Taken from the imaging jobs in configuration.csb.");
		add(requirements, "Bit depth", Tier.TIER_1, hasSeries && !anyMissingBitDepth,
				"Taken from the image table.");
		add(requirements, "Excitation / emission wavelength", Tier.TIER_1, hasSeries && !anyMissingWavelength,
				"Inferred from the filter labels of the channel definition, e.g. \"Chroma-HQ620\". Rename filters to contain their wavelength, or add explicit metadata.");
		add(requirements, "Fluorophore", Tier.TIER_1, metadata.getProperty("Fluorophore") != null,
				"Fill in the \"Fluorophore\" metadata property.");

		// Illumination
		add(requirements, "Illumination power", Tier.TIER_1, metadata.getProperty("Illumination power") != null,
				"Not recorded by YouScope. It changes whenever illumination intensity, filters or aperture change, so it has to be measured as part of quality control and entered as metadata. Add \"Illumination power\" as a metadata definition under quality/.");
		add(requirements, "Imaging environment temperature", Tier.TIER_2,
				metadata.getProperty("Temperature") != null,
				"Fill in the \"Temperature\" metadata property.");

		// Sample
		add(requirements, "Species", Tier.TIER_2, metadata.getProperty("Species") != null,
				"Fill in the \"Species\" metadata property.");
		add(requirements, "Sample preparation", Tier.TIER_2,
				metadata.getProperty("Fixation") != null || metadata.getProperty("Mounting medium") != null,
				"Fill in the \"Fixation\" or \"Mounting medium\" metadata properties.");

		return requirements;
	}

	/**
	 * Runs the checklist and returns human readable warnings for the unsatisfied
	 * requirements, plus a
	 * summary line. Used by {@link FairExportEngine}.
	 * 
	 * @param metadata Parsed measurement.
	 * @return Warnings, empty if the checklist is fully satisfied.
	 */
	public static List<String> validate(ExperimentMetadata metadata) {
		List<Requirement> requirements = check(metadata);
		List<String> warnings = new ArrayList<String>();
		for (Requirement requirement : requirements) {
			if (requirement.satisfied)
				continue;
			// Only genuinely missing fields are warnings. The completeness summary is
			// informational and
			// is reported separately by the engine, so a fully complete export shows zero
			// warnings.
			warnings.add("QUAREP " + (requirement.tier == Tier.TIER_1 ? "Tier 1" : "Tier 2") + "  -  "
					+ requirement.name + " is missing. " + requirement.detail);
		}
		return warnings;
	}

	/**
	 * Returns the one-line completeness summary for a measurement, always
	 * informational (never a
	 * warning). Reported by the engine regardless of whether anything is missing.
	 * 
	 * @param metadata Parsed measurement.
	 * @return The summary line.
	 */
	public static String getSummaryLine(ExperimentMetadata metadata) {
		return getSummary(check(metadata));
	}

	/**
	 * Returns a one line completeness summary, e.g. for the export dialog or the
	 * measurement log.
	 * 
	 * @param requirements Result of {@link #check(ExperimentMetadata)}.
	 * @return Summary line.
	 */
	public static String getSummary(List<Requirement> requirements) {
		int tier1Total = 0;
		int tier1Satisfied = 0;
		int tier2Total = 0;
		int tier2Satisfied = 0;
		for (Requirement requirement : requirements) {
			if (requirement.tier == Tier.TIER_1) {
				tier1Total++;
				if (requirement.satisfied)
					tier1Satisfied++;
			} else {
				tier2Total++;
				if (requirement.satisfied)
					tier2Satisfied++;
			}
		}
		return "QUAREP-LiMi completeness: Tier 1 " + tier1Satisfied + "/" + tier1Total
				+ ", Tier 2 " + tier2Satisfied + "/" + tier2Total
				+ ". Missing fields are omitted from the metadata rather than guessed.";
	}

	private static void add(List<Requirement> requirements, String name, Tier tier, boolean satisfied, String detail) {
		requirements.add(new Requirement(name, tier, satisfied, detail));
	}
}
