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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.youscope.clientinterfaces.MetadataCategoryProvider;
import org.youscope.plugin.fairexport.model.ExperimentMetadata;
import org.youscope.plugin.fairexport.ome.OmeMetadataService;
import org.youscope.plugin.fairexport.parser.ExperimentMetadataParser;

/**
 * Runs the FAIR export. Headless: contains no Swing code and no reference to
 * the addon API, so that
 * the same engine serves both entry points: (1) the post-processing dialog, and
 * (2) automatic export at the
 * end of a measurement.
 * <p>
 * That separation is the point. The dialog and the automatic run differ only in
 * who watches the
 * result, and duplicating the pipeline for them would guarantee they drift
 * apart.
 * </p>
 * <p>
 * The engine never throws for metadata problems. Missing or ambiguous values
 * are collected as
 * warnings and written to a report file inside the output folder, because the
 * automatic export runs
 * unattended: a warning nobody sees is a warning that does not exist. Only
 * genuine I/O failures and
 * an unreadable measurement folder produce an unsuccessful result.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class FairExportEngine {
	/** Name of the report written into the output folder on every run. */
	public static final String REPORT_FILE_NAME = "fair-export-report.txt";
	/** Name of the standalone OME-XML written next to the exported data. */
	public static final String OME_XML_FILE_NAME = "metadata.ome.xml";
	/**
	 * Name of the sub-folder created inside the measurement folder when no output
	 * folder is given.
	 */
	public static final String DEFAULT_OUTPUT_FOLDER_NAME = "fair-export";

	/**
	 * Options controlling what is written.
	 */
	public static class Options {
		/** Write OME-TIFF with embedded OME-XML. */
		public boolean writeOmeTiff = true;
		/** Write OME-Zarr. */
		public boolean writeOmeZarr = false;
		/** Write an RO-Crate manifest describing the exported dataset. */
		public boolean writeRoCrate = true;
		/**
		 * Write a useq-schema acquisition plan ({@code sequence.useq.json} /
		 * {@code .yaml}). Only
		 * emitted for cell-detection / feedback measurements; a plain scan is skipped
		 * (with a note) even
		 * when this is on, because its plan is already implied by the OME metadata.
		 */
		public boolean writeUseq = false;
		/**
		 * When true, the in-store RO-Crate is written only at each {@code *.ome.zarr}
		 * root (spec-compliant).
		 * When false (default), a minimal crate is also written into each plate well /
		 * image sub-group, so
		 * the OME-NGFF validator does not report "No ro-crate-metadata.json found" if
		 * the user points it at
		 * a sub-group (e.g. a well {@code A/1}) rather than the store root.
		 */
		public boolean rootOnlyStoreCrate = false;
		/**
		 * Always write the standalone OME-XML, which is cheap and useful for
		 * inspection.
		 */
		public boolean writeOmeXml = true;
		/** Output folder; if null, a sub-folder of the measurement folder is used. */
		public File outputFolder = null;

		/**
		 * OME-TIFF split mode. COMBINED (default) writes one multi-channel OME-TIFF per
		 * image/well, to
		 * match the OME-Zarr model. PER_CHANNEL writes one single-channel OME-TIFF per
		 * channel per
		 * image/well, e.g. needed by tools such as Argolight analysis that expect a
		 * single
		 * channel,
		 * single image or z-stack, not a whole plate.
		 */
		public TiffSplit tiffSplit = TiffSplit.COMBINED;

		/**
		 * Chunk size (x and y) for OME-Zarr. 1024 is a good default; 2048 = one chunk
		 * per full plane.
		 */
		public int zarrChunkSize = 1024;
		/**
		 * If true, use Zarr v3 sharding so many chunks are packed into fewer files
		 * (large images).
		 */
		public boolean zarrSharding = false;
	}

	/** OME-TIFF split mode. */
	public enum TiffSplit {
		/** One multi-channel OME-TIFF per image/well. */
		COMBINED,
		/** One single-channel OME-TIFF per channel per image/well. */
		PER_CHANNEL
	}

	/**
	 * Outcome of an export run.
	 */
	public static class Result {
		/** True if the export completed; warnings may still be present. */
		public boolean successful = false;
		/**
		 * Metadata problems found while parsing, building or validating. Never a reason
		 * to fail.
		 */
		public final List<String> warnings = new ArrayList<String>();
		/** Files written by this run. */
		public final List<File> writtenFiles = new ArrayList<File>();
		/** The failure which aborted the run, or null. */
		public Throwable failure = null;
		/** The parsed measurement, or null if parsing failed. */
		public ExperimentMetadata metadata = null;
		/** One-line QUAREP completeness summary, informational (not a warning). */
		public String completenessSummary = null;
		/** Folder the export was written to, or null. */
		public File outputFolder = null;
	}

	/**
	 * Receives progress information. Implemented by the dialog to drive a progress
	 * bar, and by the
	 * automatic export to write to the measurement log.
	 */
	public interface ProgressListener {
		/**
		 * @param step     Human readable description of the current step.
		 * @param progress Fraction between 0 and 1, or a negative value if unknown.
		 */
		void progress(String step, double progress);
	}

	/**
	 * Classes the export needs at runtime, mapped to the jar which provides them.
	 * Checked before any
	 * work is done, because the JVM reports a missing jar as a bare
	 * NoClassDefFoundError whose message
	 * is only the class name and which tells a user nothing about what to install.
	 */
	private static final String[][] REQUIRED_CLASSES = new String[][] {
			{ "loci.formats.meta.IMetadata", "formats-api.jar" },
			{ "loci.formats.services.OMEXMLService", "formats-api.jar" },
			{ "loci.common.services.ServiceFactory", "ome-common.jar" },
			{ "ome.units.UNITS", "ome-common.jar" },
			{ "ome.xml.model.MapPair", "ome-xml.jar" },
			{ "ome.xml.model.primitives.Timestamp", "ome-xml.jar" },
			{ "org.joda.time.DateTime", "joda-time.jar" },
			{ "loci.formats.services.OMEXMLServiceImpl", "formats-bsd.jar" }
	};

	/**
	 * Verifies that the Bio-Formats libraries are reachable.
	 * <p>
	 * {@code OMEXMLServiceImpl} deserves special mention: it is resolved
	 * reflectively by
	 * {@code ServiceFactory}, so omitting formats-bsd compiles and links cleanly
	 * and only fails deep
	 * inside the builder.
	 * </p>
	 * 
	 * @return Null if everything is present, otherwise a message naming what is
	 *         missing.
	 */
	public static String checkRuntimeLibraries() {
		List<String> missingJars = new ArrayList<String>();
		List<String> missingClasses = new ArrayList<String>();
		for (String[] entry : REQUIRED_CLASSES) {
			try {
				Class.forName(entry[0], false, BioFormatsClassLoader.get());
			} catch (@SuppressWarnings("unused") Throwable e) {
				missingClasses.add(entry[0]);
				if (!missingJars.contains(entry[1]))
					missingJars.add(entry[1]);
			}
		}
		if (missingClasses.isEmpty())
			return null;

		StringBuilder builder = new StringBuilder();
		builder.append("The Bio-Formats libraries are not on the classpath, so no OME metadata can be written.\n\n");
		builder.append("Missing class");
		builder.append(missingClasses.size() == 1 ? ": " : "es: ");
		for (int i = 0; i < missingClasses.size(); i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(missingClasses.get(i));
		}
		builder.append("\n\nProvided by: ");
		for (int i = 0; i < missingJars.size(); i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(missingJars.get(i));
		}
		builder.append("\n\nTo fix this, place the jars in the lib folder of the YouScope installation, next to ");
		builder.append("youscope.jar, and make sure every one of them is named on the Class-Path attribute in the ");
		builder.append("manifest of the fair-export plugin. A jar which is named there but not present is ignored ");
		builder.append("silently by the JVM. Running the Gradle task printDeployedLibs lists the required entries.");
		return builder.toString();
	}

	private final MetadataCategoryProvider categoryProvider;

	/**
	 * Constructor.
	 * 
	 * @param categoryProvider Provider used to attach a category to every metadata
	 *                         property; the
	 *                         category becomes the namespace of the exported map
	 *                         annotation. May be null.
	 */
	public FairExportEngine(MetadataCategoryProvider categoryProvider) {
		this.categoryProvider = categoryProvider;
	}

	/**
	 * Exports a measurement.
	 * 
	 * @param measurementFolder Folder of the saved measurement.
	 * @param options           Export options, or null for the defaults.
	 * @param listener          Progress listener, or null.
	 * @return The result. Check {@link Result#successful} and
	 *         {@link Result#warnings}.
	 */
	public Result export(File measurementFolder, Options options, ProgressListener listener) {
		Result result = new Result();
		Options effectiveOptions = options == null ? new Options() : options;

		try {
			// Checked before parsing so that the user gets an actionable message instead of
			// a
			// NoClassDefFoundError after the measurement has already been read.
			String libraryProblem = checkRuntimeLibraries();
			if (libraryProblem != null)
				throw new IllegalStateException(libraryProblem);

			report(listener, "Reading measurement", 0.0);
			ExperimentMetadataParser parser = new ExperimentMetadataParser(categoryProvider);
			result.metadata = parser.parse(measurementFolder);
			result.warnings.addAll(parser.getWarnings());

			report(listener, "Building OME metadata", 0.3);
			// All Bio-Formats contact happens inside the isolated classloader, behind this
			// bridge, so
			// that the sealed JAI packages never reach the classpath the server scans at
			// startup.
			OmeMetadataService omeService = OmeServiceLoader.load();
			String omeXml = omeService.buildOmeXml(result.metadata, result.warnings);

			report(listener, "Checking QUAREP-LiMi completeness", 0.4);
			result.completenessSummary = QuarepValidator.getSummaryLine(result.metadata);
			result.warnings.addAll(QuarepValidator.validate(result.metadata));

			result.outputFolder = effectiveOptions.outputFolder != null
					? effectiveOptions.outputFolder
					: new File(measurementFolder, DEFAULT_OUTPUT_FOLDER_NAME);
			if (!result.outputFolder.exists() && !result.outputFolder.mkdirs())
				throw new Exception("Could not create output folder " + result.outputFolder.getAbsolutePath() + ".");

			if (effectiveOptions.writeOmeXml) {
				report(listener, "Writing OME-XML", 0.5);
				File omeXmlFile = new File(result.outputFolder, OME_XML_FILE_NAME);
				writeText(omeXmlFile, omeXml);
				result.writtenFiles.add(omeXmlFile);
			}

			if (effectiveOptions.writeOmeTiff) {
				report(listener, "Writing OME-TIFF", 0.6);
				List<String> tiffFiles = omeService.writeOmeTiff(result.metadata, measurementFolder,
						result.outputFolder, effectiveOptions.tiffSplit == TiffSplit.PER_CHANNEL, result.warnings);
				for (String tiffFile : tiffFiles)
					result.writtenFiles.add(new File(result.outputFolder, tiffFile));
				if (tiffFiles.isEmpty())
					result.warnings.add("No OME-TIFF files were written; see the warnings above for why.");
			}
			if (effectiveOptions.writeOmeZarr) {
				report(listener, "Writing OME-Zarr", 0.75);
				List<String> zarrStores = omeService.writeOmeZarr(result.metadata, measurementFolder,
						result.outputFolder, effectiveOptions.zarrChunkSize, effectiveOptions.zarrSharding,
						result.warnings);
				for (String zarrStore : zarrStores)
					result.writtenFiles.add(new File(result.outputFolder, zarrStore));

				// Write an ro-crate-metadata.json into each store as well as the
				// folder-level one
				// below. The OME-NGFF validator (and the NGFF RO-Crate profile) look for the
				// crate at the
				// root of each .ome.zarr group; without it the validator reports
				// "No ro-crate-metadata.json found". Gated on writeRoCrate (on by default), so
				// a user who
				// unticks RO-Crate gets neither crate. A per-store crate failure is a warning,
				// never fatal.
				if (effectiveOptions.writeRoCrate) {
					for (String zarrStore : zarrStores) {
						File storeFolder = new File(result.outputFolder, zarrStore);
						try {
							String imageName = imageNameForStore(result.metadata, zarrStore);
							File crate = RoCrateWriter.writeStoreCrate(result.metadata, storeFolder, imageName,
									result.warnings);
							result.writtenFiles.add(crate);
							// Also drop a minimal crate into each sub-group (well / image) unless the user
							// asked
							// for root-only, so the validator is satisfied at any level the user selects.
							if (!effectiveOptions.rootOnlyStoreCrate) {
								for (File subGroup : findZarrSubGroups(storeFolder)) {
									String subName = zarrStore + "/"
											+ storeFolder.toPath().relativize(subGroup.toPath()).toString()
													.replace('\\', '/');
									File subCrate = RoCrateWriter.writeSubGroupCrate(subGroup, subName);
									if (subCrate != null)
										result.writtenFiles.add(subCrate);
								}
							}
						} catch (Throwable e) {
							String message = e.getMessage();
							result.warnings.add("Could not write the in-store RO-Crate for \"" + zarrStore
									+ "\": " + (message == null ? e.toString() : message)
									+ ". The OME-NGFF validator may report a missing ro-crate-metadata.json for it.");
						}
					}
				}
			}
			if (effectiveOptions.writeUseq) {
				// The useq acquisition plan is only meaningful for adaptive (cell-detection /
				// feedback) measurements; for a plain scan the plan is already implied by the
				// OME
				// metadata, so skip it with a note rather than writing a trivial sequence.
				if (result.metadata != null && result.metadata.feedbackMeasurement) {
					report(listener, "Writing useq acquisition plan", 0.85);
					List<String> useqFiles = new UseqWriter(System.getProperty("user.dir", "."))
							.write(result.metadata, result.outputFolder, result.warnings);
					for (String useqFile : useqFiles)
						result.writtenFiles.add(new File(result.outputFolder, useqFile));
				} else {
					result.warnings.add("useq export skipped: this is not a cell-detection / feedback "
							+ "measurement, so its acquisition plan is already fully described by the OME "
							+ "metadata. (Enable it only for adaptive measurements.)");
				}
			}
			if (effectiveOptions.writeRoCrate) {
				report(listener, "Writing RO-Crate manifest", 0.9);
				// Reference the data files in the output folder, not only the ones written in
				// this run.
				// This makes RO-Crate correct whether it runs alongside the other formats or on
				// its own
				// (e.g. as a standalone automatic post-processor after OME-TIFF/OME-Zarr were
				// written in
				// a previous run or by a different automatic step).
				java.util.LinkedHashSet<String> dataFileNames = new java.util.LinkedHashSet<String>();
				for (File written : result.writtenFiles) {
					if (!written.getName().equals(RoCrateWriter.FILE_NAME))
						dataFileNames.add(written.getName());
				}
				File[] existing = result.outputFolder.listFiles();
				if (existing != null) {
					for (File file : existing) {
						String name = file.getName();
						String lower = name.toLowerCase();
						if (lower.endsWith(".ome.tiff") || lower.endsWith(".ome.tif")
								|| lower.endsWith(".ome.xml") || lower.contains(".ome.zarr")
								|| lower.endsWith(".useq.json") || lower.endsWith(".useq.yaml"))
							dataFileNames.add(name);
					}
				}
				File manifest = RoCrateWriter.write(result.metadata, result.outputFolder,
						new ArrayList<String>(dataFileNames));
				result.writtenFiles.add(manifest);
			}

			result.successful = true;
		} catch (Throwable e) {
			result.successful = false;
			result.failure = e;
		}

		// The report is written even when the run failed: for an unattended export it
		// is the only
		// trace of what happened.
		try {
			if (result.outputFolder != null && result.outputFolder.isDirectory()) {
				File reportFile = new File(result.outputFolder, REPORT_FILE_NAME);
				writeText(reportFile, buildReport(measurementFolder, result));
				result.writtenFiles.add(reportFile);
			}
		} catch (@SuppressWarnings("unused") Throwable e) {
			// A failure to write the report must not mask the actual result.
		}

		report(listener, result.successful ? "Finished" : "Failed", 1.0);
		return result;
	}

	/**
	 * Finds sub-groups of an OME-Zarr store that carry their own {@code zarr.json}
	 * group node (plate
	 * wells and image/field groups). Used to place a minimal RO-Crate at every
	 * level the validator might
	 * be pointed at. The store root itself is excluded (it gets the full crate).
	 * The walk recurses through
	 * intermediate path segments that have no {@code zarr.json} of their own (e.g.
	 * the plate row "A",
	 * which is just a directory) but never descends into array nodes (their
	 * children are chunk shards).
	 */
	private static List<File> findZarrSubGroups(File storeRoot) {
		List<File> groups = new ArrayList<File>();
		collectZarrGroups(storeRoot, storeRoot, groups, 0, 5);
		return groups;
	}

	private static void collectZarrGroups(File storeRoot, File dir, List<File> out, int depth, int maxDepth) {
		if (depth > maxDepth)
			return;
		File[] children = dir.listFiles();
		if (children == null)
			return;
		for (File child : children) {
			if (!child.isDirectory() || child.getName().startsWith("."))
				continue;
			String nodeType = zarrNodeType(child);
			if ("array".equals(nodeType))
				continue; // do not add or descend: an array's children are chunk shards
			if ("group".equals(nodeType) && !child.equals(storeRoot))
				out.add(child);
			// Recurse into groups AND into intermediate directories with no zarr.json (e.g.
			// a plate row).
			collectZarrGroups(storeRoot, child, out, depth + 1, maxDepth);
		}
	}

	/**
	 * @return "group", "array", or null if the directory has no readable
	 *         {@code zarr.json}.
	 */
	private static String zarrNodeType(File dir) {
		File zarrJson = new File(dir, "zarr.json");
		if (!zarrJson.isFile())
			return null;
		try {
			String text = new String(java.nio.file.Files.readAllBytes(zarrJson.toPath()),
					java.nio.charset.Charset.forName("UTF-8"));
			// lightweight: avoid a JSON dependency; look for the node_type value
			int idx = text.indexOf("\"node_type\"");
			if (idx >= 0) {
				int colon = text.indexOf(':', idx);
				if (colon >= 0) {
					String rest = text.substring(colon + 1).trim();
					if (rest.startsWith("\"array\""))
						return "array";
					if (rest.startsWith("\"group\""))
						return "group";
				}
			}
			// zarr.json without node_type: treat as a group (OME groups always declare it,
			// arrays too,
			// but a missing value is safer treated as a non-array group so we still
			// recurse).
			return "group";
		} catch (@SuppressWarnings("unused") java.io.IOException e) {
			return null;
		}
	}

	private static void report(ProgressListener listener, String step, double progress) {
		if (listener != null)
			listener.progress(step, progress);
	}

	/**
	 * Best-effort human-readable name for the image or plate held by a store, for
	 * the in-store RO-Crate.
	 * <p>
	 * The store name is a file-system-safe transform of the plate or series name,
	 * so an exact reverse
	 * lookup is not always possible; matching on the safe form is enough for the
	 * crate's {@code name}
	 * field, and {@link RoCrateWriter#writeStoreCrate} falls back to the store
	 * folder name if this
	 * returns null.
	 * </p>
	 */
	private static String imageNameForStore(ExperimentMetadata metadata, String storeName) {
		if (metadata == null || storeName == null)
			return null;
		String bare = storeName.toLowerCase().endsWith(".ome.zarr")
				? storeName.substring(0, storeName.length() - ".ome.zarr".length())
				: storeName;
		if (metadata.plate != null && metadata.plate.name != null)
			return metadata.plate.name;
		for (ExperimentMetadata.ImageSeries series : metadata.series) {
			if (series.name == null)
				continue;
			// match either the raw name or a safe-name-ish comparison (spaces/punctuation
			// folded away)
			if (series.name.equals(bare) || sameAfterSafening(series.name, bare))
				return series.name;
		}
		return null;
	}

	/**
	 * Compares two names after reducing each to [A-Za-z0-9_-], mirroring the
	 * store-name safening.
	 */
	private static boolean sameAfterSafening(String a, String b) {
		return safen(a).equals(safen(b));
	}

	/**
	 * Mirrors {@code OmeMetadataServiceImpl.toSafeFileName}: keep [A-Za-z0-9._-],
	 * collapse other runs to '_'.
	 */
	private static String safen(String value) {
		if (value == null || value.trim().isEmpty())
			return "image";
		return value.trim().replaceAll("[^a-zA-Z0-9._-]+", "_");
	}

	private static String buildReport(File measurementFolder, Result result) {
		StringBuilder builder = new StringBuilder();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		builder.append("YouScope FAIR export report\n");
		builder.append("===========================\n\n");
		builder.append("Measurement : ").append(measurementFolder.getAbsolutePath()).append('\n');
		builder.append("Exported    : ").append(format.format(new Date())).append('\n');
		builder.append("Result      : ").append(result.successful ? "completed" : "FAILED").append("\n\n");

		if (result.metadata != null) {
			builder.append("Images written as ").append(result.metadata.series.size())
					.append(" OME image(s), one per well and image save name:\n");
			for (ExperimentMetadata.ImageSeries series : result.metadata.series) {
				builder.append("  ").append(series.name)
						.append("  X=").append(series.sizeX).append(" Y=").append(series.sizeY)
						.append(" Z=").append(series.sizeZ).append(" C=").append(series.sizeC)
						.append(" T=").append(series.sizeT)
						.append(channelNames(series))
						.append("  pixel size=")
						.append(series.physicalSizeXMicrons == null ? "unknown" : series.physicalSizeXMicrons + " um")
						.append('\n');
			}
			builder.append('\n');
		}

		if (result.failure != null) {
			builder.append("Failure\n-------\n");
			// NoClassDefFoundError carries only the class name as its message, so fall back
			// to
			// toString() rather than printing a bare "null".
			String message = result.failure.getMessage();
			builder.append(message == null || message.isEmpty()
					? result.failure.toString()
					: result.failure.getClass().getSimpleName() + ": " + message);
			builder.append("\n\n");
		}

		if (result.completenessSummary != null)
			builder.append(result.completenessSummary).append("\n\n");

		if (result.warnings.isEmpty())
			builder.append("No warnings.\n");
		else {
			builder.append("Warnings (").append(result.warnings.size()).append(")\n");
			builder.append("--------\n");
			for (String warning : result.warnings)
				builder.append("  - ").append(warning).append('\n');
			builder.append('\n');
			builder.append("Warnings do not indicate a failed export. Values which could not be determined\n");
			builder.append("were left out of the metadata rather than guessed. Fixing the cause and running\n");
			builder.append("the export again from the post-processing menu will produce a more complete result;\n");
			builder.append("no re-acquisition is needed.\n");
		}

		if (!result.writtenFiles.isEmpty()) {
			builder.append("\nFiles written\n-------------\n");
			for (File file : result.writtenFiles)
				builder.append("  ").append(file.getName()).append('\n');
		}

		// How to validate / view the OME-Zarr (the live local URL only exists while
		// the local server runs, so point at the tool/results action for that).
		boolean hasZarr = false;
		for (File file : result.writtenFiles) {
			if (file.getName().toLowerCase().endsWith(".ome.zarr")) {
				hasZarr = true;
				break;
			}
		}
		if (hasZarr) {
			builder.append("\nValidate / view the OME-Zarr\n----------------------------\n");
			builder.append("Use \"Validate / view OME-Zarr...\" in the FAIR Export tool (or the post-\n");
			builder.append("processing dialog) to serve this folder locally and open it in the OME-NGFF\n");
			builder.append("validator; from there you can open it in vizarr, neuroglancer and vol-e, or\n");
			builder.append("open it directly in napari with the \"Open in napari\" button. The validator is:\n");
			builder.append("  https://ome.github.io/ome-ngff-validator/\n");
			builder.append("(A local web server is started on demand because browser viewers fetch the\n");
			builder.append("data over http; it is not running while you read this file.)\n\n");
			builder.append("Set fairexport_open_launcher=1 in graalpy_config.txt to instead open a local\n");
			builder.append("launcher page listing every store with an \"Open in napari\" button that opens\n");
			builder.append("napari for real (a browser page cannot spawn napari; the local server does it\n");
			builder.append("on the page's behalf). That page works offline; the web viewers still need\n");
			builder.append("internet unless a self-hosted build is configured via\n");
			builder.append("fairexport_self_hosted_validator.\n");
		}
		return builder.toString();
	}

	private static void writeText(File file, String content) throws Exception {
		FileOutputStream stream = new FileOutputStream(file);
		try {
			Writer writer = new OutputStreamWriter(stream, Charset.forName("UTF-8"));
			writer.write(content);
			writer.flush();
		} finally {
			stream.close();
		}
	}

	/**
	 * Formats the channel names of a series for the report, e.g. " (Cit, BF)".
	 */
	private static String channelNames(ExperimentMetadata.ImageSeries series) {
		if (series.channels.isEmpty())
			return "";
		StringBuilder builder = new StringBuilder(" (");
		for (int i = 0; i < series.channels.size(); i++) {
			if (i > 0)
				builder.append(", ");
			String name = series.channels.get(i).name;
			builder.append(name == null ? "channel " + i : name);
		}
		return builder.append(")").toString();
	}
}
