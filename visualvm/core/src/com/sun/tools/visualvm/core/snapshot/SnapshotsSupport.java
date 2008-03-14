/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.snapshot;

import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import java.io.File;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;

/**
 * Support for snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class SnapshotsSupport {
    
    private static SnapshotsSupport instance;


    /**
     * Returns singleton instance of SnapshotsSupport.
     * 
     * @return singleton instance of SnapshotsSupport.
     */
    public static synchronized SnapshotsSupport getInstance() {
        if (instance == null) instance = new SnapshotsSupport();
        return instance;
    }
    
    
    public void saveAs(final Snapshot snapshot, String dialogTitle) {
        final File file = snapshot.getFile();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setSelectedFile(new File(snapshot.getFile().getName()));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(snapshot.getCategory().getFileFilter());
//        chooser.setFileView(category.getFileView());
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File copy = chooser.getSelectedFile();
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    ProgressHandle pHandle = null;
                    try {
                        pHandle = ProgressHandleFactory.createHandle("Saving " + DataSourceDescriptorFactory.getDescriptor(snapshot).getName() + "...");
                        pHandle.setInitialDelay(0);
                        pHandle.start();
                        Utils.copyFile(file, copy);
                    } finally {
                        final ProgressHandle pHandleF = pHandle;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() { if (pHandleF != null) pHandleF.finish(); }
                        });
                    }
                }
            });
        }
    }
    
    public String getTimeStamp(long time) {
        return org.netbeans.lib.profiler.utils.StringUtils.formatUserDate(new Date(time));
    }
    
    
    private SnapshotsSupport() {
        DataSourceDescriptorFactory.getDefault().registerFactory(new SnapshotsContainerDescriptorProvider());
        new SnapshotsContainerProvider().initialize();
        SnapshotActionProvider.getInstance().initialize();
    }

}
