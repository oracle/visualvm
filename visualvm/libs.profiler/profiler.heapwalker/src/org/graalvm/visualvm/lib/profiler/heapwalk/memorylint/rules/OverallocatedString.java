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

import java.util.HashMap;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.*;
import org.openide.util.NbBundle;


//@org.openide.util.lookup.ServiceProvider(service=org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.Rule.class)
public class OverallocatedString extends IteratingRule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldCount;
    private FieldAccess fldOffset;
    private FieldAccess fldValue;
    private JavaClass clsString;
    private Map<Instance, Integer> covered = new HashMap<>();
    private int total;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public OverallocatedString() {
        super(NbBundle.getMessage(OverallocatedString.class, "LBL_OverStr_Name"),
                NbBundle.getMessage(OverallocatedString.class, "LBL_OverStr_Desc"),
                "java.lang.String"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(OverallocatedString.class, "LBL_OverStr_LongDesc");
    }

    protected void perform(Instance in) {
        int off = fldOffset.getIntValue(in);
        int cnt = fldCount.getIntValue(in);
        PrimitiveArrayInstance arrValue = (PrimitiveArrayInstance) fldValue.getRefValue(in);

        if (arrValue == null) {
            return; // empty
        }

        if ((off > 0) || (arrValue.getLength() > cnt)) {
            if (covered.containsKey(arrValue)) {
                // simplification - don't track shared char arrays
                total -= covered.remove(arrValue);
            } else {
                int waste = (2 * off) + (2 * (arrValue.getLength() - (cnt + off)));
                covered.put(arrValue, waste);
                total += waste;
            }
        }
    }

    protected void prepareRule(MemoryLint context) {
        Heap heap = context.getHeap();
        clsString = heap.getJavaClassByName("java.lang.String"); // NOI18N
        fldOffset = new FieldAccess(clsString, "offset"); // NOI18N
        fldCount = new FieldAccess(clsString, "count"); // NOI18N
        fldValue = new FieldAccess(clsString, "value"); // NOI18N
    }

    @Override
    protected void summary() {
        getContext().appendResults(NbBundle.getMessage(OverallocatedString.class, "FMT_OverStr_Result", total));

        Histogram<Histogram.Entry> h = new Histogram<>();

        for (Map.Entry<Instance, Integer> e : covered.entrySet()) {
            String incomming = getContext().getRootIncommingString(e.getKey());
            incomming = Utils.printClass(getContext(), incomming);
            h.add(incomming, new Histogram.Entry(e.getValue()));
        }

        getContext().appendResults(h.toString(5000));
    }
}
