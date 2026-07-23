/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.graalpy;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.youscope.common.MessageListener;
import org.youscope.common.measurement.Measurement;
import org.youscope.common.measurement.MeasurementListener;
import org.youscope.common.measurement.MeasurementState;
import org.youscope.common.saving.MeasurementFileLocations;
import org.youscope.common.saving.MeasurementSaver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;

/**
 * Writes a reproducible Python environment snapshot to the measurement
 * results folder when a measurement starts.
 *
 * <h3>What is written</h3>
 * Two files are written to the same folder as {@code configuration.csb}:
 * <dl>
 * <dt>{@code graalpy_environment.txt}</dt>
 * <dd>Human-readable summary: GraalPy version, Python version,
 * all installed packages (pip freeze format), timestamp, and
 * the YouScope GraalPy plugin version.</dd>
 * <dt>{@code graalpy_requirements.txt}</dt>
 * <dd>Standard {@code pip freeze} output that can be passed directly to
 * {@code pip install -r graalpy_requirements.txt} on another machine
 * to reproduce the exact environment.</dd>
 * </dl>
 *
 * <h3>When it runs</h3>
 * A {@link MeasurementListener} registered on the measurement fires when
 * {@link MeasurementState#RUNNING} is reached. The results folder already
 * exists at that point (identical to the custom microplate copy pattern).
 *
 * <h3>Jython engine name alias</h3>
 * {@code ScriptingJobImpl} matches engines by
 * {@code factory.getEngineName().compareToIgnoreCase(scriptEngineName)}.
 * Existing measurements configured with engine name {@code "jython"} must
 * continue to work. This class is not directly responsible for that but it is
 * handled in {@link GraalPyScriptEngineFactory#getNames()} which advertises
 * {@code "jython"} alongside {@code "GraalPy"}.
 * The matching loop in {@code ScriptingJobImpl} uses
 * {@code compareToIgnoreCase}, but it matches only against
 * {@code getEngineName()}. To to match {@code "jython"} we must return
 * {@code "jython"} from {@code getEngineName()} or override the match
 * logic. Since we cannot change {@code ScriptingJobImpl}, the cleanest
 * solution is a second factory registration:
 * {@link GraalPyJythonAliasEngineFactory} registers a factory whose
 * {@code getEngineName()} returns {@code "jython"} but whose
 * {@code getScriptEngine()} returns the same {@link GraalPyScriptEngine}.
 * See that class.
 *
 */
public class GraalPyEnvironmentLogger {
    private static final String ENV_FILE_NAME = "graalpy_environment.txt";
    private static final String REQS_FILE_NAME = "graalpy_requirements.txt";

    private GraalPyEnvironmentLogger() {
    }

