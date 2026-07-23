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

import javax.script.*;
import java.io.*;
import java.util.Map;

/**
 * JSR-223 {@link ScriptEngine} backed by a GraalPy {@link Context}.
 *
 * <h3>Lifecycle</h3>
 * <ul>
 * <li>One {@link GraalPyScriptEngine} instance is created per
 * {@link org.youscope.plugin.scriptingjob.ScriptingJobImpl} (via
 * {@link GraalPyScriptEngineFactory#getScriptEngine()}).</li>
 * <li>The engine is initialised in {@code ScriptingJobImpl.initializeJob()}
 * and discarded in {@code uninitializeJob()}. Note it is never reused across
 * measurements.</li>
 * <li>Each call to {@link #eval} executes in the same {@link Context}, so
 * Python globals (imports, helper functions) accumulate across iterations
 * exactly as they did with Jython.</li>
 * </ul>
 *
 * <h3>Binding injection</h3>
 * {@code ScriptingJobImpl} calls {@link #put(String, Object)} to inject:
 * <ul>
 * <li>{@code microscope} –
 * {@link org.youscope.common.microscope.Microscope}</li>
 * <li>{@code measurementContext} –
 * {@link org.youscope.common.MeasurementContext}</li>
 * <li>{@code jobs} – {@code Job[]}</li>
 * <li>{@code evaluationNumber} – {@code int}</li>
 * <li>{@code well} – {@code String} (well name, optional)</li>
 * <li>{@code position} – {@code int[]} (optional)</li>
 * <li>{@code executionInformation} –
 * {@link org.youscope.common.ExecutionInformation}</li>
 * </ul>
 * Each binding is made available in Python via the polyglot API so that
 * {@code microscope.getCamera()} etc. work directly without any import.
 *
 * <h3>Output capture</h3>
 * GraalPy routes {@code print()} calls to the {@link Writer} set on the
 * {@link ScriptContext}, matching the behaviour of the Jython engine so that
 * {@code ScriptingJobImpl.receiveEngineMessages()} continues to work.
 *
 * <h3>pyBIS / OME-Zarr readiness</h3>
 * Because GraalPy supports pure-Python packages via its built-in pip, scripts
 * can do:
 * 
 * <pre>
 *   import subprocess, sys
 *   subprocess.run([sys.executable, "-m", "pip", "install", "--quiet", "pybis"])
 *   from pybis import Openbis
 *   o = Openbis("https://openbis.example.org")
 *   o.login("user", "password")
 *   # ... register experiment ...
 * </pre>
 * 
 * or use pre-installed packages from a venv managed by
 * {@link GraalPyPackageManager}.
 *
 * @see GraalPyScriptEngineFactory
 */
public class GraalPyScriptEngine extends AbstractScriptEngine implements Compilable {
    private final GraalPyScriptEngineFactory factory;

    /**
     * The GraalPy execution context for this engine instance.
     * Created lazily on first use so that engines that are constructed but
     * never evaluated (e.g. during UI preview) do not allocate resources.
     */
    private Context context = null;
    private final Object contextLock = new Object();

    // Known compat functions shown in workspace before any eval runs
    private static final String[] COMPAT_FUNCTION_NAMES = {
            "takeImage", "toNumpyImage", "displayImage", "saveImage",
            "run_cellpose", "run_in_cpython", "exec_in_cpython",
            "cpython_import", "spool_for_openbis_upload",
            "numpy", "matplotlib", "skimage", "imageio"
    };

    // Core preamble source (core_preamble.py loaded from resources)
    private String corePreambleSource = null;

    GraalPyScriptEngine(GraalPyScriptEngineFactory factory) {
        this.factory = factory;
        // Pre-populate workspace with known compat helpers
        for (String fn : COMPAT_FUNCTION_NAMES)
            pendingBindings.put(fn, "");
    }

