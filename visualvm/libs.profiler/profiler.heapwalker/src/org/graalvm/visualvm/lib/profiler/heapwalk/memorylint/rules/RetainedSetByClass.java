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

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.*;
import org.openide.util.NbBundle;


public class RetainedSetByClass extends Rule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private MemoryLint context;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RetainedSetByClass() {
        super(NbBundle.getMessage(RetainedSetByClass.class, "LBL_RSBC_Name"),
                NbBundle.getMessage(RetainedSetByClass.class, "LBL_RSBC_Desc"));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(RetainedSetByClass.class, "LBL_RSBC_LongDesc");
    }

    public void perform() {
        Heap heap = context.getHeap();
        @SuppressWarnings("unchecked")
        List<JavaClass> classes = heap.getAllClasses();

        // TODO access to progress
        //        BoundedRangeModel progress = context.getProgress();
        //        progress.setMaximum(classes.size());
        Histogram<Histogram.Entry> hist = new Histogram<>();

        for (JavaClass cls : classes) {
            Logger.getLogger(RetainedSetByClass.class.getName()).log(Level.FINE, "Executing rule on class {0}.", cls); // NOI18N
            performClass(cls, hist);

            if (context.isInterruped()) {
                return;
            }

            // TODO access to progress
            //            progress.setValue(progress.getValue()+1);
        }

        summary(hist);
    }

    @Override
    public void prepare(MemoryLint context) {
        this.context = context;
    }

    @Override
    protected JComponent createCustomizer() {
        return null;
    }

    @SuppressWarnings("unchecked")
    private void performClass(JavaClass clz, Histogram<Histogram.Entry> hist) {
        Set<Instance> retained = Utils.getRetainedSet(clz.getInstances(), context.getHeap());
        String name = clz.getName();
        name = Utils.printClass(context, name);

        for (Instance i : retained) {
            hist.add(name, new Histogram.Entry<>(i.getSize()));
        }
    }

    private void summary(Histogram h) {
//        context.appendResults("<hr>Histogram of retained size:<br>");
        context.appendResults(h.toString(0));
    }
}
