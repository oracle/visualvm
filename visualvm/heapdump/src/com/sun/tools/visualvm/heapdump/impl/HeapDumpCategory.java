/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.heapdump.impl;

import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import com.sun.tools.visualvm.heapdump.HeapDump;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.netbeans.api.actions.Openable;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class HeapDumpCategory extends SnapshotCategory<HeapDump> {
    
    private static final Logger LOGGER =
            Logger.getLogger(HeapDumpCategory.class.getName());
    
    private static final String HPROF_HEADER = "JAVA PROFILE 1.0";
    private static final long MIN_HPROF_SIZE = 1024*1024L;
    private static final String NAME = NbBundle.getMessage(HeapDumpCategory.class, "LBL_Heap_Dumps");   // NOI18N
    private static final String PREFIX = "heapdump";    // NOI18N
    private static final String SUFFIX = ".hprof";  // NOI18N
    
    public HeapDumpCategory() {
        super(NAME, HeapDump.class, PREFIX, SUFFIX, 20);
    }
    
    public boolean supportsOpenSnapshot() {
        return true;
    }
    
    public void openSnapshot(File file) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle(
                            NbBundle.getMessage(HeapDumpCategory.class,
                                                "MSG_Opening_Heap_Dump")); // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    try {
                        FileObject fileObject = FileUtil.toFileObject(file);
                        DataObject dobj = DataObject.find(fileObject);
                        Openable openCookie = dobj.getLookup().lookup(Openable.class);
                        openCookie.open();
                    } catch (Exception e) {
                        LOGGER.log(Level.INFO, "Error loading profiler snapshot", e); // NOI18N
                        // TODO: enable once HeapDump module is a friend of Profiler API
//                        SwingUtilities.invokeLater(new Runnable() {
//                            public void run() {
//                                ProfilerDialogs.displayError(
//                                        NbBundle.getMessage(HeapDumpCategory.class,
//                                                            "MSG_Opening_Heap_Dump_failed")); // NOI18N
//                            }   
//                        });
                    }
                } finally {
                    final ProgressHandle pHandleF = pHandle;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { if (pHandleF != null) pHandleF.finish(); }
                    });
                }
            }
        });
    }

    protected boolean isSnapshot(File file) {
        if (super.isSnapshot(file)) {
            return true;
        }
        return checkHprofFile(file);
    }

    private boolean checkHprofFile(File file) {
        try {
            if (file.isFile() && file.canRead() && file.length()>MIN_HPROF_SIZE) { // heap dump must be 1M and bigger
                byte[] prefix = new byte[HPROF_HEADER.length()+4];
                RandomAccessFile raf = new RandomAccessFile(file,"r");  // NOI18H
                raf.readFully(prefix);
                raf.close();
                if (new String(prefix).startsWith(HPROF_HEADER)) {
                    return true;
                }
            }
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            return false;
        }
        return false;
    }

    public FileFilter getFileFilter() {
        return new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || isSnapshot(f);
            }
            public String getDescription() {
                String suff = getSuffix();
                return getName() + (suff != null ? " (*" + suff +", *.*"+ ")" : "");    // NOI18N
            }
        };
    }    
    
}
