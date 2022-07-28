/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.visualvm.tools.attach.AttachModel;
import org.graalvm.visualvm.tools.jfr.JfrModel;
import org.graalvm.visualvm.tools.jmx.JmxModel;

/**
 *
 * @author Tomas Hurka
 */
public class JfrModelImpl extends JfrModel {

    private static final int ATTACH_CMD = 0;
    private static final int JMX_CMD = 1;
    private static final String[] JCMD_JFR_DUMP = new String[]{"JFR.dump", "jfrDump"};    // NOI18N
    private static final String JCMD_JFR_DUMP_FILENAME = "filename";    // NOI18N
    private static final String JCMD_JFR_DUMP_RECORDING = "recording";    // NOI18N
    private static final String JCMD_JFR_DUMP_NAME = "name";    // NOI18N
    private static final String[] JCMD_JFR_CHECK = new String[]{"JFR.check", "jfrCheck"};   // NOI18N
    private static final String JCMD_JFR_CHECK_RECORDING_ID = "recording=";     // NOI18N
    private static final String JCMD_JFR_CHECK_RECORDING_ID1 = "Recording ";     // NOI18N
    private static final String JCMD_JFR_CHECK_HELP_OPTIONS_ID = "Options: ";        // NOI18N
    private static final String JCMD_JFR_CHECK_HELP_RECORDING_ID = "recording : ";        // NOI18N
    private static final String[] JCMD_JFR_START = new String[]{"JFR.start", "jfrStart"};   // NOI18N
    private static final String JCMD_JFR_START_NAME = "name"; // NOI18N
    private static final String JCMD_JFR_START_SETTINGS = "settings"; // NOI18N
    private static final String JCMD_JFR_START_DELAY = "delay"; // NOI18N
    private static final String JCMD_JFR_START_DURATION = "duration"; // NOI18N
    private static final String JCMD_JFR_START_DISK = "disk"; // NOI18N
    private static final String JCMD_JFR_START_FILENAME = "filename"; // NOI18N
    private static final String JCMD_JFR_START_MAXAGE = "maxage"; // NOI18N
    private static final String JCMD_JFR_START_MAXSIZE = "maxsize"; // NOI18N
    private static final String JCMD_JFR_START_DUMPONEXIT = "dumponexit"; // NOI18N
    private static final String[] JCMD_JFR_STOP = new String[]{"JFR.stop", "jfrStop"};   // NOI18N
    private static final String JCMD_JFR_STOP_NAME = "name";   // NOI18N
    private static final String JCMD_JFR_UNLOCK_ID = "Use VM.unlock_commercial_features to enable"; // NOI18N
    private static final String[] JCMD_UNLOCK_CF = new String[]{"VM.unlock_commercial_features", "vmUnlockCommercialFeatures"}; // NOI18N
    private static final String[] JCMD_HELP = new String[]{"help", "help"};                 // NOI18N
    private static final String JCMD_CF_ID = " unlocked.";   // NOI18N
    private static final String JCMD_JFR_UNKNOWN_COMMAND = "Unknown diagnostic command";   // NOI18N
    private static final Map<String, Object> EMPTY_PARS = Collections.singletonMap("", null);

    private boolean oldJFR;
    private AttachModel attach;
    private JmxModel jmx;

    JfrModelImpl(AttachModel attachModel) {
        attach = attachModel;
    }

    JfrModelImpl(JmxModel jmxModel) {
        jmx = jmxModel;
    }

    boolean isJfrAvailable() {
        boolean jfrAvailable;
        String recordings = executeJCmd(JCMD_JFR_CHECK, EMPTY_PARS);
        if (recordings == null) {
            return false;
        } else {
            if (recordings.contains(JCMD_JFR_UNLOCK_ID)) {
                jfrAvailable = unlockCommercialFeature();
            } else if (recordings.contains(JCMD_JFR_UNKNOWN_COMMAND)) {
                jfrAvailable = false;
            } else {
                jfrAvailable = true;
            }
        }
        if (jfrAvailable) {
            oldJFR = checkForOldJFR();
        }
        return jfrAvailable;
    }

    public List<Long> jfrCheck() {
        String recordings = executeJCmd(JCMD_JFR_CHECK, EMPTY_PARS);
        if (recordings == null) {
            return Collections.emptyList();
        }
        String[] lines = recordings.split("\\r?\\n");       // NOI18N
        List<Long> recNumbers = new ArrayList<>(lines.length);

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
        Map<String, Object> pars = new HashMap<>();
        pars.put(JCMD_JFR_DUMP_FILENAME, fileName);
        pars.put(oldJFR ? JCMD_JFR_DUMP_RECORDING : JCMD_JFR_DUMP_NAME, recording);
        return executeJCmd(JCMD_JFR_DUMP, pars);
    }

    public boolean startJfrRecording(String name, String[] settings, String delay,
            String duration, Boolean disk, String path, String maxAge, String maxSize,
            Boolean dumpOnExit) {
        Map<String, Object> pars = new HashMap<>();
        if (name != null) {
            pars.put(JCMD_JFR_START_NAME, name);
        }
        if (settings != null) {
            for (String setting : settings) {
                pars.put(JCMD_JFR_START_SETTINGS, setting);
            }
        }
        if (delay != null) {
            pars.put(JCMD_JFR_START_DELAY, delay);
        }
        if (duration != null) {
            pars.put(JCMD_JFR_START_DURATION, duration);
        }
        if (maxAge != null) {
            pars.put(JCMD_JFR_START_MAXAGE, maxAge);
        }
        if (maxSize != null) {
            pars.put(JCMD_JFR_START_MAXSIZE, maxSize);
        }
        if (dumpOnExit != null) {
            pars.put(JCMD_JFR_START_DUMPONEXIT, dumpOnExit);
        }
        if (path != null) {
            pars.put(JCMD_JFR_START_FILENAME, path);
        }
        if (disk != null && !oldJFR) {
            pars.put(JCMD_JFR_START_DISK, disk);
        }

        if (pars.isEmpty()) {
            pars = EMPTY_PARS;
        }
        executeJCmd(JCMD_JFR_START, pars);
        return true;
    }

    public boolean stopJfrRecording() {
        String recKey = oldJFR ? JCMD_JFR_DUMP_RECORDING : JCMD_JFR_STOP_NAME;
        for (Long recording : jfrCheck()) {
            Map<String, Object> pars = Collections.singletonMap(recKey, recording);
            executeJCmd(JCMD_JFR_STOP, pars);
        }
        return true;
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

    private String getJCmdHelp(String[] command) {
        // help command needs Attach API style commands on JMX
        return executeJCmd(JCMD_HELP, Collections.singletonMap(command[ATTACH_CMD], null));
    }

    private String executeJCmd(String[] command, Map<String, Object> pars) {
        if (attach != null) {
            return attach.executeJCmd(command[ATTACH_CMD], pars);
        }
        return jmx.executeJCmd(command[JMX_CMD], pars);
    }

    private String executeJCmd(String[] string) {
        return executeJCmd(string, Collections.emptyMap());
    }
}
