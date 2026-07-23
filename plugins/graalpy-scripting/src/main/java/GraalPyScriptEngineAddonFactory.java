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

import org.youscope.addon.component.ComponentAddonFactoryAdapter;

/**
 * YouScope addon factory entry point for the GraalPy scripting plugin.
 *
 * <p>
 * This class is referenced in the JAR manifest under the key
 * {@code YouScope-Addon-Factories}. YouScope's plugin loader instantiates it
 * at server startup to register the plugin.
 *
 * <p>
 * The primary job of this factory is lifecycle management of the shared
 * GraalPy {@link org.graalvm.polyglot.Engine}:
 * <ul>
 * <li>The engine is created lazily on first use by
 * {@link GraalPyScriptEngineFactory#getSharedEngine()}.</li>
 * <li>It is shut down when the YouScope server stops, via
 * {@link #shutdownPlugin()} if YouScope calls it, or via a JVM shutdown
 * hook as a safety net.</li>
 * </ul>
 *
 * <p>
 * The JSR-223 {@link GraalPyScriptEngineFactory} is discovered automatically
 * by {@link javax.script.ScriptEngineManager} via the ServiceLoader file at
 * {@code META-INF/services/javax.script.ScriptEngineFactory}.
 * No explicit registration is needed here for the scripting job to find it.
 *
 */
public class GraalPyScriptEngineAddonFactory extends ComponentAddonFactoryAdapter {
    /**
     * Constructor called by YouScope's plugin loader at server startup.
     * Registers a JVM shutdown hook to close the shared GraalPy engine.
     */
    public GraalPyScriptEngineAddonFactory() {
        super(); // ComponentAddonFactoryAdapter with no configuration addon

        // Explicitly register the GraalPy engine factories with the default
        // ScriptEngineManager. This is necessary because YouScope's plugin
        // classloader is separate from the system classloader, so the
        // ServiceLoader file in META-INF/services/ is not picked up by
        // ScriptEngineManager instances created with a different classloader.
        // Registering here ensures the engine appears in all ScriptEngineManager
        // instances regardless of which classloader they use.
        try {
            javax.script.ScriptEngineManager mgr = new javax.script.ScriptEngineManager();
            mgr.registerEngineName("GraalPy", new GraalPyScriptEngineFactory());
            mgr.registerEngineName("graalpy", new GraalPyScriptEngineFactory());
            mgr.registerEngineName("python3", new GraalPyScriptEngineFactory());
            mgr.registerEngineName("jython", new GraalPyJythonAliasEngineFactory());
            mgr.registerEngineName("Jython", new GraalPyJythonAliasEngineFactory());
            // Also register with the thread context classloader manager
            javax.script.ScriptEngineManager mgr2 = new javax.script.ScriptEngineManager(
                    Thread.currentThread().getContextClassLoader());
            mgr2.registerEngineName("GraalPy", new GraalPyScriptEngineFactory());
            mgr2.registerEngineName("graalpy", new GraalPyScriptEngineFactory());
            mgr2.registerEngineName("python3", new GraalPyScriptEngineFactory());
            mgr2.registerEngineName("jython", new GraalPyJythonAliasEngineFactory());
            mgr2.registerEngineName("Jython", new GraalPyJythonAliasEngineFactory());
        } catch (Exception e) {
            System.err.println("GraalPyScriptEngineAddonFactory: "
                    + "Could not register engines with ScriptEngineManager: " + e.getMessage());
        }

        // Shutdown hook as safety net
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                GraalPyScriptEngineFactory.shutdownSharedEngine();
            }
        }, "GraalPy-shutdown"));
    }

    /**
     * Called by the YouScope server when it is shutting down.
     * Closes the shared GraalPy engine to release all Truffle resources.
     */
    public void shutdownPlugin() {
        GraalPyScriptEngineFactory.shutdownSharedEngine();
    }
}
