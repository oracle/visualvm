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

package com.sun.tools.visualvm.heapviewer.java.impl;

import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.text.NumberFormat;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapSummary;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaHeapSummaryView_Name=Summary",
    "JavaHeapSummaryView_Description=Summary",
    "JavaHeapSummaryView_NotAvailableMsg=&lt;not available&gt;",
    "JavaHeapSummaryView_SystemPropertiesString=System Properties:",
    "JavaHeapSummaryView_SummaryString=Basic Info:",
    "JavaHeapSummaryView_EnvironmentString=Environment:",
    "JavaHeapSummaryView_FileItemString=<b>File: </b>{0}",
    "JavaHeapSummaryView_FileSizeItemString=<b>File Size: </b>{0}",
    "JavaHeapSummaryView_DateTakenItemString=<b>Date Taken: </b>{0}",
    "JavaHeapSummaryView_TotalBytesItemString=<b>Total Bytes: </b>{0}",
    "JavaHeapSummaryView_TotalClassesItemString=<b>Total Classes: </b>{0}",
    "JavaHeapSummaryView_TotalInstancesItemString=<b>Total Instances: </b>{0}",
    "JavaHeapSummaryView_ClassloadersItemString=<b>Classloaders: </b>{0}",
    "JavaHeapSummaryView_GcRootsItemString=<b>GC Roots: </b>{0}",
    "JavaHeapSummaryView_FinalizersItemString=<b>Number of Objects Pending for Finalization: </b>{0}",
    "JavaHeapSummaryView_OOMELabelString=<b>Heap dumped on OutOfMemoryError exception</b>",
    "JavaHeapSummaryView_OOMEItemString=<b>Thread Causing OutOfMemoryError Exception: </b>{0}",
    "JavaHeapSummaryView_OsItemString=<b>OS: </b>{0} ({1}) {2}",
    "JavaHeapSummaryView_ArchitectureItemString=<b>Architecture: </b>{0} {1}",
    "JavaHeapSummaryView_JavaHomeItemString=<b>Java Home: </b>{0}",
    "JavaHeapSummaryView_JavaVersionItemString=<b>Java Version: </b>{0}",
    "JavaHeapSummaryView_JavaVendorItemString=<b>Java Vendor: </b>{0}",
    "JavaHeapSummaryView_JvmItemString=<b>JVM: </b>{0}  ({1}, {2})",
    "JavaHeapSummaryView_ShowSysPropsLinkString=Show System Properties",
    "JavaHeapSummaryView_ThreadsString=Threads at the Heap Dump:",
    "JavaHeapSummaryView_ShowThreadsLinkString=Show Threads"
})
public class JavaHeapSummaryView extends HeapViewerFeature {
    
    private final HeapContext context;
    
    private JComponent component;
    private ProfilerToolbar toolbar;
    
    private String summaryText;
    
    
    public JavaHeapSummaryView(HeapContext context) {
        super("java_heap_summary", Bundle.JavaHeapSummaryView_Name(), Bundle.JavaHeapSummaryView_Description(),
               Icons.getIcon(HeapWalkerIcons.PROPERTIES), 100); // NOI18N
        this.context = context;
        
        this.summaryText = createSummary();
    }
    
    
    public boolean isDefault() {
        return true;
    }

    
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    private void init() {
        toolbar = ProfilerToolbar.create(false);
        
        
        HTMLTextArea text = new HTMLTextArea(summaryText);
        text.setCaretPosition(0);
        text.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        component = new ScrollableContainer(text);
        
        summaryText = null;
    }
    
    
    private static final String LINE_PREFIX = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"; // NOI18N
    
