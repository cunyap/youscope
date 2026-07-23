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
import org.graalvm.polyglot.Engine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.List;

/**
 * JSR-223 {@link ScriptEngineFactory} for GraalPy (Python 3.11+).
 *
 * <p>
 * Registered via {@code META-INF/services/javax.script.ScriptEngineFactory}
 * so that {@link javax.script.ScriptEngineManager} discovers it automatically
 * when YouScope's plugin classloader loads this JAR.
 *
 * <p>
 * {@link ScriptingJobImpl} matches engines by
 * {@code factory.getEngineName().compareToIgnoreCase(scriptEngineName)}.
 * This factory advertises the name {@code "GraalPy"} so existing
 * Jython-targeted scripts (engine name {@code "jython"}) continue to use
 * the old JAR during a transition period. Users select GraalPy explicitly
 * by choosing engine name {@code "GraalPy"} or {@code "python3"} in the
 * scripting job configuration UI.
 *
 * <p>
 * A single {@link Engine} instance is shared across all scripts in one
 * JVM session to avoid the ~2 s Truffle warm-up penalty on every job
 * execution. Each script execution gets its own {@link Context} so that
 * Python global state is fully isolated between jobs.
 *
 */
public class GraalPyScriptEngineFactory implements ScriptEngineFactory {
    /**
     * Name reported to {@link javax.script.ScriptEngineManager}.
     * Must match what users type in {@code ScriptingJobConfiguration.scriptEngine}.
     */
    static final String ENGINE_NAME = "GraalPy";

    /**
     * Shared Truffle engine. Initialised once, reused for every Context.
     * Lazy to avoid any overhead when the plugin is loaded but not used.
     */
    private static volatile Engine sharedEngine = null;
    private static final Object ENGINE_LOCK = new Object();

    /**
     * Returns (and lazily creates) the shared GraalPy {@link Engine}.
     */
    public static Engine getSharedEngine() {
        if (sharedEngine == null) {
            synchronized (ENGINE_LOCK) {
                if (sharedEngine == null) {
                    sharedEngine = Engine.newBuilder("python")
                            // Suppress "interpreter only" warning on standard JDK
                            .option("engine.WarnInterpreterOnly", "false")
                            .build();
                }
            }
        }
        return sharedEngine;
    }

    /**
     * Shuts down the shared engine. Called by
     * {@link GraalPyScriptEngineAddonFactory} when the YouScope server stops.
     */
    static void shutdownSharedEngine() {
        synchronized (ENGINE_LOCK) {
            if (sharedEngine != null) {
                sharedEngine.close();
                sharedEngine = null;
            }
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public String getEngineVersion() {
        return "GraalPy 24.1 (Python 3.11)";
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("py");
    }

    @Override
    public List<String> getMimeTypes() {
        return Arrays.asList("text/x-python", "application/x-python");
    }

    private static final String STARTUP_GUIDE = "QUICK START:\n"
            + "  event = takeImage(youscopeServer, \"Channel\", \"FITC\", 20)\n"
            + "  displayImage(event)          # show via matplotlib (CPython)\n"
            + "  saveImage(event, \"img.tif\")  # save TIFF\n"
            + "  run_cellpose(event)           # segment cells\n"
            + "\n"
            + "BUILT-IN (no import needed): takeImage  toNumpyImage  displayImage\n"
            + "  saveImage  run_cellpose  exec_in_cpython  spool_for_openbis_upload\n"
            + "  clear / clc / cls  (clear console)\n"
            + "\n"
            + "IMPORTS (transparent -- GraalPy venv first, CPython fallback):\n"
            + "  import numpy as np        import matplotlib.pyplot as plt\n"
            + "  import skimage, imageio   import tifffile, requests\n"
            + "\n"
            + "JAVA INTEROP:\n"
            + "  m = youscopeServer.getMicroscope()\n"
            + "  java.type(\"java.lang.System\").getProperty(\"user.dir\")\n"
            + "\n"
            + "INSTALL PACKAGES (no restart needed after install):\n"
            + "  # PowerShell (replace <YouScope> with install path):\n"
            + "  $uv = '<YouScope>\\uv\\uv.exe'\n"
            + "  $py = '<YouScope>\\cpython-env\\Scripts\\python.exe'\n"
            + "  & $uv pip install napari --python $py   # any CPython package\n"
            + "  <YouScope>\\graalpy-venv\\Scripts\\pip.exe install tifffile  # GraalPy\n"
            + "\n"
            + "  After install: just 'import napari' works -- no restart needed.\n"
            + "\n"
            + "LIMITATIONS:\n"
            + "  C-extension calls return JSON (lists/numbers/strings).\n"
            + "  Large arrays (>10MB): use run_cellpose() or exec_in_cpython().\n"
            + "  matplotlib opens in a separate CPython window.\n";

    @Override
    public Object getParameter(String key) {
        if ("startup.guide".equals(key))
            return STARTUP_GUIDE;
        switch (key) {
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            case ScriptEngine.NAME:
                return getNames().get(0);
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            default:
                return null;
        }
    }

    @Override
    public List<String> getNames() {
        // All names that ScriptEngineManager.getEngineByName() will resolve.
        return Arrays.asList("GraalPy", "graalpy", "python3", "python");
    }

    @Override
    public String getLanguageName() {
        return "python";
    }

    @Override
    public String getLanguageVersion() {
        return "3.11";
    }

    @Override
    public String getMethodCallSyntax(String obj, String method, String... args) {
        return obj + "." + method + "(" + String.join(", ", args) + ")";
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    @Override
    public String getProgram(String... statements) {
        return String.join("\n", statements);
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new GraalPyScriptEngine(this);
    }
}