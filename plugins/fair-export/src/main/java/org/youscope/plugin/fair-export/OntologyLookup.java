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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.youscope.plugin.fairexport.model.ExperimentMetadata;

/**
 * Resolves controlled-vocabulary identifiers for the in-store RO-Crate:
 * NCBI taxon IDs for the
 * organism and FBbi (Biological Imaging Methods Ontology) IDs for the imaging
 * method.
 * <p>
 * The OME-NGFF validator only turns its "Organism" and "Imaging method" rows
 * green when the crate
 * carries an actual ontology {@code @id} ({@code NCBI:txid...} /
 * {@code obo:FBbi_...}). A plain species
 * or modality name is shown as "Not found". YouScope's FAIR metadata usually
 * holds only the free-text
 * name, so this class bridges the two.
 * </p>
 * <p>
 * <b>We never invent an ID.</b> Resolution order for each field is:
 * </p>
 * <ol>
 * <li>an explicit ID the user entered in the FAIR metadata (e.g. a
 * {@code "Species NCBI ID"} /
 * {@code "Imaging method FBbi ID"} property): used verbatim;</li>
 * <li>a lookup of the free-text name against a small built-in table of common
 * model organisms and
 * acquisition modes;</li>
 * <li>otherwise no ID and the name is still emitted as plain text, and the
 * caller
 * records a warning so
 * the user knows the row will read "Not found" until an ID is supplied.</li>
 * </ol>
 * <p>
 * The built-in tables are intentionally small and cover only unambiguous,
 * widely-used terms; anything
 * not listed simply falls through to plain text rather than risking a wrong
 * identifier.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
final class OntologyLookup {
	/**
	 * Property names a user may set to override the organism ID directly (checked
	 * in order, case-insensitive).
	 */
	private static final String[] ORGANISM_ID_KEYS = {
			"Species NCBI ID", "Organism NCBI ID", "NCBI taxon ID", "NCBI ID", "Organism ID", "taxon_id"
	};
	/** Property names a user may set to override the imaging-method ID directly. */
	private static final String[] MODALITY_ID_KEYS = {
			"Imaging method FBbi ID", "Imaging method ID", "FBbi ID", "Modality FBbi ID", "Modality ID"
	};
	/** Property names holding the free-text species/organism name. */
	private static final String[] ORGANISM_NAME_KEYS = { "Species", "Organism" };
	/** Property names holding the free-text imaging method / acquisition mode. */
	private static final String[] MODALITY_NAME_KEYS = { "Imaging method", "Imaging modality", "Modality",
			"Acquisition mode" };

	/**
	 * name (lower-case) -> NCBI taxon compact id, e.g. "NCBI:txid4932". Small,
	 * unambiguous entries only.
	 */
	private static final Map<String, String> ORGANISM_TABLE = new LinkedHashMap<String, String>();
	/** name (lower-case) -> FBbi compact id, e.g. "obo:FBbi_00000246". */
	private static final Map<String, String> MODALITY_TABLE = new LinkedHashMap<String, String>();
	static {
		ORGANISM_TABLE.put("saccharomyces cerevisiae", "NCBI:txid4932");
		ORGANISM_TABLE.put("s. cerevisiae", "NCBI:txid4932");
		ORGANISM_TABLE.put("budding yeast", "NCBI:txid4932");
		ORGANISM_TABLE.put("schizosaccharomyces pombe", "NCBI:txid4896");
		ORGANISM_TABLE.put("escherichia coli", "NCBI:txid562");
		ORGANISM_TABLE.put("e. coli", "NCBI:txid562");
		ORGANISM_TABLE.put("homo sapiens", "NCBI:txid9606");
		ORGANISM_TABLE.put("human", "NCBI:txid9606");
		ORGANISM_TABLE.put("mus musculus", "NCBI:txid10090");
		ORGANISM_TABLE.put("mouse", "NCBI:txid10090");
		ORGANISM_TABLE.put("rattus norvegicus", "NCBI:txid10116");
		ORGANISM_TABLE.put("rat", "NCBI:txid10116");
		ORGANISM_TABLE.put("danio rerio", "NCBI:txid7955");
		ORGANISM_TABLE.put("zebrafish", "NCBI:txid7955");
		ORGANISM_TABLE.put("drosophila melanogaster", "NCBI:txid7227");
		ORGANISM_TABLE.put("caenorhabditis elegans", "NCBI:txid6239");
		ORGANISM_TABLE.put("c. elegans", "NCBI:txid6239");
		ORGANISM_TABLE.put("arabidopsis thaliana", "NCBI:txid3702");

		// FBbi (Biological Imaging Methods Ontology) terms for common light-microscopy
		// modalities.
		MODALITY_TABLE.put("widefield", "obo:FBbi_00000246");
		MODALITY_TABLE.put("wide-field", "obo:FBbi_00000246");
		MODALITY_TABLE.put("wide field", "obo:FBbi_00000246");
		MODALITY_TABLE.put("widefield fluorescence", "obo:FBbi_00000246");
		MODALITY_TABLE.put("fluorescence", "obo:FBbi_00000246");
		MODALITY_TABLE.put("fluorescence microscopy", "obo:FBbi_00000246");
		MODALITY_TABLE.put("epifluorescence", "obo:FBbi_00000246");
		MODALITY_TABLE.put("confocal", "obo:FBbi_00000251");
		MODALITY_TABLE.put("confocal microscopy", "obo:FBbi_00000251");
		MODALITY_TABLE.put("spinning disk confocal", "obo:FBbi_00000253");
		MODALITY_TABLE.put("spinning disk", "obo:FBbi_00000253");
		MODALITY_TABLE.put("brightfield", "obo:FBbi_00000243");
		MODALITY_TABLE.put("bright-field", "obo:FBbi_00000243");
		MODALITY_TABLE.put("bright field", "obo:FBbi_00000243");
		MODALITY_TABLE.put("transmitted light", "obo:FBbi_00000243");
		MODALITY_TABLE.put("phase contrast", "obo:FBbi_00000247");
		MODALITY_TABLE.put("dic", "obo:FBbi_00000249");
		MODALITY_TABLE.put("differential interference contrast", "obo:FBbi_00000249");
		MODALITY_TABLE.put("spim", "obo:FBbi_00000369");
		MODALITY_TABLE.put("light sheet", "obo:FBbi_00000369");
		MODALITY_TABLE.put("lightsheet", "obo:FBbi_00000369");
		MODALITY_TABLE.put("tirf", "obo:FBbi_00000371");
		MODALITY_TABLE.put("total internal reflection fluorescence", "obo:FBbi_00000371");
		MODALITY_TABLE.put("two photon", "obo:FBbi_00000252");
		MODALITY_TABLE.put("two-photon", "obo:FBbi_00000252");
		MODALITY_TABLE.put("multiphoton", "obo:FBbi_00000252");
	}

	/**
	 * Resolved term: a human-readable {@link #name} and, when known, a
	 * controlled-vocabulary {@link #id}.
	 */
	static final class Term {
		final String name;
		/**
		 * Compact ontology id (e.g. "NCBI:txid4932" / "obo:FBbi_00000246"), or null if
		 * unresolved.
		 */
		final String id;
		/**
		 * True if the id came from a user-supplied override rather than the lookup
		 * table.
		 */
		final boolean fromOverride;

		Term(String name, String id, boolean fromOverride) {
			this.name = name;
			this.id = id;
			this.fromOverride = fromOverride;
		}

		boolean hasId() {
			return id != null && !id.isEmpty();
		}
	}

	private OntologyLookup() {
	}

	/**
	 * Resolves the organism term from the measurement's FAIR metadata.
	 * 
	 * @param metadata Parsed measurement (may be null).
	 * @return the resolved term, or null if no organism name/ID is present at all.
	 */
	static Term organism(ExperimentMetadata metadata) {
		if (metadata == null)
			return null;
		String override = firstProperty(metadata, ORGANISM_ID_KEYS);
		String name = firstProperty(metadata, ORGANISM_NAME_KEYS);
		if (override != null)
			return new Term(name != null ? name : override, normalizeTaxon(override), true);
		if (name == null)
			return null;
		String id = ORGANISM_TABLE.get(name.trim().toLowerCase(Locale.ROOT));
		return new Term(name, id, false);
	}

	/**
	 * Resolves the imaging-method term. Falls back to the channel
	 * {@code AcquisitionMode} when no
	 * explicit imaging-method metadata property is present.
	 * 
	 * @param metadata                Parsed measurement (may be null).
	 * @param acquisitionModeFallback Acquisition mode read from the OME channels
	 *                                (e.g. "WideField"), or null.
	 * @return the resolved term, or null if nothing is available.
	 */
	static Term imagingMethod(ExperimentMetadata metadata, String acquisitionModeFallback) {
		String override = metadata == null ? null : firstProperty(metadata, MODALITY_ID_KEYS);
		String name = metadata == null ? null : firstProperty(metadata, MODALITY_NAME_KEYS);
		if (name == null)
			name = acquisitionModeFallback;
		if (override != null)
			return new Term(name != null ? name : override, normalizeFbbi(override), true);
		if (name == null)
			return null;
		String id = MODALITY_TABLE.get(name.trim().toLowerCase(Locale.ROOT));
		return new Term(name, id, false);
	}

	// Normalisation of user-supplied ids into the compact forms the validator's
	// @context expects

	/**
	 * Accepts "4932", "txid4932", "NCBI:txid4932", "NCBITaxon_4932" →
	 * "NCBI:txid4932".
	 */
	private static String normalizeTaxon(String raw) {
		if (raw == null)
			return null;
		String value = raw.trim();
		if (value.isEmpty())
			return null;
		String digits = value.replaceAll("(?i).*?(\\d+)\\s*$", "$1");
		if (digits.matches("\\d+"))
			return "NCBI:txid" + digits;
		return value; // already some other explicit form; use as-is
	}

	/**
	 * Accepts "FBbi_00000246", "FBbi:00000246", "obo:FBbi_00000246", "00000246" →
	 * "obo:FBbi_00000246".
	 */
	private static String normalizeFbbi(String raw) {
		if (raw == null)
			return null;
		String value = raw.trim();
		if (value.isEmpty())
			return null;
		String digits = value.replaceAll("(?i).*?FBbi[_:]?(\\d+)\\s*$", "$1");
		if (!digits.matches("\\d+"))
			digits = value.replaceAll(".*?(\\d{6,})\\s*$", "$1");
		if (digits.matches("\\d+"))
			return "obo:FBbi_" + digits;
		return value;
	}

	private static String firstProperty(ExperimentMetadata metadata, String[] keys) {
		for (String key : keys) {
			String value = metadata.getProperty(key);
			if (value != null && !value.trim().isEmpty())
				return value.trim();
		}
		return null;
	}
}
