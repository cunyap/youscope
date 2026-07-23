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
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.PolyglotException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the per-YouScope-installation GraalPy virtual environment.
 *
 * <h3>Purpose</h3>
 * GraalPy supports {@code pip install} for pure-Python packages. This class
 * provides a stable venv location under the YouScope installation directory
 * and a simple Java API for installing / listing / uninstalling packages.
 *
 * <h3>Location</h3>
 * The venv lives at:
 * 
 * <pre>
 *   &lt;youscope_install_root&gt;/graalpy-venv/
 * </pre>
 * 
 * where {@code youscope_install_root} is
 * {@code System.getProperty("user.dir")},
 * the working directory YouScope sets at startup.
 *
 * <h3>Key packages</h3>
 * <ul>
 * <li><b>pyBIS</b> ({@code pybis}): openBIS V3 REST client for experiment
 * registration. Install via {@code installPackage("pybis")}.</li>
 * <li><b>OME-Zarr</b> ({@code ome-zarr}): read/write OME-NGFF Zarr stores.
 * Install via {@code installPackage("ome-zarr")}.</li>
 * <li><b>tifffile</b>: TIFF stack reading for post-acquisition
 * conversion.</li>
 * <li><b>numpy</b>: available as a pure-Python wheel on GraalPy.</li>
 * </ul>
 *
 * @see GraalPyScriptEngineAddonFactory
 */
public class GraalPyPackageManager {
    /** Sub-directory name for the venv relative to the YouScope install root. */
    private static final String VENV_DIR_NAME = "graalpy-venv";

    /**
     * Sub-directory inside the venv where GraalPy's site-packages are.
     * This is passed as {@code python.PythonHome} to the GraalPy Context.
     */
    private static final String VENV_LIB_SUBDIR = "lib" + java.io.File.separator + "python3.11";

    private GraalPyPackageManager() {
    }

    /**
     * Returns the absolute path to the GraalPy venv root, creating the
     * directory if it does not exist yet.
     *
     * @return Absolute path string, or {@code null} if creation failed.
     */
    public static String getVenvRoot() {
        Path venv = Paths.get(System.getProperty("user.dir", "."), VENV_DIR_NAME);
        try {
            Files.createDirectories(venv);
            return venv.toAbsolutePath().toString();
        } catch (IOException e) {
            System.err.println("GraalPyPackageManager: Cannot create venv directory: "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the {@code python.PythonHome} value to pass to a GraalPy
     * {@link Context} so it picks up packages installed in the managed venv.
     *
     * @return Path string for {@code python.PythonHome}, or {@code null}.
     */
    public static String getVenvHome() {
        String root = getVenvRoot();
        if (root == null)
            return null;
        return Paths.get(root, VENV_LIB_SUBDIR).toAbsolutePath().toString();
    }

    /**
     * Installs a Python package into the managed venv using GraalPy's
     * embedded pip.
     *
     * <p>
     * This call blocks until pip finishes. Run it from a background thread
     * if called from the YouScope UI so the Swing event thread is not blocked.
     *
     * <p>
     * Example: install pyBIS for openBIS experiment registration:
     * 
     * <pre>
     * GraalPyPackageManager.installPackage("pybis");
     * </pre>
     *
     * <p>
     * Example: install OME-Zarr tools for post-acquisition conversion:
     * 
     * <pre>
     * GraalPyPackageManager.installPackage("ome-zarr");
     * GraalPyPackageManager.installPackage("tifffile");
     * </pre>
     *
     * @param packageSpec Package name and optional version, e.g.
     *                    {@code "pybis"} or {@code "pybis>=20240101"}.
     * @return {@code true} if installation succeeded, {@code false} otherwise.
     */
    public static boolean installPackage(String packageSpec) {
        return runPip("install", "--target", getVenvRoot(), packageSpec);
    }

    /**
     * Uninstalls a package from the managed venv.
     *
     * @param packageName Package name, e.g. {@code "pybis"}.
     * @return {@code true} if uninstallation succeeded.
     */
    public static boolean uninstallPackage(String packageName) {
        return runPip("uninstall", "--yes", packageName);
    }

    /**
     * Returns a list of installed packages as {@code "name==version"} strings.
     *
     * <p>
     * Parses the output of {@code pip list --format=freeze}.
     */
    public static List<String> listInstalledPackages() {
        List<String> result = new ArrayList<>();
        // Use a temporary context to run pip list that avoids polluting the
        // shared measurement contexts.
        try (Context ctx = Context.newBuilder("python")
                .engine(GraalPyScriptEngineFactory.getSharedEngine())
                .allowAllAccess(true)
                .build()) {
            // Capture pip freeze output via a Python string buffer
            StringBuilder sb = new StringBuilder();
            ctx.getPolyglotBindings().putMember("_output_collector", sb);
            ctx.eval("python",
                    "import subprocess, sys, polyglot\n" +
                            "_buf = polyglot.import_value('_output_collector')\n" +
                            "_proc = subprocess.run(\n" +
                            "    [sys.executable, '-m', 'pip', 'list', '--format=freeze'],\n" +
                            "    capture_output=True, text=True)\n" +
                            "for line in _proc.stdout.splitlines():\n" +
                            "    _buf.append(line)\n" +
                            "    _buf.append('\\n')\n");
            for (String line : sb.toString().split("\n")) {
                line = line.trim();
                if (!line.isEmpty())
                    result.add(line);
            }
        } catch (PolyglotException e) {
            System.err.println("GraalPyPackageManager: pip list failed: " + e.getMessage());
        }
        return result;
    }

    /**
     * Checks whether a package is installed in the managed venv.
     *
     * @param packageName Package name (case-insensitive), e.g. {@code "pybis"}.
     * @return {@code true} if the package is installed.
     */
    public static boolean isPackageInstalled(String packageName) {
        return listInstalledPackages().stream()
                .anyMatch(p -> p.toLowerCase().startsWith(packageName.toLowerCase() + "==")
                        || p.equalsIgnoreCase(packageName));
    }

    /**
     * Runs a pip sub-command in a short-lived GraalPy context.
     *
     * @param args pip sub-command and its arguments (without "pip" prefix).
     * @return {@code true} if pip exited with code 0.
     */
    private static boolean runPip(String... args) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("import subprocess, sys\n");
        cmd.append("_result = subprocess.run(\n");
        cmd.append("    [sys.executable, '-m', 'pip'");
        for (String arg : args)
            cmd.append(", '").append(arg.replace("'", "\\'")).append("'");
        cmd.append("],\n");
        cmd.append("    capture_output=False)\n");
        cmd.append("_exit_code = _result.returncode\n");

        try (Context ctx = Context.newBuilder("python")
                .engine(GraalPyScriptEngineFactory.getSharedEngine())
                .allowAllAccess(true)
                .build()) {
            ctx.eval("python", cmd.toString());
            Value exitCode = ctx.getBindings("python").getMember("_exit_code");
            return exitCode != null && exitCode.asInt() == 0;
        } catch (PolyglotException e) {
            System.err.println("GraalPyPackageManager: pip command failed: " + e.getMessage());
            return false;
        }
    }
}
