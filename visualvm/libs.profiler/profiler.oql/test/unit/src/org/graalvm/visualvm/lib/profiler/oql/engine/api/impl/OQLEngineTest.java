/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.graalvm.visualvm.lib.jfluid.heap.HeapFactory;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLEngine;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLEngine.ObjectVisitor;
import static org.junit.Assert.*;

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
    public void setUp() throws IOException, URISyntaxException {
        URL url = getClass().getResource("small_heap.bin");
        instance = new OQLEngine(HeapFactory.createHeap(new File(url.toURI())));
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAltTypeNames() throws Exception {
        instance.executeQuery("select a from [I a", null);
        instance.executeQuery("select a from [B a", null);
        instance.executeQuery("select a from [C a", null);
        instance.executeQuery("select a from [S a", null);
        instance.executeQuery("select a from [J a", null);
        instance.executeQuery("select a from [F a", null);
        instance.executeQuery("select a from [Z a", null);

        instance.executeQuery("select a from [java.lang.String a", null);

//        try {
//            instance.executeQuery("select a from [[I a", null);
//            fail();
//        } catch (Exception e) {}
//
//        instance.executeQuery("select a from [[B a", null);
//        instance.executeQuery("select a from [[C a", null);
//        instance.executeQuery("select a from [[S a", null);
//        instance.executeQuery("select a from [[J a", null);
//        instance.executeQuery("select a from [[F a", null);
//        instance.executeQuery("select a from [[Z a", null);
//
//        instance.executeQuery("select a from [[java.lang.String a", null);
    }

    @Test
    public void testIntResult() throws Exception {
        final boolean[] rslt = new boolean[]{true};
        instance.executeQuery("select a.count from java.lang.String a", new ObjectVisitor() {

            public boolean visit(Object o) {
                if (!(o instanceof Integer)) {
                    rslt[0] = false;
                    return true;
                }
                return false;
            }
        });
        assertTrue(rslt[0]);
    }

    @Test
    public void testClassFields() throws Exception {
        System.out.println("test class fields");

        final String[] values = new String[] {"", "prefixLength = int"};

        instance.executeQuery("select map(heap.findClass(\"java.io.File\").fields, 'toHtml(it.name) + \" = \" + toHtml(it.signature)')", new ObjectVisitor() {

            public boolean visit(Object o) {
                values[0] = o.toString();
                return true;
            }
        });

        assertEquals(values[1], values[0]);
    }

    @Test
    public void testObjectClass() throws Exception {
        System.out.println("test object class accessor");

        instance.executeQuery("select map(a.clazz.statics, 'toHtml(it)') from java.lang.String a", new ObjectVisitor() {

            public boolean visit(Object o) {
                return true;
            }
        });
    }

    @Test
    public void testHeapForEachClass() throws Exception {
        System.out.println("heap.forEachClass");
        String query = "select heap.forEachClass(function(xxx) { print(xxx.name); print(\"\\n\");})";

        instance.executeQuery(query, null);
    }

    @Test
    public void testHeapForEachObject() throws Exception {
        System.out.println("heap.forEachObject");
        String query = "select heap.forEachObject(function(xxx) { print(xxx.id); print(\"\\n\");}, \"java.io.File\")";

        instance.executeQuery(query, null);
    }

    @Test
    public void testHeapFindObject() throws Exception {
        System.out.println("heap.findObject");
        final int[] counter = new int[1];
        String query = "select heap.findObject(1684166976)";

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

        String query = "select heap.roots()";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                counter[0]++;
                return false;
            }
        });
        assertTrue(counter[0] == 491);
    }

    @Test
    public void testHeapClasses() throws Exception {
        System.out.println("heap.classes");
        final int[] counter = new int[1];

        String query = "select heap.classes()";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                counter[0]++;
                return false;
            }
        });
        assertTrue(counter[0] == 443);
    }

    @Test
    public void testHeapFinalizables() throws Exception {
        System.out.println("heap.finalizables");
        final int[] counter = new int[1];

        String query = "select heap.finalizables()";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                counter[0]++;
                return false;
            }
        });
        assertTrue(counter[0] == 0);
    }

    @Test
    public void testHeapLivePaths() throws Exception {
        System.out.println("heap.livepaths");
        final int[] counter = new int[1];

        String query = "select heap.livepaths(s) from java.lang.String s";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                if (o != null) {
                    counter[0]++;
                    return true;
                } else {
                    return false;
                }
            }
        });
        assertTrue(counter[0] > 0);
    }

    @Test
    public void testHeapObjects() throws Exception {
        System.out.println("heap.objects");

        final int[] count = new int[]{0,0};

        instance.executeQuery("select heap.objects(\"java.io.InputStream\", true)", new ObjectVisitor() {

            public boolean visit(Object o) {
                count[0]++;
                return false;
            }
        });
        instance.executeQuery("select heap.objects(\"java.io.InputStream\", false)", new ObjectVisitor() {

            public boolean visit(Object o) {
                count[1]++;
                return false;
            }
        });

        assertNotSame(count[0], count[1]);

        assertEquals(4, count[0]);
        assertEquals(0, count[1]);
    }

    @Test
    public void testSubclasses() throws Exception {
        System.out.println("subclasses");
        final int[] counter = new int[1];

        String query = "select heap.findClass(\"java.io.InputStream\").subclasses()";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(((JavaClass)o).getName());
                counter[0]++;
                return false;
            }
        });
        assertTrue(counter[0] > 0);
    }

    @Test
    public void testSuperlasses() throws Exception {
        System.out.println("superclasses");
        final int[] counter = new int[1];

        String query = "select heap.findClass(\"java.io.DataInputStream\").superclasses()";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(((JavaClass)o).getName());
                counter[0]++;
                return true;
            }
        });
        assertTrue(counter[0] > 0);
    }

    @Test
    public void testforEachReferrer() throws Exception {
        System.out.println("forEachReferrer");

        String query = "select forEachReferrer(function(xxx) { print(\"referrer: \" + xxx.id); print(\"\\n\");}, heap.findObject(1684166976))";

        instance.executeQuery(query, null);
    }

    @Test
    public void testforEachReferee() throws Exception {
        System.out.println("forEachReferee");

        String query = "select forEachReferee(function(xxx) { print(\"referee: \" + xxx.id); print(\"\\n\");}, heap.findObject(1684166976))";

        instance.executeQuery(query, null);
    }

    @Test
    public void testReferrersInstance() throws Exception {
        System.out.println("referrers-instance");

        String query = "select referrers(heap.findObject(1684166976))";
        long[] referrersTest = new long[] {1684166952};
        final List<Long> referrers = new ArrayList<>();

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                referrers.add(((Instance)o).getInstanceId());
                return false;
            }
        });

        assertEquals(referrersTest.length, referrers.size());
        for(long referee : referrersTest) {
            if (!referrers.contains(referee)) fail();
        }
    }

    @Test
    public void testRefereesInstance() throws Exception {
        System.out.println("referees-instance");

        String query = "select referees(heap.findObject(1684166976))";
        long[] refereesTest = new long[] {1684166992};
        final List<Long> referees = new ArrayList<>();

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                referees.add(((Instance)o).getInstanceId());
                return false;
            }
        });

        assertEquals(refereesTest.length, referees.size());
        for(long referee : refereesTest) {
            if (!referees.contains(referee)) fail();
        }
    }

    @Test
    public void testRefereesClass() throws Exception {
        System.out.println("referees-class");

        String query = "select referees(heap.findClass(\"java.io.File\"))";
        long[] refereesTest = new long[] {1684106928, 1684106888, 1684106848, 1684106408};
        final List<Long> referees = new ArrayList<>();

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                referees.add(((Instance)o).getInstanceId());
                return false;
            }
        });

        assertEquals(refereesTest.length, referees.size());
        for(long referee : refereesTest) {
            if (!referees.contains(referee)) fail();
        }
    }

    @Test
    public void testRefers() throws Exception  {
        System.out.println("refers");

        String query = "select refers(heap.findObject(1684166976), heap.findObject(1684166992))";

        final boolean[] result = new boolean[1];

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                result[0] = (Boolean)o;
                return true;
            }
        });
        assertTrue(result[0]);

        query = "select refers(heap.findObject(1684166992), heap.findObject(1684166976))";
        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                result[0] = (Boolean)o;
                return true;
            }
        });
        assertFalse(result[0]);
    }

    @Test
    public void testReachables() throws Exception {
        System.out.println("reachables");
        final int count[] = new int[1];

        String query = "select reachables(p) from java.util.Properties p";

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                count[0]++;
                return false;
            }
        });
        assertEquals(352, count[0]);
    }

    @Test
    public void testInstanceOf() throws Exception {
        System.out.println("instanceof");

        String query = "select classof(cl).name from instanceof java.lang.ClassLoader cl";
        final int[] counter = new int[1];

        instance.executeQuery(query, new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(o);
                counter[0]++;
                return false;
            }
        });
        assertEquals(2, counter[0]); // although there is 8 subclasses of ClassLoader only 2 of them have instances
    }

    @Test
    public void testSizeOf() throws Exception {
        System.out.println("sizeof");
        final int[] counter = new int[1];

        instance.executeQuery("select sizeof(o) from [I o", new ObjectVisitor() {

            public boolean visit(Object o) {
                if (o instanceof Number) counter[0]++;
                return false;
            }
        });

        assertTrue(counter[0] > 0);
    }

    @Test
    public void testRoot() throws Exception {
        System.out.println("root");

        final int[] count = new int[1];

        instance.executeQuery("select root(heap.findObject(1684166976))", new ObjectVisitor() {

            public boolean visit(Object o) {
                count[0]++;
                return false;
            }
        });

        assertTrue(count[0] > 0);
    }

    @Test
    public void testContains() throws Exception {
        System.out.println("contains");

        final int[] count = new int[1];

        instance.executeQuery("select s from java.lang.String s where contains(referrers(s), \"classof(it).name == 'java.lang.Class'\")", new ObjectVisitor() {

            public boolean visit(Object o) {
                count[0]++;
                return false;
            }
        });

        assertTrue(count[0] > 0);
    }

    @Test
    public void testMap() throws Exception {
        System.out.println("map");

        final String[] output = new String[] {"", "$assertionsDisabled=true\nserialVersionUID=301077366599181567\ntmpdir=null\ncounter=-1\ntmpFileLock=<a href='file://instance/1684106928' name='1684106928'>java.lang.Object#6</a>\npathSeparator=<a href='file://instance/1684106888' name='1684106888'>java.lang.String#101</a>\npathSeparatorChar=:\nseparator=<a href='file://instance/1684106848' name='1684106848'>java.lang.String#100</a>\nseparatorChar=/\nfs=<a href='file://instance/1684106408' name='1684106408'>java.io.UnixFileSystem#1</a>\n<classLoader>=null\n"};

        instance.executeQuery("select map(heap.findClass(\"java.io.File\").statics, \"index + '=' + toHtml(it)\")", new ObjectVisitor() {

            public boolean visit(Object o) {
                output[0] += o.toString() + "\n";
                return false;
            }
        });
        assertEquals(output[1], output[0]);
    }

    @Test
    public void testFilter() throws Exception {
        System.out.println("filter");

        final int[] size = new int[]{0};
        final boolean sorted[] = new boolean[] {true};


        instance.executeQuery("select map(sort(filter(heap.objects('[C'), 'it.length > 0'), 'sizeof(lhs) - sizeof(rhs)'), \"sizeof(it)\")", new ObjectVisitor() {

            public boolean visit(Object o) {
                int aSize = ((Number)o).intValue();
                if (aSize < size[0]) {
                    sorted[0] = false;
                    return true;
                }
                size[0] = aSize;
                return false;
            }
        });

        assertTrue(sorted[0]);
    }

    @Test
    public void testSort() throws Exception {
        System.out.println("sort");

        final int[] size = new int[]{0};
        final boolean sorted[] = new boolean[] {true};


        instance.executeQuery("select map(sort(heap.objects('[C'), 'sizeof(lhs) - sizeof(rhs)'), \"sizeof(it)\")", new ObjectVisitor() {

            public boolean visit(Object o) {
                int aSize = ((Number)o).intValue();
                if (aSize < size[0]) {
                    sorted[0] = false;
                    return true;
                }
                size[0] = aSize;
                return false;
            }
        });

        assertTrue(sorted[0]);
    }

    @Test
    public void testLength() throws Exception {
        System.out.println("length");

        final Class[] rsltClass = new Class[1];
//        final boolean sorted[] = new boolean[] {true};


        instance.executeQuery("select length(a.value) from java.lang.String a", new ObjectVisitor() {

            public boolean visit(Object o) {
                rsltClass[0] = o.getClass();
                return true;
            }
        });

        assertTrue(Number.class.isAssignableFrom(rsltClass[0]));
    }

    @Test
    public void testCountNoClosure() throws Exception {
        System.out.println("count - no closure");

        final Class[] rsltClass = new Class[1];
//        final boolean sorted[] = new boolean[] {true};


        instance.executeQuery("select count(a.value) from java.lang.String a", new ObjectVisitor() {

            public boolean visit(Object o) {
                rsltClass[0] = o.getClass();
                return true;
            }
        });

        assertTrue(Number.class.isAssignableFrom(rsltClass[0]));
    }

    @Test
    public void testCount() throws Exception {
        System.out.println("count");

        final Class[] rsltClass = new Class[1];
//        final boolean sorted[] = new boolean[] {true};


        instance.executeQuery("select count(a.value, 'true') from java.lang.String a", new ObjectVisitor() {

            public boolean visit(Object o) {
                rsltClass[0] = o.getClass();
                return true;
            }
        });

        assertEquals(Double.class, rsltClass[0]);
    }

    @Test
    public void testMultivalue() throws Exception {
        System.out.println("multi-value");

        final Class[] rsltClass = new Class[1];
//        final boolean sorted[] = new boolean[] {true};


        instance.executeQuery("select { name: t.name? t.name.toString() : \"null\", thread: t }  from instanceof java.lang.Thread t", new ObjectVisitor() {

            public boolean visit(Object o) {
                rsltClass[0] = o.getClass();
                return true;
            }
        });

        assertTrue(Map.class.isAssignableFrom(rsltClass[0]));
    }

    @Test
    public void testComplexStatement1() throws Exception {
        System.out.println("complex statement 1");

        final String[] rslt = new String[1];

        instance.executeQuery(
            "select map(filter(heap.findClass('java.lang.System').statics.props.table, 'it != null && it.key != null && it.value != null'),  " +
                "function (it) { " +
                    "return 'MapEntry{' + it.key.toString() + ' = ' + it.value.toString() + '}' ;" +
                "}" +
            ")", new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(o);
                rslt[0] = o.toString();
                return true;
            }
        });

        assertEquals("MapEntry{sun.cpu.isalist = }", rslt[0]);
    }

    @Test
    public void testComplexStatement2() throws Exception {
        System.out.println("complex statement 2");

        final String[] rslt = new String[1];

        instance.executeQuery(
            "select map(filter(heap.findClass('java.lang.System').statics.props.table, 'it != null && it.key != null && it.value != null'), " +
            "'{ key: it.key.toString(), value: it.value.toString() }')", new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(o);
                rslt[0] = o.toString();
                return true;
            }
        });
        assertEquals("{value=, key=sun.cpu.isalist}", rslt[0]);
    }

    @Test
    public void testMapWrapping() throws Exception {
        System.out.println("map wrapping");

        final String[] result = new String[] {"", "<a href='file://class/1746081976' name='1746081976'>class java.util.HashMap$Entry[]</a>"};

        instance.executeQuery("select unique(map(filter(reachables(a), 'it != null'), 'toHtml(it.clazz)')) from instanceof java.util.HashMap a", new ObjectVisitor() {

            public boolean visit(Object o) {
                result[0] = o.toString();
                return true;
            }
        });

        assertEquals(result[1], result[0]);
    }

    @Test
    public void testUnwrapIterator() throws Exception {
        System.out.println("unwrap iterator");

        instance.executeQuery("select map(filter(a.table, 'it != null'), 'reachables(it)') from instanceof java.util.HashMap a", new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(o);
                return true;
            }
        });
    }

    @Test
    public void testUnwrapIteratorComplex() throws Exception {
        System.out.println("unwrap iterator complex");

        instance.executeQuery("select map(map(filter(a.table, 'it != null'), 'reachables(it)'), 'it.clazz.statics') from instanceof java.util.HashMap a", new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(o);
                return true;
            }
        });
    }

    @Test
    public void testTop() throws Exception {
        System.out.println("top 5");

        instance.executeQuery("select top(heap.objects('java.lang.String', false, '(2 * it.offset) + (2 * (it.value.length - (1*it.count + 1*it.offset))) > 0'), '((2 * rhs.offset) + (2 * (rhs.value.length - (1*rhs.count + 1*rhs.offset)))) - ((2 * lhs.offset) + (2 * (lhs.value.length - (1*lhs.count + 1*lhs.offset))))')", new ObjectVisitor() {

            public boolean visit(Object o) {
                System.out.println(o);
                return false;
            }
        });
    }
}
