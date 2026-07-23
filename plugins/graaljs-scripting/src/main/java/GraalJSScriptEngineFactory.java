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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

/**
 * JSR-223 ScriptEngineFactory for GraalJS, replacing Oracle Nashorn (removed JDK 15+).
 *
 * <p>Implements a minimal ScriptEngine directly on top of
 * {@link org.graalvm.polyglot.Context} without using js-scriptengine.jar.
 *
 * <p>Why not use js-scriptengine.jar: GraalJSScriptEngine.createDefaultContext()
 * internally sets options js.syntax-extensions and js.script-engine-global-scope-import
 * which do not exist in the js-language version available on the classpath,
 * causing IllegalArgumentException on every engine initialization.
 *
 * <p>Registered under "graaljs", "GraalJS", "javascript", "js", "nashorn" so
 * existing scripts referencing "nashorn" keep working unmodified.
 *
 * <p>Compile-time dependencies: only org.graalvm.polyglot:polyglot (already
 * on classpath for GraalPy). No js-scriptengine.jar needed at compile or runtime.
 */
public class GraalJSScriptEngineFactory implements ScriptEngineFactory
{
    private static volatile Engine sharedJsEngine = null;

    /** Returns (creating if necessary) the shared GraalJS polyglot Engine. */
    static synchronized Engine getSharedEngine()
    {
        if (sharedJsEngine == null)
        {
            sharedJsEngine = Engine.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        }
        return sharedJsEngine;
    }

    /** Shut down the shared engine on YouScope exit. */
    public static synchronized void shutdownSharedEngine()
    {
        if (sharedJsEngine != null)
        {
            try { sharedJsEngine.close(); } catch (Exception ignored) {}
            sharedJsEngine = null;
        }
    }

    @Override public String getEngineName()      { return "GraalJS"; }
    @Override public String getEngineVersion()   { return "24.1.0"; }
    @Override public String getLanguageName()    { return "ECMAScript"; }
    @Override public String getLanguageVersion() { return "2023"; }

    @Override
    public List<String> getNames()
    {
        // "nashorn" alias: old scripts/config referencing "nashorn" keep working.
        return Arrays.asList("graaljs", "GraalJS", "javascript", "js",
                             "nashorn", "Nashorn", "JavaScript", "ECMAScript");
    }

    @Override
    public List<String> getMimeTypes()
    {
        return Arrays.asList("application/javascript", "text/javascript",
                             "application/ecmascript");
    }

    @Override public List<String> getExtensions() { return Arrays.asList("js"); }

    @Override
    public Object getParameter(String key)
    {
        switch (key)
        {
            case ScriptEngine.ENGINE:           return getEngineName();
            case ScriptEngine.ENGINE_VERSION:   return getEngineVersion();
            case ScriptEngine.NAME:             return getNames().get(0);
            case ScriptEngine.LANGUAGE:         return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION: return getLanguageVersion();
            default: return null;
        }
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args)
    {
        StringBuilder sb = new StringBuilder(obj).append('.').append(m).append('(');
        for (int i = 0; i < args.length; i++) { if (i > 0) sb.append(','); sb.append(args[i]); }
        return sb.append(')').toString();
    }

    @Override public String getOutputStatement(String s) { return "print(" + s + ")"; }

    @Override
    public String getProgram(String... stmts)
    {
        StringBuilder sb = new StringBuilder();
        for (String s : stmts) sb.append(s).append(";\n");
        return sb.toString();
    }

    @Override
    public ScriptEngine getScriptEngine()
    {
        return new GraalJSScriptEngineImpl(this);
    }

    /**
     * Minimal JSR-223 ScriptEngine backed directly by a GraalVM polyglot Context.
     *
     * <p>Variables set via {@code engine.put("youscopeServer", server)} are
     * synced into the JS global scope before every eval() call so scripts can
     * access them as plain globals:
     * <pre>
     *   var m = youscopeServer.getMicroscope();
     *   var Java = Java; // available via allowAllAccess(true)
     *   var ArrayList = Java.type("java.util.ArrayList");
     * </pre>
     */
    static final class GraalJSScriptEngineImpl extends AbstractScriptEngine
    {
        private final GraalJSScriptEngineFactory factory;
        private volatile Context polyCtx = null;

