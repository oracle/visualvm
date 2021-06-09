/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.ui;

import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import java.awt.Dimension;
import org.openide.util.NbBundle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.SwingWorker;
import org.graalvm.visualvm.lib.jfluid.heap.HeapSummary;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.heapviewer.HeapViewer;
import javax.swing.BorderFactory;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Exceptions;

@NbBundle.Messages({
    "HeapDumpInfoAction_ActionName=Heap dump information",
    "HeapDumpInfoAction_ActionDescr=Display heap dump information",
    "HeapDumpInfoAction_WindowCaption=Heap Dump Information",
    "HeapDumpInfoAction_SummaryString=Summary:",
    "HeapDumpInfoAction_NotAvailableMsg=&lt;not available&gt;",
    "HeapDumpInfoAction_FileItemString=<b>File: </b>{0}",
    "HeapDumpInfoAction_FileSizeItemString=<b>File Size: </b>{0}",
    "HeapDumpInfoAction_DateTakenItemString=<b>Date Taken: </b>{0}",
    "HeapDumpInfoAction_ComputingInfo=computing heap dump information..."
})
class HeapDumpInfoAction extends AbstractAction {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final HeapViewer heapViewer;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HeapDumpInfoAction(HeapViewer heapViewer) {
        putValue(Action.NAME, Bundle.HeapDumpInfoAction_ActionName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.HeapDumpInfoAction_ActionDescr());
        putValue(Action.SMALL_ICON, Icons.getIcon(GeneralIcons.INFO));
        putValue("iconBase", Icons.getResource(GeneralIcons.INFO)); // NOI18N
        this.heapViewer = heapViewer;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        DialogDescriptor dd = new DialogDescriptor(infoComponent(heapViewer),
                              Bundle.HeapDumpInfoAction_WindowCaption(), true,
                              new Object[] { DialogDescriptor.OK_OPTION }, 
                              DialogDescriptor.OK_OPTION, DialogDescriptor.DEFAULT_ALIGN,
                              null, null);
        DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
    }
    
    
    private static JComponent infoComponent(final HeapViewer heapViewer) {
        HTMLTextArea text = new HTMLTextArea();
        text.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        createInfo(text, heapViewer);
        
        ScrollableContainer textScroll = new ScrollableContainer(text);
        textScroll.setPreferredSize(new Dimension(500, 175));
        return textScroll;
    }
    
    private static void createInfo(final HTMLTextArea text, final HeapViewer heapViewer) {
        SwingWorker<String, String> worker = new SwingWorker<String, String>() {
            protected String doInBackground() throws Exception {
                return computeInfo(heapViewer);
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
            text.setText(SUMMARY_SECTION_PREFIX + "<div style='margin-left: 10px;'>" + Bundle.HeapDumpInfoAction_ComputingInfo() + "</div>"); // NOI18N
            text.setCaretPosition(0);
        }
    }
    
    private static final String SUMMARY_SECTION_PREFIX = "<b><img border='0' align='bottom' src='nbresloc:/" + // NO18N
                                                         Icons.getResource(GeneralIcons.INFO) +
                                                         "'>&nbsp;&nbsp;" + Bundle.HeapDumpInfoAction_SummaryString() + // NO18N
                                                         "</b><br><hr>"; // NO18N
    
    private static String computeInfo(HeapViewer heapViewer) {
        File file = heapViewer.getFile();
        HeapSummary hsummary = heapViewer.getFragments().get(0).getHeap().getSummary();
        NumberFormat numberFormat = (NumberFormat)NumberFormat.getInstance().clone();
        numberFormat.setMaximumFractionDigits(1);
        
        String filename = Bundle.HeapDumpInfoAction_FileItemString(
                    file != null && file.exists() ? file.getAbsolutePath() : Bundle.HeapDumpInfoAction_NotAvailableMsg());
        
        String filesize = Bundle.HeapDumpInfoAction_FileSizeItemString(
                    file != null && file.exists() ? numberFormat.format(file.length()/(1024 * 1024.0)) + " MB" : // NOI18N
                        Bundle.HeapDumpInfoAction_NotAvailableMsg());
        
        String dateTaken = Bundle.HeapDumpInfoAction_DateTakenItemString(new Date(hsummary.getTime()).toString());
        
        return SUMMARY_SECTION_PREFIX + "<div style='margin-left: 10px;'>" + dateTaken + "<br>" + filename + "<br>" + filesize + "</div>"; // NOI18N
    }
    
}
