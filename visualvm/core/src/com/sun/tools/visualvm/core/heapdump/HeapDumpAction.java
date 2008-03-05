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
package com.sun.tools.visualvm.core.heapdump;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.CoreDump;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import com.sun.tools.visualvm.core.model.jvm.JVM;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.utils.IDEUtils;

public final class HeapDumpAction extends AbstractAction {
    
    private static HeapDumpAction instance;
    
    
    public static synchronized HeapDumpAction getInstance() {
        if (instance == null) instance = new HeapDumpAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        DataSource dataSource = getSelectedDataSource();
        
        if (dataSource.getState() != DataSource.STATE_AVAILABLE) {
            System.err.println("Tried to take heap dump on unavailable " + dataSource);
        }
        
        if (dataSource instanceof Application) {
            Application application = (Application)dataSource;
            HeapDumpSupport.getInstance().getHeapDumpProvider().createHeapDump(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        } else if (dataSource instanceof CoreDump) {
            CoreDump coreDump = (CoreDump)dataSource;
            HeapDumpSupport.getInstance().getHeapDumpProvider().createHeapDump(coreDump, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        } else {
            throw new UnsupportedOperationException("Cannot take heap dump from " + dataSource);
        }
    }
    
    void updateEnabled() {
        final boolean isEnabled;
        DataSource selectedDataSource = getSelectedDataSource();
        if (selectedDataSource == null) {
            isEnabled = false;
        } else if (selectedDataSource.getState() != DataSource.STATE_AVAILABLE) {
            isEnabled = false;
        } else if (selectedDataSource instanceof Application) {
            Application application = (Application)selectedDataSource;
            JVM jvm = JVMFactory.getJVMFor(application);
            isEnabled = jvm != null && jvm.isTakeHeapDumpSupported();
        } else if (selectedDataSource instanceof CoreDump) {
            isEnabled = true;
        } else {
            isEnabled = false;
        }
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() { setEnabled(isEnabled); }
        });
    }
    
    private DataSource getSelectedDataSource() {
        return ExplorerSupport.sharedInstance().getSelectedDataSource();
    }
    
    
    private HeapDumpAction() {
        putValue(Action.NAME, "Heap Dump");
        putValue(Action.SHORT_DESCRIPTION, "Heap Dump");
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(DataSource selected) {
                updateEnabled();
            }
        });
    }
}
