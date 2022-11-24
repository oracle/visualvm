/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.*;
import org.openide.util.NbBundle;


//@org.openide.util.lookup.ServiceProvider(service=org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.Rule.class)
public class HashMapHistogram extends IteratingRule {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class HashmapEntry extends Histogram.Entry<HashmapEntry> {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        int hmeCount;
        int strCount;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        HashmapEntry(long size, int hmeCount, int strCount) {
            super(size);
            this.hmeCount = hmeCount;
            this.strCount = strCount;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public String toString() {
            return "#:" + getCount() + "/" + getSize() + "B, " + hmeCount + " HMEs, " + strCount + " Strings<br>"; // NOI18N
        }

        @Override
        protected void add(HashmapEntry source) {
            hmeCount += source.hmeCount;
            strCount += source.strCount;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldHMEKey;
    private FieldAccess fldHMENext;
    private FieldAccess fldHMEValue;
    private FieldAccess fldHMTable;
    private FieldAccess fldSValue;
    private Histogram<HashmapEntry> byIncomming;
    private JavaClass clsHM;
    private JavaClass clsHME;
    private JavaClass clsString;
    private Set<Instance> known = new HashSet<>();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public HashMapHistogram() {
        super(NbBundle.getMessage(HashMapHistogram.class, "LBL_HMH_Name"),
                NbBundle.getMessage(HashMapHistogram.class, "LBL_HMH_Desc"),
                "java.util.HashMap"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(HashMapHistogram.class, "LBL_HMH_LongDesc");
    }

    protected void perform(Instance hm) {
        HashmapEntry he = sizeOfHashmap(hm);
        String incomming = getContext().getRootIncommingString(hm);
        incomming = Utils.printClass(getContext(), incomming);
        byIncomming.add(incomming, he);
    }

    protected @Override void prepareRule(MemoryLint context) {
        Heap heap = context.getHeap();
        clsString = heap.getJavaClassByName("java.lang.String"); // NOI18N
        clsHM = heap.getJavaClassByName("java.util.HashMap"); // NOI18N
        clsHME = heap.getJavaClassByName("java.util.HashMap$Entry"); // NOI18N
        fldSValue = new FieldAccess(clsString, "value"); // NOI18N
        fldHMTable = new FieldAccess(clsHM, "table"); // NOI18N
        fldHMEKey = new FieldAccess(clsHME, "key"); // NOI18N
        fldHMEValue = new FieldAccess(clsHME, "value"); // NOI18N
        fldHMENext = new FieldAccess(clsHME, "next"); // NOI18N
        byIncomming = new Histogram<>();
    }

    protected @Override void summary() {
        getContext().appendResults(byIncomming.toString(50000));
    }

    private boolean add(Instance inst) {
        if (known.contains(inst)) {
            return false;
        }

        known.add(inst);

        return true;
    }

    private long sizeIfNewString(Instance obj) {
        if (obj == null) {
            return 0;
        }

        if ("java.lang.String".equals(obj.getJavaClass().getName())) { // NOI18N
            if (add(obj)) {
                long sz = obj.getSize();
                Instance arr = fldSValue.getRefValue(obj);

                if ((arr != null) && add(arr)) {
                    sz += arr.getSize();
                }

                return sz;
            }
        }

        return 0;
    }

    private HashmapEntry sizeOfHashmap(Instance hm) {
        ObjectArrayInstance table = (ObjectArrayInstance) fldHMTable.getRefValue(hm);
        long sum = hm.getSize() + table.getSize();
        int hmeCount = 0;
        int strCount = 0;

        List<Instance> tval = table.getValues();

        for (Instance entry : tval) {
            while (entry != null) {
                hmeCount++;
                sum += entry.getSize(); // size of entry

                long sz = sizeIfNewString(fldHMEKey.getRefValue(entry));

                if (sz != 0) {
                    strCount++;
                }

                sum += sz;
                sz = sizeIfNewString(fldHMEValue.getRefValue(entry));

                if (sz != 0) {
                    strCount++;
                }

                sum += sz;
                entry = fldHMENext.getRefValue(entry);
            }
        }

        HashmapEntry hme = new HashmapEntry(sum, hmeCount, strCount);

        return hme;
    }
}
