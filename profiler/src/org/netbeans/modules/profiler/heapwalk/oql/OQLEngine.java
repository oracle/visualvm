
/*
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/, and in the file LICENSE.html in the
 * doc directory.
 * 
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun. Portions created by Bill Foote and others
 * at Javasoft/Sun are Copyright (C) 1997-2004. All Rights Reserved.
 * 
 * In addition to the formal license, I ask that you don't
 * change the history or donations files without permission.
 * 
 */
package org.netbeans.modules.profiler.heapwalk.oql;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalk.oql.model.Snapshot;
import org.openide.util.Exceptions;

/**
 * This is Object Query Language Interpreter
 *
 * @author A. Sundararajan [jhat @(#)OQLEngine.java	1.9 06/06/20]
 * @authoe J. Bachorik [NB Profiler]
 */
public class OQLEngine {

    static {
        try {
            // Do we have javax.script support?

            Class managerClass = Class.forName("javax.script.ScriptEngineManager");
            Object manager = managerClass.newInstance();
            
            // check that we have JavaScript engine
            Method getEngineMethod = managerClass.getMethod("getEngineByName",
                    new Class[]{String.class});
            Object engine =  getEngineMethod.invoke(manager, new Object[]{"JavaScript"});

            oqlSupported = engine != null;
        } catch (Exception ex) {
            if (!(ex instanceof ClassNotFoundException)) {
                Exceptions.printStackTrace(ex);
            }
            oqlSupported = false;
        }
    }

    // check OQL is supported or not before creating OQLEngine 
    public static boolean isOQLSupported() {
        return oqlSupported;
    }

