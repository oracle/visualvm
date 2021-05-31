/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jmx.impl;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import org.graalvm.visualvm.tools.jmx.JvmMXBeansFactory;
import org.openide.ErrorManager;
import org.openide.util.Exceptions;

/**
 *
 * @author Tomas Hurka
 */
public class JmxSupport {
    private final static Logger LOGGER = Logger.getLogger(JmxSupport.class.getName());
    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME =
            "com.sun.management:type=HotSpotDiagnostic";    // NOI18N
    private static final String DIAGNOSTIC_COMMAND_MXBEAN_NAME =
            "com.sun.management:type=DiagnosticCommand";    // NOI18N
    private static final String ALL_OBJECTS_OPTION = "-all";    // NOI18N
    private static final String HISTOGRAM_COMMAND = "gcClassHistogram";       // NOI18N
    private static final String CMDLINE_COMMAND = "vmCommandLine";       // NOI18N
    private static final String CMDLINE_PREFIX = "java_command: ";       // NOI18N
    private static final String JCMD_JFR_DUMP = "jfrDump";    // NOI18N
    private static final String JCMD_JFR_DUMP_FILENAME = "filename";    // NOI18N
    private static final String JCMD_JFR_DUMP_RECORDING = "recording";    // NOI18N
    private static final String JCMD_JFR_DUMP_NAME = "name";    // NOI18N
    private static final String JCMD_JFR_CHECK = "jfrCheck";   // NOI18N
    private static final String JCMD_JFR_CHECK_RECORDING_ID = "recording=";     // NOI18N
    private static final String JCMD_JFR_CHECK_RECORDING_ID1 = "Recording ";     // NOI18N
    private static final String JCMD_JFR_CHECK_HELP_OPTIONS_ID = "Options: ";        // NOI18N
    private static final String JCMD_JFR_CHECK_HELP_RECORDING_ID = "recording : ";        // NOI18N
    private static final String JCMD_JFR_START = "jfrStart";   // NOI18N
    private static final String JCMD_JFR_START_NAME = "name"; // NOI18N
    private static final String JCMD_JFR_START_SETTINGS = "settings"; // NOI18N
    private static final String JCMD_JFR_START_DELAY = "delay"; // NOI18N
    private static final String JCMD_JFR_START_DURATION = "duration"; // NOI18N
    private static final String JCMD_JFR_START_DISK = "disk"; // NOI18N
    private static final String JCMD_JFR_START_FILENAME = "filename"; // NOI18N
    private static final String JCMD_JFR_START_MAXAGE = "maxage"; // NOI18N
    private static final String JCMD_JFR_START_MAXSIZE = "maxsize"; // NOI18N
    private static final String JCMD_JFR_START_DUMPONEXIT = "dumponexit"; // NOI18N
    private static final String JCMD_JFR_STOP = "jfrStop";   // NOI18N
    private static final String JCMD_JFR_STOP_NAME = "name";   // NOI18N
    private static final String JCMD_JFR_UNLOCK_ID = "Use VM.unlock_commercial_features to enable"; // NOI18N
    private static final String JCMD_UNLOCK_CF = "vmUnlockCommercialFeatures"; // NOI18N
    private static final String JCMD_HELP = "help";             // NOI18N
    private static final String JCMD_CF_ID = " unlocked.";   // NOI18N
    private static final Map EMPTY_PARS = Collections.singletonMap("", null);

    private JvmMXBeans mxbeans;
    private JmxModel jmxModel;
    // HotspotDiagnostic
    private boolean hotspotDiagnosticInitialized;
    private final Object hotspotDiagnosticLock = new Object();
    private HotSpotDiagnosticMXBean hotspotDiagnosticMXBean;
    private final Object readOnlyConnectionLock = new Object();
    private Boolean readOnlyConnection;
    
    private Boolean hasDumpAllThreads;
    private final Object hasDumpAllThreadsLock = new Object();
    private Boolean jfrAvailable;
    private Boolean oldJFR;
    
    private String commandLine;
    private final Object commandLineLock = new Object();

    JmxSupport(JmxModel jmx) {
        jmxModel = jmx;
    }
    
    private RuntimeMXBean getRuntime() {
        JvmMXBeans jmx = getJvmMXBeans();
        if (jmx != null) {
            return jmx.getRuntimeMXBean();
        }
        return null;
    }
    
    private synchronized JvmMXBeans getJvmMXBeans() {
        if (mxbeans == null) {
            if (jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
            }
        }
        return mxbeans;
    }
    
