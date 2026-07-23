/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *      Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.graaljs;

import org.youscope.addon.component.ComponentAddonFactoryAdapter;

/**
 * YouScope addon factory entry point for the GraalJS scripting plugin.
 *
 * <p>Registers GraalJS as a ScriptEngine under "graaljs", "javascript",
 * "js", "nashorn", and related aliases so it appears in the scripting
 * console engine dropdown and replaces the Oracle Nashorn engine that was
 * removed from JDK 15+.
 *
 * <p>Also manages lifecycle of the shared GraalJS polyglot Engine.
 */
public class GraalJSScriptEngineAddonFactory extends ComponentAddonFactoryAdapter
{
    public GraalJSScriptEngineAddonFactory()
    {
        super();

        // Register with both the default and thread-context ScriptEngineManagers
        // so the engine is discoverable regardless of which classloader is used.
        try
        {
            GraalJSScriptEngineFactory factory = new GraalJSScriptEngineFactory();

            javax.script.ScriptEngineManager mgr =
                new javax.script.ScriptEngineManager();
            mgr.registerEngineName("graaljs",    factory);
            mgr.registerEngineName("GraalJS",    factory);
            mgr.registerEngineName("javascript", factory);
            mgr.registerEngineName("js",         factory);
            mgr.registerEngineName("nashorn",    factory);
            mgr.registerEngineName("Nashorn",    factory);
            mgr.registerEngineName("JavaScript", factory);

            javax.script.ScriptEngineManager mgr2 =
                new javax.script.ScriptEngineManager(
                    Thread.currentThread().getContextClassLoader());
            mgr2.registerEngineName("graaljs",    factory);
            mgr2.registerEngineName("GraalJS",    factory);
            mgr2.registerEngineName("javascript", factory);
            mgr2.registerEngineName("js",         factory);
            mgr2.registerEngineName("nashorn",    factory);
            mgr2.registerEngineName("Nashorn",    factory);
            mgr2.registerEngineName("JavaScript", factory);
        }
        catch (Exception e)
        {
            System.err.println("GraalJSScriptEngineAddonFactory: "
                + "Could not register engines: " + e.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override public void run()
            {
                GraalJSScriptEngineFactory.shutdownSharedEngine();
            }
        }, "GraalJS-shutdown"));
    }

    public void shutdownPlugin()
    {
        GraalJSScriptEngineFactory.shutdownSharedEngine();
    }
}
