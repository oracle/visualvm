/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.attach;

import com.sun.tools.attach.VirtualMachine;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.tools.attach.AttachModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 *
 * @author Tomas Hurka
 */
class AttachModelImpl extends AttachModel {
    static final String LIVE_OBJECTS_OPTION = "-live";  // NOI18N
    static final String ALL_OBJECTS_OPTION = "-all";    // NOI18N
    static final String HEAP_DUMP_NO_SPACE_ID = "No space left on device";  // NOI18N
    static final String JCMD_VM_COMMAND_LINE = "VM.command_line";    // NOI18N
    static final String JCMD_JFR_DUMP = "JFR.dump";    // NOI18N
    static final String JCMD_JFR_DUMP_FILENAME = "filename";    // NOI18N
    static final String JCMD_JFR_DUMP_RECORDING = "recording";    // NOI18N
    static final String JCMD_JFR_DUMP_NAME = "name";    // NOI18N
    static final String JCMD_JFR_CHECK = "JFR.check";   // NOI18N
    static final String JCMD_JFR_CHECK_RECORDING_ID = "recording=";     // NOI18N
    static final String JCMD_JFR_CHECK_RECORDING_ID1 = "Recording ";     // NOI18N
    static final String JCMD_JFR_CHECK_HELP_OPTIONS_ID = "Options: ";        // NOI18N
    static final String JCMD_JFR_CHECK_HELP_RECORDING_ID = "recording : ";        // NOI18N
    static final String JCMD_JFR_START = "JFR.start";   // NOI18N
    private static final String JCMD_JFR_START_NAME = "name"; // NOI18N
    private static final String JCMD_JFR_START_SETTINGS = "settings"; // NOI18N
    private static final String JCMD_JFR_START_DELAY = "delay"; // NOI18N
    private static final String JCMD_JFR_START_DURATION = "duration"; // NOI18N
    private static final String JCMD_JFR_START_DISK = "disk"; // NOI18N
    private static final String JCMD_JFR_START_FILENAME = "filename"; // NOI18N
    private static final String JCMD_JFR_START_MAXAGE = "maxage"; // NOI18N
    private static final String JCMD_JFR_START_MAXSIZE = "maxsize"; // NOI18N
    private static final String JCMD_JFR_START_DUMPONEXIT = "dumponexit"; // NOI18N
    static final String JCMD_JFR_STOP = "JFR.stop";   // NOI18N
    static final String JCMD_JFR_STOP_NAME = "name";   // NOI18N
    static final String JCMD_JFR_UNLOCK_ID = "Use VM.unlock_commercial_features to enable"; // NOI18N
    static final String JCMD_UNLOCK_CF = "VM.unlock_commercial_features"; // NOI18N
    static final String JCMD_HELP = "help";                 // NOI18N
    static final String JCMD_CF_ID = " unlocked.";   // NOI18N
    static final Logger LOGGER = Logger.getLogger(AttachModelImpl.class.getName());

    String pid;
    HotSpotVirtualMachine vm;
    Map<String,String> commandLineMap;
    boolean oldJFR;
    Boolean jfrAvailable;
    
    AttachModelImpl(Application app) {
        pid = Integer.toString(app.getPid());
    }
    
