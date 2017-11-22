/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.heapviewer.truffle.python;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageSummaryView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import java.text.NumberFormat;
import java.util.Iterator;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
public class PythonHeapSummaryView extends TruffleLanguageSummaryView {

    public PythonHeapSummaryView(HeapContext context) {
        super(PythonSupport.createBadgedIcon(HeapWalkerIcons.PROPERTIES), context);
    }

    @Override
    protected String computeSummary(HeapContext context) {
        long rObjects = 0;
        long robjectsSize = 0;
        NumberFormat numberFormat = NumberFormat.getInstance();
        PythonHeapFragment fragment = (PythonHeapFragment)context.getFragment();
        Iterator<Instance> robjIter = fragment.getPythonObjectsIterator();

        while (robjIter.hasNext()) {
            Instance robj = robjIter.next();
            Instance data = (Instance) robj.getValueOfField("data"); // NOI18N
            rObjects++;
            robjectsSize += robj.getSize();
            if (data != null) {
                robjectsSize += data.getSize();
            }
        }

        String header = super.computeSummary(context);
        String bytes = LINE_PREFIX + "<b>Total bytes:&nbsp;</b>" + numberFormat.format(robjectsSize) + "<br>"; // NOI18N
        String jobjects = LINE_PREFIX + "<b>Total objects:&nbsp;</b>" + numberFormat.format(rObjects) + "<br><br>"; // NOI18N
        return header + bytes + jobjects;
   }

    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class Provider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (PythonHeapFragment.isPythonHeap(context))
                return new PythonHeapSummaryView(context);

            return null;
        }

    }

}
