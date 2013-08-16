/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.lib.profiler.heap;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
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
        List result = heap.getAllClasses();
        assertEquals(443, result.size());
    }

    /**
     * Test of getGCRoot method, of class Heap.
     */
    @Test
    public void testGetGCRoot() {
        System.out.println("getGCRoot");
        Instance instance_2 = heap.getInstanceByID(1684081264);
        GCRoot result = heap.getGCRoot(instance_2);
        assertEquals(instance_2, result.getInstance());
        assertEquals(GCRoot.MONITOR_USED, result.getKind());
    }

    /**
     * Test of getGCRoots method, of class Heap.
     */
    @Test
    public void testGetGCRoots() {
        System.out.println("getGCRoots");
        Collection result = heap.getGCRoots();
        assertEquals(404, result.size());
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
        Instance printStreamInstance = (Instance) printStream.getInstances().get(0);
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
        Collection result = heap.getJavaClassesByRegExp(regexp);
        assertEquals(9, result.size());
    }

    /**
     * Test of getSummary method, of class Heap.
     */
    @Test
    public void testGetSummary() {
        System.out.println("getSummary");
        HeapSummary result = heap.getSummary();
        assertEquals(139369,result.getTotalLiveBytes());
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
        List result = heap.getBiggestObjectsByRetainedSize(2);
        Instance i1 = (Instance) result.get(0);
        Instance i2 = (Instance) result.get(1);
        assertEquals(66596, i1.getRetainedSize());
        assertEquals(25056, i2.getRetainedSize());
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
        
        assertEquals(18044, string.getRetainedSizeByClass());
        assertEquals(11315, hashMap.getRetainedSizeByClass());
        assertEquals(566, array.getRetainedSizeByClass());
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
        Collection classes = heap.getAllClasses();
        out.println("Classes size "+classes.size());
        out.println("System properties: ");
        for(Object en : heap.getSystemProperties().entrySet()) {
            Map.Entry entry = (Map.Entry) en;
            
            out.println(entry.getKey()+" "+entry.getValue());
        }
        for (Object c : classes) {
            JavaClass jc = (JavaClass) c;
            JavaClass sc = jc.getSuperClass();
            out.println(" Id 0x"+Long.toHexString(jc.getJavaClassId())+" Class "+jc.getName()+" SuperClass "+(sc==null?"null":sc.getName())+
                    " Instance size "+jc.getInstanceSize()+" Instance count "+jc.getInstancesCount()+" All Instances Size "+jc.getAllInstancesSize());
            
            for (Object v : jc.getStaticFieldValues()) {
                FieldValue fv = (FieldValue) v;

                out.println("  Static Field name "+fv.getField().getName()+" type "+fv.getField().getType().getName()+" value "+fv.getValue());
                if (fv instanceof ObjectFieldValue) {
                    ObjectFieldValue objectField = (ObjectFieldValue)fv;
                    Instance refInstance = objectField.getInstance();
                    if (refInstance != null) {
                        out.println("   Ref object "+refInstance.getJavaClass().getName()+"#"+refInstance.getInstanceNumber());
                    }
                }
            }
                        
            for (Object f : jc.getFields()) {
                Field in = (Field) f;
                
                out.println("  Field name "+in.getName()+" type "+in.getType().getName());
            }
            
            for (Object i : jc.getInstances()) {
                Instance in = (Instance) i;
                
                out.println("  Instance Id 0x"+Long.toHexString(in.getInstanceId())+" number "+in.getInstanceNumber()+" retained size "+in.getRetainedSize());
                
                for (Object v : in.getFieldValues()) {
                    FieldValue inField = (FieldValue) v;
                    
                    out.println("   Instance Field name "+inField.getField().getName()+" type "+inField.getField().getType().getName()+" value "+inField.getValue());
                    if (inField instanceof ObjectFieldValue) {
                        ObjectFieldValue objectField = (ObjectFieldValue) inField;
                        Instance refInstance = objectField.getInstance();
                        if (refInstance != null) {
                            out.println("    Ref object "+refInstance.getJavaClass().getName()+"#"+refInstance.getInstanceNumber());
                        }
                    }
                }
                Collection references = in.getReferences();
                out.println("   References count "+references.size());
                for (Object v : references) {
                    Value val = (Value) v;
                    
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
        Collection roots = heap.getGCRoots();
        roots = sortGCRoots(roots);
        out.println("GC roots "+roots.size());
        
        for(Object g : roots) {
            GCRoot root = (GCRoot) g;
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

    private Collection sortGCRoots(Collection roots) {
        List r = new ArrayList(roots);
        Collections.sort(r, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                GCRoot r1 = (GCRoot)o1;
                GCRoot r2 = (GCRoot)o2;
                int kind = r1.getKind().compareTo(r2.getKind());
                
                if (kind != 0) {
                    return kind;
                }
                
                long x = r1.getInstance().getInstanceId();
                long y = r2.getInstance().getInstanceId();                
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }
            
        });
        return r;
    }
}
