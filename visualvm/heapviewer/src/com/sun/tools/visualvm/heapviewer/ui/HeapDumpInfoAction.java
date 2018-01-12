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

package com.sun.tools.visualvm.heapviewer.ui;

import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.awt.Dimension;
import org.openide.util.NbBundle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.SwingWorker;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapSummary;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import com.sun.tools.visualvm.heapviewer.HeapViewer;
import javax.swing.BorderFactory;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;

@NbBundle.Messages({
    "HeapDumpInfoAction_ActionName=Heap dump information",
    "HeapDumpInfoAction_ActionDescr=Display heap dump information",
    "HeapDumpInfoAction_WindowCaption=Heap Dump Information",
    
    "HeapDumpInfoAction_SummaryString=Summary:",
    "HeapDumpInfoAction_NotAvailableMsg=&lt;not available&gt;",
    "HeapDumpInfoAction_FileItemString=<b>File: </b>{0}",
    "HeapDumpInfoAction_FileSizeItemString=<b>File Size: </b>{0}",
    "HeapDumpInfoAction_DateTakenItemString=<b>Date Taken: </b>{0}",
    "HeapDumpInfoAction_UpTimeItemString=<b>JVM Uptime: </b>{0}",
    "HeapDumpInfoAction_SystemPropertiesString=System Properties:",
    "HeapDumpInfoAction_EnvironmentString=Environment:",
    "HeapDumpInfoAction_TotalBytesItemString=<b>Total bytes: </b>{0}",
    "HeapDumpInfoAction_TotalClassesItemString=<b>Total classes: </b>{0}",
    "HeapDumpInfoAction_TotalInstancesItemString=<b>Total instances: </b>{0}",
    "HeapDumpInfoAction_ClassloadersItemString=<b>Classloaders: </b>{0}",
    "HeapDumpInfoAction_GcRootsItemString=<b>GC roots: </b>{0}",
    "HeapDumpInfoAction_FinalizersItemString=<b>Number of objects pending for finalization: </b>{0}",
    "HeapDumpInfoAction_OOMELabelString=<b>Heap dumped on OutOfMemoryError exception</b>",
    "HeapDumpInfoAction_OOMEItemString=<b>Thread causing OutOfMemoryError exception: </b>{0}",
    "HeapDumpInfoAction_OsItemString=<b>OS: </b>{0} ({1}) {2}",
    "HeapDumpInfoAction_ArchitectureItemString=<b>Architecture: </b>{0} {1}",
    "HeapDumpInfoAction_JavaHomeItemString=<b>Java Home: </b>{0}",
    "HeapDumpInfoAction_JavaVersionItemString=<b>Java Version: </b>{0}",
    "HeapDumpInfoAction_JavaVendorItemString=<b>Java Vendor: </b>{0}",
    "HeapDumpInfoAction_JvmItemString=<b>JVM: </b>{0}  ({1}, {2})",
    "HeapDumpInfoAction_ShowSysPropsLinkString=Show System Properties",
    "HeapDumpInfoAction_ThreadsString=Threads at the heap dump:",
    "HeapDumpInfoAction_ShowThreadsLinkString=Show Threads",
    "HeapDumpInfoAction_ComputingInfo=computing heap dump information..."
})
class HeapDumpInfoAction extends AbstractAction {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final HeapViewer heapViewer;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public HeapDumpInfoAction(HeapViewer heapViewer) {
        putValue(Action.NAME, Bundle.HeapDumpInfoAction_ActionName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.HeapDumpInfoAction_ActionDescr());
        putValue(Action.SMALL_ICON, Icons.getIcon(GeneralIcons.INFO));
        putValue("iconBase", Icons.getResource(GeneralIcons.INFO)); // NOI18N
        this.heapViewer = heapViewer;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        HelpCtx helpCtx = new HelpCtx("SnapshotInfo.HelpCtx"); // NOI18N
        DialogDescriptor dd = new DialogDescriptor(infoComponent(heapViewer),
                              Bundle.HeapDumpInfoAction_WindowCaption(), true,
                              new Object[] { DialogDescriptor.OK_OPTION }, 
                              DialogDescriptor.OK_OPTION, DialogDescriptor.DEFAULT_ALIGN,
                              helpCtx, null);
        DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
    }
    
    
    private static JComponent infoComponent(final HeapViewer heapViewer) {
        HTMLTextArea text = new HTMLTextArea();
        text.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        createInfo(text, heapViewer);
        
        ScrollableContainer textScroll = new ScrollableContainer(text);
        textScroll.setPreferredSize(new Dimension(750, 400));
        return textScroll;
    }
    
