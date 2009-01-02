/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler.heapwalk.oql;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.lib.profiler.heap.JavaClass;
import static org.junit.Assert.*;
import org.netbeans.modules.profiler.heapwalk.oql.model.Snapshot;

/**
 *
 * @author Jaroslav Bachorik
 */
public class OQLEngineTest {
    private OQLEngine instance;

    public OQLEngineTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        instance = new OQLEngine(new Snapshot(HeapFactory.createHeap(new File("/home/jb198685/test.hprof"))));
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of executeQuery method, of class OQLEngine.
     */
    @Test
    public void testExecuteQuery() throws Exception {
        System.out.println("executeQuery");
        
        String query = "select filter(heap.classes(), \"/java\\.net\\./(it.name)\")";
        
        final int[] counter = new int[]{0};

        ObjectVisitor visitor = new ObjectVisitor() {

            public boolean visit(Object o) {
                if (o instanceof Iterator) {
                    Iterator e = (Iterator)o;
                    while (e.hasNext()) {
                        Object ob = instance.unwrapJavaObject(e.next());
                        if (ob != null) {
                            System.out.println(((JavaClass)ob).getName());
                        }
                    }
//                    while (e.hasMoreElements()) {
//                        System.out.println("kurva");
//                    }
                }
                counter[0]++;
                return false;
            }
        };

        instance.executeQuery(query, visitor);
        
        assertTrue(counter[0] != 0);
    }

    /**
     * Test of evalScript method, of class OQLEngine.
     */
    @Test
    public void testEvalScript() throws Exception {
        System.out.println("evalScript");
        String script = "select s from java.lang.String s where s.count >= 100";

        Object expResult = null;
        Object result = instance.evalScript(script);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    

    /**
     * Test of call method, of class OQLEngine.
     */
    @Test
    public void testCall() throws Exception {
        System.out.println("call");
        String func = "";
        Object[] args = null;
        OQLEngine instance = null;
        Object expResult = null;
        Object result = instance.call(func, args);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}