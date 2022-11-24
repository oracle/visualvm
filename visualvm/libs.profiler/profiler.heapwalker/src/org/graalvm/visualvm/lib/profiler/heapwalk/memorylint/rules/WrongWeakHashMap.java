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
public class WrongWeakHashMap extends IteratingRule {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class WHMRecord {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Instance hm;
        private Instance key;
        private Instance value;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        WHMRecord(Instance hm, Instance key, Instance value) {
            this.hm = hm;
            this.key = key;
            this.value = value;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public String toString() {
            return NbBundle.getMessage(WrongWeakHashMap.class, "FMT_WWHM_Entry",
                    new Object[] {
                        Utils.printClass(getContext(), getContext().getRootIncommingString(hm)),
                        Utils.printInstance(hm),
                        Utils.printInstance(key),
                        Utils.printInstance(value)
                    }
            );
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldHMEKey;
    private FieldAccess fldHMENext;
    private FieldAccess fldHMEValue;
    private FieldAccess fldHMTable;
    private JavaClass clsHM;
    private JavaClass clsHME;
    private Set<WHMRecord> poorWHM = new HashSet<>();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public WrongWeakHashMap() {
        super(NbBundle.getMessage(WrongWeakHashMap.class, "LBL_WWHM_Name"),
                NbBundle.getMessage(WrongWeakHashMap.class, "LBL_WWHM_Desc"),
                "java.util.WeakHashMap");
        
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(WrongWeakHashMap.class, "LBL_WWHM_LongDesc");
    }

    protected void perform(Instance hm) {
        scanWeakHashmap(hm);
    }

    @Override
    protected void prepareRule(MemoryLint context) {
        // TODO WeakHashMap might not be present in the dump
        Heap heap = context.getHeap();
        clsHM = heap.getJavaClassByName("java.util.WeakHashMap"); // NOI18N
        clsHME = heap.getJavaClassByName("java.util.WeakHashMap$Entry"); // NOI18N
        fldHMTable = new FieldAccess(clsHM, "table"); // NOI18N

        JavaClass ref = heap.getJavaClassByName("java.lang.ref.Reference"); // NOI18N
        fldHMEKey = new FieldAccess(ref, "referent"); // NOI18N
        fldHMEValue = new FieldAccess(clsHME, "value"); // NOI18N
        fldHMENext = new FieldAccess(clsHME, "next"); // NOI18N
    }

    @Override
    protected void summary() {
        for (WHMRecord whm : poorWHM) {
            getContext().appendResults(whm.toString());
        }
    }

    private void scanWeakHashmap(Instance hm) {
        ObjectArrayInstance table = (ObjectArrayInstance) fldHMTable.getRefValue(hm);

        if (table == null) { // ? 

            return;
        }

        @SuppressWarnings("unchecked")
        List<Instance> tval = table.getValues();

        for (Instance entry : tval) {
            while (entry != null) {
                Instance key = fldHMEKey.getRefValue(entry);

                if (key != null) { // XXX can also scan for weak HM pending cleanup

                    Instance value = fldHMEValue.getRefValue(entry);

                    if (Utils.isReachableFrom(value, key)) {
                        poorWHM.add(new WHMRecord(hm, key, value));

                        return;
                    }
                }

                entry = fldHMENext.getRefValue(entry);
            }
        }

        return;
    }
}