        GraalJSScriptEngineImpl(GraalJSScriptEngineFactory factory)
        {
            this.factory = factory;
        }

        private synchronized Context ensureContext()
        {
            if (polyCtx == null)
            {
                final ScriptContext sc = this.context;
                polyCtx = Context.newBuilder("js")
                    .engine(GraalJSScriptEngineFactory.getSharedEngine())
                    // allowAllAccess(true) enables Java interop broadly.
                    // HostAccess. All is required explicitly for interface proxy
                    // creation: new java.awt.event.ActionListener({...})
                    // Without it, "Message not supported" is thrown.
                    .allowAllAccess(true)
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(className -> true)
                    .out(new OutputStream() {
                        @Override public void write(int b) {}
                        @Override public void write(byte[] b, int off, int len) {
                            if (sc != null && sc.getWriter() != null) {
                                try { sc.getWriter().write(new String(b, off, len)); }
                                catch (IOException ignored) {}
                            }
                        }
                    })
                    .err(new OutputStream() {
                        @Override public void write(int b) {}
                        @Override public void write(byte[] b, int off, int len) {
                            if (sc != null && sc.getErrorWriter() != null) {
                                try { sc.getErrorWriter().write(new String(b, off, len)); }
                                catch (IOException ignored) {}
                            }
                        }
                    })
                    .build();
            }
            return polyCtx;
        }

        @Override
        public Object eval(String script, ScriptContext scriptContext) throws ScriptException
        {
            Context ctx = ensureContext();

            // Sync ENGINE_SCOPE bindings into JS globals before each eval.
            // This is the mechanism by which youscopeServer, youscopeClient,
            // microscope, measurementContext etc. become available as globals.
            Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
            if (bindings != null)
            {
                Value jsBindings = ctx.getBindings("js");
                for (Map.Entry<String, Object> entry : bindings.entrySet())
                {
                    try { jsBindings.putMember(entry.getKey(), entry.getValue()); }
                    catch (Exception ignored) {}
                }
            }

            try
            {
                Value result = ctx.eval("js", script);
                if (result == null || result.isNull()) return null;
                // For expression results return a string; for statement blocks return null.
                if (result.isString())  return result.asString();
                if (result.isNumber())  return result.asDouble();
                if (result.isBoolean()) return result.asBoolean();
                return null; // suppress object/undefined returns (no console noise)
            }
            catch (PolyglotException e)
            {
                int line = e.getSourceLocation() != null
                    ? e.getSourceLocation().getStartLine() : -1;
                String src = e.getSourceLocation() != null
                    ? e.getSourceLocation().getSource().getName() : "<script>";
                ScriptException se = new ScriptException(e.getMessage(), src, line);
                se.initCause(e);
                throw se;
            }
        }

        @Override
        public Object eval(Reader reader, ScriptContext ctx) throws ScriptException
        {
            try
            {
                BufferedReader br = new BufferedReader(reader);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                return eval(sb.toString(), ctx);
            }
            catch (IOException e) { throw new ScriptException(e); }
        }

        @Override
        public Bindings createBindings() { return new SimpleBindings(); }

        /**
         * Returns the ENGINE_SCOPE bindings as a live view of the JS global scope.
         * This is what ScriptExcecuter.getDefinedVariables() reads to display
         * variables in the scripting console workspace panel.
         *
         * For ENGINE_SCOPE we return a special Bindings backed by the polyglot
         * Context's JS bindings, so variables defined in JS (a=1, function foo)
         * actually appear. For other scopes we use the standard JSR-223 approach.
         */
        @Override
        public Bindings getBindings(int scope)
        {
            if (scope == ScriptContext.ENGINE_SCOPE)
            {
                return new JsGlobalBindings(this);
            }
            return super.getBindings(scope);
        }