    private String createSummary() {
//        File file = context.getFile();
        Heap heap = context.getFragment().getHeap();
        HeapSummary hsummary = heap.getSummary();
        long finalizers = computeFinalizers(heap);
        int nclassloaders = 0;
        JavaClass cl = heap.getJavaClassByName("java.lang.ClassLoader"); // NOI18N
        NumberFormat numberFormat = (NumberFormat)NumberFormat.getInstance().clone();
        numberFormat.setMaximumFractionDigits(1);
        
//        oome = getOOMEThread(heap);
        if (cl != null) {
            nclassloaders = cl.getInstancesCount();
            
            Collection<JavaClass> jcs = cl.getSubClasses();
            
            for (JavaClass jc : jcs) {
                nclassloaders += jc.getInstancesCount();
            }
        }
        
//        String filename = LINE_PREFIX
//                + Bundle.JavaHeapSummaryView_FileItemString(
//                    file != null && file.exists() ? file.getAbsolutePath() : 
//                        Bundle.JavaHeapSummaryView_NotAvailableMsg());
//        
//        String filesize = LINE_PREFIX
//                + Bundle.JavaHeapSummaryView_FileSizeItemString(
//                    file != null && file.exists() ?
//                        numberFormat.format(file.length()/(1024 * 1024.0)) + " MB" : // NOI18N
//                        Bundle.JavaHeapSummaryView_NotAvailableMsg());
//        
//        String dateTaken = LINE_PREFIX
//                + Bundle.JavaHeapSummaryView_DateTakenItemString(new Date(hsummary.getTime()).toString());
        
        String liveBytes = LINE_PREFIX
                + Bundle.JavaHeapSummaryView_TotalBytesItemString(numberFormat.format(hsummary.getTotalLiveBytes()));
        
        String liveClasses = LINE_PREFIX
                + Bundle.JavaHeapSummaryView_TotalClassesItemString(numberFormat.format(heap.getAllClasses().size()));
        
        String liveInstances = LINE_PREFIX
                + Bundle.JavaHeapSummaryView_TotalInstancesItemString(numberFormat.format(hsummary.getTotalLiveInstances()));
        
        String classloaders = LINE_PREFIX
                + Bundle.JavaHeapSummaryView_ClassloadersItemString(numberFormat.format(nclassloaders));
        
        String gcroots = LINE_PREFIX
                + Bundle.JavaHeapSummaryView_GcRootsItemString(numberFormat.format(heap.getGCRoots().size()));
        
        String finalizersInfo = LINE_PREFIX
                + Bundle.JavaHeapSummaryView_FinalizersItemString(
                          finalizers >= 0 ? numberFormat.format(finalizers) :
                          Bundle.JavaHeapSummaryView_NotAvailableMsg()
                );

        String oomeString = ""; // NOI18N
//        if (oome != null) {
//            Instance thread = oome.getInstance();
//            String threadName = htmlize(getThreadName(heap, thread));
//            String threadUrl = "<a href='"+ THREAD_URL_PREFIX + thread.getJavaClass().getName() + "/" + thread.getInstanceId() + "'>" + threadName + "</a>"; // NOI18N
//            oomeString = "<br><br>" + LINE_PREFIX // NOI18N
//                + org.netbeans.modules.profiler.heapwalk.Bundle.OverviewController_OOMELabelString() + "<br>" + LINE_PREFIX // NOI18N
//                + org.netbeans.modules.profiler.heapwalk.Bundle.OverviewController_OOMEItemString(threadUrl);
//        }
        String memoryRes = Icons.getResource(ProfilerIcons.HEAP_DUMP);
        return "<b><img border='0' align='bottom' src='nbresloc:/" + memoryRes + "'>&nbsp;&nbsp;" + // NOI18N
                Bundle.JavaHeapSummaryView_SummaryString() + "</b><br><hr>" +/* dateTaken + "<br>" + filename + "<br>" + filesize + "<br><br>" +*/  // NOI18N
                 liveBytes + "<br>" + liveClasses + "<br>" + liveInstances + "<br>" + classloaders + "<br>" + gcroots + "<br>" + finalizersInfo + oomeString; // NOI18N
    }
    
    private long computeFinalizers(Heap heap) {
        JavaClass finalizerClass = heap.getJavaClassByName("java.lang.ref.Finalizer"); // NOI18N
        if (finalizerClass != null) {
            Instance queue = (Instance) finalizerClass.getValueOfStaticField("queue"); // NOI18N
            if (queue != null) {
                Long len = (Long) queue.getValueOfField("queueLength"); // NOI18N
                if (len != null) {
                    return len.longValue();
                }
            }
        }
        return -1;
    }
    
}
