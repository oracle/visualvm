/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * InstrumentationTest.java
 * JUnit based test
 *
 * Created on November 7, 2006, 2:14 PM
 */
package org.graalvm.visualvm.lib.jfluid.tests.jfluid.perf;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import junit.framework.*;
import junit.textui.TestRunner;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.classfile.ClassRepository;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.instrumentation.Instrumentor;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.CommonProfilerTestCase;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.InstrumentMethodGroupResponse;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.RootClassLoadedCommand;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbPerformanceTest;


/**
 *
 * @author ehucka
 */
public class InstrumentationTest extends CommonProfilerTestCase implements NbPerformanceTest {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    ArrayList<NbPerformanceTest.PerformanceData> data = new ArrayList();
    ProfilerEngineSettings settings;
    String[] classNames;
    byte[][] classesBytes;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public InstrumentationTest(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return NbModuleSuite.create(
            NbModuleSuite.createConfiguration(InstrumentationTest.class).addTest(
            "testJ2SE",
            "testJaxb",
            "testJaxbNoGettersEmpties",
            "testSimple",
            "testSimpleNoEmpties",
            "testSimpleNoGetters")
            .honorAutoloadEager(true).enableModules(".*").clusters(".*").gui(false));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public NbPerformanceTest.PerformanceData[] getPerformanceData() {
        return data.toArray(new NbPerformanceTest.PerformanceData[0]);
    }

    public void reportPerformance(String name, long value, String unit) {
        NbPerformanceTest.PerformanceData d = new NbPerformanceTest.PerformanceData();
        d.name = name;
        d.value = value;
        d.unit = unit;
        d.threshold = NbPerformanceTest.PerformanceData.NO_THRESHOLD;
        data.add(d);
    }

    public void testJ2SE() {
        try {
            String jarPath = "/perfdata/j2se-simple.jar";
            File f = new File(getDataDir(), jarPath);
            assertTrue("Instrumented jar file doesn't exist.", f.exists());
            initTest(f.getAbsolutePath());
            startInstrumentationTest(f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    public void testJaxb() {
        try {
            String jarPath = "/perfdata/jaxb-xjc.jar";
            File f = new File(getDataDir(), jarPath);
            assertTrue("Instrumented jar file doesn't exist.", f.exists());
            initTest(f.getAbsolutePath());
            startInstrumentationTest(f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    public void testJaxbNoGettersEmpties() {
        try {
            String jarPath = "/perfdata/jaxb-xjc.jar";
            File f = new File(getDataDir(), jarPath);
            assertTrue("Instrumented jar file doesn't exist.", f.exists());
            initTest(f.getAbsolutePath());
            settings.setInstrumentGetterSetterMethods(false);
            settings.setInstrumentEmptyMethods(false);
            startInstrumentationTest(f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    public void testSimple() {
        try {
            String jarPath = "/perfdata/oneclass.jar";
            File f = new File(getDataDir(), jarPath);
            assertTrue("Instrumented jar file doesn't exist.", f.exists());
            initTest(f.getAbsolutePath());
            startInstrumentationTest(f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    public void testSimpleNoEmpties() {
        try {
            String jarPath = "/perfdata/oneclass.jar";
            File f = new File(getDataDir(), jarPath);
            assertTrue("Instrumented jar file doesn't exist.", f.exists());
            initTest(f.getAbsolutePath());
            settings.setInstrumentEmptyMethods(false);
            startInstrumentationTest(f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    public void testSimpleNoGetters() {
        try {
            String jarPath = "/perfdata/oneclass.jar";
            File f = new File(getDataDir(), jarPath);
            assertTrue("Instrumented jar file doesn't exist.", f.exists());
            initTest(f.getAbsolutePath());
            settings.setInstrumentGetterSetterMethods(false);
            startInstrumentationTest(f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    protected void setClasses(String jarPath) throws Exception {
        ArrayList<String> names = new ArrayList(16);
        ArrayList<byte[]> bytes = new ArrayList(16);
        JarFile file = new JarFile(jarPath);
        Enumeration<JarEntry> entries = file.entries();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        int read = 0;
        byte[] buffer = new byte[1024];

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry.getName().endsWith(".class")) {
                String nm = entry.getName();
                nm = nm.substring(0, nm.lastIndexOf('.'));
                names.add(nm);

                BufferedInputStream bis = new BufferedInputStream(file.getInputStream(entry));

                while ((read = bis.read(buffer)) > -1) {
                    bos.write(buffer, 0, read);
                }

                bis.close();
                bytes.add(bos.toByteArray());
                bos.reset();
            }
        }

        classNames = names.toArray(new String[0]);
        classesBytes = bytes.toArray(new byte[0][]);
    }

    protected void setRootMethods(String jarFile) throws Exception {
        JarFile file = new JarFile(jarFile);
        HashSet<String> list = new HashSet(8);

        for (Enumeration<JarEntry> entries = file.entries(); entries.hasMoreElements();) {
            JarEntry entry = entries.nextElement();

            if (entry.getName().endsWith(".class")) {
                String name = entry.getName();
                int idx = name.lastIndexOf('/');
                String packageName = (idx == -1) ? name : name.substring(0, idx);
                packageName = packageName.replace('/', '.');
                list.add(packageName);
            }
        }

        ClientUtils.SourceCodeSelection[] ret = new ClientUtils.SourceCodeSelection[list.size()];
        String[] cls = list.toArray(new String[0]);

        for (int i = 0; i < list.size(); i++) {
            ret[i] = new ClientUtils.SourceCodeSelection(cls[i] + ".", "", ""); //NOI18N
        }

        settings.setInstrumentationRootMethods(ret);
    }

    protected boolean checkBytes(String className, byte[] bytes) {
        String clnm = className.replace('.', '/');
        int clindex = -1;

        for (int i = 0; i < classNames.length; i++) {
            if (classNames[i].equals(clnm)) {
                clindex = i;

                break;
            }
        }

        if (clindex == -1) {
            throw new IllegalStateException("Class " + className + " has not original.");
        }

        byte[] origbytes = classesBytes[clindex];

        if (origbytes.length != bytes.length) {
            return false;
        }

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != origbytes[i]) {
                return false;
            }
        }

        return true;
    }

    protected ProfilerEngineSettings initTest(String pathToJar)
                                       throws Exception {
        settings = new ProfilerEngineSettings();
        settings.setPortNo(5140);
        settings.setSeparateConsole(false);
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_TOTAL);
        settings.setInstrumentEmptyMethods(true);
        settings.setInstrumentGetterSetterMethods(true);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentSpawnedThreads(true);
        settings.setJVMArgs("");

        setRootMethods(pathToJar);

        setTargetVM(settings);
        //setClassPath(settings);
        setProfilerHome(settings);

        setStatus(STATUS_NONE);

        return settings;
    }

    protected void startInstrumentationTest(String jarFile)
                                     throws Exception {
        ProfilingSessionStatus status = new ProfilingSessionStatus();
        status.targetJDKVersionString = settings.getTargetJDKVersionString();

        PrintStream oldOutStream = System.out;
        PrintStream oldErrStream = System.err;
        System.setOut(getLogStream());
        System.setErr(getLogStream());

        ClassRepository classRepo = new ClassRepository();
        classRepo.initClassPaths(settings.getWorkingDir(), settings.getVMClassPaths());
        Instrumentor instr = new Instrumentor(classRepo, status, settings);
        instr.setStatusInfoFromSourceCodeSelection(settings.getInstrumentationRootMethods());
        status.currentInstrType = CommonConstants.INSTR_RECURSIVE_FULL;

        setClasses(jarFile);

        int[] loadersIDs = new int[classNames.length];

        for (int i = 0; i < classNames.length; i++) {
            loadersIDs[i] = 20;
        }

        int[] parentloadersIDs = new int[classNames.length];

        for (int i = 0; i < classNames.length; i++) {
            parentloadersIDs[i] = 0;
        }

        int[] superClasses = new int[classNames.length];
        int[][] ifaces = new int[classNames.length][];
        
        RootClassLoadedCommand cmd = new RootClassLoadedCommand(classNames, loadersIDs, classesBytes, superClasses, ifaces,
                                                                classNames.length, parentloadersIDs);
        log("Start instrumenting ...");

        InstrumentMethodGroupResponse resp = null;
        long time = System.currentTimeMillis();
        resp = instr.createInitialInstrumentMethodGroupResponse(cmd);
        time = System.currentTimeMillis() - time;
        ref("Number of Classes: " + classNames.length);

        byte[][] clbytes = resp.getReplacementClassFileBytes();
        ref("Instrumented Classes: " + resp.getBase().getNClasses());
        ref("Instrumented Methods: " + resp.getBase().getNMethods());

        if (resp.getErrorMessage() != null) {
            log("Error Message: " + resp.getErrorMessage());
        }

        String[] clnames = resp.getMethodClasses();
        byte[][] bts = resp.getReplacementClassFileBytes();
        boolean comp = false;

        for (int i = 0; i < clnames.length; i++) {
            if (checkBytes(clnames[i], bts[i])) {
                log("Equals bytes: " + clnames[i]);
            }
        }

        System.setOut(oldOutStream);
        System.setErr(oldErrStream);
        reportPerformance(getName(), time, "ms");
    }
}
