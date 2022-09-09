/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.java.impl;

import javax.swing.JComponent;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.SummaryView;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaSummaryView_Name=Summary",
    "JavaSummaryView_Description=Summary"
})
public class JavaSummaryView extends HeapViewerFeature {
    
    private static final String VIEW_ID = "java_summary"; // NOI18N
    
    private final SummaryView summaryView;
    
    
    public JavaSummaryView(HeapContext context, HeapViewerActions actions) {
        super(VIEW_ID, Bundle.JavaSummaryView_Name(), Bundle.JavaSummaryView_Description(),
               Icons.getIcon(HeapWalkerIcons.PROPERTIES), 100); // NOI18N
        
        summaryView = new SummaryView(VIEW_ID, context, actions);
    }
    
    
    public boolean isDefault() {
        return true;
    }
    
    public JComponent getComponent() {
        return summaryView.getComponent();
    }

    public ProfilerToolbar getToolbar() {
        return summaryView.getToolbar();
    }
    
}
