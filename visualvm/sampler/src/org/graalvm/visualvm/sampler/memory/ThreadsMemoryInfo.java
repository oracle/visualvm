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

package org.graalvm.visualvm.sampler.memory;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Tomas Hurka
 */
class ThreadsMemoryInfo {
    
    private List<ThreadInfo> threads = new ArrayList();
    private List<Long> allocatedBytes = new ArrayList();
    private Map<Long,Long> allocatedBytesMap;
    private long totalBytes;
    private long timestamp;
    private long totalDiffBytes;
    private long totalAllocatedBytesPerSecond;
    
    ThreadsMemoryInfo(long time, ThreadInfo[] tinfo, long[] minfo) {
        allocatedBytesMap = new HashMap(threads.size()*4/3);
        totalBytes = 0;
        for (int i = 0; i <tinfo.length; i++) {
            ThreadInfo ti = tinfo[i];
            if (ti != null) {
                threads.add(ti);
                allocatedBytes.add(minfo[i]);
                allocatedBytesMap.put(ti.getThreadId(),minfo[i]);
                totalBytes+=minfo[i];
            }
        }
        timestamp = time;
    }
    
    List<ThreadInfo> getThreads() {
        return threads;
    }
    
    List<Long> getAllocatedBytes() {
        return allocatedBytes;
    }
    
    long getTotalBytes() {
        return totalBytes;
    }
    
    List<Long> getAllocatedDiffBytes(ThreadsMemoryInfo info) {
        List<Long> allocDiff = new ArrayList(threads.size());
        List<ThreadInfo> newThreads = info.getThreads();
        List<Long> newAllocatedBytes = info.getAllocatedBytes();
        
        totalDiffBytes = 0;
        for (int i=0; i<newThreads.size(); i++) {
            ThreadInfo ti = newThreads.get(i);
            Long oldAlloc = allocatedBytesMap.get(ti.getThreadId());
            long diff;
            
            if (oldAlloc == null) {
                oldAlloc = Long.valueOf(0);
            }
            diff = newAllocatedBytes.get(i)-oldAlloc;
            allocDiff.add(diff);
            totalDiffBytes += diff;
        }
        return allocDiff;
    }
    
    long getTotalDiffBytes() {
        return totalDiffBytes;
    }
    
    List<Long> getAllocatedBytesPerSecond(ThreadsMemoryInfo newInfo) {
        assert newInfo.timestamp >= timestamp;
        List<Long> diff = getAllocatedDiffBytes(newInfo);
        double secs = (newInfo.timestamp - timestamp) / 1000.0;
        List<Long> diffPerSec = new ArrayList(diff.size());
        
        for (Long d : diff) {
            diffPerSec.add(new Long((long)(d/secs)));
        }
        totalAllocatedBytesPerSecond = (long) (getTotalDiffBytes()/secs);
        return diffPerSec;
    }

    long getTotalAllocatedBytesPerSecond() {
        return totalAllocatedBytesPerSecond;
    }
}