    /**
     * Returns (creating if necessary) the GraalPy {@link Context} for this engine.
     *
     * <p>
     * The context is configured to:
     * <ul>
     * <li>Allow full Java interop ({@code allowAllAccess(true)}) so that
     * Python scripts can call any injected Java object directly.</li>
     * <li>Route stdout/stderr to the JSR-223 {@link ScriptContext} writer
     * so YouScope captures script output via the existing
     * {@code receiveEngineMessages()} mechanism.</li>
     * <li>Use the venv managed by {@link GraalPyPackageManager} as the
     * home for user-installed packages (pyBIS, ome-zarr, etc.).</li>
     * </ul>
     */

    /**
     * Injects the Python compatibility preamble into the context.
     * This runs once and makes Java-injected bindings accessible as plain
     * Python names, mirrors Jython's direct-access behaviour.
     */
    /** Whether the compat preamble has been successfully injected. */
    private boolean preambleInjected = false;

    /**
     * Loads preamble source once and stores it for deferred injection.
     */
    private String preambleSource = null;

    Context getOrCreateContext() {
        synchronized (contextLock) {
            if (context == null) {
                String youScopeHome = System.getProperty("user.dir", ".");
                String venvExe = youScopeHome + java.io.File.separator
                        + "graalpy-venv" + java.io.File.separator + "Scripts"
                        + java.io.File.separator + "graalpy.exe";
                if (!new java.io.File(venvExe).exists())
                    venvExe = youScopeHome + java.io.File.separator
                            + "graalpy-venv" + java.io.File.separator + "bin"
                            + java.io.File.separator + "graalpy";
                boolean hasVenv = new java.io.File(venvExe).exists();

                Writer outputWriter = getContext().getWriter();
                Writer errorWriter = getContext().getErrorWriter();
                OutputStream out = outputWriter != null ? new WriterOutputStream(outputWriter) : System.out;
                OutputStream err = errorWriter != null ? new WriterOutputStream(errorWriter) : System.err;

                Context.Builder builder = Context.newBuilder("python")
                        .engine(GraalPyScriptEngineFactory.getSharedEngine())
                        .allowAllAccess(true)
                        .allowHostAccess(org.graalvm.polyglot.HostAccess.ALL)
                        .allowHostClassLookup(className -> true)
                        .allowIO(org.graalvm.polyglot.io.IOAccess.ALL)
                        .out(out).err(err)
                        .option("python.EmulateJython", "true")
                        .option("python.WarnExperimentalFeatures", "false");

                if (hasVenv)
                    builder = builder.option("python.Executable", venvExe);

                context = builder.build();

                if (hasVenv) {
                    try {
                        context.eval("python", "import site");
                    } catch (Exception ignored) {
                    }
                }

                // sys.path cleanup after import site
                try {
                    context.eval("python",
                            "import sys as _s\n"
                                    + "_s.path = [p for p in _s.path if p and p != '.']\n"
                                    + "del _s\n");
                } catch (Exception ignored) {
                }

                // nt module patches for Windows/numpy compatibility
                try {
                    context.eval("python",
                            "import nt as _nt\n"
                                    + "class _YouScopeDllCookie:\n"
                                    + "    def __enter__(self): return self\n"
                                    + "    def __exit__(self, *a): return None\n"
                                    + "    def close(self): pass\n"
                                    + "def _youscope_add_dll_directory(path): return _YouScopeDllCookie()\n"
                                    + "def _youscope_remove_dll_directory(cookie): pass\n"
                                    + "if not hasattr(_nt, '_add_dll_directory'):\n"
                                    + "    _nt._add_dll_directory = _youscope_add_dll_directory\n"
                                    + "if not hasattr(_nt, '_remove_dll_directory'):\n"
                                    + "    _nt._remove_dll_directory = _youscope_remove_dll_directory\n"
                                    + "del _nt\n");
                } catch (Exception ignored) {
                }

                loadPreambleSource();
            }
            return context;
        }
    }

