/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

package org.netbeans.lib.profiler.results.memory;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.netbeans.lib.profiler.wireprotocol.HeapHistogramResponse;

/**
 *
 * @author Tomas Hurka
 */
public class HeapHistogramManager {

    Map<Integer, String> classesIdMap = new HashMap(8000);

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
        for (int i = 0; i < ids.length; i++) {
            ClassInfoImpl ci = new ClassInfoImpl(classesIdMap.get(ids[i]), instances[i], bytes[i]);
            histogram.addClassInfo(ci, false);
        }
        return histogram;
    }

    class HeapHistogramImpl extends HeapHistogram {

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
