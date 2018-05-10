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

package com.sun.tools.visualvm.sampler.cpu;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Tomas Hurka
 */
class ThreadsCPUInfo {
    
    private List<ThreadInfo> threads = new ArrayList();
    private List<Long> cputime = new ArrayList();
    private Map<Long,Long> cputimeMap;
    private long totalCPUTime;
    private long timestamp;
    private long totalDiffCPUTime;
    
    ThreadsCPUInfo(long time, ThreadInfo[] tinfo, long[] cpuinfo) {
        cputimeMap = new HashMap(threads.size()*4/3);
        totalCPUTime = 0;
        for (int i = 0; i <tinfo.length; i++) {
            ThreadInfo ti = tinfo[i];
            if (ti != null) {
                threads.add(ti);
                cputime.add(cpuinfo[i]);
                cputimeMap.put(ti.getThreadId(),cpuinfo[i]);
                totalCPUTime+=cpuinfo[i];
            }
        }
        timestamp = time;
    }
    
    List<ThreadInfo> getThreads() {
        return threads;
    }
    
    List<Long> getThreadCPUTime() {
        return cputime;
    }
    
    long getTotalCPUTime() {
        return totalCPUTime;
    }
    
    List<Long> getThreadCPUTimeDiff(ThreadsCPUInfo info) {
        List<Long> cpuTimeDiff = new ArrayList(threads.size());
        List<ThreadInfo> newThreads = info.getThreads();
        List<Long> newCPUTime = info.getThreadCPUTime();
        
        totalDiffCPUTime = 0;
        for (int i=0; i<newThreads.size(); i++) {
            ThreadInfo ti = newThreads.get(i);
            Long oldAlloc = cputimeMap.get(ti.getThreadId());
            long diff;
            
            if (oldAlloc == null) {
                oldAlloc = Long.valueOf(0);
            }
            diff = newCPUTime.get(i)-oldAlloc;
            cpuTimeDiff.add(diff);
            totalDiffCPUTime += diff;
        }
        return cpuTimeDiff;
    }
    
    long getTotalDiffCPUTime() {
        return totalDiffCPUTime;
    }
    
    List<Long> getCPUTimePerSecond(ThreadsCPUInfo newInfo) {
        assert newInfo.timestamp >= timestamp;
        List<Long> diff = getThreadCPUTimeDiff(newInfo);
        double secs = (newInfo.timestamp - timestamp) / 1000.0;
        List<Long> diffPerSec = new ArrayList(diff.size());
        
        for (Long d : diff) {
            diffPerSec.add(new Long((long)(d/secs)));
        }
        return diffPerSec;
    }
}