    final java.util.concurrent.ConcurrentHashMap<String, Object> pendingBindings = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void put(String key, Object value) {
        pendingBindings.put(key, value);
        synchronized (contextLock) {
            if (context != null) {
                try {
                    context.getPolyglotBindings().putMember(key, value);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public Object get(String key) {
        return pendingBindings.get(key);
    }

    @Override
    public Bindings getBindings(int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE)
            return new PyMainBindings(this);
        return super.getBindings(scope);
    }

    @Override
    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
        try {
            StringBuilder sb = new StringBuilder(4096);
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1)
                sb.append(buf, 0, n);
            return eval(sb.toString(), scriptContext);
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object eval(String script, ScriptContext scriptContext) throws ScriptException {
        Context ctx = getOrCreateContext();
        injectCompatPreamble();

        // Sync pending bindings (youscopeServer, youscopeClient etc.) to polyglot
        for (java.util.Map.Entry<String, Object> entry : pendingBindings.entrySet()) {
            try {
                ctx.getPolyglotBindings().putMember(entry.getKey(), entry.getValue());
            } catch (Exception ignored) {
            }
        }

        // Pull bindings into Python __main__ globals
        try {
            ctx.eval("python", "_refresh_bindings()");
        } catch (Exception ignored) {
        }

        // Detect file loads vs interactive input
        Object filenameObj = scriptContext.getAttribute(ScriptEngine.FILENAME);
        String filename = (filenameObj instanceof String) ? (String) filenameObj : null;
        boolean isFileLoad = filename != null
                && !filename.isEmpty()
                && !filename.equals("User Input")
                && !filename.equals("<input>")
                && !filename.equals("<script>");

        try {
            ctx.getPolyglotBindings().putMember("_youscope_script_src", script);
            String executor = "import sys as _ys, polyglot as _yp\n" +
                    "if _ys.path and _ys.path[0] in ('', '.'): _ys.path.pop(0)\n" +
                    "_src = _yp.import_value('_youscope_script_src')\n" +
                    "_globs = _ys.modules.get('__main__', _ys.modules['__main__']).__dict__\n" +
                    "exec(compile(str(_src), '<input>', 'exec'), _globs)\n";
            ctx.eval("python", executor);
            return null;
        } catch (PolyglotException e) {
            int line = e.getSourceLocation() != null ? e.getSourceLocation().getStartLine() : -1;
            int column = e.getSourceLocation() != null ? e.getSourceLocation().getStartColumn() : -1;
            if (isFileLoad) {
                System.out.println("[YouScope] Script file '" + filename
                        + "' error at line " + line + ": " + e.getMessage());
                return null;
            }
            throw new ScriptException(e.getMessage(), "<script>", line, column);
        }
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        try {
            org.graalvm.polyglot.Source src = org.graalvm.polyglot.Source
                    .newBuilder("python", script, "<compiled>").buildLiteral();
            getOrCreateContext().parse(src);
        } catch (PolyglotException e) {
            throw new ScriptException(e.getMessage(), "<compiled>",
                    e.getSourceLocation() != null ? e.getSourceLocation().getStartLine() : -1);
        }
        final String fs = script;
        final GraalPyScriptEngine self = this;
        return new CompiledScript() {
            @Override
            public Object eval(ScriptContext ctx) throws ScriptException {
                return self.eval(fs, ctx);
            }

            @Override
            public ScriptEngine getEngine() {
                return self;
            }
        };
    }

    @Override
    public CompiledScript compile(Reader r) throws ScriptException {
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1)
                sb.append(buf, 0, n);
            return compile(sb.toString());
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    private static String loadResource(String path) {
        try (InputStream is = GraalPyScriptEngine.class.getResourceAsStream(path)) {
            if (is == null)
                return null;
            byte[] buf = new byte[8192];
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = is.read(buf)) != -1)
                baos.write(buf, 0, n);
            return baos.toString("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private void loadPreambleSource() {
        corePreambleSource = loadResource(
                "/org/youscope/plugin/graalpy/core_preamble.py");
        preambleSource = loadResource(
                "/org/youscope/plugin/graalpy/youscope_compat.py");
    }

    private void injectCompatPreamble() {
        if (preambleInjected)
            return;

        // Phase 1: eval core_preamble.py
        // The file self-injects into __main__.__dict__ at its bottom,
        // so takeImage/toNumpyImage/displayImage etc. are available to user scripts.
        if (corePreambleSource == null) {
            System.out.println("GraalPyScriptEngine: core_preamble.py not found in JAR");
            return;
        }
        try {
            context.eval("python", corePreambleSource);
        } catch (Exception e) {
            System.out.println("GraalPyScriptEngine: core preamble failed: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return;
        }

        // Phase 2: youscope_compat.py (CPython bridge, run_cellpose, openBIS etc.)
        // Note: import hook is already installed by core_preamble.py above.
        if (preambleSource != null) {
            try {
                context.eval("python", preambleSource);
                // Copy public compat names to __main__
                context.eval("python",
                        "import sys as _cph2\n"
                                + "_mn2 = _cph2.modules['__main__'].__dict__\n"
                                + "_pub = ['run_in_cpython','run_cellpose','exec_in_cpython',\n"
                                + "        'cpython_import','spool_for_openbis_upload']\n"
                                + "for _n2 in _pub:\n"
                                + "    _v2 = globals().get(_n2)\n"
                                + "    if _v2 is not None: _mn2[_n2] = _v2\n"
                                + "del _cph2, _mn2, _pub, _n2, _v2\n");
            } catch (Exception e) {
                System.out.println("GraalPyScriptEngine: compat preamble error (non-fatal): "
                        + e.getClass().getSimpleName() + ": "
                        + (e.getMessage() != null ? e.getMessage().split("\n")[0] : ""));
            }
        }
        preambleInjected = true;

        // Compat functions already pre-populated in constructor via
        // COMPAT_FUNCTION_NAMES
    }

    private boolean startupGuideShown = false;

    private void printStartupGuide() {
        String d = System.getProperty("user.dir", "C:\\Program Files\\YouScope");
        String uvP = d + "\\uv\\uv.exe pip install <pkg> --python " + d + "\\cpython-env\\Scripts\\python.exe";
        String gpP = d + "\\graalpy-venv\\Scripts\\pip.exe install <pkg>";
        String jv = System.getProperty("java.version", "?");
        context.getPolyglotBindings().putMember("_sg_dir", d);
        context.getPolyglotBindings().putMember("_sg_uv", uvP);
        context.getPolyglotBindings().putMember("_sg_gpy", gpP);
        context.getPolyglotBindings().putMember("_sg_jv", jv);
        try {
            context.eval("python",
                    "import polyglot as _pg\n"
                            + "_sep = '=' * 60\n"
                            + "_d   = str(_pg.import_value('_sg_dir'))\n"
                            + "_uv  = str(_pg.import_value('_sg_uv'))\n"
                            + "_gpy = str(_pg.import_value('_sg_gpy'))\n"
                            + "_jv  = str(_pg.import_value('_sg_jv'))\n"
                            + "print(_sep)\n"
                            + "print('YouScope GraalPy  |  Python 3.11  |  Java', _jv)\n"
                            + "print(_sep)\n"
                            + "print('QUICK START:')\n"
                            + "print('  event = takeImage(youscopeServer, \\'Channels\\', \\'FITC\\', 20)')\n"
                            + "print('  displayImage(event)')\n"
                            + "print('  saveImage(event, \\'img.tif\\')')\n"
                            + "print('  run_cellpose(event)')\n"
                            + "print()\n"
                            + "print('BUILT-IN: takeImage  toNumpyImage  displayImage  saveImage')\n"
                            + "print('          run_cellpose  exec_in_cpython  clear/clc/cls')\n"
                            + "print()\n"
                            + "print('IMPORTS (transparent):')\n"
                            + "print('  import numpy as np  import matplotlib.pyplot as plt')\n"
                            + "print('  import skimage, imageio, cv2, scipy')\n"
                            + "print()\n"
                            + "print('JAVA INTEROP:')\n"
                            + "print('  m = youscopeServer.getMicroscope()')\n"
                            + "print()\n"
                            + "print('INSTALL PACKAGES:')\n"
                            + "print('  CPython:', _uv)\n"
                            + "print('  GraalPy:', _gpy)\n"
                            + "print(_sep)\n"
                            + "del _pg, _sep, _d, _uv, _gpy, _jv\n");
        } catch (Exception e) {
            System.out.println("[YouScope GraalPy] Guide unavailable: " + e.getMessage());
        }
    }

    private class WriterOutputStream extends OutputStream {
        WriterOutputStream(Writer initialWriter) {
        }

        private Writer currentWriter() {
            Writer w = getContext().getWriter();
            return w != null ? w : new OutputStreamWriter(System.out);
        }

        @Override
        public void write(int b) throws IOException {
            currentWriter().write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            currentWriter().write(new String(b, off, len, java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public void flush() throws IOException {
            currentWriter().flush();
        }
    }

    // Live view of Python __main__.__dict__ for workspace panel

    private static final class PyMainBindings implements javax.script.Bindings {
        private final GraalPyScriptEngine engine;

        PyMainBindings(GraalPyScriptEngine e) {
            this.engine = e;
        }

        private static final String GET_MAIN_KEYS = "import sys as _sys\n"
                + "list(_sys.modules[\"__main__\"].__dict__.keys())\n";

        private String makeGetScript(String key) {
            return "import sys as _sys, polyglot as _pol\n"
                    + "_ws_main = _sys.modules[\"__main__\"].__dict__\n"
                    + "_ws_k = str(_pol.import_value(\"_ws_lookup_key\"))\n"
                    + "_ws_v = _ws_main.get(_ws_k)\n"
                    + "repr(_ws_v)\n";
        }

        private String makeRemoveScript() {
            return "import sys as _sys, polyglot as _pol\n"
                    + "_ws_k = str(_pol.import_value(\"_ws_lookup_key\"))\n"
                    + "_sys.modules[\"__main__\"].__dict__.pop(_ws_k, None)\n";
        }

        private String makePutScript(String varName) {
            return "import sys as _sys, polyglot as _pol\n"
                    + "_ws_main = _sys.modules[\"__main__\"].__dict__\n"
                    + "_ws_main[\"" + varName + "\"] = _pol.import_value(\"_ws_put_value\")\n";
        }

        private org.graalvm.polyglot.Value runPy(String src) {
            try {
                return engine.getOrCreateContext().eval("python", src);
            } catch (Exception e) {
                return null;
            }
        }

        private boolean isFiltered(String k) {
            return k.startsWith("__") || k.startsWith("_youscope")
                    || k.startsWith("_refresh") || k.startsWith("_YOUSCOPE")
                    || k.startsWith("_ws_") || k.startsWith("_sys")
                    || k.startsWith("_pol") || k.startsWith("_ys")
                    || k.startsWith("_yp") || k.startsWith("_src")
                    || k.startsWith("_globs") || k.startsWith("_time")
                    || k.startsWith("_json") || k.startsWith("_pathlib")
                    || k.startsWith("_SPOOL") || k.startsWith("_DEFAULT")
                    || k.startsWith("_p") || k.startsWith("_s")
                    || k.equals("site")
                    || k.startsWith("_You") || k.startsWith("_you")
                    || k.startsWith("_require") || k.startsWith("_inj")
                    || k.equals("pip") || k.equals("xrange")
                    || k.equals("unicode") || k.equals("basestring") || k.equals("long")
                    || k.startsWith("java_") || k.startsWith("to_java") || k.startsWith("from_java")
                    || k.startsWith("youscope_")
                    || k.startsWith("get_image") || k.startsWith("get_openbis") || k.startsWith("get_zarr")
                    || k.startsWith("System_")
                    || k.startsWith("_write_") || k.equals("_DllCookie")
                    || k.equals("_plt_proxy") || k.startsWith("_preproxy")
                    || k.equals("_hook_class") || k.startsWith("_cpython_")
                    || k.equals("_CPythonFallbackFinder")
                    || k.equals("_CPythonModuleProxy")
                    || k.equals("_CPythonAttrProxy")
                    || k.startsWith("_startup_")
                    || k.startsWith("_sg_")
                    || k.equals("_AutoCPythonHook")
                    || k.equals("_CPythonImportHook")
                    || k.equals("_CPythonProxy")
                    || k.equals("_inject")
                    || k.equals("_main")
                    || k.equals("_sys")
                    || k.startsWith("_cps") || k.startsWith("_cpp")
                    || k.startsWith("_mn") || k.startsWith("_pub")
                    || k.startsWith("_n2") || k.startsWith("_v2")
                    || k.startsWith("_main_inject") || k.startsWith("_sys_core")
                    || k.startsWith("_public_names") || k.startsWith("_sys_inject");
        }

        @Override
        public java.util.Set<String> keySet() {
            java.util.Set<String> keys = new java.util.LinkedHashSet<>();
            for (String k : engine.pendingBindings.keySet())
                if (!k.startsWith("javax.script") && !isFiltered(k))
                    keys.add(k);
            try {
                org.graalvm.polyglot.Value names = runPy(GET_MAIN_KEYS);
                if (names != null && names.hasArrayElements())
                    for (long i = 0; i < names.getArraySize(); i++) {
                        String k = names.getArrayElement(i).asString();
                        if (!isFiltered(k))
                            keys.add(k);
                    }
            } catch (Exception ignored) {
            }
            return keys;
        }

        @Override
        public Object get(Object key) {
            if (key == null)
                return null;
            Object pending = engine.pendingBindings.get(key.toString());
            if (pending != null && !"".equals(pending))
                return pending.getClass().getSimpleName() + ": " + pending.toString().split("@")[0];
            try {
                engine.getOrCreateContext()
                        .getPolyglotBindings().putMember("_ws_lookup_key", key.toString());
                org.graalvm.polyglot.Value v = runPy(makeGetScript(key.toString()));
                if (v == null || v.isNull())
                    return null;
                return v.asString();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public Object put(String name, Object value) {
            Object old = get(name);
            try {
                engine.getOrCreateContext()
                        .getPolyglotBindings().putMember("_ws_put_value", value);
                runPy(makePutScript(name));
            } catch (Exception ignored) {
            }
            return old;
        }

        @Override
        public Object remove(Object key) {
            Object old = get(key);
            try {
                engine.getOrCreateContext()
                        .getPolyglotBindings().putMember("_ws_lookup_key", key.toString());
                runPy(makeRemoveScript());
            } catch (Exception ignored) {
            }
            return old;
        }

        @Override
        public void putAll(java.util.Map<? extends String, ?> m) {
            for (java.util.Map.Entry<? extends String, ?> e : m.entrySet())
                put(e.getKey(), e.getValue());
        }

        @Override
        public void clear() {
            for (String k : new java.util.ArrayList<>(keySet()))
                remove(k);
        }

        @Override
        public boolean containsKey(Object key) {
            return key != null && keySet().contains(key.toString());
        }

        @Override
        public boolean containsValue(Object v) {
            return values().contains(v);
        }

        @Override
        public java.util.Set<java.util.Map.Entry<String, Object>> entrySet() {
            java.util.Set<java.util.Map.Entry<String, Object>> set = new java.util.LinkedHashSet<>();
            for (String k : keySet())
                set.add(new java.util.AbstractMap.SimpleEntry<>(k, get(k)));
            return set;
        }

        @Override
        public java.util.Collection<Object> values() {
            java.util.List<Object> v = new java.util.ArrayList<>();
            for (String k : keySet())
                v.add(get(k));
            return v;
        }

        @Override
        public int size() {
            return keySet().size();
        }

        @Override
        public boolean isEmpty() {
            return keySet().isEmpty();
        }
    }
}