    public OQLEngine(Snapshot snapshot) {
        if (!isOQLSupported()) {
            throw new UnsupportedOperationException("OQL not supported");
        }
        init(snapshot);
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
//        debugPrint("query : " + query);
        StringTokenizer st = new StringTokenizer(query);
        if (st.hasMoreTokens()) {
            String first = st.nextToken();
            if (!first.equals("select")) {
                // Query does not start with 'select' keyword.
                // Just treat it as plain JavaScript and eval it.
                try {
                    Object res = evalScript(query);
                    visitor.visit(res);
                } catch (Exception e) {
                    throw new OQLException(e);
                }
                return;
            }
        } else {
            throw new OQLException("query syntax error: no 'select' clause");
        }

        String selectExpr = "";
        boolean seenFrom = false;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("from")) {
                seenFrom = true;
                break;
            }
            selectExpr += " " + tok;
        }

        if (selectExpr.equals("")) {
            throw new OQLException("query syntax error: 'select' expression can not be empty");
        }

        String className = null;
        boolean isInstanceOf = false;
        String whereExpr = null;
        String identifier = null;

        if (seenFrom) {
            if (st.hasMoreTokens()) {
                String tmp = st.nextToken();
                if (tmp.equals("instanceof")) {
                    isInstanceOf = true;
                    if (!st.hasMoreTokens()) {
                        throw new OQLException("no class name after 'instanceof'");
                    }
                    className = st.nextToken();
                } else {
                    className = tmp;
                }
            } else {
                throw new OQLException("query syntax error: class name must follow 'from'");
            }

            if (st.hasMoreTokens()) {
                identifier = st.nextToken();
                if (identifier.equals("where")) {
                    throw new OQLException("query syntax error: identifier should follow class name");
                }
                if (st.hasMoreTokens()) {
                    String tmp = st.nextToken();
                    if (!tmp.equals("where")) {
                        throw new OQLException("query syntax error: 'where' clause expected after 'from' clause");
                    }

                    whereExpr = "";
                    while (st.hasMoreTokens()) {
                        whereExpr += " " + st.nextToken();
                    }
                    if (whereExpr.equals("")) {
                        throw new OQLException("query syntax error: 'where' clause cannot have empty expression");
                    }
                }
            } else {
                throw new OQLException("query syntax error: identifier should follow class name");
            }
        }

        executeQuery(new OQLQuery(selectExpr, isInstanceOf, className,
                identifier, whereExpr), visitor);
    }

    private void executeQuery(OQLQuery q, ObjectVisitor visitor)
            throws OQLException {
        visitor = visitor != null ? visitor : ObjectVisitor.DEFAULT;

        JavaClass clazz = null;
        if (q.className != null) {
            String className = q.className;

            clazz = snapshot.findClass(className);
            if (clazz == null) {
                throw new OQLException(className + " is not found!");
            }
        }

        StringBuffer buf = new StringBuffer();
        buf.append("function __select__(");
        if (q.identifier != null) {
            buf.append(q.identifier);
        }
        buf.append(") { return ");
        buf.append(q.selectExpr.replace('\n', ' '));
        buf.append("; }\n");
        buf.append("__select__(" + q.identifier + ")");

        String selectCode = buf.toString();

        // compile select expression and where condition 
        try {
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

            CompiledScript whereCs = null;
            CompiledScript selectCs = null;
            selectCs = ((Compilable)engine).compile(selectCode);
            
            if (q.whereExpr != null) {
                whereCs = ((Compilable)engine).compile(q.whereExpr.replace('\n', ' '));
            }

            if (q.className != null) {
//                Enumeration objects = clazz.getInstances(q.isInstanceOf);

                Stack toInspect = new Stack();
                toInspect.push(clazz);

                Object inspecting = null;
                while(!toInspect.isEmpty()) {
                    inspecting = toInspect.pop();
                    JavaClass clz = (JavaClass)inspecting;
                    if (q.isInstanceOf) {
                        for(Object subclass : clz.getSubClasses()) {
                            toInspect.push(subclass);
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
//                if (visitor.visit(unwrapJavaObject(iter.next()))) return true;
            }
            return false;
        } else if (jsObject instanceof Enumeration) {
            Enumeration enm = (Enumeration) jsObject;
            while (enm.hasMoreElements()) {
                Object elem = enm.nextElement();
                if (dispatchValue(elem, visitor)) return true;
//                if (elem != null) {
//                    elem = unwrapJavaObject(elem);
//
//                    if (visitor.visit(unwrapJavaObject(elem))) return true;
//                }
            }
            return false;
        } else {

            Object object = unwrapJavaObject(jsObject, true);
            if (object instanceof Object[]) {
                for (Object obj1 : (Object[]) object) {
                    if (dispatchValue(obj1, visitor)) return true;
//                    if (visitor.visit(unwrapJavaObject(obj1, true))) {
//                        return true;
//                    }
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
        return call("wrapJavaObject", new Object[]{obj});
    }

    public Object toHtml(Object obj) throws Exception {
        return call("toHtml", new Object[]{obj});
    }

    public Object call(String func, Object[] args) throws Exception {

        return ((Invocable)engine).invokeFunction(func, args);
    }

    public Object unwrapJavaObject(Object object) {
        return unwrapJavaObject(object, false);
    }

    public Object unwrapJavaObject(Object object, boolean tryAssociativeArray) {
        if (object == null) return null;
        boolean isNativeJS = object.getClass().getName().contains(".javascript.");

        try {
            Object ret = ((Invocable)engine).invokeFunction("unwrapJavaObject", object);
            if (isNativeJS && (ret == null || ret == object) && tryAssociativeArray) {
                ret = ((Invocable)engine).invokeFunction("unwrapMap", object);
            }
            return ret;
        } catch (Exception ex) {
            if (debug) {
                ex.printStackTrace(System.err);
            }
        }
        return null;
    }

    private static void debugPrint(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    private void init(Snapshot snapshot) throws RuntimeException {
        this.snapshot = snapshot;
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            engine = manager.getEngineByName("JavaScript");
            InputStream strm = getInitStream();
            CompiledScript cs = ((Compilable)engine).compile(new InputStreamReader(strm));
            cs.eval();
            Object heap = ((Invocable)engine).invokeFunction("wrapHeapSnapshot", snapshot);
            engine.put("heap", heap);
        } catch (Exception e) {
            if (debug) {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    private InputStream getInitStream() {
        return getClass().getResourceAsStream("/org/netbeans/modules/profiler/heapwalk/oql/resources/hat.js");
    }
    private ScriptEngine engine;
    private Snapshot snapshot;
    private static boolean debug = true;
    private static boolean oqlSupported;
}
