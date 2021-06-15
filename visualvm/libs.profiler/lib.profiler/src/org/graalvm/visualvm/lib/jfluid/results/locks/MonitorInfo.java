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
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;

/**
 *
 * @author Tomas Hurka
 */
class MonitorInfo {

    final private int monitorId;
    private String className;
    private Map<ThreadInfo, OpenThread> openThreads;
    private Map<ThreadInfo, ThreadDetail> waitThreads;
    private Map<ThreadInfo, ThreadDetail> ownerThreads;

    MonitorInfo(int id) {
        monitorId = id;
        waitThreads = new HashMap();
        ownerThreads = new HashMap();
        openThreads = new HashMap();
        className = "*unknown*"; // NOI18N
    }

    MonitorInfo(int id, String cname) {
        this(id);
        className = StringUtils.userFormClassName(cname);
    }

    void setClassName(String cname) {
        className = StringUtils.userFormClassName(cname);
    }

   @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MonitorInfo) {
            MonitorInfo mi = (MonitorInfo) obj;
            return mi.monitorId == monitorId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return monitorId;
    }

    void openThread(ThreadInfo ti, ThreadInfo owner, long timeStamp0) {
        assert openThreads.get(ti) == null;
        openThreads.put(ti, new OpenThread(ti, owner, timeStamp0));
    }

    void closeThread(ThreadInfo ti, long timeStamp0) {
        OpenThread openThread = openThreads.remove(ti);
        assert openThread != null;
        long wait = timeStamp0 - openThread.timeStamp;
        addThread(waitThreads, ti, openThread.owner, wait);
        addThread(ownerThreads, openThread.owner, ti, wait);
    }

    private static void addThread(Map<ThreadInfo,ThreadDetail> threads, ThreadInfo master, ThreadInfo detail, long wait) {
        ThreadDetail td = threads.get(master);
        if (td == null) {
            td = new ThreadDetail(master);
            threads.put(master, td);
        }
        td.addWait(detail, wait);
    }

    void timeAdjust(ThreadInfo ti, long timeDiff) {
        OpenThread openThread = openThreads.get(ti);
        assert openThread != null;
        openThread.timeAdjust(timeDiff);
    }

    List<ThreadDetail> cloneWaitThreadDetails() {
        return cloneThreadDetails(waitThreads);
    }

    List<ThreadDetail> cloneOwnerThreadDetails() {
        return cloneThreadDetails(ownerThreads);
    }

    static List<ThreadDetail> cloneThreadDetails(Map<ThreadInfo,ThreadDetail> threads) {
        List details = new ArrayList(threads.size());
        for (ThreadDetail d : threads.values()) {
            details.add(new ThreadDetail(d));
        }
        return details;

    }

    String getName() {
        return new StringBuffer(className).append('(').append(Integer.toHexString(monitorId)).append(')').toString(); // NOI18N
    }

    private static class OpenThread {

        private final ThreadInfo threadInfo;
        private final ThreadInfo owner;
        private long timeStamp;

        OpenThread(ThreadInfo ti, ThreadInfo ownerTi, long ts) {
            threadInfo = ti;
            owner = ownerTi;
            timeStamp = ts;
        }

        private void timeAdjust(long timeDiff) {
            timeStamp += timeDiff;
        }
    }

    static class ThreadDetail {

        final ThreadInfo threadInfo;
        private Map<ThreadInfo, ThreadDetail> threads;
        long count;
        long waitTime;

        ThreadDetail(ThreadInfo ti) {
            threadInfo = ti;
            threads = new HashMap();
        }

        ThreadDetail(ThreadDetail d) {
            threadInfo = d.threadInfo;
            count = d.count;
            waitTime = d.waitTime;
            threads = new HashMap();
            for (ThreadDetail td : d.threads.values()) {
                threads.put(td.threadInfo, new ThreadDetail(td));
            }
        }

        List<ThreadDetail> cloneThreadDetails() {
            return MonitorInfo.cloneThreadDetails(threads);
        }

        void addWait(ThreadInfo ti, long wait) {
            waitTime += wait;
            count++;
            if (ti != null) {
                addThread(ti, wait);
            }
        }
        
        private void addThread(ThreadInfo ti, long wait) {
            ThreadDetail td = threads.get(ti);
            
            if (td == null) {
                td = new ThreadDetail(ti);
                threads.put(ti, td);
            }
            td.addWait(null, wait);
        }
    }
}
