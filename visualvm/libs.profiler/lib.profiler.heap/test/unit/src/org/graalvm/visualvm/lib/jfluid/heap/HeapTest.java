/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.io.BufferedOutputStream;
import java.util.Map;
import java.util.Date;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tomas Hurka
 */
public class HeapTest {
    private Heap heap;

    public HeapTest() {
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
        heap = HeapFactory.createHeap(new File(url.toURI()));
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getAllClasses method, of class Heap.
     */
    @Test
    public void testGetAllClasses() {
        System.out.println("getAllClasses");
        List<JavaClass> result = heap.getAllClasses();
        assertEquals(443, result.size());
    }

    /**
     * Test of getGCRoot method, of class Heap.
     */
    @Test
    public void testGetGCRoot() {
        System.out.println("getGCRoot");
        Instance instance_2 = heap.getInstanceByID(1684081264);
        Collection<GCRoot> resultList = heap.getGCRoots(instance_2);
        assertEquals(resultList.size(), 2);
        GCRoot[] results = resultList.toArray(new GCRoot[0]);
        GCRoot result = results[0];
        assertEquals(instance_2, result.getInstance());
        assertEquals(GCRoot.JAVA_FRAME, result.getKind());
        result = results[1];
        assertEquals(instance_2, result.getInstance());
        assertEquals(GCRoot.MONITOR_USED, result.getKind());
    }

    /**
     * Test of getGCRoots method, of class Heap.
     */
    @Test
    public void testGetGCRoots() {
        System.out.println("getGCRoots");
        Collection<GCRoot> result = heap.getGCRoots();
        assertEquals(491, result.size());
    }
    
    /**
     * Test of getJavaClassByName method, of class Heap.
     */
    @Test
    public void testGetJavaClassByName() {
        System.out.println("getJavaClassByName");
        String fqn = "java.io.PrintStream";
        JavaClass result = heap.getJavaClassByName(fqn);
        assertEquals(fqn, result.getName());
    }
    
    /**
     * Test of getInstanceByID method, of class Heap.
     */
    @Test
    public void testGetInstanceByID() {
        System.out.println("getInstanceByID");
        String fqn = "java.io.PrintStream";
        JavaClass printStream = heap.getJavaClassByName(fqn);
        Instance printStreamInstance = printStream.getInstances().get(0);
        long instanceId = printStreamInstance.getInstanceId();
        Instance result = heap.getInstanceByID(instanceId);
        assertEquals(instanceId, result.getInstanceId());
    }

    /**
     * Test of getJavaClassByID method, of class Heap.
     */
    @Test
    public void testGetJavaClassByID() {
        System.out.println("getJavaClassByID");
        String fqn = "java.io.PrintStream";
        JavaClass printStream = heap.getJavaClassByName(fqn);
        long javaclassId = printStream.getJavaClassId();
        JavaClass result = heap.getJavaClassByID(javaclassId);
        assertEquals(javaclassId, result.getJavaClassId());
    }

    /**
     * Test of getJavaClassesByRegExp method, of class Heap.
     */
    @Test
    public void testGetJavaClassesByRegExp() {
        System.out.println("getJavaClassesByRegExp");
        String regexp = ".*Lock.*";
        Collection<JavaClass> result = heap.getJavaClassesByRegExp(regexp);
        assertEquals(9, result.size());
    }

    /**
     * Test of getSummary method, of class Heap.
     */
    @Test
    public void testGetSummary() {
        System.out.println("getSummary");
        HeapSummary result = heap.getSummary();
        assertEquals(140120,result.getTotalLiveBytes());
        assertEquals(2208,result.getTotalLiveInstances());
    }

    /**
     * Test of getSystemProperties method, of class Heap.
     */
    @Test
    public void testGetSystemProperties() {
        System.out.println("getSystemProperties");
        Properties result = heap.getSystemProperties();
        assertEquals("2.4.18-openmosix4", result.getProperty("os.version"));
    }

    /**
     * Test of getBiggestObjectsByRetainedSize method, of class Heap.
     */
    @Test
    public void testGetBiggestObjectsByRetainedSize() {
        System.out.println("getBiggestObjectsByRetainedSize");
        List<Instance> result = heap.getBiggestObjectsByRetainedSize(2);
        Instance i1 = result.get(0);
        Instance i2 = result.get(1);
        assertEquals(66544, i1.getRetainedSize());
        assertEquals(25080, i2.getRetainedSize());
    }
    
    /**
     * Test of getRetainedSizeByClass method, of class JavaClass.
     */
    @Test
    public void testGetRetainedSizeByClass() {
        System.out.println("getRetainedSizeByClass");
        JavaClass string = heap.getJavaClassByName(String.class.getName());
        JavaClass hashMap = heap.getJavaClassByName(HashMap.class.getName());
        JavaClass array = heap.getJavaClassByName(ArrayList.class.getName());
        
        assertEquals(17864, string.getRetainedSizeByClass());
        assertEquals(11712, hashMap.getRetainedSizeByClass());
        assertEquals(600, array.getRetainedSizeByClass());
    }

    /**
     * Test of getAllInstancesIterator method, of class Heap.
     */
    @Test
    public void getAllInstancesIterator() {
        System.out.println("getAllInstancesIterator");
        Iterator<Instance> instanceIt = heap.getAllInstancesIterator();
        int instances = 0;
        
        while (instanceIt.hasNext()) {
            Instance i = instanceIt.next();
            instances++;
        }
        assertEquals(instances, heap.getSummary().getTotalLiveInstances());
    }

    /**
     * Test of getInstancesIterator method, of class JavaClass.
     */
    @Test
    public void getInstancesIterator() {
        System.out.println("getInstancesIterator");
        List<JavaClass> classes = heap.getAllClasses();
        
        for (JavaClass clazz : classes) {
            List<Instance> instances = clazz.getInstances();
            Iterator<Instance> instIt = clazz.getInstancesIterator();
            
            for (Instance i : instances) {
                assertTrue(instIt.hasNext());
                assertEquals(i, instIt.next());
            }
            assertFalse(instIt.hasNext());
        }
    }
    
    @Test
    public void testHeapDumpLog() throws IOException, URISyntaxException {
        System.out.println("testHeapDumpLog");
        File outFile = File.createTempFile("testHeapDumpLog", ".txt");
        URL url = getClass().getResource("testHeapDumpLog.txt");
        File goledFile = new File(url.toURI()); 
        OutputStream outs = new FileOutputStream(outFile);
        PrintStream out = new PrintStream(new BufferedOutputStream(outs,128*1024),false,"UTF-8");
        HeapSummary summary = heap.getSummary();
        out.println("Heap Summary");
        DateFormat df = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss aaa");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        out.println("Time "+df.format(new Date(summary.getTime())));
        out.println("Live instances "+summary.getTotalLiveInstances());
        out.println("Live bytes "+summary.getTotalLiveBytes());
        out.println("Total alloc instances "+summary.getTotalAllocatedInstances());
        out.println("Total alloc bytes "+summary.getTotalAllocatedBytes());
        Collection<JavaClass> classes = heap.getAllClasses();
        out.println("Classes size "+classes.size());
        out.println("System properties: ");
        Properties props = heap.getSystemProperties();
        for(Map.Entry<?, ?> entry : new TreeMap<>(props).entrySet()) {
            
            out.println(entry.getKey()+" "+entry.getValue());
        }
        for (JavaClass jc : classes) {
            JavaClass sc = jc.getSuperClass();
            out.println(" Id 0x"+Long.toHexString(jc.getJavaClassId())+" Class "+jc.getName()+" SuperClass "+(sc==null?"null":sc.getName())+
                    " Instance size "+jc.getInstanceSize()+" Instance count "+jc.getInstancesCount()+" All Instances Size "+jc.getAllInstancesSize());
            
            for (FieldValue fv : jc.getStaticFieldValues()) {
                out.println("  Static Field name "+fv.getField().getName()+" type "+fv.getField().getType().getName()+" value "+fv.getValue());
                if (fv instanceof ObjectFieldValue) {
                    ObjectFieldValue objectField = (ObjectFieldValue)fv;
                    Instance refInstance = objectField.getInstance();
                    if (refInstance != null) {
                        out.println("   Ref object "+refInstance.getJavaClass().getName()+"#"+refInstance.getInstanceNumber());
                    }
                }
            }
                        
            for (Field in : jc.getFields()) {
                out.println("  Field name "+in.getName()+" type "+in.getType().getName());
            }
            
            for (Instance in : jc.getInstances()) {
                out.println("  Instance Id 0x"+Long.toHexString(in.getInstanceId())+" number "+in.getInstanceNumber()+" retained size "+in.getRetainedSize());
                
                for (FieldValue inField : in.getFieldValues()) {
                    out.println("   Instance Field name "+inField.getField().getName()+" type "+inField.getField().getType().getName()+" value "+inField.getValue());
                    if (inField instanceof ObjectFieldValue) {
                        ObjectFieldValue objectField = (ObjectFieldValue) inField;
                        Instance refInstance = objectField.getInstance();
                        if (refInstance != null) {
                            out.println("    Ref object "+refInstance.getJavaClass().getName()+"#"+refInstance.getInstanceNumber());
                        }
                    }
                }
                Collection<Value> references = in.getReferences();
                out.println("   References count "+references.size());
                for (Value val : references) {
                    
                    if (val instanceof ArrayItemValue) {
                        ArrayItemValue arrVal = (ArrayItemValue) val;
                        
                        out.println("   Element "+arrVal.getIndex()+" of array 0x"+Long.toHexString(arrVal.getDefiningInstance().getInstanceId()));
                    } else if (val instanceof FieldValue) {
                        FieldValue fieldVal = (FieldValue) val;
                        Field f = fieldVal.getField();
                        
                        if (f.isStatic()) {
                            out.println("   Field "+f.getName()+" of Class "+f.getDeclaringClass().getName());
                        } else {
                            out.println("   Field "+f.getName()+" of instance 0x"+Long.toHexString(fieldVal.getDefiningInstance().getInstanceId()));
                        }
                    } else {
                        out.println("   Error "+val);
                    }
                }
                out.println("   Path to nearest GC root");
                Instance p = in;
                Instance next = p.getNearestGCRootPointer();
                while (!p.equals(next)) {
                    if (next == null) {
                        out.println("    Null");
                        break;
                    }
                    out.println("    Next object "+next.getJavaClass().getName()+"#"+next.getInstanceNumber());
                    p = next;
                    next = next.getNearestGCRootPointer();
                }
            }
        }
        Collection<GCRoot> roots = heap.getGCRoots();
        out.println("GC roots "+roots.size());
        
        for(GCRoot root : roots) {
            Instance i = root.getInstance();
            
            out.println("Root kind "+root.getKind()+" Class "+i.getJavaClass().getName()+"#"+i.getInstanceNumber());
        }
        out.close();
        compareTextFiles(goledFile,outFile);
        outFile.delete();
    }

    private void compareTextFiles(File goledFile, File outFile) throws IOException {
        InputStreamReader goldenIsr = new InputStreamReader(new FileInputStream(goledFile),"UTF-8");
        LineNumberReader goldenReader = new LineNumberReader(goldenIsr);
        InputStreamReader isr = new InputStreamReader(new FileInputStream(outFile),"UTF-8");
        LineNumberReader reader = new LineNumberReader(isr);
        String goldenLine = "";
        String line = "";
        
        while(goldenLine != null && goldenLine.equals(line)) {
            goldenLine = goldenReader.readLine();
            line = reader.readLine();
        }
        assertEquals("File "+goledFile.getAbsolutePath()+" and "+outFile.getAbsolutePath()+" differs on line "+goldenReader.getLineNumber(), goldenLine, line);
    }
}
