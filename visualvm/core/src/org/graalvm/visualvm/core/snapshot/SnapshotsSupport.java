/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.core.snapshot;

import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.properties.PropertiesSupport;
import java.awt.Image;
import java.io.File;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 * Support for snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class SnapshotsSupport {
    
    private static final Image SNAPSHOT_BADGE = ImageUtilities.loadImage("com/sun/tools/visualvm/core/ui/resources/snapshotBadge.png", true);    // NOI18N
    
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
    
    
    /**
     * Saves the snapshot to a used-defined file (opens Save File dialog with defined caption).
     * 
     * @param snapshot Snapshot to be saved.
     * @param dialogTitle Save File dialog caption.
     */
    public void saveAs(final Snapshot snapshot, String dialogTitle) {
        final File file = snapshot.getFile();
        if (file == null) {
            ProfilerDialogs.displayError(NbBundle.getMessage(SnapshotsSupport.class, "LBL_CannotSave"));  // NOI18N
        } else {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(dialogTitle);
            chooser.setSelectedFile(new File(snapshot.getFile().getName()));
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(snapshot.getCategory().getFileFilter());
    //        chooser.setFileView(category.getFileView());
            if (chooser.showSaveDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
                String categorySuffix = snapshot.getCategory().getSuffix();
                String filePath = chooser.getSelectedFile().getAbsolutePath();
                if (!filePath.endsWith(categorySuffix)) filePath += categorySuffix;
                final File copy = new File(filePath);
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        ProgressHandle pHandle = null;
                        try {
                            pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(SnapshotsSupport.class, "LBL_Saving",DataSourceDescriptorFactory.getDescriptor(snapshot).getName()));  // NOI18N
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
    }
    
    /**
     * Creates a timestamp String (typically used in Snaphshot filename).
     * 
     * @param time
     * @return timestamp String (typically used in Snaphshot filename).
     */
    public String getTimeStamp(long time) {
        return org.netbeans.lib.profiler.utils.StringUtils.formatUserDate(new Date(time));
    }
    
    /**
     * Creates icon for a snapshot by adding a snapshot badge to the provided image.
     * 
     * @param icon original image.
     * @return image with snapshot badge.
     */
    public Image createSnapshotIcon(Image icon) {
        return ImageUtilities.mergeImages(icon, SNAPSHOT_BADGE, 0, 0);
    }
    
    
    private SnapshotsSupport() {
        PropertiesSupport.sharedInstance().registerPropertiesProvider(
                new GeneralPropertiesProvider(), Snapshot.class);
    }

}
