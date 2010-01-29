/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.lib.profiler.tests.jfluid;

import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.diff.LineDiff;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.client.AppStatusHandler;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.client.ClientUtils.SourceCodeSelection;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.tests.jfluid.utils.DumpStream;
import org.netbeans.lib.profiler.utils.MiscUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public abstract class CommonProfilerTestCase extends NbTestCase {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int STATUS_ERROR = 255;
    public static final int STATUS_NONE = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_RESULTS_AVAILABLE = 2;
    public static final int STATUS_APP_FINISHED = 4;
    public static final int STATUS_MEASURED = 8;
    public static final int STATUS_FINISHED = 16;
    public static final int STATUS_LIVERESULTS_AVAILABLE = 32;
    private static final boolean CREATE_GOLDENS = false;
    private static final String GOLDENS_CVS_PATH = CommonProfilerTestCase.class.getResource("CommonProfilerTestCase.class").toString().replace("classes/org/netbeans/lib/profiler/tests/jfluid/CommonProfilerTestCase.class", "data/goldenfiles").replace("file:/", "");    

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected DumpStream err;
    protected DumpStream out;
    protected File diff;
    protected File ref;
    protected Process profilingProcess = null;
    PrintStream goldenStream;
    PrintStream logStream;
    PrintStream refStream;
    private String mainClass;
    private String projectName;
    private String[][] rootMethods;
    private volatile int status = 0;
    private File workdir;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CommonProfilerTestCase(String name) {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setRootMethods(String[][] rootMethods) {
        this.rootMethods = rootMethods;
    }

    public String[][] getRootMethods() {
        return rootMethods;
    }

    public synchronized void setStatus(int status) {
        log("STATUS: set status " + getStatus(status));
        System.err.println("STATUS: set status " + getStatus(status));

        if (STATUS_ERROR == status) {
            new Exception("STATUS_ERROR set").printStackTrace();
        }

        this.status |= status;
        notifyAll();
    }

    public synchronized int getStatus() {
        return status;
    }

    public synchronized String getStatus(int status) {
        StringBuffer sb = new StringBuffer();

        if (status == STATUS_ERROR) {
            sb.append("ERROR");
        } else {
            if ((status & STATUS_APP_FINISHED) > 0) {
                sb.append("APP_FINISHED ");
            }

            if ((status & STATUS_FINISHED) > 0) {
                sb.append("FINISHED ");
            }

            if ((status & STATUS_MEASURED) > 0) {
                sb.append("MEASURED ");
            }

            if (status == 0) {
                sb.append("NONE ");
            }

            if ((status & STATUS_RESULTS_AVAILABLE) > 0) {
                sb.append("RESULTS_AVAILABLE ");
            }

            if ((status & STATUS_RUNNING) > 0) {
                sb.append("RUNNING ");
            }

            if ((status & STATUS_LIVERESULTS_AVAILABLE) > 0) {
                sb.append("LIVERESULTS_AVAILABLE ");
            }
        }

        return sb.toString();
    }

    public synchronized boolean isStatus(int status) {
        if (status == STATUS_ERROR) {
            return (getStatus() == status);
        }

        return ((getStatus() & status) > 0);
    }

    public void log(Throwable t) {
        t.printStackTrace(getLogStream());
    }

    public void log(String s) {
        getLogStream().println(s);
    }

    public void log(Object o) {
        log(o.toString());
    }

    public void log(double[] ar) {
        StringBuffer sb = new StringBuffer(ar.length * 10);
        sb.append("[");

        for (int i = 0; i < ar.length; i++) {
            sb.append(String.valueOf(ar[i]));

            if (i < (ar.length - 1)) {
                sb.append(", ");
            }
        }

        sb.append("]");
        log(sb.toString());
    }

    public void log(int[] ar) {
        StringBuffer sb = new StringBuffer(ar.length * 10);
        sb.append("[");

        for (int i = 0; i < ar.length; i++) {
            sb.append(String.valueOf(ar[i]));

            if (i < (ar.length - 1)) {
                sb.append(", ");
            }
        }

        sb.append("]");
        log(sb.toString());
    }

    public void log(HashMap map) {
        Object[] keys = map.keySet().toArray();
        Arrays.sort(keys);

        for (int i = 0; i < keys.length; i++) {
            log((String) keys[i] + " = " + (String) (map.get(keys[i])));
        }
    }

    public void log(ProfilerEngineSettings settings) {
        HashMap map = new HashMap(32);
        storeSettings(settings, map);
        log(map);
    }

    public void ref(String s) {
        //System.out.println(s);
        getRefStream().println(s);

        if (CREATE_GOLDENS) {
            goldenStream.println(s);
        }
    }

    public void ref(String[] s) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < s.length; i++) {
            sb.append(s[i]);

            if (i < (s.length - 1)) {
                sb.append(", ");
            }
        }

        ref(sb);
    }

    public void ref(Object o) {
        ref(o.toString());
    }

    public void runTest() throws Throwable {
        try {
            super.runTest();
        } catch (Throwable td) {
            td.printStackTrace();

            if (!isStatus(STATUS_ERROR)) {
                setStatus(STATUS_ERROR);
            }

            throw td;
        }
    }

    public synchronized void unsetStatus(int status) {
        if (status != STATUS_ERROR) {
            log("STATUS: unset status " + getStatus(status));
            System.err.println("STATUS: unset status " + getStatus(status));
            this.status &= ~status;
            notifyAll();
        }
    }

    public void waitForStatus(int status) {
        waitForStatus(status, 0L);
    }

    public synchronized void waitForStatus(int status, long timeout) {
        log("STATUS: wait for status " + getStatus(status));
        System.err.println("STATUS: wait for status " + getStatus(status));

        while (!isStatus(status) && !isStatus(STATUS_ERROR)) {
            try {
                wait(timeout);
            } catch (InterruptedException e) {
            }
        }

        log("STATUS: reached status " + getStatus(getStatus()));
        System.err.println("STATUS: reached status " + getStatus(getStatus()));

        if (isStatus(STATUS_ERROR)) {
            assertTrue("Error state of test", false);
        }
    }

    protected void setClassPath(ProfilerEngineSettings settings) {
        String projPath = getProjectPath(getProjectName());
        settings.setMainClassPath(projPath);
        //coverage
        //settings.setMainClassPath("/space/tmp/testrun/emma/lib/emma.jar:" + xData + jarPath);
        settings.setMainClass(getMainClass());
        ClassRepository.initClassPaths(getDataDir().getAbsolutePath(), new String[] { projPath, "", "" });
    }

    protected PrintStream getLogStream() {
        if (logStream == null) {
            logStream = getLog();
        }

        return logStream;
    }

    protected void setProfilerHome(ProfilerEngineSettings settings) {
        try {
            String profilerHome = System.getProperty("profiler.home");

            if ((profilerHome == null) || !new File(profilerHome).exists()) {
                profilerHome = System.getProperty("netbeans.home").replace("platform", "profiler");
            }

            settings.initialize(profilerHome + "/lib");
        } catch (IOException ex) {
            ex.printStackTrace();
            assertFalse("Error in initialization", true);
        }
    }

    protected String getProjectPath(String projectName) {
        String jarPath = "/projects/" + projectName + "/distrib/" + projectName + ".jar";

        if (!new File(getDataDir(), jarPath).exists()) {
            jarPath = "/projects/" + projectName + "/dist/" + projectName + ".jar";
        }

        if (!new File(getDataDir(), jarPath).exists()) {
            assertTrue("There is not profiled application", false);

            return "";
        }

        String xData = getDataDir().getAbsolutePath();

        return xData + jarPath;
    }

    protected PrintStream getRefStream() {
        if (refStream == null) {
            refStream = getRef();

            if (CREATE_GOLDENS) {
                File file = new File(GOLDENS_CVS_PATH);
                file = new File(file, getClass().getName().replace('.', '/') + "/" + getName() + ".pass");

                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try {
                    goldenStream = new PrintStream(new FileOutputStream(file));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return refStream;
    }

    protected void setRootMethods(ProfilerEngineSettings settings, String[][] rootMethods) {
        this.rootMethods = rootMethods;

        if (rootMethods == null) {
            this.rootMethods = new String[][] {
                                   { mainClass, "main", "([Ljava/lang/String;)V" }
                               };
        }

        ArrayList al = new ArrayList();

        for (int i = 0; i < this.rootMethods.length; i++) {
            al.add(new ClientUtils.SourceCodeSelection(this.rootMethods[i][0], this.rootMethods[i][1], this.rootMethods[i][2]));
        }

        settings.setInstrumentationRootMethods((ClientUtils.SourceCodeSelection[]) al.toArray(new ClientUtils.SourceCodeSelection[al
                                                                                                                                  .size()]));
    }

    protected void setTargetVM(ProfilerEngineSettings settings) {
        String vers = System.getProperty("java.version");

        if (vers.startsWith("1.5")) {
            if (vers.startsWith("1.5.0")
                    && ((vers.charAt("1.5.0".length()) != '_')
                           || (Integer.parseInt(vers.substring("1.5.0".length() + 1, "1.5.0".length() + 3)) < 4))) {
                System.err.println("Illegal version of JVM: " + vers);

                return;
            }

            settings.setTargetJDKVersionString(CommonConstants.JDK_15_STRING);
        } else if (vers.startsWith("1.6")) {
            settings.setTargetJDKVersionString(CommonConstants.JDK_16_STRING);
        } else if (vers.startsWith("1.7")) {
            settings.setTargetJDKVersionString(CommonConstants.JDK_17_STRING);
        }

        String home = System.getProperty("java.home");

        if (File.separatorChar == '/') {
            settings.setTargetJVMExeFile(home + "/bin/java");
        } else {
            settings.setTargetJVMExeFile(home + "\\bin\\java.exe");
        }

        settings.setSystemArchitecture(Platform.getSystemArchitecture());
    }

    protected void setUp() throws Exception {
        System.err.println("START TEST " + getClass().getName() + "." + getName());

        //System.setProperty("org.netbeans.lib.profiler.TargetAppRunner", "true");
        workdir = getWorkDir();
        diff = new File(workdir, getName() + ".diff");
        ref = new File(workdir, getName() + ".ref");
        log("Test Source: http://hg.netbeans.org/main-golden/file/tip/lib.profiler/test/qa-functional/src/"
            + getClass().getName().replace('.', '/') + ".java");

        //check for running server
        try {
            java.net.Socket sock = new java.net.Socket("localhost", 5140);
            sock.getOutputStream().write(1);
            sock.close();
            assertTrue("Another server is running on port 5140", false);
        } catch (Exception ex) {
        }
    }

    protected void addJVMArgs(ProfilerEngineSettings settings, String arg) {
        String old = settings.getJVMArgsAsSingleString();

        if ((old == null) || (old.length() == 0)) {
            settings.setJVMArgs(arg);
        } else {
            settings.setJVMArgs(old + " " + arg);
        }
    }

    protected void bindStreams(Process p) {
        err = new DumpStream(p, p.getErrorStream(), getLogStream(), "[App error] ");
        err.start();
        out = new DumpStream(p, p.getInputStream(), getLogStream(), "[App output] ");
        out.start();
    }

    protected String complete(String s, int chars) {
        StringBuffer sb = new StringBuffer(chars);
        int tot = chars - s.length();
        sb.append(s);

        for (int i = 0; i < tot; i++) {
            sb.append(" ");
        }

        return sb.substring(0, chars);
    }

    protected void finalizeTest(TargetAppRunner runner) {
        //finish client
        if (!isStatus(STATUS_MEASURED)) { // to release handleShutdown call
            System.err.println("must be set measured");
            setStatus(STATUS_MEASURED);
        }

        if (!isStatus(STATUS_APP_FINISHED)) { //not handled shutdown
            System.err.println("target vm must be terminated");
            runner.terminateTargetJVM();
        }

        //wait for agent death
        int cycles = 50;

        while ((cycles > 0) && runner.targetJVMIsAlive()) {
            try {
                Thread.sleep(500);
                cycles--;
            } catch (InterruptedException ex) {
            }
        }

        assertFalse("Target JVM is running after finish", runner.targetJVMIsAlive());

        //test the profiled proces is finished
        if (profilingProcess != null) {
            try {
                profilingProcess.waitFor();

                if (out != null) {
                    out.join();
                    err.join();
                }

                profilingProcess.destroy();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            profilingProcess = null;
        }

        //log settings
        log("\nProfiler settings\n");
        log(runner.getProfilerEngineSettings());
        log("");
        System.err.println("Test " + getName() + " finalized.");
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
    }

    protected void initAppByStream(TargetAppRunner runner) {
        Process p = runner.getRunningAppProcess();
        assert p != null;

        try {
            PrintStream ps = new PrintStream(p.getOutputStream());
            ps.print("start");
            ps.close();
        } catch (Exception ex) {
        }
    }

    protected ProfilerEngineSettings initTest(String projectName, String mainClass, String[][] rootMethods) {
        ProfilerEngineSettings settings = new ProfilerEngineSettings();
        settings.setPortNo(5140);
        settings.setSeparateConsole(true);
        settings.setInstrScheme(ProfilerEngineSettings.INSTRSCHEME_TOTAL);
        settings.setJVMArgs("");
        settings.setWorkingDir(workdir.getAbsolutePath());
        //coverage
        //addJVMArgs(settings, "-Demma.coverage.out.file=/space/tmp/testrun/coverage.emma");
        setProjectName(projectName);
        setMainClass(mainClass);
        setRootMethods(settings, rootMethods);

        setTargetVM(settings);
        setClassPath(settings);
        setProfilerHome(settings);

        setStatus(STATUS_NONE);

        return settings;
    }

    protected Process startTargetVM(TargetAppRunner runner) {
        ProfilerEngineSettings settings = runner.getProfilerEngineSettings();
        AppStatusHandler handler = runner.getAppStatusHandler();
        ArrayList commands = new ArrayList(10);

        commands.add(settings.getTargetJVMExeFile());

        //agentpath with options
        String jfNativeLibFullName = Platform.getAgentNativeLibFullName(settings.getJFluidRootDirName(), false,
                                                                        settings.getTargetJDKVersionString(), -1);
        String libpath = jfNativeLibFullName.substring(0, jfNativeLibFullName.indexOf("deployed") - 1);
        String timeOut = System.getProperty("profiler.agent.connect.timeout", "10");
        commands.add("-agentpath:" + jfNativeLibFullName + "=" + libpath + "," + Integer.toString(settings.getPortNo()) + ","
                     + timeOut);

        if (!Platform.isWindows() && settings.getTargetWindowRemains()) {
            commands.add("-XX:+ShowMessageBoxOnError"); // NOI18N
        }

        //classptah
        commands.add("-classpath");
        commands.add(settings.getMainClassPath());

        //jvm arguments
        for (int i = 0; i < settings.getJVMArgs().length; i++) {
            commands.add(settings.getJVMArgs()[i]);
        }

        // debugging property for agent side - wire I/O
        if (System.getProperty("org.netbeans.lib.profiler.wireprotocol.WireIO.agent") != null) { // NOI18N
            commands.add("-Dorg.netbeans.lib.profiler.wireprotocol.WireIO=true"); // NOI18N
        }

        // debugging property for agent side - Class loader hook
        if (System.getProperty("org.netbeans.lib.profiler.server.ProfilerInterface.classLoadHook") != null) { // NOI18N
            commands.add("-Dorg.netbeans.lib.profiler.server.ProfilerInterface.classLoadHook=true"); // NOI18N
        }

        //main class of application
        commands.add(settings.getMainClassName());

        //arguments of application
        for (int i = 0; i < settings.getMainArgs().length; i++) {
            commands.add(settings.getMainArgs()[i]);
        }

        String[] cmdArray = new String[commands.size()];
        commands.toArray(cmdArray);

        MiscUtils.printInfoMessage("Starting target application..."); // NOI18N
        MiscUtils.printVerboseInfoMessage(cmdArray);

        System.err.println("Starting VM with " + cmdArray.length + " commands."); // NOI18N

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < cmdArray.length; i++) {
            sb.append(cmdArray[i]);
            sb.append(' ');
        }

        System.err.println(sb.toString());

        try {
            profilingProcess = Runtime.getRuntime().exec(cmdArray, null, new File(settings.getWorkingDir()));

            if (profilingProcess != null) {
                runner.initiateSession(1, false);
            } else {
                throw new NullPointerException();
            }

            return profilingProcess;
        } catch (IOException ex) {
            String s = ""; // NOI18N

            for (int i = 0; i < cmdArray.length; i++) {
                s = s + cmdArray[i] + "\n"; // NOI18N
            }

            handler.displayError("When starting target JVM, with command: " + s + ", caught an exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        return null;
    }

    protected void storeSettings(ProfilerEngineSettings settings, HashMap map) {
        map.put("profiler.settings.cpu.profiling.type",
                ((settings.getCPUProfilingType() == CommonConstants.CPU_INSTR_FULL) ? "CPU_INSTR_FULL" : "CPU_INSTR_SAMPLED"));

        if (settings.getInstrScheme() == CommonConstants.INSTRSCHEME_LAZY) {
            map.put("profiler.settings.instr.scheme", "INSTRSCHEME_LAZY");
        } else if (settings.getInstrScheme() == CommonConstants.INSTRSCHEME_EAGER) {
            map.put("profiler.settings.instr.scheme", "INSTRSCHEME_EAGER");
        } else {
            map.put("profiler.settings.instr.scheme", "INSTRSCHEME_TOTAL");
        }

        map.put("profiler.settings.override.working.dir", settings.getWorkingDir());
        map.put("profiler.settings.override.jvm.args", toString(settings.getJVMArgs()));
        map.put("profiler.settings.override.port.no", Integer.toString(settings.getPortNo()));
        map.put("profiler.settings.thread.cpu.timer.on", Boolean.toString(settings.getThreadCPUTimerOn()));
        map.put("profiler.settings.istrument.getter.setter.methods", Boolean.toString(settings.getInstrumentGetterSetterMethods()));
        map.put("profiler.settings.instrument.empty.methods", Boolean.toString(settings.getInstrumentEmptyMethods()));
        map.put("profiler.settings.instrument.method.invoke", Boolean.toString(settings.getInstrumentMethodInvoke()));
        map.put("profiler.settings.instrument.spawned.threads", Boolean.toString(settings.getInstrumentSpawnedThreads()));
        map.put("profiler.settings.n.profiled.threads.limit", Integer.toString(settings.getNProfiledThreadsLimit()));
        map.put("profiler.settings.sort.results.by.thread.cpu.time", Boolean.toString(settings.getSortResultsByThreadCPUTime()));
        map.put("profiler.settings.sampling.interval", Integer.toString(settings.getSamplingInterval()));
        map.put("profiler.settings.code.region.cpu.res.buf.size", Integer.toString(settings.getCodeRegionCPUResBufSize()));
        map.put("profiler.settings.run.gc.on.get.results.in.memory.profiling",
                Boolean.toString(settings.getRunGCOnGetResultsInMemoryProfiling()));
        map.put("profiler.settings.obj.alloc.stack.sampling.interval", Integer.toString(settings.getAllocTrackEvery()));
        map.put("profiler.settings.obj.alloc.stack.sampling.depth", Integer.toString(settings.getAllocStackTraceLimit()));
        map.put("profiler.settings.exclude.wait.time", Boolean.toString(settings.getExcludeWaitTime()));

        InstrumentationFilter filter = settings.getInstrumentationFilter();

        if (filter != null) {
            map.put("profiler.settings.instrumentation.filter.string", toString(filter.getFilterStrings()));
            map.put("profiler.settings.instrumentation.filter.type",
                    ((filter.getFilterType() == InstrumentationFilter.INSTR_FILTER_EXCLUSIVE) ? "EXCLUSIVE" : "INCLUSIVE"));
        } else {
            map.put("profiler.settings.instrumentation.filter.selected", "NONE");
        }

        SourceCodeSelection[] roots = settings.getInstrumentationRootMethods();

        if ((roots != null) && (roots.length > 0)) {
            map.put("profiler.settings.instrumentation.root.methods.size", Integer.toString(roots.length));

            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < roots.length; i++) {
                if (roots[i].getStartLine() > -1) {
                    sb.append(roots[i].getClassName());
                    sb.append("[");
                    sb.append(roots[i].getStartLine());
                    sb.append(", ");
                    sb.append(roots[i].getEndLine());
                    sb.append("]");
                } else {
                    sb.append(roots[i].getClassName());
                    sb.append(".");
                    sb.append(roots[i].getMethodName());
                    sb.append(roots[i].getMethodSignature());
                }

                if (i < (roots.length - 1)) {
                    sb.append(", ");
                }
            }

            map.put("profiler.settings.istrumentation.root.methods-", sb.toString()); //prefix
        } else {
            map.put("profiler.settings.instrumentation.root.methods.size", "0");
            map.put("profiler.settings.istrumentation.root.methods-", "");
        }
    }

    protected void tearDown() throws Exception {
        if (refStream != null) {
            refStream.close();

            if (CREATE_GOLDENS) {
                goldenStream.close();
            } else {
                boolean bgolden = true;

                try {
                    getGoldenFile();
                } catch (Throwable t) {
                    bgolden = false;
                }

                if ((bgolden || (ref.length() > 0)) && !isStatus(STATUS_ERROR)) {
                    LineDiff ld = new LineDiff();
                    assertFalse("Golden files differ", ld.diff(ref, getGoldenFile(), diff));
                }
            }
        }

        if (logStream != null) {
            logStream.close();
        }

        System.err.println("Test " + getName() + " finished.");
    }

    protected String toString(String[] array) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            sb.append(" ");
        }

        return sb.toString();
    }

    protected String toString(int[] array) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            sb.append(" ");
        }

        return sb.toString();
    }
}
