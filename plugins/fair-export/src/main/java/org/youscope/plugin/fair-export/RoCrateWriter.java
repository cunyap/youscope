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

import java.io.File;
import java.util.List;

import org.youscope.plugin.fairexport.model.ExperimentMetadata;

/**
 * Writes an RO-Crate manifest ({@code ro-crate-metadata.json}) describing the
 * exported dataset.
 * <p>
 * RO-Crate is a JSON-LD convention, so this writer needs no Bio-Formats and
 * lives outside the isolated
 * classloader. It is intentionally hand-rolled rather than using a JSON
 * library: the structure is
 * small and fixed, and avoiding a dependency keeps the plugin's footprint
 * minimal. All strings are
 * escaped for JSON.
 * </p>
 * <p>
 * The manifest references the files the export produced (the OME-XML, and any
 * OME-TIFF / OME-Zarr) and
 * carries the study-level metadata, so a repository ingesting the folder sees a
 * self-describing,
 * FAIR-aligned dataset rather than a bag of files.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
class RoCrateWriter {
	static final String FILE_NAME = "ro-crate-metadata.json";

	/** Media type used for the OME-Zarr group so the crate is self-describing. */
	static final String ZARR_MEDIA_TYPE = "application/vnd.zarr";

	/**
	 * The {@code @context} used by the in-store crate. Extends the RO-Crate 1.1
	 * context with the OME
	 * NGFF Challenge terms ({@code organism_classification}, {@code biosample},
	 * {@code specimen},
	 * {@code fbbi_id}, ...) so the OME-NGFF validator resolves Organism and Imaging
	 * method. Kept byte-for-
	 * byte compatible with the challenge example crate the validator was written
	 * against.
	 */
	private static final String NGFF_CONTEXT = "  \"@context\": [\n"
			+ "    \"https://w3id.org/ro/crate/1.1/context\",\n"
			+ "    {\n"
			+ "      \"organism_classification\": \"https://schema.org/taxonomicRange\",\n"
			+ "      \"BioChemEntity\": \"https://schema.org/BioChemEntity\",\n"
			+ "      \"obo\": \"http://purl.obolibrary.org/obo/\",\n"
			+ "      \"FBcv\": \"http://ontobee.org/ontology/FBcv/\",\n"
			+ "      \"biological_entity\": \"https://schema.org/about\",\n"
			+ "      \"biosample\": \"http://purl.obolibrary.org/obo/OBI_0002648\",\n"
			+ "      \"preparation_method\": \"https://www.wikidata.org/wiki/Property:P1537\",\n"
			+ "      \"specimen\": \"http://purl.obolibrary.org/obo/HSO_0000308\"\n"
			+ "    }\n"
			+ "  ]";

	/**
	 * Writes a per-store {@code ro-crate-metadata.json} at the root of a single
	 * {@code *.ome.zarr}
	 * group (T-RC), following the OME NGFF Challenge RO-Crate structure.
	 * <p>
	 * The folder-level crate written by {@link #write} describes the whole export
	 * (it sits one level up,
	 * at the YouScope experiment / output-folder level); this one describes just
	 * the one OME-Zarr image
	 * or plate and lives <em>inside</em> the store, which is what the OME-NGFF
	 * validator and the NGFF
	 * RO-Crate profile expect. Both are wanted.
	 * </p>
	 * <p>
	 * Beyond name/description/license on the root {@code Dataset}, this extracts
	 * the FAIR metadata the
	 * validator surfaces: the <b>organism</b> (as a {@code biosample} whose
	 * {@code organism_classification}
	 * points at an NCBI taxon) and the <b>imaging method</b> (as an
	 * {@code image_acquisition} whose
	 * {@code fbbi_id} points at an FBbi term), linked through a {@code specimen}
	 * and {@code resultOf}, as
	 * in the challenge example. Ontology IDs come from {@link OntologyLookup},
	 * which uses a user-supplied
	 * ID if present, else a small built-in lookup, else emits the name as plain
	 * text and never a guessed ID.
	 * Values that are absent are omitted rather than invented; any that resolve to
	 * a name without an ID
	 * add a warning (the validator will show that row as "Not found" until an ID is
	 * supplied).
	 * </p>
	 * 
	 * @param metadata    Parsed measurement (study-level fields are copied in).
	 * @param storeFolder The {@code *.ome.zarr} directory to write the crate into.
	 * @param imageName   Human-readable name of the image or plate this store holds
	 *                    (may be null).
	 * @param warnings    List to which "resolved to a name but no ontology ID"
	 *                    notes are added (may be null).
	 * @return The manifest file written inside the store.
	 * @throws Exception If the file cannot be written.
	 */
	static File writeStoreCrate(ExperimentMetadata metadata, File storeFolder, String imageName,
			java.util.List<String> warnings) throws Exception {
		boolean hasGroupJson = new File(storeFolder, "zarr.json").isFile();

		// Resolve organism / imaging method up front so we know whether to emit those
		// graph nodes.
		OntologyLookup.Term organism = OntologyLookup.organism(metadata);
		OntologyLookup.Term imaging = OntologyLookup.imagingMethod(metadata, firstAcquisitionMode(metadata));

		String acquisitionId = "#image_acquisition";
		String specimenId = "#specimen";
		String biosampleId = "#biosample";

		StringBuilder json = new StringBuilder();
		json.append("{\n");
		json.append(NGFF_CONTEXT).append(",\n");
		json.append("  \"@graph\": [\n");

		// Descriptor.
		json.append("    {\n");
		json.append("      \"@type\": \"CreativeWork\",\n");
		json.append("      \"@id\": \"ro-crate-metadata.json\",\n");
		json.append("      \"conformsTo\": { \"@id\": \"https://w3id.org/ro/crate/1.1\" },\n");
		json.append("      \"about\": { \"@id\": \"./\" }\n");
		json.append("    },\n");

		// Root dataset = the OME-Zarr group itself.
		json.append("    {\n");
		json.append("      \"@type\": \"Dataset\",\n");
		json.append("      \"@id\": \"./\",\n");
		json.append("      \"conformsTo\": { \"@id\": \"https://w3id.org/ro/crate/1.1\" },\n");
		json.append("      \"encodingFormat\": ").append(quote(ZARR_MEDIA_TYPE)).append(",\n");
		String storeName = imageName != null ? imageName
				: (metadata != null && metadata.name != null ? metadata.name : storeFolder.getName());
		if (storeName != null)
			json.append("      \"name\": ").append(quote(storeName)).append(",\n");
		String description = resolveDescription(metadata);
		if (description != null)
			json.append("      \"description\": ").append(quote(description)).append(",\n");
		String license = resolveLicense(metadata);
		if (license != null)
			json.append("      \"license\": ").append(quote(license)).append(",\n");
		if (metadata != null && metadata.acquisitionDate != null)
			json.append("      \"datePublished\": ")
					.append(quote(new java.text.SimpleDateFormat("yyyy-MM-dd").format(metadata.acquisitionDate)))
					.append(",\n");
		String experimenter = firstNonNull(property(metadata, "Principal Investigator"),
				property(metadata, "Operator"), property(metadata, "User"),
				property(metadata, "Experimenter"));
		if (experimenter != null)
			json.append("      \"author\": ").append(quote(experimenter)).append(",\n");
		// Link organism (via specimen to biosample) and imaging method (via resultOf to
		// image_acquisition).
		if (organism != null)
			json.append("      \"biological_entity\": { \"@id\": \"").append(specimenId).append("\" },\n");
		if (imaging != null)
			json.append("      \"resultOf\": { \"@id\": \"").append(acquisitionId).append("\" },\n");
		// Reference the group metadata file when present, so the crate points at real
		// content.
		if (hasGroupJson)
			json.append("      \"hasPart\": [ { \"@id\": \"zarr.json\" } ]\n");
		else
			json.append("      \"hasPart\": []\n");
		json.append("    }");

		if (hasGroupJson) {
			json.append(",\n    {\n");
			json.append("      \"@type\": \"File\",\n");
			json.append("      \"@id\": \"zarr.json\",\n");
			json.append("      \"name\": \"OME-Zarr group metadata\",\n");
			json.append("      \"encodingFormat\": \"application/json\"\n");
			json.append("    }");
		}

		// Organism: specimen to biosample to organism_classification {@id
		// NCBI:txid...}.
		if (organism != null) {
			json.append(",\n    {\n");
			json.append("      \"@id\": \"").append(specimenId).append("\",\n");
			json.append("      \"@type\": \"specimen\",\n");
			json.append("      \"biosample\": { \"@id\": \"").append(biosampleId).append("\" }\n");
			json.append("    }");

			json.append(",\n    {\n");
			json.append("      \"@id\": \"").append(biosampleId).append("\",\n");
			json.append("      \"@type\": \"biosample\",\n");
			if (organism.hasId())
				json.append("      \"organism_classification\": { \"@id\": ").append(quote(organism.id))
						.append(" },\n");
			json.append("      \"name\": ").append(quote(organism.name == null ? organism.id : organism.name))
					.append("\n");
			json.append("    }");
			if (!organism.hasId() && warnings != null)
				warnings.add("RO-Crate: organism \"" + organism.name + "\" has no NCBI taxon ID, so the validator's "
						+ "Organism row will read \"Not found\". Add an NCBI ID (e.g. a \"Species NCBI ID\" metadata "
						+ "field) to resolve it.");
		}

		// Imaging method: image_acquisition to fbbi_id {@id obo:FBbi_...}, linked to
		// the specimen.
		if (imaging != null) {
			json.append(",\n    {\n");
			json.append("      \"@id\": \"").append(acquisitionId).append("\",\n");
			json.append("      \"@type\": \"image_acquisition\",\n");
			if (imaging.hasId())
				json.append("      \"fbbi_id\": { \"@id\": ").append(quote(imaging.id)).append(" },\n");
			if (organism != null)
				json.append("      \"specimen\": { \"@id\": \"").append(specimenId).append("\" },\n");
			json.append("      \"name\": ").append(quote(imaging.name == null ? imaging.id : imaging.name))
					.append("\n");
			json.append("    }");
			if (!imaging.hasId() && warnings != null)
				warnings.add("RO-Crate: imaging method \"" + imaging.name + "\" has no FBbi ID, so the validator's "
						+ "Imaging method row will read \"Not found\". Add an FBbi ID (e.g. an \"Imaging method FBbi ID\" "
						+ "metadata field) to resolve it.");
		}

		json.append("\n  ]\n}\n");

		File target = new File(storeFolder, FILE_NAME);
		writeUtf8(target, json.toString());
		return target;
	}

	/**
	 * Writes a minimal, valid {@code ro-crate-metadata.json} into a sub-group of a
	 * store (a plate well
	 * such as {@code A/1}, or an image group such as {@code A/1/0}). Used so the
	 * OME-NGFF validator does
	 * not report "No ro-crate-metadata.json found" when the user points it at a
	 * sub-group rather than the
	 * store root. The crate simply states that this group is a {@code Dataset}; the
	 * descriptive metadata
	 * lives in the store-root crate. Writing these is optional (controlled by the
	 * caller); the root crate
	 * alone is spec-compliant.
	 * 
	 * @param groupFolder The sub-group directory (must contain, or will contain, a
	 *                    {@code zarr.json}).
	 * @param name        A human-readable name for the group (e.g. the well path),
	 *                    or null.
	 * @return the manifest written, or null if the folder does not exist.
	 * @throws Exception If the file cannot be written.
	 */
	static File writeSubGroupCrate(File groupFolder, String name) throws Exception {
		if (groupFolder == null || !groupFolder.isDirectory())
			return null;
		boolean hasGroupJson = new File(groupFolder, "zarr.json").isFile();
		StringBuilder json = new StringBuilder();
		json.append("{\n");
		json.append("  \"@context\": \"https://w3id.org/ro/crate/1.1/context\",\n");
		json.append("  \"@graph\": [\n");
		json.append("    {\n");
		json.append("      \"@type\": \"CreativeWork\",\n");
		json.append("      \"@id\": \"ro-crate-metadata.json\",\n");
		json.append("      \"conformsTo\": { \"@id\": \"https://w3id.org/ro/crate/1.1\" },\n");
		json.append("      \"about\": { \"@id\": \"./\" }\n");
		json.append("    },\n");
		json.append("    {\n");
		json.append("      \"@type\": \"Dataset\",\n");
		json.append("      \"@id\": \"./\",\n");
		json.append("      \"conformsTo\": { \"@id\": \"https://w3id.org/ro/crate/1.1\" },\n");
		json.append("      \"encodingFormat\": ").append(quote(ZARR_MEDIA_TYPE));
		if (name != null)
			json.append(",\n      \"name\": ").append(quote(name));
		if (hasGroupJson)
			json.append(",\n      \"hasPart\": [ { \"@id\": \"zarr.json\" } ]\n");
		else
			json.append("\n");
		json.append("    }");
		if (hasGroupJson) {
			json.append(",\n    {\n");
			json.append("      \"@type\": \"File\",\n");
			json.append("      \"@id\": \"zarr.json\",\n");
			json.append("      \"name\": \"OME-Zarr group metadata\",\n");
			json.append("      \"encodingFormat\": \"application/json\"\n");
			json.append("    }");
		}
		json.append("\n  ]\n}\n");
		File target = new File(groupFolder, FILE_NAME);
		writeUtf8(target, json.toString());
		return target;
	}

	// Field resolution

	/**
	 * Description precedence (#2): the measurement description (which itself
	 * prefers information.xml then
	 * configuration.csb's {@code <description>}); if that is empty, a short
	 * composed line from Project and
	 * Laboratory so the field is never blank.
	 */
	private static String resolveDescription(ExperimentMetadata metadata) {
		if (metadata == null)
			return null;
		if (metadata.description != null && !metadata.description.trim().isEmpty())
			return metadata.description.trim();
		String project = property(metadata, "Project");
		String lab = firstNonNull(property(metadata, "Laboratory"), property(metadata, "Institution"));
		if (project != null && lab != null)
			return project + " \u2014 " + lab;
		if (project != null)
			return project;
		if (lab != null)
			return lab;
		return null;
	}

	/**
	 * License precedence: a user-entered "License" property, mapped to a URL when
	 * it is a recognised
	 * SPDX/Creative-Commons identifier (the challenge crate uses a license URL),
	 * else used verbatim.
	 */
	private static String resolveLicense(ExperimentMetadata metadata) {
		String value = property(metadata, "License");
		if (value == null)
			return null;
		String url = LICENSE_URLS.get(value.trim().toUpperCase(java.util.Locale.ROOT));
		return url != null ? url : value.trim();
	}

	/**
	 * First channel AcquisitionMode across the images (imaging-method fallback), or
	 * null.
	 */
	private static String firstAcquisitionMode(ExperimentMetadata metadata) {
		if (metadata == null)
			return null;
		for (ExperimentMetadata.ImageSeries series : allSeries(metadata))
			for (ExperimentMetadata.Channel channel : series.channels)
				if (channel.acquisitionMode != null && !channel.acquisitionMode.trim().isEmpty())
					return channel.acquisitionMode.trim();
		return null;
	}

	private static java.util.List<ExperimentMetadata.ImageSeries> allSeries(ExperimentMetadata metadata) {
		java.util.List<ExperimentMetadata.ImageSeries> all = new java.util.ArrayList<ExperimentMetadata.ImageSeries>();
		all.addAll(metadata.series);
		if (metadata.plate != null)
			for (ExperimentMetadata.PlateWell well : metadata.plate.wells)
				if (well.image != null)
					all.add(well.image);
		return all;
	}

	private static String property(ExperimentMetadata metadata, String key) {
		return metadata == null ? null : metadata.getProperty(key);
	}

	private static String firstNonNull(String... values) {
		for (String value : values)
			if (value != null && !value.trim().isEmpty())
				return value.trim();
		return null;
	}

	private static void writeUtf8(File target, String content) throws java.io.IOException {
		java.io.FileOutputStream stream = new java.io.FileOutputStream(target);
		try {
			java.io.Writer writer = new java.io.OutputStreamWriter(stream, java.nio.charset.Charset.forName("UTF-8"));
			writer.write(content);
			writer.flush();
		} finally {
			stream.close();
		}
	}

	/**
	 * Recognised license identifiers to canonical URL (the challenge crate uses
	 * license URLs).
	 */
	private static final java.util.Map<String, String> LICENSE_URLS = new java.util.HashMap<String, String>();
	static {
		LICENSE_URLS.put("CC0-1.0", "https://creativecommons.org/publicdomain/zero/1.0/");
		LICENSE_URLS.put("CC0", "https://creativecommons.org/publicdomain/zero/1.0/");
		LICENSE_URLS.put("CC-BY-4.0", "https://creativecommons.org/licenses/by/4.0/");
		LICENSE_URLS.put("CC-BY-SA-4.0", "https://creativecommons.org/licenses/by-sa/4.0/");
		LICENSE_URLS.put("CC-BY-NC-4.0", "https://creativecommons.org/licenses/by-nc/4.0/");
		LICENSE_URLS.put("CC-BY-ND-4.0", "https://creativecommons.org/licenses/by-nd/4.0/");
		LICENSE_URLS.put("CC-BY-NC-SA-4.0", "https://creativecommons.org/licenses/by-nc-sa/4.0/");
		LICENSE_URLS.put("MIT", "https://opensource.org/license/mit/");
		LICENSE_URLS.put("BSD-2-CLAUSE", "https://opensource.org/license/bsd-2-clause/");
		LICENSE_URLS.put("BSD-3-CLAUSE", "https://opensource.org/license/bsd-3-clause/");
		LICENSE_URLS.put("APACHE-2.0", "https://www.apache.org/licenses/LICENSE-2.0");
		LICENSE_URLS.put("GPL-3.0", "https://www.gnu.org/licenses/gpl-3.0.html");
	}

	/**
	 * Writes the manifest into the output folder.
	 * 
	 * @param metadata     Parsed measurement.
	 * @param outputFolder Folder the export was written to.
	 * @param dataFiles    Names of the data files already written (OME-XML,
	 *                     OME-TIFF, ...), referenced by
	 *                     the manifest.
	 * @return The manifest file.
	 * @throws Exception If the file cannot be written.
	 */
	static File write(ExperimentMetadata metadata, File outputFolder, List<String> dataFiles) throws Exception {
		StringBuilder json = new StringBuilder();
		json.append("{\n");
		json.append("  \"@context\": \"https://w3id.org/ro/crate/1.1/context\",\n");
		json.append("  \"@graph\": [\n");

		// Descriptor.
		json.append("    {\n");
		json.append("      \"@type\": \"CreativeWork\",\n");
		json.append("      \"@id\": \"ro-crate-metadata.json\",\n");
		json.append("      \"conformsTo\": { \"@id\": \"https://w3id.org/ro/crate/1.1\" },\n");
		json.append("      \"about\": { \"@id\": \"./\" }\n");
		json.append("    },\n");

		// Root dataset.
		json.append("    {\n");
		json.append("      \"@type\": \"Dataset\",\n");
		json.append("      \"@id\": \"./\",\n");
		if (metadata.name != null)
			json.append("      \"name\": ").append(quote(metadata.name)).append(",\n");
		if (metadata.description != null)
			json.append("      \"description\": ").append(quote(metadata.description)).append(",\n");
		if (metadata.acquisitionDate != null) {
			// ISO-8601 date; RO-Crate expects datePublished as a string.
			json.append("      \"datePublished\": ")
					.append(quote(new java.text.SimpleDateFormat("yyyy-MM-dd").format(metadata.acquisitionDate)))
					.append(",\n");
		}
		String experimenter = metadata.getProperty("Operator");
		if (experimenter == null)
			experimenter = metadata.getProperty("User");
		if (experimenter != null)
			json.append("      \"author\": ").append(quote(experimenter)).append(",\n");

		json.append("      \"hasPart\": [\n");
		for (int i = 0; i < dataFiles.size(); i++) {
			String id = dataFiles.get(i).toLowerCase().contains(".zarr") ? dataFiles.get(i) + "/" : dataFiles.get(i);
			json.append("        { \"@id\": ").append(quote(id)).append(" }");
			json.append(i < dataFiles.size() - 1 ? ",\n" : "\n");
		}
		json.append("      ]\n");
		json.append("    }");

		// One entity per data file. OME-Zarr stores are directories, so they are typed
		// Dataset (with a
		// trailing slash on the @id, per RO-Crate convention for directories);
		// everything else is a File.
		for (String dataFile : dataFiles) {
			boolean isZarr = dataFile.toLowerCase().contains(".zarr");
			json.append(",\n    {\n");
			json.append("      \"@type\": ").append(isZarr ? "\"Dataset\"" : "\"File\"").append(",\n");
			json.append("      \"@id\": ").append(quote(isZarr ? dataFile + "/" : dataFile)).append(",\n");
			json.append("      \"encodingFormat\": ").append(quote(mediaType(dataFile))).append("\n");
			json.append("    }");
		}

		json.append("\n  ]\n}\n");

		File target = new File(outputFolder, FILE_NAME);
		java.io.FileOutputStream stream = new java.io.FileOutputStream(target);
		try {
			java.io.Writer writer = new java.io.OutputStreamWriter(stream, java.nio.charset.Charset.forName("UTF-8"));
			writer.write(json.toString());
			writer.flush();
		} finally {
			stream.close();
		}
		return target;
	}

	private static String mediaType(String fileName) {
		String lower = fileName.toLowerCase();
		if (lower.endsWith(".ome.tiff") || lower.endsWith(".ome.tif"))
			return "image/tiff";
		if (lower.endsWith(".ome.xml") || lower.endsWith(".xml"))
			return "application/xml";
		if (lower.endsWith(".zarr") || lower.contains(".zarr"))
			return "application/vnd.zarr";
		if (lower.endsWith(".useq.json"))
			return "application/json";
		if (lower.endsWith(".useq.yaml") || lower.endsWith(".yaml") || lower.endsWith(".yml"))
			return "application/yaml";
		if (lower.endsWith(".txt"))
			return "text/plain";
		return "application/octet-stream";
	}

	private static String quote(String value) {
		StringBuilder builder = new StringBuilder();
		builder.append('"');
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			switch (character) {
				case '"':
					builder.append("\\\"");
					break;
				case '\\':
					builder.append("\\\\");
					break;
				case '\n':
					builder.append("\\n");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '\t':
					builder.append("\\t");
					break;
				default:
					if (character < 0x20)
						builder.append(String.format("\\u%04x", Integer.valueOf(character)));
					else
						builder.append(character);
			}
		}
		builder.append('"');
		return builder.toString();
	}
}