    public synchronized Properties getSystemProperties() {
        try {
            return getVirtualMachine().getSystemProperties();
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"getSystemProperties",ex);    // NOI18N
        }
        return null;
    }
    
    public synchronized boolean takeHeapDump(String fileName) {
        try {
            InputStream in = getVirtualMachine().dumpHeap(fileName,LIVE_OBJECTS_OPTION);
            String out = readToEOF(in);
            if (!out.isEmpty()) {
                LOGGER.log(Level.INFO,"takeHeapDump",out);  // NOI18N
            }
            Path f = Paths.get(fileName);
            if (out.contains(HEAP_DUMP_NO_SPACE_ID)) {
                Files.deleteIfExists(f);
                return false;
            }
            return Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(f);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"takeHeapDump",ex);   // NOI18N
        }
        return false;
    }
    
    public synchronized String takeThreadDump() {
        try {
            InputStream in = getVirtualMachine().remoteDataDump("-l");  // NOI18N
            return readToEOF(in);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"takeThreadDump",ex);     // NOI18N
        }
        return null;
    }
    
    public synchronized String printFlag(String name) {
        try {
            InputStream in = getVirtualMachine().printFlag(name);
            return readToEOF(in);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"printFlag",ex);  // NOI18N
        }
        return null;
    }
    
    public synchronized void setFlag(String name, String value) {
        try {
            InputStream in = getVirtualMachine().setFlag(name,value);
            String out = readToEOF(in);
            if (!out.isEmpty()) {
                LOGGER.log(Level.INFO,"setFlag",out);   // NOI18N
            }
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"setFlag",ex);    // NOI18N
        }
    }

    public synchronized HeapHistogramImpl takeHeapHistogram() {
        try {
            InputStream in = getVirtualMachine().heapHisto(ALL_OBJECTS_OPTION);
            HeapHistogramImpl h = new HeapHistogramImpl(in);
            in.close();
            return h;
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"takeHeapHistogram",ex);  // NOI18N
        }
        return null;
    }

    public String getCommandLine() {
        Map<String,String> cmdLineMap = getVMCommandLine();
        if (cmdLineMap != null) {
            return cmdLineMap.get("java_command");      // NOI18N
        }
        return null;
    }
    
    public String getJvmArgs() {
        Map<String,String> cmdLineMap = getVMCommandLine();
        if (cmdLineMap != null) {
            return cmdLineMap.get("jvm_args");          // NOI18N
        }
        return null;
    }

    public String getJvmFlags() {
        Map<String,String> cmdLineMap = getVMCommandLine();
        if (cmdLineMap != null) {
            return cmdLineMap.get("jvm_flags");         // NOI18N
        }
        return null;
    }

    public synchronized boolean isJfrAvailable() {
        if (jfrAvailable == null) {
            String recordings = executeJCmd(JCMD_JFR_CHECK);
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

    public List<Long> jfrCheck() {
        if (!isJfrAvailable()) {
            throw new UnsupportedOperationException();
        }
        String recordings = executeJCmd(JCMD_JFR_CHECK);
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

    public String takeJfrDump(long recording, String fileName) {
        if (!isJfrAvailable()) {
            throw new UnsupportedOperationException();
        }
        Map<String, Object> pars = new HashMap();
        pars.put(JCMD_JFR_DUMP_FILENAME, fileName);
        pars.put(oldJFR ? JCMD_JFR_DUMP_RECORDING : JCMD_JFR_DUMP_NAME, recording);
        return executeJCmd(JCMD_JFR_DUMP, pars);
    }

    public boolean startJfrRecording(String name, String[] settings, String delay,
            String duration, Boolean disk, String path, String maxAge, String maxSize,
            Boolean dumpOnExit) {
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
        executeJCmd(JCMD_JFR_START, pars);
        return true;
    }

    public boolean stopJfrRecording() {
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

    HotSpotVirtualMachine getVirtualMachine() throws IOException {
        if (vm == null) {
            try {
                vm = (HotSpotVirtualMachine) VirtualMachine.attach(pid);
            } catch (Exception x) {
                throw new IOException(x.getLocalizedMessage(),x);
            }
        }
        return vm;
    }
    
    private String executeJCmd(String command, Map<String,Object> pars) {
        StringBuilder commandLine = new StringBuilder(command);

        for (Map.Entry<String,Object> e : pars.entrySet()) {
            String par;
            String key = e.getKey();
            Object val = e.getValue();

            if (val == null) {
                par = key;
            } else {
                par = String.format("%s=%s", key, quoteString(val.toString())); // NOI18N
            }
            commandLine.append(' ').append(par);
        }
        return executeJCmd(commandLine.toString());
    }

    private static String quoteString(String val) {
        if (val.indexOf(' ')>=0) {
            return "\""+val+"\"";   //NOI18N
        }
        return val;
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
        return executeJCmd(JCMD_HELP+" "+command);
    }

    private synchronized Map<String,String> getVMCommandLine() {
        if (commandLineMap == null) {
            String text = executeJCmd(JCMD_VM_COMMAND_LINE);
            commandLineMap = new HashMap();
            if (text != null) {
                String[] lines = text.split("\\R"); // NOI18N
                for (String line : lines) {
                    int offset = line.indexOf(':');     // NOI18N
                    if (offset != -1) {
                        String key = line.substring(0, offset).trim();
                        String value = line.substring(offset+1).trim();
                        commandLineMap.put(key, value);
                    }
                }
            }
        }
        return commandLineMap;
    }

    private synchronized String executeJCmd(String command) {
        try {
            InputStream in = getVirtualMachine().executeJCmd(command);
            return readToEOF(in);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"executeJCmd",ex);    // NOI18N
        }
        return null;
    }

    private String readToEOF(InputStream in) throws IOException {
        StringBuffer buffer = new StringBuffer(1024);
        byte b[] = new byte[256];
        int n;
        
        do {
            n = in.read(b);
            if (n > 0) {
                String s = new String(b, 0, n, "UTF-8");    // NOI18N
                
                buffer.append(s);
            }
        } while (n > 0);
        in.close();
        return buffer.toString();
    }

    protected void finalize() throws Throwable {
        if (vm != null) vm.detach();
        super.finalize();
    }
    
}