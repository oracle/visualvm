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

package org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.rules;

import java.util.HashMap;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.*;
import org.openide.util.NbBundle;


//@org.openide.util.lookup.ServiceProvider(service=org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.Rule.class)
public class DuplicatedString extends IteratingRule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldValue;
    private HashMap<String, Integer> map = new HashMap<>();
    private Histogram<Histogram.Entry> dupSources = new Histogram<>();
    private StringHelper helper;
    private int total;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DuplicatedString() {
        super(NbBundle.getMessage(DuplicatedString.class, "LBL_DupStr_Name"),
                NbBundle.getMessage(DuplicatedString.class, "LBL_DupStr_Desc"),
                "java.lang.String"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(DuplicatedString.class, "LBL_DupStr_LongDesc");
    }

    protected void perform(Instance in) {
        String str = helper.decodeString(in);
        Integer val = map.get(str);

        if (val != null) { // already known, histogram the rest.

            long strSize = in.getSize();
            Instance arr = fldValue.getRefValue(in);

            if (arr != null) {
                strSize += ((str.length() * 2) + 14); // XXX aproximation
            }

            String incomming = getContext().getRootIncommingString(in);
            incomming = Utils.printClass(getContext(), incomming);
            dupSources.add(incomming, new Histogram.Entry(strSize));
            total += strSize;
        }

        val = (val == null) ? 1 : (val + 1);
        map.put(str, val);
    }

    protected @Override void prepareRule(MemoryLint context) {
        Heap heap = context.getHeap();
        helper = context.getStringHelper();

        JavaClass clsString = heap.getJavaClassByName("java.lang.String"); // NOI18N
        fldValue = new FieldAccess(clsString, "value"); // NOI18N
    }

    protected @Override void summary() {
        getContext().appendResults(NbBundle.getMessage(DuplicatedString.class, "FMT_DupStr_Result", total));
        getContext().appendResults(dupSources.toString(50000));
    }
}
