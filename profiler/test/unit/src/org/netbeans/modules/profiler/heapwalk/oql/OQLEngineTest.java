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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.lib.profiler.heap.HeapFactory;
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

    @Test
    public void testHeapForEachClass() throws Exception {
        System.out.println("heap.forEachClass");
        String query = "select heap.forEachClass(function(xxx) { println(xxx.name); })";

        instance.executeQuery(query, null);
    }

    @Test
    public void testHeapForEachObject() throws Exception {
        System.out.println("heap.forEachObject");
        String query = "select heap.forEachObject(function(xxx) { println(xxx.id); }, \"java.net.InetAddress\")";

        instance.executeQuery(query, null);
    }

    @Test
    public void testHeapFindObject() throws Exception {
        System.out.println("heap.findObject");
        final int[] counter = new int[1];
        String query = "select heap.findObject(2834622440)";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                counter[0]++;
                return true;
            }
        });
        assertTrue(counter[0] > 0);
    }

    @Test
    public void testHeapRoots() throws Exception {
        System.out.println("heap.roots");
        final int[] counter = new int[1];

        String query = "select heap.roots";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                counter[0]++;
                return true;
            }
        });
        assertTrue(counter[0] > 0);
    }

    @Test
    public void testHeapClasses() throws Exception {
        System.out.println("heap.classes");
        final int[] counter = new int[1];

        String query = "select heap.classes";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                counter[0]++;
                return true;
            }
        });
        assertTrue(counter[0] > 0);
    }

    @Test
    public void testHeapFinalizables() throws Exception {
        System.out.println("heap.finalizables");
        final int[] counter = new int[1];

        String query = "select heap.finalizables";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                counter[0]++;
                return true;
            }
        });
        assertTrue(counter[0] > 0);
    }

    @Test
    public void testHeapLivePaths() throws Exception {
        System.out.println("heap.livepaths");
        final int[] counter = new int[1];

        String query = "select heap.livepaths(s) from java.lang.String s";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                counter[0]++;
                return true;
            }
        });
        assertTrue(counter[0] > 0);
    }

//    @Test
//    public void testClassof() throws Exception {
//        System.out.println("classof");
//        final int[] counter = new int[1];
//
//        String query = "select classof(o).name from instanceof java.util.Collection o";
//
//        instance.executeQuery(query, new ObjectVisitor() {
//
//            public boolean visit(Object o) {
//                System.out.println(instance.unwrapJavaObject(o));
//                counter[0]++;
//                return true;
//            }
//        });
//        assertTrue(counter[0] > 0);
//    }

    @Test
    public void testSubclasses() throws Exception {
        System.out.println("subclasses");
        final int[] counter = new int[1];

        String query = "select heap.findClass(\"java.io.InputStream\").subclasses()";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(instance.unwrapJavaObject(o));
                counter[0]++;
                return true;
            }
        });
        assertTrue(counter[0] > 0);
    }

    @Test
    public void testSuperlasses() throws Exception {
        System.out.println("superclasses");
        final int[] counter = new int[1];

        String query = "select heap.findClass(\"java.io.BufferedInputStream\").superclasses()";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(instance.unwrapJavaObject(o));
                counter[0]++;
                return true;
            }
        });
        assertTrue(counter[0] > 0);
    }
}