    /**
     * Registers a {@link MeasurementListener} on {@code measurement} that
     * writes the environment snapshot to the results folder when the
     * measurement transitions to {@link MeasurementState#RUNNING}.
     *
     * <p>
     * Call this from {@code GraalPyScriptEngineAddonFactory} or from
     * any scripting job initializer that uses GraalPy.
     *
     * @param measurement The measurement being started.
     * @param saver       {@code MeasurementSaver} from
     *                    {@code jobInitializer.getMeasurementSaver()}.
     * @param logger      YouScope logger for status messages.
     */
    public static void registerForMeasurement(
            Measurement measurement,
            MeasurementSaver saver,
            MessageListener logger) {
        try {
            measurement.addMeasurementListener(new MeasurementListener() {
                @Override
                public void measurementStateChanged(MeasurementState oldState,
                        MeasurementState newState) throws RemoteException {
                    if (newState != MeasurementState.RUNNING)
                        return;
                    writeEnvironmentSnapshot(saver, logger);
                }

                @Override
                public void measurementError(Exception e) throws RemoteException {
                    /* no-op */ }

                @Override
                public void measurementStructureModified() throws RemoteException {
                    /* no-op */ }
            });
        } catch (RemoteException e) {
            tryLog(logger, "GraalPyEnvironmentLogger: Could not register listener: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Writes both environment files to the measurement results folder.
     * All errors are logged and swallowed because a snapshot failure must not
     * abort the measurement.
     */
    static void writeEnvironmentSnapshot(MeasurementSaver saver,
            MessageListener logger) {
        try {
            MeasurementFileLocations locs = saver.getLastMeasurementFileLocations();
            if (locs == null) {
                tryLog(logger, "GraalPyEnvironmentLogger: file locations not available.", null);
                return;
            }
            String baseFolder = locs.getMeasurementBaseFolder();
            if (baseFolder == null || baseFolder.isEmpty())
                return;

            Path dir = Paths.get(baseFolder);
            Files.createDirectories(dir);

            EnvironmentInfo info = collectEnvironmentInfo();

            // graalpy_environment.txt
            Path envFile = dir.resolve(ENV_FILE_NAME);
            java.nio.file.Files.write(envFile,
                    info.toHumanReadable().getBytes(StandardCharsets.UTF_8));

            // graalpy_requirements.txt
            Path reqFile = dir.resolve(REQS_FILE_NAME);
            java.nio.file.Files.write(reqFile,
                    info.pipFreeze.getBytes(StandardCharsets.UTF_8));

            tryLog(logger,
                    "GraalPyEnvironmentLogger: Wrote environment snapshot to '"
                            + dir.toAbsolutePath() + "'.",
                    null);
        } catch (Exception e) {
            tryLog(logger, "GraalPyEnvironmentLogger: Could not write snapshot: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Collects Python environment information using a short-lived
     * {@link Context}. Uses the shared Engine to avoid Truffle warm-up.
     */
    static EnvironmentInfo collectEnvironmentInfo() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EnvironmentInfo info = new EnvironmentInfo();
        info.timestamp = new java.util.Date().toString();

        try (Context ctx = Context.newBuilder("python")
                .engine(GraalPyScriptEngineFactory.getSharedEngine())
                .allowAllAccess(true)
                .allowIO(IOAccess.ALL)
                .out(new PrintStream(out, true))
                .err(new PrintStream(out, true))
                .build()) {
            // Python version
            ctx.eval("python",
                    "import polyglot as _p, sys\n" +
                            "_p.export('_py_version', sys.version)\n");
            Value pv = ctx.getPolyglotBindings().getMember("_py_version");
            info.pythonVersion = pv != null ? pv.asString() : "unknown";

            // GraalPy / GraalVM version
            ctx.eval("python",
                    "import polyglot as _p\n" +
                            "try:\n" +
                            "    import sys; _ver = sys.implementation.version\n" +
                            "    _p.export('_graalpy_version',\n" +
                            "              f'{_ver.major}.{_ver.minor}.{_ver.micro}')\n" +
                            "except Exception as _e:\n" +
                            "    _p.export('_graalpy_version', str(_e))\n");
            Value gv = ctx.getPolyglotBindings().getMember("_graalpy_version");
            info.graalPyVersion = gv != null ? gv.asString() : "unknown";

            // pip freeze
            ctx.eval("python",
                    "import polyglot as _p, subprocess, sys\n" +
                            "_proc = subprocess.run(\n" +
                            "    [sys.executable, '-m', 'pip', 'freeze', '--all'],\n" +
                            "    capture_output=True, text=True)\n" +
                            "_p.export('_pip_freeze', _proc.stdout)\n");
            Value pf = ctx.getPolyglotBindings().getMember("_pip_freeze");
            info.pipFreeze = pf != null ? pf.asString() : "";

            // Package count -- Java 7 compatible (no streams, no isBlank)
            int _count = 0;
            for (String _line : info.pipFreeze.split("\n")) {
                String _trimmed = _line.trim();
                if (!_trimmed.isEmpty() && !_trimmed.startsWith("#"))
                    _count++;
            }
            info.packageCount = _count;
        } catch (Exception e) {
            info.error = e.getMessage();
            info.pipFreeze = "# Error collecting environment: " + e.getMessage() + "\n";
        }

        return info;
    }

    static class EnvironmentInfo {
        String timestamp = "";
        String pythonVersion = "";
        String graalPyVersion = "";
        String pipFreeze = "";
        int packageCount = 0;
        String error = null;

        String toHumanReadable() {
            StringBuilder sb = new StringBuilder();
            sb.append("# YouScope GraalPy Environment Snapshot\n");
            sb.append("# Generated: ").append(timestamp).append("\n");
            sb.append("# ").append("─".repeat(60)).append("\n");
            sb.append("# Python version  : ").append(pythonVersion).append("\n");
            sb.append("# GraalPy version : ").append(graalPyVersion).append("\n");
            sb.append("# Packages found  : ").append(packageCount).append("\n");
            if (error != null)
                sb.append("# WARNING         : ").append(error).append("\n");
            sb.append("# ").append("─".repeat(60)).append("\n");
            sb.append("#\n");
            sb.append("# To recreate this environment on another machine:\n");
            sb.append("#   pip install -r graalpy_requirements.txt\n");
            sb.append("# (use the graalpy_requirements.txt file in this folder)\n");
            sb.append("#\n");
            sb.append("# ").append("─".repeat(60)).append("\n");
            sb.append("# Installed packages (pip freeze output):\n");
            sb.append("# ").append("─".repeat(60)).append("\n");
            sb.append(pipFreeze);
            return sb.toString();
        }
    }

    private static void tryLog(MessageListener logger, String msg, Throwable t) {
        if (logger == null)
            return;
        try {
            if (t != null)
                logger.sendErrorMessage(msg, t);
            else
                logger.sendMessage(msg);
        } catch (RemoteException ignored) {
        }
    }
}