    Properties getSystemProperties() {
        try {
            RuntimeMXBean runtime = getRuntime();
            if (runtime != null) {
                Properties prop = new Properties();
                prop.putAll(runtime.getSystemProperties());
                return prop;
            }
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "getSystemProperties", e); // NOI18N
            return null;
        }
    }

    synchronized boolean isReadOnlyConnection() {
        synchronized (readOnlyConnectionLock) {
            if (readOnlyConnection == null) {
                readOnlyConnection = Boolean.FALSE;
                ThreadMXBean threads = getThreadBean();
                if (threads != null) {
                    try {
                        threads.getThreadInfo(1);
                    } catch (SecurityException ex) {
                        readOnlyConnection = Boolean.TRUE;
                    }
                }
            }
            return readOnlyConnection.booleanValue();
        }
    }
    
    ThreadMXBean getThreadBean() {
        JvmMXBeans jmx = getJvmMXBeans();
        if (jmx != null) {
            return jmx.getThreadMXBean();
        }
        return null;
    }
    
    HotSpotDiagnosticMXBean getHotSpotDiagnostic() {
        synchronized (hotspotDiagnosticLock) {
            if (hotspotDiagnosticInitialized) {
                return hotspotDiagnosticMXBean;
            }
            JvmMXBeans jmx = getJvmMXBeans();
            if (jmx != null) {
                try {
                    hotspotDiagnosticMXBean = jmx.getMXBean(
                            ObjectName.getInstance(HOTSPOT_DIAGNOSTIC_MXBEAN_NAME),
                            HotSpotDiagnosticMXBean.class);
                } catch (MalformedObjectNameException e) {
                    ErrorManager.getDefault().log(ErrorManager.WARNING,
                            "Couldn't find HotSpotDiagnosticMXBean: " + // NOI18N
                            e.getLocalizedMessage());
                } catch (IllegalArgumentException ex) {
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                }
            }
            hotspotDiagnosticInitialized = true;
            return hotspotDiagnosticMXBean;
        }
    }
    
    String takeThreadDump(long[] threadIds) {
        try {
            ThreadMXBean threadMXBean = getThreadBean();
            if (threadMXBean == null) {
                return null;
            }
            ThreadInfo[] threads;
            StringBuilder sb = new StringBuilder(4096);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  // NOI18N

            if (hasDumpAllThreads()) {
                threads = threadMXBean.getThreadInfo(threadIds, true, true);
            } else {
                threads = threadMXBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
            }
            sb.append(df.format(new Date()) + "\n");  // NOI18N
            printThreads(sb, threadMXBean, threads);
            return sb.toString();
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "takeThreadDump[]", e); // NOI18N
            return null;
        }
    }
    
    String takeThreadDump() {
        try {
            ThreadMXBean threadMXBean = getThreadBean();
            if (threadMXBean == null) {
                return null;
            }
            ThreadInfo[] threads;
            Properties prop = getSystemProperties();
            StringBuilder sb = new StringBuilder(4096);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  // NOI18N
            sb.append(df.format(new Date()) + "\n");
            sb.append("Full thread dump " + prop.getProperty("java.vm.name") + // NOI18N
                    " (" + prop.getProperty("java.vm.version") + " " + // NOI18N
                    prop.getProperty("java.vm.info") + "):\n");  // NOI18N
            if (hasDumpAllThreads()) {
                threads = threadMXBean.dumpAllThreads(true, true);
            } else {
                long[] threadIds = threadMXBean.getAllThreadIds();
                threads = threadMXBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
            }
            printThreads(sb, threadMXBean, threads);
            return sb.toString();
        } catch (Exception e) {
            LOGGER.log(Level.INFO,"takeThreadDump", e); // NOI18N
            return null;
        }
    }
    
    private void printThreads(final StringBuilder sb, final ThreadMXBean threadMXBean, ThreadInfo[] threads) {
        boolean jdk16 = hasDumpAllThreads();
        
        for (ThreadInfo thread : threads) {
            if (thread != null) {
                if (jdk16) {
                    print16Thread(sb, threadMXBean, thread);
                } else {
                    print15Thread(sb, thread);
                }
            }
        }
    }
    
    private void print16Thread(final StringBuilder sb, final ThreadMXBean threadMXBean, final ThreadInfo thread) {
        MonitorInfo[] monitors = null;
        if (threadMXBean.isObjectMonitorUsageSupported()) {
            monitors = thread.getLockedMonitors();
        }
        sb.append("\n\"" + thread.getThreadName() + // NOI18N
                "\" - Thread t@" + thread.getThreadId() + "\n");    // NOI18N
        sb.append("   java.lang.Thread.State: " + thread.getThreadState()); // NOI18N
        sb.append("\n");   // NOI18N
        int index = 0;
        for (StackTraceElement st : thread.getStackTrace()) {
            LockInfo lock = thread.getLockInfo();
            String lockOwner = thread.getLockOwnerName();
            
            sb.append("\tat " + st.toString() + "\n");    // NOI18N
            if (index == 0) {
                if ("java.lang.Object".equals(st.getClassName()) &&     // NOI18N
                        "wait".equals(st.getMethodName())) {                // NOI18N
                    if (lock != null) {
                        sb.append("\t- waiting on ");    // NOI18N
                        printLock(sb,lock);
                        sb.append("\n");    // NOI18N
                    }
                } else if (lock != null) {
                    if (lockOwner == null) {
                        sb.append("\t- parking to wait for ");      // NOI18N
                        printLock(sb,lock);
                        sb.append("\n");            // NOI18N
                    } else {
                        sb.append("\t- waiting to lock ");      // NOI18N
                        printLock(sb,lock);
                        sb.append(" owned by \""+lockOwner+"\" t@"+thread.getLockOwnerId()+"\n");   // NOI18N
                    }
                }
            }
            printMonitors(sb, monitors, index);
            index++;
        }
        StringBuilder jnisb = new StringBuilder();
        printMonitors(jnisb, monitors, -1);
        if (jnisb.length() > 0) {
            sb.append("   JNI locked monitors:\n");
            sb.append(jnisb);
        }
        if (threadMXBean.isSynchronizerUsageSupported()) {
            sb.append("\n   Locked ownable synchronizers:");    // NOI18N
            LockInfo[] synchronizers = thread.getLockedSynchronizers();
            if (synchronizers == null || synchronizers.length == 0) {
                sb.append("\n\t- None\n");  // NOI18N
            } else {
                for (LockInfo li : synchronizers) {
                    sb.append("\n\t- locked ");         // NOI18N
                    printLock(sb,li);
                    sb.append("\n");  // NOI18N
                }
            }
        }
    }

    private void printMonitors(final StringBuilder sb, final MonitorInfo[] monitors, final int index) {
        if (monitors != null) {
            for (MonitorInfo mi : monitors) {
                if (mi.getLockedStackDepth() == index) {
                    sb.append("\t- locked ");   // NOI18N
                    printLock(sb,mi);
                    sb.append("\n");    // NOI18N
                }
            }
        }
    }
    
    private void print15Thread(final StringBuilder sb, final ThreadInfo thread) {
        sb.append("\n\"" + thread.getThreadName() + // NOI18N
                "\" - Thread t@" + thread.getThreadId() + "\n");    // NOI18N
        sb.append("   java.lang.Thread.State: " + thread.getThreadState()); // NOI18N
        if (thread.getLockName() != null) {
            sb.append(" on " + thread.getLockName());   // NOI18N
            if (thread.getLockOwnerName() != null) {
                sb.append(" owned by: " + thread.getLockOwnerName());   // NOI18N
            }
        }
        sb.append("\n");    // NOI18N
        for (StackTraceElement st : thread.getStackTrace()) {
            sb.append("        at " + st.toString() + "\n");    // NOI18N
        }
    }
    
    private void printLock(StringBuilder sb,LockInfo lock) {
        String id = Integer.toHexString(lock.getIdentityHashCode());
        String className = lock.getClassName();
        
        sb.append("<"+id+"> (a "+className+")");       // NOI18N
    }
    
    boolean takeHeapDump(String fileName) {
        HotSpotDiagnosticMXBean hsDiagnostic = getHotSpotDiagnostic();
        if (hsDiagnostic != null) {
            Path f = Paths.get(fileName);
            try {
                hsDiagnostic.dumpHeap(fileName,true);
            } catch (IOException ex) {
                LOGGER.log(Level.INFO,"takeHeapDump", ex); // NOI18N
                try {
                    Files.deleteIfExists(f);
                } catch (IOException ex1) {
                    LOGGER.log(Level.INFO,"takeHeapDump", ex1); // NOI18N
                }
                return false;
            }
            return Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(f);
        }
        return false;
    }
    
    String getFlagValue(String name) {
        try {
            HotSpotDiagnosticMXBean hsDiagnostic = getHotSpotDiagnostic();
            if (hsDiagnostic != null) {
                VMOption option = hsDiagnostic.getVMOption(name);
                if (option != null) {
                    return option.getValue();
                }
            }
            return null;
        } catch (IllegalArgumentException ex) {
            // non-existing VM option
            LOGGER.log(Level.FINE, "getFlagValue", ex); // NOI18N
            return null;
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "getFlagValue", ex); // NOI18N
            return null;
        }
    }

    HeapHistogram takeHeapHistogram() {
        String histo = executeJCmd(HISTOGRAM_COMMAND, Collections.singletonMap(ALL_OBJECTS_OPTION, null));
        if (histo != null) {
            return new HeapHistogramImpl((String)histo);
        }
        return null;
    }
    
    void setFlagValue(String name, String value) {
        try {
        HotSpotDiagnosticMXBean hsDiagnostic = getHotSpotDiagnostic();
            if (hsDiagnostic != null) {
                hsDiagnostic.setVMOption(name,value);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.INFO,"setFlagValue", ex); // NOI18N
        }
    }
    
    private boolean hasDumpAllThreads() {
        synchronized (hasDumpAllThreadsLock) {
            if (hasDumpAllThreads == null) {
                hasDumpAllThreads = Boolean.FALSE;
                try {
                    ObjectName threadObjName = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
                    MBeanInfo threadInfo = jmxModel.getMBeanServerConnection().getMBeanInfo(threadObjName);
                    if (threadInfo != null) {
                        for (MBeanOperationInfo op : threadInfo.getOperations()) {
                            if ("dumpAllThreads".equals(op.getName())) {
                                hasDumpAllThreads = Boolean.TRUE;
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.INFO,"hasDumpAllThreads", ex); // NOI18N
                }
            }
            return hasDumpAllThreads.booleanValue();
        }
    }

    synchronized boolean isJfrAvailable() {
        if (jfrAvailable == null) {
            String recordings = getJfrCheck();
            if (recordings == null) {
                jfrAvailable = Boolean.FALSE;
            } else {
                if (recordings.contains(JCMD_JFR_UNLOCK_ID)) {
                    jfrAvailable = unlockCommercialFeature();
                } else {
                    jfrAvailable = Boolean.TRUE;
                }
            }
            if (Boolean.TRUE.equals(jfrAvailable)) {
                oldJFR = checkForOldJFR();
            }
        }
        return jfrAvailable;
    }

    List<Long> jfrCheck() {
        if (!isJfrAvailable()) {
            throw new UnsupportedOperationException();
        }
        String recordings = getJfrCheck();
        if (recordings == null) {
            return Collections.EMPTY_LIST;
        }
        String[] lines = recordings.split("\\r?\\n");       // NOI18N
        List<Long> recNumbers = new ArrayList(lines.length);

        for (String line : lines) {
            int index = line.indexOf(JCMD_JFR_CHECK_RECORDING_ID);
            if (index >= 0) {
                int recStart = index + JCMD_JFR_CHECK_RECORDING_ID.length();
                int recEnd = line.indexOf(' ', recStart);

                if (recEnd > recStart) {
                    String recordingNum = line.substring(recStart, recEnd);
                    recNumbers.add(Long.valueOf(recordingNum));
                }
            } else if (line.startsWith(JCMD_JFR_CHECK_RECORDING_ID1)) {
                int recStart = JCMD_JFR_CHECK_RECORDING_ID1.length();
                int recEnd = line.indexOf(':', recStart);

                if (recEnd > recStart) {
                    String recordingNum = line.substring(recStart, recEnd);
                    recNumbers.add(Long.valueOf(recordingNum));
                }
            }
        }
        return recNumbers;
    }

    String takeJfrDump(long recording, String fileName) {
        if (!isJfrAvailable()) {
            throw new UnsupportedOperationException();
        }
        Map<String, Object> pars = new HashMap();
        pars.put(JCMD_JFR_DUMP_FILENAME, fileName);
        pars.put(oldJFR ? JCMD_JFR_DUMP_RECORDING : JCMD_JFR_DUMP_NAME, recording);
        return executeJCmd(JCMD_JFR_DUMP, pars);
    }

    boolean startJfrRecording(String name, String[] settings, String delay, String duration, Boolean disk, String path, String maxAge, String maxSize, Boolean dumpOnExit) {
        if (!isJfrAvailable()) {
            throw new UnsupportedOperationException();
        }
        Map<String, Object> pars = new HashMap();
        if (name != null) pars.put(JCMD_JFR_START_NAME, name);
        if (settings != null) pars.put(JCMD_JFR_START_SETTINGS, settings);
        if (delay != null) pars.put(JCMD_JFR_START_DELAY, delay);
        if (duration != null) pars.put(JCMD_JFR_START_DURATION, duration);
        if (maxAge != null) pars.put(JCMD_JFR_START_MAXAGE, maxAge);
        if (maxSize != null) pars.put(JCMD_JFR_START_MAXSIZE, maxSize);
        if (dumpOnExit != null) pars.put(JCMD_JFR_START_DUMPONEXIT, dumpOnExit);
        if (path != null) pars.put(JCMD_JFR_START_FILENAME, path);
        if (disk != null && !oldJFR) pars.put(JCMD_JFR_START_DISK, disk);

        if (pars.isEmpty()) pars = EMPTY_PARS;
        executeJCmd(JCMD_JFR_START, pars);
        return true;
    }

    boolean stopJfrRecording() {
        if (!isJfrAvailable()) {
            throw new UnsupportedOperationException();
        }
        String recKey = oldJFR ? JCMD_JFR_DUMP_RECORDING : JCMD_JFR_STOP_NAME;
        for (Long recording : jfrCheck()) {
            Map<String,Object> pars = Collections.singletonMap(recKey, recording);
            executeJCmd(JCMD_JFR_STOP, pars);
        }
        return true;
    }

    String getCommandLine() {
        synchronized (commandLineLock) {
            if (commandLine == null) {
                String vmCommandLine = executeJCmd(CMDLINE_COMMAND);
                if (vmCommandLine != null) {
                    commandLine = parseVMCommandLine(vmCommandLine);
                }
                return null;
            }
            return commandLine;
        }
    }

    private String getJfrCheck() {
        return executeJCmd(JCMD_JFR_CHECK, EMPTY_PARS);
    }

    private boolean checkForOldJFR() {
        String ret = getJCmdHelp(JCMD_JFR_CHECK);

        if (ret != null) {
            int options = ret.indexOf(JCMD_JFR_CHECK_HELP_OPTIONS_ID);
            int recording = ret.indexOf(JCMD_JFR_CHECK_HELP_RECORDING_ID);

            return options != -1 && options < recording;
        }
        return false;
    }

    private boolean unlockCommercialFeature() {
        String ret = executeJCmd(JCMD_UNLOCK_CF);
        return ret.contains(JCMD_CF_ID);
    }

    private String getJCmdHelp(String command) {
        Map pars = Collections.singletonMap(command, null);
        return executeJCmd(JCMD_HELP, pars);
    }

    private String executeJCmd(String command) {
        return executeJCmd(command, Collections.EMPTY_MAP);
    }

    private String executeJCmd(String command, Map<String,Object> pars) {
        if (jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
            MBeanServerConnection conn = jmxModel.getMBeanServerConnection();
            try {
                ObjectName diagCommName = new ObjectName(DIAGNOSTIC_COMMAND_MXBEAN_NAME);
                if (conn.isRegistered(diagCommName)) {
                    Object[] params = null;
                    String[] signature = null;
                    Object ret;

                    if (!pars.isEmpty()) {
                        params = new Object[] {getJCmdParams(pars)};
                        signature = new String[] {String[].class.getName()};
                    }
                    ret = conn.invoke(diagCommName, command, params, signature);
                    if (ret instanceof String) {
                        return (String)ret;
                    }
                }
            } catch (MalformedObjectNameException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                LOGGER.log(Level.INFO,"executeJCmd", ex); // NOI18N
            } catch (InstanceNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (MBeanException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ReflectionException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }

    private String parseVMCommandLine(String vmCommandLine) {
      String[] lines = vmCommandLine.split("\\r?\\n");
      for (String line : lines) {
          if (line.startsWith(CMDLINE_PREFIX)) {
              return line.substring(CMDLINE_PREFIX.length());
          }
      }
      return null;
    }

    private static String[] getJCmdParams(Map<String, Object> pars) {
        String[] jcmdParams = new String[pars.size()];
        int i = 0;
        for (Map.Entry<String,Object> e : pars.entrySet()) {
            String par;
            String key = e.getKey();
            Object val = e.getValue();

            if (val == null) {
                par = key;
            } else {
                par = String.format("%s=%s", key, quoteString(val.toString())); // NOI18N
            }
            jcmdParams[i++] = par;
        }
        return jcmdParams;
    }

    private static String quoteString(String val) {
        if (val.indexOf(' ')>=0) {
            return "\""+val+"\"";   //NOI18N
        }
        return val;
    }

}
