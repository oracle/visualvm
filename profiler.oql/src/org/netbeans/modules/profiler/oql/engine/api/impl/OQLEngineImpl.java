/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.oql.engine.api.impl;

import org.netbeans.modules.profiler.oql.engine.api.OQLException;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine.OQLQuery;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine.ObjectVisitor;

/**
 * This is Object Query Language Interpreter
 *
 * @author A. Sundararajan
 * @authoe J. Bachorik
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
            throw new OQLException(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/oql/engine/api/impl/Bundle").getString("ERROR_NO_SELECT_CLAUSE"));
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

        if (selectExpr.equals("")) { // NOI18N
            throw new OQLException(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/oql/engine/api/impl/Bundle").getString("ERROR_EMPTY_SELECT"));
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
                        throw new OQLException(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/oql/engine/api/impl/Bundle").getString("ERROR_INSTANCEOF_NO_CLASSNAME"));
                    }
                    className = st.nextToken();
                } else {
                    className = tmp;
                }
            } else {
                throw new OQLException(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/oql/engine/api/impl/Bundle").getString("ERROR_FROM_NO_CLASSNAME"));
            }

            if (st.hasMoreTokens()) {
                identifier = st.nextToken();
                if (identifier.equals("where")) { // NOI18N
                    throw new OQLException(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/oql/engine/api/impl/Bundle").getString("ERROR_NO_IDENTIFIER"));
                }
                if (st.hasMoreTokens()) {
                    String tmp = st.nextToken();
                    if (!tmp.equals("where")) { // NOI18N
                        throw new OQLException(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/oql/engine/api/impl/Bundle").getString("ERROR_EXPECTING_WHERE"));
                    }

                    whereExpr = "";  // NOI18N
                    while (st.hasMoreTokens()) {
                        whereExpr += " " + st.nextToken(); // NOI18N
                    }
                    if (whereExpr.equals("")) { // NOI18N
                        throw new OQLException(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/oql/engine/api/impl/Bundle").getString("ERROR_EMPTY_WHERE"));
                    }
                }
            } else {
                throw new OQLException(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/oql/engine/api/impl/Bundle").getString("ERROR_NO_IDENTIFIER"));
            }
        }
        return new OQLQueryImpl(selectExpr, isInstanceOf, className, identifier, whereExpr);
    }

    private void executeQuery(OQLQueryImpl q, ObjectVisitor visitor)
            throws OQLException {
        visitor = visitor != null ? visitor : ObjectVisitor.DEFAULT;

        JavaClass clazz = null;
        if (q.className != null) {
            String className = q.className;

            clazz = snapshot.findClass(className);
            if (clazz == null) {
                throw new OQLException(className + " is not found!"); // NOI18N
            }
        }

        StringBuffer buf = new StringBuffer();
        buf.append("function __select__("); // NOI18N
        if (q.identifier != null) {
            buf.append(q.identifier);
        }
        buf.append(") { return "); // NOI18N
        buf.append(q.selectExpr.replace('\n', ' ')); // NOI18N
        buf.append("; }\n"); // NOI18N
        buf.append("__select__(" + q.identifier + ")"); // NOI18N

        String selectCode = buf.toString();

        // compile select expression and where condition 
        try {
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

            CompiledScript whereCs = null;
            CompiledScript selectCs = null;
            selectCs = ((Compilable)engine).compile(selectCode);
            
            if (q.whereExpr != null) {
                whereCs = ((Compilable)engine).compile(q.whereExpr.replace('\n', ' ')); // NOI18N
            }

            if (q.className != null) {
                Stack toInspect = new Stack();
                Set inspected = new HashSet();

                toInspect.push(clazz);

                Object inspecting = null;
                while(!toInspect.isEmpty()) {
                    inspecting = toInspect.pop();
                    inspected.add(inspecting);
                    JavaClass clz = (JavaClass)inspecting;
                    if (q.isInstanceOf) {
                        for(Object subclass : clz.getSubClasses()) {
                            if (!inspected.contains(subclass) && !toInspect.contains(subclass)) {
                                toInspect.push(subclass);
                            }
                        }
                    }
                    List objects = clz.getInstances();

                    for (Object obj : objects) {
                        Object wrapped = wrapJavaObject((Instance) obj);
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
            Iterator iter = (Iterator) jsObject;
            while (iter.hasNext()) {
                if (dispatchValue(iter.next(), visitor)) return true;
            }
            return false;
        } else if (jsObject instanceof Enumeration) {
            Enumeration enm = (Enumeration) jsObject;
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
        boolean isNativeJS = object.getClass().getName().contains(".javascript."); // NOI18N

        try {
            Object ret = ((Invocable)engine).invokeFunction("unwrapJavaObject", object); // NOI18N
            if (isNativeJS && (ret == null || ret == object) && tryAssociativeArray) {
                ret = ((Invocable)engine).invokeFunction("unwrapMap", object); // NOI18N
            }
            return ret;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error unwrapping JS object", ex); // NOI18N
        }
        return null;
    }

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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error initializing snapshot", ex); // NOI18N
            throw new RuntimeException(ex);
        }
    }

    private InputStream getInitStream() {
        return getClass().getResourceAsStream("/org/netbeans/modules/profiler/oql/engine/api/impl/hat.js"); // NOI18N
    }
}
