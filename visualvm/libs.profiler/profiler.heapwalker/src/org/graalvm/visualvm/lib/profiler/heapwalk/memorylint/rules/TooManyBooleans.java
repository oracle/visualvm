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

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.*;
import org.openide.util.NbBundle;


//@org.openide.util.lookup.ServiceProvider(service=org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.Rule.class)
public class TooManyBooleans extends IteratingRule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    Histogram<Histogram.Entry> booleans = new Histogram<>();
    private Heap heap;
    private Instance FALSE;
    private Instance TRUE;
    private StringHelper helper;
    private int count;
    private int total;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TooManyBooleans() {
        super(NbBundle.getMessage(TooManyBooleans.class, "LBL_TMB_Name"),
                NbBundle.getMessage(TooManyBooleans.class, "LBL_TMB_Desc"),
                "java.lang.Boolean"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(TooManyBooleans.class, "LBL_TMB_LongDesc");
    }

    protected void perform(Instance in) {
        if (in.equals(TRUE) || in.equals(FALSE)) {
            return;
        }

        count++;
        booleans.add(Utils.printClass(getContext(), getContext().getRootIncommingString(in)), new Histogram.Entry(in.getSize()));
    }

    protected @Override void prepareRule(MemoryLint context) {
        heap = context.getHeap();
        helper = context.getStringHelper();

        JavaClass booleanClass = heap.getJavaClassByName("java.lang.Boolean"); // NOI18N
        TRUE = (Instance) booleanClass.getValueOfStaticField("TRUE"); // NOI18N
        FALSE = (Instance) booleanClass.getValueOfStaticField("FALSE"); // NOI18N
    }

    protected @Override void summary() {
        if (count > 0) {
            getContext().appendResults(
                    NbBundle.getMessage(TooManyBooleans.class, "FMT_TMB_Result", count+2, (count * TRUE.getSize())));
            getContext().appendResults(booleans.toString(0));
        } else {
            getContext().appendResults(NbBundle.getMessage(TooManyBooleans.class, "FMT_TMB_ResultOK"));
        }
    }
}
