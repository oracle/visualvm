/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.lib.profiler.oql.engine.api.impl;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLEngine.OQLQuery;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLEngine.ObjectVisitor;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLException;
import org.openide.util.NbBundle;

/**
 * This is Object Query Language Interpreter
 *
 * @author A. Sundararajan
 * @author J. Bachorik
 */
public class OQLEngineImpl {
    final private static Logger LOGGER = Logger.getLogger(OQLEngineImpl.class.getName());

    private static boolean oqlSupported;

    static {
        try {
            // Do we have JavaScript engine?
            ScriptEngineManager manager = new ScriptEngineManager();
            Object engine = manager.getEngineByName("JavaScript"); // NOI18N

            oqlSupported = engine != null;
        } catch (Throwable ex) {
            LOGGER.log(Level.INFO,"OQLEngine init",ex); // NOI18N
            oqlSupported = false;
        }
    }

    // check OQL is supported or not before creating OQLEngine
    public static boolean isOQLSupported() {
        return oqlSupported;
    }

    private ScriptEngine engine;
    private Snapshot snapshot;

    public OQLEngineImpl(Snapshot snapshot) {
        if (!isOQLSupported()) {
            throw new UnsupportedOperationException("OQL not supported"); // NOI18N
        }
        init(snapshot);
    }

    public Snapshot getHeapHelper() {
        return snapshot;
    }

    /**
    Query is of the form

    select &lt;java script code to select&gt;
    [ from [instanceof] &lt;class name&gt; [&lt;identifier&gt;]
    [ where &lt;java script boolean expression&gt; ]
    ]
     */
    public synchronized void executeQuery(String query, ObjectVisitor visitor)
            throws OQLException {
        LOGGER.log(Level.FINE, query);

        OQLQuery parsedQuery = parseQuery(query);
        if (parsedQuery == null) {
            // Query does not start with 'select' keyword.
            // Just treat it as plain JavaScript and eval it.
            try {
                Object res = evalScript(query);
                dispatchValue(res, visitor);
            } catch (Exception e) {
                throw new OQLException(e);
            }
            return;
        }

        executeQuery((OQLQueryImpl)parsedQuery, visitor);
    }

