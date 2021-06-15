/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.locks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Tomas Hurka
 */
class ThreadInfo {

    private final int threadId;
    private final Map<MonitorInfo, MonitorDetail> waitMonitors;
    private final Map<MonitorInfo, MonitorDetail> ownerMonitors;
    private OpenMonitor openMonitor;
    private final String threadName;
    private final String threadClassName;

    ThreadInfo(int id, String name, String className) {
        threadId = id;
        threadName = name;
        threadClassName = className;
        waitMonitors = new HashMap();
        ownerMonitors = new HashMap();
    }

    void openMonitor(ThreadInfo owner, MonitorInfo mi, long timeStamp) {
        assert openMonitor == null;
        openMonitor = new OpenMonitor(mi, owner, timeStamp);
    }

    void closeMonitor(MonitorInfo mi, long timeStamp) {
        assert openMonitor != null;
        assert mi.equals(openMonitor.monitor);
        long wait = timeStamp - openMonitor.timeStamp;
        if (LockGraphBuilder.LOG.isLoggable(Level.FINEST)) {
            LockGraphBuilder.LOG.log(Level.FINEST, "Monitor exit mId = {0}, time diff = {1}", new Object[]{Integer.toHexString(mi.hashCode()), wait});
        }
        addMonitor(waitMonitors, mi, openMonitor.owner, wait);
        addMonitor(openMonitor.owner.ownerMonitors, mi, this, wait);
        openMonitor = null;
    }

    private static void addMonitor(Map<MonitorInfo,MonitorDetail> monitors, MonitorInfo mi, ThreadInfo ti, long wait) {
        MonitorDetail m = monitors.get(mi);
        if (m == null) {
            m = new MonitorDetail(mi);
            monitors.put(mi, m);
        }
        m.addWait(ti, wait);
    }

    void timeAdjust(long timeDiff) {
        if (openMonitor != null) {
            openMonitor.timeAdjust(timeDiff);
            openMonitor.monitor.timeAdjust(this, timeDiff);
        }
    }
    
    boolean isEmpty() {
        return waitMonitors.isEmpty() && ownerMonitors.isEmpty();
    }

    List<MonitorDetail> cloneWaitMonitorDetails() {
        return cloneMonitorDetails(waitMonitors);
    }

    List<MonitorDetail> cloneOwnerMonitorDetails() {
        return cloneMonitorDetails(ownerMonitors);
    }

    private static List<MonitorDetail> cloneMonitorDetails(Map<MonitorInfo,MonitorDetail> monitors) {
        List details = new ArrayList(monitors.size());
        for (MonitorDetail m : monitors.values()) {
            details.add(new MonitorDetail(m));
        }
        return details;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ThreadInfo) {
            ThreadInfo mi = (ThreadInfo) obj;
            return mi.threadId == threadId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return threadId;
    }

    String getName() {
        return threadName;
    }

    private static class OpenMonitor {

        final MonitorInfo monitor;
        final ThreadInfo owner;
        long timeStamp;

        OpenMonitor(MonitorInfo mi, ThreadInfo ti, long ts0) {
            assert mi != null;
            assert ti != null;
            monitor = mi;
            owner = ti;
            timeStamp = ts0;
        }

        private void timeAdjust(long timeDiff) {
            timeStamp += timeDiff;
        }
    }

    static class MonitorDetail {

        final MonitorInfo monitor;
        final Map<ThreadInfo, MonitorInfo.ThreadDetail> threads;
        long count;
        long waitTime;

        private MonitorDetail(MonitorInfo mi) {
            monitor = mi;
            threads = new HashMap();
        }

        MonitorDetail(MonitorDetail m) {
            monitor = m.monitor;
            count = m.count;
            waitTime = m.waitTime;
            threads = new HashMap();
            for (MonitorInfo.ThreadDetail td : m.threads.values()) {
                threads.put(td.threadInfo, td);
            }
        }

        List<MonitorInfo.ThreadDetail> cloneThreadDetails() {
            return MonitorInfo.cloneThreadDetails(threads);
        }

        private void addWait(ThreadInfo ti, long wait) {
            waitTime += wait;
            count++;
            if (ti != null) {
                addThread(ti, wait);
            }
        }
        
        private void addThread(ThreadInfo ti, long wait) {
            MonitorInfo.ThreadDetail td = threads.get(ti);
            
            if (td == null) {
                td = new MonitorInfo.ThreadDetail(ti);
                threads.put(ti, td);
            }
            td.addWait(null, wait);
        }
    }
}
