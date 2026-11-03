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

import javax.swing.Icon;

import org.youscope.addon.AddonException;
import org.youscope.addon.AddonMetadata;
import org.youscope.addon.AddonUI;
import org.youscope.addon.postprocessing.AutomaticPostProcessorFactory;
import org.youscope.addon.postprocessing.ConfigurablePostProcessorFactory;
import org.youscope.addon.postprocessing.PostProcessorAddonFactory;
import org.youscope.clientinterfaces.MetadataCategoryProvider;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.saving.MeasurementFileLocations;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.ImageLoadingTools;

/**
 * Factory registering the FAIR export post-processor.
 * <p>
 * Discovered through {@link java.util.ServiceLoader}: the file
 * {@code META-INF/services/org.youscope.addon.postprocessing.PostProcessorAddonFactory}
 * names this
 * class. No other registration is required.
 * </p>
 * <p>
 * The export is a post-processor rather than a save-settings addon on purpose.
 * It reads a finished
 * measurement folder and writes new files beside it, so it can be re-run any
 * number of times without
 * re-acquiring, and a measurement acquired before the plugin existed can still
 * be exported. The
 * acquisition path stays untouched.
 * </p>
 * <p>
 * The factory additionally implements {@link AutomaticPostProcessorFactory},
 * which makes the export
 * selectable as an automatic step in the measurement wizard. Both paths run the
 * same
 * {@link FairExportEngine}, so the interactive and the unattended export cannot
 * drift apart.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class FairExportFactory
		implements PostProcessorAddonFactory, AutomaticPostProcessorFactory, ConfigurablePostProcessorFactory {
	/**
	 * Type identifier of the combined FAIR export post-processor (all enabled
	 * formats). Kept as the
	 * primary identifier for the interactive post-processing dialog and the Tools
	 * entry.
	 */
	public static final String TYPE_IDENTIFIER = "YouScope.FairExport.OmeExport";
	/**
	 * Automatic post-processor writing only OME-TIFF (+ the standalone OME-XML).
	 */
	public static final String TYPE_IDENTIFIER_TIFF = "YouScope.FairExport.OmeTiff";
	/** Automatic post-processor writing only OME-Zarr. */
	public static final String TYPE_IDENTIFIER_ZARR = "YouScope.FairExport.OmeZarr";
	/** Automatic post-processor writing only the RO-Crate manifest. */
	public static final String TYPE_IDENTIFIER_ROCRATE = "YouScope.FairExport.RoCrate";
	/**
	 * Prefix of a fully-configured automatic export. The remainder of the
	 * identifier is a query string
	 * encoding the chosen formats and their options, e.g.
	 * {@code YouScope.FairExport.Configured?tiff=1&zarr=1&rocrate=1&split=combined&chunk=1024&shard=0}.
	 * Encoding the options into the identifier lets the full export definition live
	 * in the measurement
	 * configuration (configuration.csb) and be chosen up front with the protocol,
	 * rather than being set
	 * in a later manual re-run. The core stores automatic post-processors as
	 * identifier strings, so no
	 * core change is needed.
	 */
	public static final String TYPE_IDENTIFIER_CONFIGURED_PREFIX = "YouScope.FairExport.Configured";

	private static final String[] SUPPORTED_TYPE_IDENTIFIERS = new String[] {
			TYPE_IDENTIFIER, TYPE_IDENTIFIER_CONFIGURED_PREFIX,
			TYPE_IDENTIFIER_TIFF, TYPE_IDENTIFIER_ZARR, TYPE_IDENTIFIER_ROCRATE };

	@Override
	public AddonUI<? extends AddonMetadata> createPostProcessorUI(String typeIdentifier, YouScopeClient client,
			YouScopeServer server, MeasurementFileLocations measurementFileLocations) throws AddonException {
		if (!isSupportingTypeIdentifier(typeIdentifier))
			throw new AddonException(
					"Post-processor type identifier " + typeIdentifier + " is not supported by this factory.");
		if (measurementFileLocations == null)
			throw new AddonException(
					"No measurement file locations provided; the FAIR export needs a saved measurement folder.");
		// The interactive dialog always offers all formats via its own checkboxes, so
		// it is provided for
		// the combined identifier. The per-format identifiers exist for the automatic
		// wizard checkboxes.
		return new FairExportAddonUI(client, server, measurementFileLocations);
	}

	@Override
	public String[] getSupportedTypeIdentifiers() {
		return SUPPORTED_TYPE_IDENTIFIERS.clone();
	}

	@Override
	public boolean isSupportingTypeIdentifier(String typeIdentifier) {
		if (typeIdentifier != null && typeIdentifier.startsWith(TYPE_IDENTIFIER_CONFIGURED_PREFIX))
			return true;
		for (String supported : SUPPORTED_TYPE_IDENTIFIERS) {
			if (supported.equals(typeIdentifier))
				return true;
		}
		return false;
	}

	@Override
	public AddonMetadata getPostProcessorMetadata(String typeIdentifier) throws AddonException {
		if (!isSupportingTypeIdentifier(typeIdentifier))
			throw new AddonException(
					"Post-processor type identifier " + typeIdentifier + " is not supported by this factory.");
		return new FairExportMetadata(typeIdentifier);
	}

	// Automatic execution

	/**
	 * Builds the export options for an automatic identifier: one format each, or
	 * all three for the
	 * combined identifier. RO-Crate references whatever data files are present in
	 * the output folder, so
	 * if the user ticks RO-Crate together with the others they are written first in
	 * the same run; ticked
	 * alone it manifests the files already there.
	 */
	private static FairExportEngine.Options optionsForType(String typeIdentifier) {
		if (typeIdentifier != null && typeIdentifier.startsWith(TYPE_IDENTIFIER_CONFIGURED_PREFIX))
			return FairExportOptionsCodec.decode(typeIdentifier);

		FairExportEngine.Options options = new FairExportEngine.Options();
		if (TYPE_IDENTIFIER_TIFF.equals(typeIdentifier)) {
			options.writeOmeTiff = true;
			options.writeOmeZarr = false;
			options.writeRoCrate = false;
			options.writeOmeXml = true;
		} else if (TYPE_IDENTIFIER_ZARR.equals(typeIdentifier)) {
			options.writeOmeTiff = false;
			options.writeOmeZarr = true;
			options.writeRoCrate = false;
			options.writeOmeXml = true;
		} else if (TYPE_IDENTIFIER_ROCRATE.equals(typeIdentifier)) {
			options.writeOmeTiff = false;
			options.writeOmeZarr = false;
			options.writeRoCrate = true;
			options.writeOmeXml = true;
		} else {
			// Combined: write everything.
			options.writeOmeTiff = true;
			options.writeOmeZarr = true;
			options.writeRoCrate = true;
			options.writeOmeXml = true;
		}
		return options;
	}

	@Override
	public boolean isSupportingAutomaticExecution(String typeIdentifier) {
		// The configured identifier is the automatic export; its options are chosen in
		// the contributed
		// wizard panel. The three per-format identifiers and the runtime-encoded
		// configured identifiers
		// remain runnable for backward compatibility and for configurations written by
		// the panel.
		// Offered to the wizard: only the configured identifier, which the contributed
		// panel renders
		// with per-format options. The per-format identifiers remain fully runnable
		// (the automatic
		// runner calls runPostProcessor for whatever identifiers a configuration
		// contains) but are not
		// presented as separate wizard checkboxes, so the wizard shows a single, richer
		// FAIR panel.
		return typeIdentifier != null && typeIdentifier.startsWith(TYPE_IDENTIFIER_CONFIGURED_PREFIX);
	}

	// Configurable execution (contributes a wizard panel with options)

	@Override
	public boolean isConfigurable(String typeIdentifier) {
		// We contribute a single configuration panel, advertised against the configured
		// prefix. Only
		// this identifier is rendered as our panel; the per-format identifiers are
		// legacy/plain.
		return typeIdentifier != null && typeIdentifier.startsWith(TYPE_IDENTIFIER_CONFIGURED_PREFIX);
	}

	@Override
	public ConfigurablePostProcessorFactory.ConfigurationPanel createConfigurationPanel(String typeIdentifier) {
		return new FairExportConfigurationPanel();
	}

	@Override
	public void runPostProcessor(String typeIdentifier, YouScopeClient client, YouScopeServer server,
			MeasurementFileLocations measurementFileLocations) throws AddonException {
		if (!isSupportingTypeIdentifier(typeIdentifier))
			throw new AddonException(
					"Post-processor type identifier " + typeIdentifier + " is not supported by this factory.");
		if (measurementFileLocations == null)
			throw new AddonException(
					"No measurement file locations provided; the FAIR export needs a saved measurement folder.");

		String folderName = measurementFileLocations.getMeasurementBaseFolder();
		if (folderName == null)
			throw new AddonException("The folder of the finished measurement could not be determined.");
		File measurementFolder = new File(folderName);

		MetadataCategoryProvider categoryProvider = null;
		Object provider = client.getMeasurementMetadataProvider();
		if (provider instanceof MetadataCategoryProvider)
			categoryProvider = (MetadataCategoryProvider) provider;

		// Each automatic identifier writes exactly one format, so any combination the
		// user ticks in the
		// wizard produces exactly those outputs. The combined identifier writes all
		// three.
		FairExportEngine.Options options = optionsForType(typeIdentifier);
		FairExportEngine.Result result = new FairExportEngine(categoryProvider)
				.export(measurementFolder, options, null);

		// The measurement itself finished successfully whatever happened here: the
		// images are already
		// written. A failed export is reported and never escalated beacuse throwing
		// would risk
		// the caller
		// treating the measurement as failed.
		if (!result.successful) {
			client.sendError("FAIR export of measurement " + measurementFolder.getName()
					+ " failed. The measurement data itself is unaffected and the export can be repeated "
					+ "from the post-processing menu.", result.failure);
			return;
		}
		if (!result.warnings.isEmpty()) {
			client.sendMessage("FAIR export of measurement " + measurementFolder.getName() + " completed with "
					+ result.warnings.size() + " warning(s). See "
					+ FairExportEngine.REPORT_FILE_NAME + " in " + result.outputFolder.getName() + ".");
		} else
			client.sendMessage("FAIR export of measurement " + measurementFolder.getName() + " completed.");
	}

	/**
	 * Metadata of the FAIR export post-processor.
	 * <p>
	 * {@link AddonMetadata} is implemented directly rather than through
	 * {@code org.youscope.addon.AddonMetadataAdapter}, so that this class does not
	 * depend on the
	 * adapter's constructor signature. Switching to the adapter later is a drop-in
	 * change.
	 * </p>
	 */
	static class FairExportMetadata implements AddonMetadata {
		private final String typeIdentifier;

		FairExportMetadata() {
			this(TYPE_IDENTIFIER);
		}

		FairExportMetadata(String typeIdentifier) {
			this.typeIdentifier = typeIdentifier;
		}

		@Override
		public String getName() {
			if (TYPE_IDENTIFIER_TIFF.equals(typeIdentifier))
				return "FAIR Export: OME-TIFF";
			if (TYPE_IDENTIFIER_ZARR.equals(typeIdentifier))
				return "FAIR Export: OME-Zarr";
			if (TYPE_IDENTIFIER_ROCRATE.equals(typeIdentifier))
				return "FAIR Export: RO-Crate manifest";
			return "FAIR Export (OME-TIFF, OME-Zarr, RO-Crate)";
		}

		@Override
		public String getDescription() {
			if (TYPE_IDENTIFIER_TIFF.equals(typeIdentifier))
				return "Automatically writes the measurement as OME-TIFF (with embedded OME-XML) after it "
						+ "finishes. Tick this together with OME-Zarr and/or RO-Crate to write several formats. "
						+ "Uses default export settings; for per-channel splitting or other options, run FAIR "
						+ "Export from the Tools menu or the post-processing dialog.";
			if (TYPE_IDENTIFIER_ZARR.equals(typeIdentifier))
				return "Automatically writes the measurement as OME-Zarr (NGFF; an HCS plate for microplate "
						+ "measurements) after it finishes. Requires the OME-Zarr backend jars in the lib folder. "
						+ "Uses default chunk settings; for chunk size or sharding options, run FAIR Export from "
						+ "the Tools menu or the post-processing dialog.";
			if (TYPE_IDENTIFIER_ROCRATE.equals(typeIdentifier))
				return "Automatically writes an RO-Crate manifest describing the exported dataset after the "
						+ "measurement finishes. Tick it together with OME-TIFF and/or OME-Zarr so the manifest "
						+ "references those files; ticked alone it describes whatever is already in the export folder.";
			return "Exports the measurement into community standard formats (OME-TIFF, OME-Zarr, RO-Crate).\n\n"
					+ "The user metadata, the microscope configuration and the image table are combined into a "
					+ "single OME metadata model, embedded into OME-TIFF and OME-Zarr and described by an RO-Crate "
					+ "manifest.\n\n"
					+ "Tip: tick the individual OME-TIFF / OME-Zarr / RO-Crate entries to choose exactly which "
					+ "formats are written automatically after a measurement. For finer options (per-channel "
					+ "OME-TIFF, chunk size, sharding), run FAIR Export from the Tools menu.\n\n"
					+ "The export reads only saved files, so it can be repeated at any time without re-acquiring.";
		}

		@Override
		public String getTypeIdentifier() {
			return typeIdentifier;
		}

		@Override
		public Icon getIcon() {
			return ImageLoadingTools.getResourceIcon("icons/database-export.png", "FAIR Export");
		}

		@Override
		public String[] getClassification() {
			return new String[] { "Export" };
		}
	}
}
