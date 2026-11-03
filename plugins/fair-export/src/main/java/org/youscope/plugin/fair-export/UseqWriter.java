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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.youscope.plugin.fairexport.model.ExperimentMetadata;

/**
 * Writes the useq-schema acquisition plan for a measurement.
 * <p>
 * The mapping from YouScope's {@link ExperimentMetadata} to useq fields is done
 * in Java by
 * {@link UseqPayloadBuilder}; the <em>schema guarantee</em> is done in Python:
 * this class hands the
 * payload to the bundled {@code useq_export.py}, which constructs a real
 * {@code useq.MDASequence} and
 * serialises it to {@code sequence.useq.json} (+ {@code .yaml}). Splitting it
 * this way means the output
 * format is always exactly what the installed {@code useq-schema} produces.
 * </p>
 * <p>
 * Self-contained like the rest of the plugin's Python plumbing: the interpreter
 * and work directory are
 * resolved through {@link FairExportPython} from {@code graalpy_config.txt}
 * (env key
 * {@code fairexport_cpython_executable}, falling back to the shared
 * cell-detection / cpython env). The
 * {@code useq-schema} package must be installed in that env; if it is not, the
 * writer records a warning
 * and writes nothing, exactly like the OME-Zarr backend does when zarr-java is
 * missing and never a fatal
 * error.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
final class UseqWriter {
	/** Resource path of the bundled writer script. */
	private static final String SCRIPT_RESOURCE = "/org/youscope/plugin/fairexport/useq_export.py";
	/**
	 * Base name of the written files (extensions {@code .json} / {@code .yaml} are
	 * appended by the script).
	 */
	static final String BASENAME = "sequence.useq";

	private final String youscopeDir;

	/**
	 * @param youscopeDir YouScope working directory containing
	 *                    {@code graalpy_config.txt}
	 *                    (usually {@code System.getProperty("user.dir")}).
	 */
	UseqWriter(String youscopeDir) {
		this.youscopeDir = youscopeDir == null ? "." : youscopeDir;
	}

	/**
	 * Writes {@code sequence.useq.json} (and {@code .yaml} when PyYAML is present)
	 * into
	 * {@code outputFolder}.
	 * 
	 * @param metadata     Parsed measurement.
	 * @param outputFolder Folder the plan is written into (the FAIR-export output
	 *                     folder).
	 * @param warnings     List to which mapping warnings and any backend problem
	 *                     are added; a missing
	 *                     {@code useq-schema} package or a Python failure is a
	 *                     warning, not an exception.
	 * @return The names of the files written (may be empty if the backend is
	 *         unavailable).
	 */
	List<String> write(ExperimentMetadata metadata, File outputFolder, List<String> warnings) {
		List<String> written = new ArrayList<String>();

		UseqPayloadBuilder builder = new UseqPayloadBuilder();
		String payload = builder.build(metadata);
		if (warnings != null)
			warnings.addAll(builder.getWarnings());

		String python = FairExportPython.resolvePython(youscopeDir);
		String workDir = FairExportPython.resolveWorkDir(youscopeDir);
		String script = workDir + File.separator + "useq_export.py";
		try {
			FairExportPython.extractScript(SCRIPT_RESOURCE, script);
		} catch (IOException e) {
			if (warnings != null)
				warnings.add("useq export skipped: could not extract useq_export.py (" + messageOf(e) + ").");
			return written;
		}

		// Write the payload next to the script so the child reads it from a file
		// (avoids any stdin
		// buffering surprises across platforms).
		File payloadFile = new File(workDir, "useq_payload.json");
		try {
			writeUtf8(payloadFile, payload);
		} catch (IOException e) {
			if (warnings != null)
				warnings.add("useq export skipped: could not write the payload file (" + messageOf(e) + ").");
			return written;
		}

		ProcessBuilder pb = new ProcessBuilder(python, script,
				"--payload", payloadFile.getAbsolutePath(),
				"--out-dir", outputFolder.getAbsolutePath(),
				"--basename", BASENAME);
		pb.redirectErrorStream(true);

		try {
			Process process = pb.start();
			// The script prints "USEQ_WRITTEN <path>" per file and "USEQ_ERROR ..." /
			// "USEQ_NOTE ..."
			// otherwise; collect those so a failure is reported as a readable warning.
			List<String> writtenPaths = new ArrayList<String>();
			List<String> notes = new ArrayList<String>();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("USEQ_WRITTEN "))
					writtenPaths.add(line.substring("USEQ_WRITTEN ".length()).trim());
				else if (line.startsWith("USEQ_ERROR ") || line.startsWith("USEQ_NOTE "))
					notes.add(line);
				else if (!line.isEmpty())
					notes.add(line);
			}
			int exit = process.waitFor();

			if (exit != 0 || writtenPaths.isEmpty()) {
				String detail = notes.isEmpty() ? "no useq files were written" : join(notes);
				if (warnings != null)
					warnings.add("useq export did not complete (" + detail + "). Ensure 'useq-schema' is installed "
							+ "in the configured Python environment (pip install useq-schema).");
				return written;
			}
			for (String path : writtenPaths)
				written.add(new File(path).getName());
			// A YAML note (e.g. PyYAML missing) is informational, not a failure.
			for (String note : notes) {
				if (note.contains("PyYAML"))
					warnings.add("useq: YAML form not written (" + note.replace("USEQ_NOTE ", "").trim()
							+ "). The JSON form is authoritative.");
			}
		} catch (IOException e) {
			if (warnings != null)
				warnings.add("useq export skipped: could not run '" + python + "' (" + messageOf(e)
						+ "). Configure fairexport_cpython_executable in graalpy_config.txt.");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			if (warnings != null)
				warnings.add("useq export was interrupted before it finished.");
		}
		return written;
	}

	private static String messageOf(Throwable t) {
		String m = t.getMessage();
		return m == null || m.isEmpty() ? t.toString() : m;
	}

	private static String join(List<String> lines) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0)
				builder.append("; ");
			builder.append(lines.get(i));
		}
		return builder.toString();
	}

	private static void writeUtf8(File file, String content) throws IOException {
		if (file.getParentFile() != null)
			file.getParentFile().mkdirs();
		OutputStream stream = new java.io.FileOutputStream(file);
		try {
			java.io.Writer writer = new java.io.OutputStreamWriter(stream, Charset.forName("UTF-8"));
			writer.write(content);
			writer.flush();
		} finally {
			stream.close();
		}
	}
}
