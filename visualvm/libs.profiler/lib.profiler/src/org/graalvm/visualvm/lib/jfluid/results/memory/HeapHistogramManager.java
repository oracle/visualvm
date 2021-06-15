/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.results.memory;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.HeapHistogramResponse;

/**
 *
 * @author Tomas Hurka
 */
public class HeapHistogramManager {

    private Map<Integer, String> classesIdMap = new HashMap(8000);
    private final ProfilerEngineSettings settings;

    public HeapHistogramManager(ProfilerEngineSettings settings) {
        this.settings = settings;
    }

    public HeapHistogram getHistogram(HeapHistogramResponse resp) {
        String[] newNames = resp.getNewNames();
        int[] newIds = resp.getNewids();

        for (int i = 0; i < newNames.length; i++) {
            classesIdMap.put(newIds[i], newNames[i]);
        }
        int ids[] = resp.getIds();
        long instances[] = resp.getInstances();
        long bytes[] = resp.getBytes();
        HeapHistogramImpl histogram = new HeapHistogramImpl(resp.getTime());
        GenericFilter classFilter = settings.getInstrumentationFilter();
        for (int i = 0; i < ids.length; i++) {
            String className = classesIdMap.get(ids[i]);

            if (classFilter.passes(className.replace('.', '/'))) { // NOI18N
                ClassInfoImpl ci = new ClassInfoImpl(className, instances[i], bytes[i]);
                histogram.addClassInfo(ci, false);
            }
        }
        return histogram;
    }

    static class HeapHistogramImpl extends HeapHistogram {

        private Date time;
        private long totalHeapInstances;
        private long totalHeapBytes;
        private Set<ClassInfo> heap;
        private long totalPermInstances;
        private long totalPermBytes;
        private Set<ClassInfo> perm;

        HeapHistogramImpl(Date t) {
            time = t;
            heap = new HashSet(4096);
            perm = new HashSet();
        }

        void addClassInfo(ClassInfo ci, boolean permInfo) {
            if (permInfo) {
                perm.add(ci);
                totalPermInstances += ci.getInstancesCount();
                totalPermBytes += ci.getBytes();
            } else {
                heap.add(ci);
                totalHeapInstances += ci.getInstancesCount();
                totalHeapBytes += ci.getBytes();
            }
        }

        @Override
        public Date getTime() {
            return time;
        }

        @Override
        public long getTotalInstances() {
            return totalHeapInstances + totalPermInstances;
        }

        @Override
        public long getTotalBytes() {
            return totalHeapBytes + totalPermBytes;
        }

        @Override
        public Set<ClassInfo> getHeapHistogram() {
            return heap;
        }

        @Override
        public long getTotalHeapInstances() {
            return totalHeapInstances;
        }

        @Override
        public long getTotalHeapBytes() {
            return totalHeapBytes;
        }

        @Override
        public Set<ClassInfo> getPermGenHistogram() {
            return perm;
        }

        @Override
        public long getTotalPerGenInstances() {
            return totalPermInstances;
        }

        @Override
        public long getTotalPermGenHeapBytes() {
            return totalPermBytes;
        }
    }

    static class ClassInfoImpl extends HeapHistogram.ClassInfo {

        private String name;
        private long instances;
        private long bytes;

        ClassInfoImpl(String n, long i, long b) {
            name = n;
            instances = i;
            bytes = b;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getInstancesCount() {
            return instances;
        }

        @Override
        public long getBytes() {
            return bytes;
        }
    }
}
