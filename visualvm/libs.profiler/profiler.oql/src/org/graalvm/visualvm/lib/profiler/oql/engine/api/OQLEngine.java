/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.oql.engine.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.impl.OQLEngineImpl;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.impl.Snapshot;

/**
 * This is Object Query Language Interpreter
 *
 * @author J. Bachorik
 */
final public class OQLEngine {
    final private static Logger LOGGER = Logger.getLogger(OQLEngine.class.getName());
    final private OQLEngineImpl delegate;
    final private Heap heap;

    /**
     * This represents a parsed OQL query
     *
     * @author A. Sundararajan
     */
    public static abstract class OQLQuery {

    }

    /**
     * This visitor is supplied to OQLEngine.executeQuery
     * to receive result set objects one by one.
     *
     * @author A. Sundararajan
     * @author J. Bachorik
     */
    public static interface ObjectVisitor {
        // return true to terminate the result set callback earlier
        public boolean visit(Object o);

        public static final ObjectVisitor DEFAULT = new ObjectVisitor() {

            public boolean visit(Object o) {
                if (o != null && LOGGER.isLoggable(Level.FINEST)) LOGGER.finest(o.toString());

                return true; // prevent calling "visit" for the rest of the result set
            }
        };
    }

    // check OQL is supported or not before creating OQLEngine
    public static boolean isOQLSupported() {
        return OQLEngineImpl.isOQLSupported();
    }

    public OQLEngine(Heap heap) {
        delegate = new OQLEngineImpl(new Snapshot(heap, this));
        this.heap = heap;
    }

    public Heap getHeap() {
        return heap;
    }

    /**
    Query is of the form

    select &lt;java script code to select&gt;
    [ from [instanceof] &lt;class name&gt; [&lt;identifier&gt;]
    [ where &lt;java script boolean expression&gt; ]
    ]
     */
    public void executeQuery(String query, ObjectVisitor visitor)
            throws OQLException {
        delegate.executeQuery(query, visitor);
    }

    public OQLQuery parseQuery(String query) throws OQLException {
        return delegate.parseQuery(query);
    }

    public void cancelQuery() throws OQLException {
        delegate.cancelQuery();
    }

    public Object unwrapJavaObject(Object object) {
        return delegate.unwrapJavaObject(object);
    }

    public Object unwrapJavaObject(Object object, boolean tryAssociativeArray) {
        return delegate.unwrapJavaObject(object, tryAssociativeArray);
    }

    public boolean isCancelled() {
        return delegate.isCancelled();
    }
}
