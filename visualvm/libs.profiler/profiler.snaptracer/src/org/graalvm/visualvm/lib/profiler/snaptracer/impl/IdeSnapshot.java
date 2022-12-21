/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.snaptracer.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogRecord;
import javax.swing.Icon;
import org.graalvm.visualvm.lib.jfluid.results.cpu.PrestimeCPUCCTNode;
import org.graalvm.visualvm.lib.profiler.LoadedSnapshot;
import org.graalvm.visualvm.lib.profiler.SampledCPUSnapshot;
import org.graalvm.visualvm.lib.profiler.snaptracer.logs.LogReader;
import org.openide.filesystems.FileObject;

/** Reads xml log and npss snapshot from file.
 *
 * @author Tomas Hurka
 */
public final class IdeSnapshot {

    private final SampledCPUSnapshot cpuSnapshot;
    private LogReader xmlLogs;
    private LogRecord lastRecord;
    private Map<Integer, LogRecord> recordsMap;
    private Map<Integer, LogRecordInfo> infosMap;
    private final FileObject npssFileObject;

    public IdeSnapshot(FileObject npssFO, FileObject uigestureFO) throws IOException {
        cpuSnapshot = new SampledCPUSnapshot(npssFO);
        npssFileObject = npssFO;
        if (uigestureFO != null) {
            xmlLogs = new LogReader(uigestureFO);
            xmlLogs.load();
            recordsMap = new HashMap();
            infosMap = new HashMap();
        }
    }

    int getSamplesCount() {
        return cpuSnapshot.getSamplesCount();
    }

    long getTimestamp(int sampleIndex) throws IOException {
        return cpuSnapshot.getTimestamp(sampleIndex);
    }

    FileObject getNpssFileObject() {
        return npssFileObject;
    }

    public boolean hasUiGestures() {
        return xmlLogs != null;
    }

    LoadedSnapshot getCPUSnapshot(int startIndex, int endIndex) throws IOException {
        return cpuSnapshot.getCPUSnapshot(startIndex, endIndex);
    }

    public long getValue(int sampleIndex, int valIndex) throws IOException {
        if (valIndex == 0) {
            return cpuSnapshot.getValue(sampleIndex, valIndex);
        } else if (xmlLogs != null) {
            Integer val = getLogRecordValue(sampleIndex);
            if (val != null) {
                return val.intValue();
            }
        }
        return 0;
    }

    public LogRecordInfo getLogInfoForValue(long loggerValue) {
        if (xmlLogs == null || loggerValue == 0) {
            return null;
        }
        Integer index = new Integer((int) loggerValue);
        LogRecordInfo info = infosMap.get(index);

        if (info == null) {
            LogRecord rec = recordsMap.get(index);

            assert rec != null : "Null record for value "+index;        // NOI18N
            info = new LogRecordInfo(rec);
            infosMap.put(index, info);
        }
        return info;
    }

    private Integer getLogRecordValue(int sampleIndex) throws IOException {
        long timestamp = getTimestamp(sampleIndex);
        LogRecord rec = xmlLogs.getRecordFor(timestamp / 1000000);
        if (rec != null) {
            long startTime = cpuSnapshot.getStartTime();
            long endTime = getTimestamp(getSamplesCount() - 1);
            long recTime = rec.getMillis() * 1000000;
            if (recTime > startTime && recTime < endTime) {
                if (rec != lastRecord) {
                    Integer index = new Integer(sampleIndex+1);
                    lastRecord = rec;
                    recordsMap.put(index, rec);
                    return index;
                }
            }
        }
        return null;
    }

    String getThreadDump(int sampleIndex) throws IOException {
        return cpuSnapshot.getThreadDump(sampleIndex);
    }

    List<Integer> getIntervals(int start, int end, PrestimeCPUCCTNode node) throws IOException {
        return cpuSnapshot.getIntervals(start,end,node);
    }

    public static final class LogRecordInfo {
        private static final int MAX_DISPLAY_NAME = 40;
        
        private String name;
        private String displayName;
        private String toolTip;
        private Icon icon;
        private LogRecord record;

        LogRecordInfo(LogRecord rec) {
            record = rec;
        }

        void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            if (displayName == null) {
                String message = record.getMessage();
                
                if (message != null && !message.isEmpty()) {
                    StringBuilder sb = new StringBuilder(message);
                    Object[] arr = record.getParameters();
                    
                    if (arr != null && arr.length > 0) {
                        String sep = " (";  // NOI18N
                        
                        for (Object par : arr) {
                            sb.append(sep);
                            sb.append(par);
                            sep = ", ";     // NOI18N
                            if (sb.length() > MAX_DISPLAY_NAME) {
                                return sb.substring(0,MAX_DISPLAY_NAME).concat(" ..."); // NOI18N
                            }
                        }
                        sb.append(")");     // NOI18N
                    }
                    return sb.toString();
                }
            }
            return displayName;
        }

        void setToolTip(String toolTip) {
            this.toolTip = toolTip;
        }

        public String getToolTip() {
            return toolTip;
        }

        void setIcon(Icon icon) {
            this.icon = icon;
        }

        public Icon getIcon() {
            return icon;
        }
    }
}