    private static void createInfo(final HTMLTextArea text, final HeapViewer heapViewer) {
        SwingWorker<String, String> worker = new SwingWorker<String, String>() {
            protected String doInBackground() throws Exception {
                Heap heap = heapViewer.getFragments().get(0).getHeap();
                Properties sysprops = heap.getSystemProperties();
                
                return computeInfo(heapViewer) + "<br><br>" + // NO18N
                       computeEnvironment(heap, sysprops) + "<br><br>" + // NO18N
                       computeSystemProperties(sysprops);
            }
            protected void done() {
                try {
                    text.setText(get());
                    text.setCaretPosition(0);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (ExecutionException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        };
        worker.execute();
        try {
            worker.get(UIThresholds.VIEW_LOAD, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (TimeoutException ex) {
            text.setText(SUMMARY_SECTION_PREFIX + LINE_PREFIX + Bundle.HeapDumpInfoAction_ComputingInfo());
            text.setCaretPosition(0);
        }
    }
    
    private static final String LINE_PREFIX = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"; // NOI18N
    private static final String SUMMARY_SECTION_PREFIX = "<b><img border='0' align='bottom' src='nbresloc:/" + // NO18N
                                                         Icons.getResource(GeneralIcons.INFO) +
                                                         "'>&nbsp;&nbsp;" + Bundle.HeapDumpInfoAction_SummaryString() + // NO18N
                                                         "</b><br><hr>"; // NO18N
    
    private static String computeInfo(HeapViewer heapViewer) {
        File file = heapViewer.getFile();
        HeapSummary hsummary = heapViewer.getFragments().get(0).getHeap().getSummary();
        NumberFormat numberFormat = (NumberFormat)NumberFormat.getInstance().clone();
        numberFormat.setMaximumFractionDigits(1);
        
        String filename = LINE_PREFIX + Bundle.HeapDumpInfoAction_FileItemString(
                    file != null && file.exists() ? file.getAbsolutePath() : Bundle.HeapDumpInfoAction_NotAvailableMsg());
        
        String filesize = LINE_PREFIX + Bundle.HeapDumpInfoAction_FileSizeItemString(
                    file != null && file.exists() ? numberFormat.format(file.length()/(1024 * 1024.0)) + " MB" : // NOI18N
                        Bundle.HeapDumpInfoAction_NotAvailableMsg());
        
        String dateTaken = LINE_PREFIX + Bundle.HeapDumpInfoAction_DateTakenItemString(new Date(hsummary.getTime()).toString());
        
        return SUMMARY_SECTION_PREFIX + dateTaken + "<br>" + filename + "<br>" + filesize; // NOI18N
    }
    
    private static String computeEnvironment(Heap heap, Properties sysprops) {
        String sysinfoRes = Icons.getResource(HeapWalkerIcons.SYSTEM_INFO);
        String header =  "<b><img border='0' align='bottom' src='nbresloc:/" + sysinfoRes + "'>&nbsp;&nbsp;" // NOI18N
                + Bundle.HeapDumpInfoAction_EnvironmentString() + "</b><br><hr>";   // NOI18N
//        Properties sysprops = getSystemProperties(heapViewer);
        
        if (sysprops == null) {
            return header + LINE_PREFIX + Bundle.HeapDumpInfoAction_NotAvailableMsg();
        }
        
        HeapSummary hsummary = heap.getSummary();
        long startupTime = computeStartupTime(heap);

        String patchLevel = sysprops.getProperty("sun.os.patch.level", ""); // NOI18N
        String os = LINE_PREFIX
                + Bundle.HeapDumpInfoAction_OsItemString(
                    sysprops.getProperty("os.name", Bundle.HeapDumpInfoAction_NotAvailableMsg()), // NOI18N
                    sysprops.getProperty("os.version", ""), // NOI18N
                    ("unknown".equals(patchLevel) ? "" : patchLevel) // NOI18N
        );
        
        String arch = LINE_PREFIX
                + Bundle.HeapDumpInfoAction_ArchitectureItemString(
                    sysprops.getProperty("os.arch", Bundle.HeapDumpInfoAction_NotAvailableMsg()), // NOI18N
                    sysprops.getProperty("sun.arch.data.model", "?") + "bit" // NOI18N
        );
        
        String jdk = LINE_PREFIX
                + Bundle.HeapDumpInfoAction_JavaHomeItemString(
                    sysprops.getProperty("java.home", Bundle.HeapDumpInfoAction_NotAvailableMsg())); // NOI18N

        String version = LINE_PREFIX
                + Bundle.HeapDumpInfoAction_JavaVersionItemString(
                    sysprops.getProperty("java.version", Bundle.HeapDumpInfoAction_NotAvailableMsg())); // NOI18N
        
        String jvm = LINE_PREFIX
                + Bundle.HeapDumpInfoAction_JvmItemString(
                    sysprops.getProperty("java.vm.name", Bundle.HeapDumpInfoAction_NotAvailableMsg()), // NOI18N
                    sysprops.getProperty("java.vm.version", ""), // NOI18N
                    sysprops.getProperty("java.vm.info", "") // NOI18N
        );

        String vendor = LINE_PREFIX
                + Bundle.HeapDumpInfoAction_JavaVendorItemString(
                    sysprops.getProperty("java.vendor", Bundle.HeapDumpInfoAction_NotAvailableMsg())); // NOI18N
        
        String uptimeInfo = LINE_PREFIX
                + Bundle.HeapDumpInfoAction_UpTimeItemString(startupTime >= 0 ? getTime(hsummary.getTime()-startupTime) :
                          Bundle.HeapDumpInfoAction_NotAvailableMsg()
                );


        return header + os + "<br>" + arch + "<br>" + jdk + "<br>" + version + "<br>" + jvm + "<br>" + vendor + // NOI18N
                "<br>" + uptimeInfo ; // NOI18N
    }

    private static long computeStartupTime(Heap heap) {
        JavaClass jmxFactoryClass = heap.getJavaClassByName("sun.management.ManagementFactoryHelper"); // NOI18N
        if (jmxFactoryClass == null) {
            jmxFactoryClass = heap.getJavaClassByName("sun.management.ManagementFactory"); // NOI18N
        }
        if (jmxFactoryClass != null) {
            Instance runtimeImpl = (Instance) jmxFactoryClass.getValueOfStaticField("runtimeMBean"); // NOI18N
            if (runtimeImpl != null) {
                Long len = (Long) runtimeImpl.getValueOfField("vmStartupTime"); // NOI18N
                if (len != null) {
                    return len.longValue();
                }
            }
        }
        return -1;
    }
    
    private static String computeSystemProperties(Properties sysprops) {
        String propertiesRes = Icons.getResource(HeapWalkerIcons.PROPERTIES);
        String header = "<b><img border='0' align='bottom' src='nbresloc:/" + propertiesRes + "'>&nbsp;&nbsp;" // NOI18N
                + Bundle.HeapDumpInfoAction_SystemPropertiesString() + "</b><br><hr>"; // NOI18N
        
        if (sysprops == null) {
            return header + LINE_PREFIX + Bundle.HeapDumpInfoAction_NotAvailableMsg();
        }
        
        return header + formatSystemProperties(sysprops);
    }
    
    private static String formatSystemProperties(Properties properties) {
        StringBuilder text = new StringBuilder(200);
        List keys = new ArrayList();
        Enumeration en = properties.propertyNames();
        Iterator keyIt;
        
        while (en.hasMoreElements()) {
            keys.add(en.nextElement());
        }
        Collections.sort(keys);
        keyIt = keys.iterator();
        
        while (keyIt.hasNext()) {
            String key = (String) keyIt.next();
            String val = properties.getProperty(key);
            
            if ("line.separator".equals(key) && val != null) {  // NOI18N
                val = val.replace("\n", "\\n"); // NOI18N
                val = val.replace("\r", "\\r"); // NOI18N
            }
            
            text.append("<nobr>"+ LINE_PREFIX +"<b>"); // NOI18N
            text.append(key);
            text.append("</b>="); // NOI18N
            text.append(val);
            text.append("</nobr><br>"); // NOI18N
        }
        
        return text.toString();
    }

    @NbBundle.Messages({
        "HeapDumpInfoAction_FORMAT_hms={0} hrs {1} min {2} sec",
        "HeapDumpInfoAction_FORMAT_ms={0} min {1} sec"
    })
    private static String getTime(long millis) {
        // Hours
        long hours = millis / 3600000;
        String sHours = (hours == 0 ? "" : "" + hours); // NOI18N
        millis = millis % 3600000;

        // Minutes
        long minutes = millis / 60000;
        String sMinutes = (((hours > 0) && (minutes < 10)) ? "0" + minutes : "" + minutes); // NOI18N
        millis = millis % 60000;

        // Seconds
        long seconds = millis / 1000;
        String sSeconds = ((seconds < 10) ? "0" + seconds : "" + seconds); // NOI18N

        if (sHours.length() == 0) {
            return Bundle.HeapDumpInfoAction_FORMAT_ms(sMinutes, sSeconds);
        } else {
            return Bundle.HeapDumpInfoAction_FORMAT_hms(sHours, sMinutes, sSeconds);
        }
    }
    
}