    @NbBundle.Messages({
        "ERROR_NO_SELECT_CLAUSE=query syntax error: no 'select' clause",
        "ERROR_EMPTY_SELECT=query syntax error: 'select' expression can not be empty",
        "ERROR_INSTANCEOF_NO_CLASSNAME=no class name after 'instanceof'",
        "ERROR_FROM_NO_CLASSNAME=query syntax error: class name must follow 'from'",
        "ERROR_NO_IDENTIFIER=query syntax error: identifier should follow class name",
        "ERROR_EXPECTING_WHERE=query syntax error: 'where' clause expected after 'from' clause",
        "ERROR_EMPTY_WHERE=query syntax error: 'where' clause cannot have empty expression"
    })
    public OQLQuery parseQuery(String query) throws OQLException {
        StringTokenizer st = new StringTokenizer(query);
        if (st.hasMoreTokens()) {
            String first = st.nextToken();
            if (!first.equals("select")) { // NOI18N
                // Query does not start with 'select' keyword.
                // Just treat it as plain JavaScript and eval it.
                return null;
            }
        } else {
            throw new OQLException(Bundle.ERROR_NO_SELECT_CLAUSE());
        }

        String selectExpr = ""; // NOI18N
        boolean seenFrom = false;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("from")) { // NOI18N
                seenFrom = true;
                break;
            }
            selectExpr += " " + tok; // NOI18N
        }

        if (selectExpr.isEmpty()) { // NOI18N
            throw new OQLException(Bundle.ERROR_EMPTY_SELECT());
        }

        String className = null;
        boolean isInstanceOf = false;
        String whereExpr = null;
        String identifier = null;

        if (seenFrom) {
            if (st.hasMoreTokens()) {
                String tmp = st.nextToken();
                if (tmp.equals("instanceof")) { // NOI18N
                    isInstanceOf = true;
                    if (!st.hasMoreTokens()) {
                        throw new OQLException(Bundle.ERROR_INSTANCEOF_NO_CLASSNAME());
                    }
                    className = st.nextToken();
                } else {
                    className = tmp;
                }
            } else {
                throw new OQLException(Bundle.ERROR_FROM_NO_CLASSNAME());
            }

            if (st.hasMoreTokens()) {
                identifier = st.nextToken();
                if (identifier.equals("where")) { // NOI18N
                    throw new OQLException(Bundle.ERROR_NO_IDENTIFIER());
                }
                if (st.hasMoreTokens()) {
                    String tmp = st.nextToken();
                    if (!tmp.equals("where")) { // NOI18N
                        throw new OQLException(Bundle.ERROR_EXPECTING_WHERE());
                    }

                    whereExpr = "";  // NOI18N
                    while (st.hasMoreTokens()) {
                        whereExpr += " " + st.nextToken(); // NOI18N
                    }
                    if (whereExpr.isEmpty()) { // NOI18N
                        throw new OQLException(Bundle.ERROR_EMPTY_WHERE());
                    }
                }
            } else {
                throw new OQLException(Bundle.ERROR_NO_IDENTIFIER());
            }
        }
        return new OQLQueryImpl(selectExpr, isInstanceOf, className, identifier, whereExpr);
    }

    public void cancelQuery() throws OQLException {
        cancelled.set(true);
    }
    
    public boolean isCancelled() {
        return cancelled.get();
    }

    private void executeQuery(OQLQueryImpl q, ObjectVisitor visitor)
            throws OQLException {

        cancelled.set(false);
        visitor = visitor != null ? visitor : ObjectVisitor.DEFAULT;

        JavaClass clazz = null;
        if (q.className != null) {
            String className = q.className;

            clazz = snapshot.findClass(className);
            if (clazz == null) {
                throw new OQLException(className + " was not found!"); // NOI18N
            }
        }

        StringBuilder buf = new StringBuilder();
        buf.append("function __select__("); // NOI18N
        if (q.identifier != null) {
            buf.append(q.identifier);
        }
        buf.append(") { return "); // NOI18N
        buf.append(q.selectExpr.replace('\n', ' ')); // NOI18N
        buf.append("; }\n"); // NOI18N
        buf.append("__select__(").append(q.identifier).append(")"); // NOI18N

        String selectCode = buf.toString();

        // compile select expression and where condition 
        try {
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

            CompiledScript whereCs = null;
            CompiledScript selectCs = ((Compilable)engine).compile(selectCode);
            
            if (q.whereExpr != null) {
                whereCs = ((Compilable)engine).compile(q.whereExpr.replace('\n', ' ')); // NOI18N
            }

            if (q.className != null) {
                Stack<JavaClass> toInspect = new Stack<>();
                Set<JavaClass> inspected = new HashSet<>();

                toInspect.push(clazz);

                while(!toInspect.isEmpty()) {
                    JavaClass inspecting = toInspect.pop();
                    inspected.add(inspecting);
                    if (q.isInstanceOf) {
                        for(JavaClass subclass : inspecting.getSubClasses()) {
                            if (!inspected.contains(subclass) && !toInspect.contains(subclass)) {
                                toInspect.push(subclass);
                            }
                        }
                    }
                    Iterator<Instance> objectsIt = inspecting.getInstancesIterator();

                    while (objectsIt.hasNext()) {
                        Object wrapped = wrapJavaObject(objectsIt.next());
                        boolean b = (whereCs == null);
                        if (!b) {
                            bindings.put(q.identifier, wrapped);
                            Object res = whereCs.eval(bindings);
                            if (res instanceof Boolean) {
                                b = ((Boolean) res).booleanValue();
                            } else if (res instanceof Number) {
                                b = ((Number) res).intValue() != 0;
                            } else {
                                b = (res != null);
                            }
                        }

                        if (b) {
                            bindings.put(q.identifier, wrapped);
                            Object select = selectCs.eval(bindings);
                            if (dispatchValue(select, visitor)) {
                                return;
                            }
                        }
                    }
                }
            } else {
                // simple "select <expr>" query
                Object select = selectCs.eval();
                if (dispatchValue(select, visitor)) {
                    return;
                }
            }
        } catch (Exception e) {
            throw new OQLException(e);
        }
    }

    private boolean dispatchValue(Object jsObject, ObjectVisitor visitor) {
        if (jsObject == null) {
            return false;
        }

        if (jsObject instanceof Iterator) {
            Iterator<?> iter = (Iterator) jsObject;
            while (iter.hasNext()) {
                if (dispatchValue(iter.next(), visitor)) return true;
            }
            return false;
        } else if (jsObject instanceof Enumeration) {
            Enumeration<?> enm = (Enumeration) jsObject;
            while (enm.hasMoreElements()) {
                Object elem = enm.nextElement();
                if (dispatchValue(elem, visitor)) return true;
            }
            return false;
        } else {
            Object object = unwrapJavaObject(jsObject, true);
            if (object instanceof Object[]) {
                for (Object obj1 : (Object[]) object) {
                    if (dispatchValue(obj1, visitor)) return true;
                }
                return false;
            }
            if (visitor.visit(object)) {
                return true;
            }
        }
        return false;
    }

    public Object evalScript(String script) throws Exception {
        cancelled.set(false);
        CompiledScript cs = ((Compilable)engine).compile(script);
        return cs.eval();
    }

    public Object wrapJavaObject(Instance obj) throws Exception {
        return call("wrapJavaObject", new Object[]{obj}); // NOI18N
    }

    public Object toHtml(Object obj) throws Exception {
        return call("toHtml", new Object[]{obj}); // NOI18N
    }

    public Object call(String func, Object[] args) throws Exception {

        return ((Invocable)engine).invokeFunction(func, args);
    }

    public Object unwrapJavaObject(Object object) {
        return unwrapJavaObject(object, false);
    }

    public Object unwrapJavaObject(Object object, boolean tryAssociativeArray) {
        if (object == null) return null;
        String className = object.getClass().getName();
        boolean isNativeJS = className.startsWith("com.oracle.truffle.object.") // NOI18N
                          || className.equals("org.graalvm.polyglot.Value"); // NOI18N

        try {
            Object ret = ((Invocable)engine).invokeFunction("unwrapJavaObjectRes", object); // NOI18N
            if (isNativeJS && ret == null && tryAssociativeArray) {
                ret = ((Invocable)engine).invokeFunction("unwrapMap", object); // NOI18N
            }
            return ret == null ? object : ret;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error unwrapping JS object", ex); // NOI18N
        }
        return null;
    }

    final private AtomicBoolean cancelled = new AtomicBoolean(false);
    private void init(Snapshot snapshot) throws RuntimeException {
        this.snapshot = snapshot;
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            engine = manager.getEngineByName("JavaScript"); // NOI18N
            InputStream strm = getInitStream();
            CompiledScript cs = ((Compilable)engine).compile(new InputStreamReader(strm));
            cs.eval();
            Object heap = ((Invocable)engine).invokeFunction("wrapHeapSnapshot", snapshot); // NOI18N
            engine.put("heap", heap); // NOI18N
            engine.put("cancelled", cancelled); // NOI18N
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "Error initializing snapshot", ex); // NOI18N
            throw new RuntimeException(ex);
        }
    }

    private InputStream getInitStream() {
        return getClass().getResourceAsStream("/org/graalvm/visualvm/lib/profiler/oql/engine/api/impl/hat.js"); // NOI18N
    }
}