        @Override
        public void setBindings(Bindings bindings, int scope)
        {
            // For ENGINE_SCOPE, also sync any pre-set bindings into JS globals
            // (e.g. youscopeServer set by ScriptExcecuter.initializeEngineVariables)
            if (scope == ScriptContext.ENGINE_SCOPE && bindings != null)
            {
                try
                {
                    Context ctx = ensureContext();
                    Value jsGlobals = ctx.getBindings("js");
                    for (Map.Entry<String, Object> e : bindings.entrySet())
                    {
                        try { jsGlobals.putMember(e.getKey(), e.getValue()); }
                        catch (Exception ignored) {}
                    }
                }
                catch (Exception ignored) {}
            }
            super.setBindings(bindings, scope);
        }

        @Override public ScriptEngineFactory getFactory() { return factory; }
    }

    /**
     * A Bindings implementation that reads and writes directly from/to the
     * GraalJS polyglot Context's global JS scope. This makes variables defined
     * in scripts (a = 1, function foo() {}) visible in YouScope's scripting
     * console workspace panel, matching Nashorn's behaviour.
     */
    static final class JsGlobalBindings implements Bindings
    {
        private final GraalJSScriptEngineImpl engine;

        JsGlobalBindings(GraalJSScriptEngineImpl engine) { this.engine = engine; }

        private Value jsGlobals()
        {
            return engine.ensureContext().getBindings("js");
        }

        @Override
        public Object put(String name, Object value)
        {
            Object old = get(name);
            try { jsGlobals().putMember(name, value); } catch (Exception ignored) {}
            return old;
        }

        @Override
        public void putAll(java.util.Map<? extends String, ?> m)
        {
            for (Map.Entry<? extends String, ?> e : m.entrySet()) put(e.getKey(), e.getValue());
        }

        @Override
        public boolean containsKey(Object key)
        {
            try { return jsGlobals().hasMember(key.toString()); }
            catch (Exception e) { return false; }
        }

        @Override
        public Object get(Object key)
        {
            try
            {
                Value v = jsGlobals().getMember(key.toString());
                if (v == null || v.isNull()) return null;
                if (v.isString())  return v.asString();
                if (v.isNumber())  return v.asDouble();
                if (v.isBoolean()) return v.asBoolean();
                if (v.canExecute()) return "<function>";
                return v.toString();
            }
            catch (Exception e) { return null; }
        }

        @Override
        public Object remove(Object key)
        {
            Object old = get(key);
            try { jsGlobals().removeMember(key.toString()); } catch (Exception ignored) {}
            return old;
        }

        @Override
        public void clear()
        {
            try
            {
                Value g = jsGlobals();
                for (String k : g.getMemberKeys())
                {
                    try { g.removeMember(k); } catch (Exception ignored) {}
                }
            }
            catch (Exception ignored) {}
        }

        @Override
        public java.util.Set<String> keySet()
        {
            try { return jsGlobals().getMemberKeys(); }
            catch (Exception e) { return java.util.Collections.emptySet(); }
        }

        @Override
        public java.util.Collection<Object> values()
        {
            java.util.List<Object> vals = new java.util.ArrayList<>();
            for (String k : keySet()) vals.add(get(k));
            return vals;
        }

        @Override
        public java.util.Set<Map.Entry<String, Object>> entrySet()
        {
            java.util.Set<Map.Entry<String, Object>> set = new java.util.LinkedHashSet<>();
            for (String k : keySet())
                set.add(new java.util.AbstractMap.SimpleEntry<>(k, get(k)));
            return set;
        }

        @Override public int size() { return keySet().size(); }
        @Override public boolean isEmpty() { return keySet().isEmpty(); }
        @Override public boolean containsValue(Object v) { return values().contains(v); }
    }
}