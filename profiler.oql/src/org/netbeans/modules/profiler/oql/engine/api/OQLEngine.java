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
package org.netbeans.modules.profiler.oql.engine.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.modules.profiler.oql.engine.api.impl.OQLEngineImpl;
import org.netbeans.modules.profiler.oql.engine.api.impl.Snapshot;

/**
 * This is Object Query Language Interpreter
 *
 * @authoe J. Bachorik
 */
public class OQLEngine {
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
        delegate = new OQLEngineImpl(new Snapshot(heap));
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
}
