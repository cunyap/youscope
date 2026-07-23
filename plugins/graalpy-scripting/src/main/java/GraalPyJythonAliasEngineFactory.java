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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.List;

/**
 * A second JSR-223 {@link ScriptEngineFactory} that advertises the engine
 * name {@code "jython"} while returning the same {@link GraalPyScriptEngine}.
 *
 * <h3>Why this is needed</h3>
 * {@code ScriptingJobImpl} selects a script engine via:
 * 
 * <pre>
 * factory.getEngineName().compareToIgnoreCase(scriptEngineName) == 0
 * </pre>
 * 
 * Existing YouScope measurement configurations (CSB files) store
 * {@code <engine>jython</engine>}. Without this alias factory those
 * measurements would fail to find a matching engine after removing
 * {@code jython-standalone-2.7.0.jar}.
 *
 * <p>
 * This factory is registered alongside {@link GraalPyScriptEngineFactory}
 * in {@code META-INF/services/javax.script.ScriptEngineFactory}. The two
 * factories share the same {@link org.graalvm.polyglot.Engine} and produce
 * functionally identical {@link GraalPyScriptEngine} instances.
 *
 */
public class GraalPyJythonAliasEngineFactory implements ScriptEngineFactory {
    @Override
    public String getEngineName() {
        // Must match exactly what's stored in existing CSB files.
        // ScriptingJobImpl uses compareToIgnoreCase so "jython" matches "Jython".
        return "jython";
    }

    @Override
    public String getEngineVersion() {
        return "GraalPy 24.1 (Python 3.11) jython compatibility alias";
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("py");
    }

    @Override
    public List<String> getMimeTypes() {
        return Arrays.asList("text/x-python");
    }

    @Override
    public List<String> getNames() {
        return Arrays.asList("jython", "Jython");
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
    public Object getParameter(String key) {
        switch (key) {
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            case ScriptEngine.NAME:
                return getEngineName();
            default:
                return null;
        }
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
        // Return the same GraalPy engine as transparent replacement for Jython.
        return new GraalPyScriptEngine(new GraalPyScriptEngineFactory());
    }
}
