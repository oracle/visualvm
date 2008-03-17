/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */
package com.sun.tools.visualvm.threaddump.impl;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.coredump.CoreDump;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.application.JVM;
import com.sun.tools.visualvm.application.JVMFactory;
import com.sun.tools.visualvm.threaddump.ThreadDumpSupport;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.RequestProcessor;

public final class ThreadDumpAction extends AbstractAction {
    
    private static ThreadDumpAction instance;
    
    
    public static synchronized ThreadDumpAction getInstance() {
        if (instance == null) instance = new ThreadDumpAction();
        return instance;
    }
    
    public void actionPerformed(final ActionEvent e) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                if (isAvailable()) {
                    Set<DataSource> threadDumpAbleDataSources = getThreadDumpAbleDataSources();
                    for (DataSource dataSource : threadDumpAbleDataSources) {
                        if (dataSource instanceof Application) {
                            Application application = (Application)dataSource;
                            ThreadDumpSupport.getInstance().takeThreadDump(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
                        } else if (dataSource instanceof CoreDump) {
                            CoreDump coreDump = (CoreDump)dataSource;
                            ThreadDumpSupport.getInstance().takeThreadDump(coreDump, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
                        }
                    }
                } else {
                    NetBeansProfiler.getDefaultNB().displayError("Cannot take thread dump for selected item(s).");
                }
            }
        });
    }
    
    private void updateEnabled() {
        Set<DataSource> threadDumpAbleDataSources = getThreadDumpAbleDataSources();
        final boolean isEnabled = !threadDumpAbleDataSources.isEmpty();
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() {
                setEnabled(isEnabled);
            }
        });
    }
    
    // Not to be called from AWT EDT (the result reflects that the action can/cannot be invoked)
    boolean isAvailable() {
        if (!isEnabled()) return false;
        Set<DataSource> threadDumpAbleDataSources = getThreadDumpAbleDataSources();
        for (DataSource dataSource : threadDumpAbleDataSources)
            if (dataSource instanceof Application) {
                Application application = (Application) dataSource;
                JVM jvm = JVMFactory.getJVMFor(application);
                if (jvm == null || !jvm.isTakeThreadDumpSupported()) return false;
            }
        return true;
    }
    
    private Set<DataSource> getThreadDumpAbleDataSources() {
        Set<DataSource> selectedDataSources = ExplorerSupport.sharedInstance().getSelectedDataSources();
        Set<DataSource> threadDumpAbleDataSources = new HashSet();
        for (DataSource dataSource : selectedDataSources)
            if ((dataSource instanceof Application && ((Application)dataSource).getState() == Stateful.STATE_AVAILABLE) || dataSource instanceof CoreDump)
                threadDumpAbleDataSources.add(dataSource);
            else return Collections.EMPTY_SET;
        return threadDumpAbleDataSources;
    }
    
    
    private ThreadDumpAction() {
        putValue(Action.NAME, "Thread Dump");
        putValue(Action.SHORT_DESCRIPTION, "Thread Dump");
        
        updateEnabled();
        // TODO: should also listen for selected Applications availability (see ApplicationSnapshotAction implementation)
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(Set<DataSource> selected) {
                updateEnabled();
            }
        });
    }
}
