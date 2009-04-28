/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.heapwalk;

import javax.swing.BoundedRangeModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.heap.*;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.heapwalk.ui.HeapWalkerUI;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import javax.swing.SwingUtilities;


/**
 *
 * @author Jiri Sedlacek
 */
public class HeapWalker {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String HEAPWALKER_DEFAULT_NAME = NbBundle.getMessage(ClassesListController.class,
                                                                              "ClassesListController_HeapWalkerDefaultName"); // NOI18N
    private static final String HEAPWALKER_CUSTOM_NAME = NbBundle.getMessage(ClassesListController.class,
                                                                             "ClassesListController_HeapWalkerCustomName"); // NOI18N
    private static final String LOADING_DUMP_MSG = NbBundle.getMessage(ClassesListController.class,
                                                                       "ClassesListController_LoadingDumpMsg"); // NOI18N
                                                                                                                // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private File heapDumpFile;
    private HeapFragmentWalker mainHeapWalker;
    private HeapWalkerUI heapWalkerUI;
    private Project heapDumpProject;
    private String heapWalkerName;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public HeapWalker(Heap heap) {
        heapWalkerName = HEAPWALKER_DEFAULT_NAME;
        createMainFragment(heap);
    }

    public HeapWalker(File heapFile) throws FileNotFoundException, IOException {
        this(createHeap(heapFile));

        heapDumpFile = heapFile;
        heapDumpProject = computeHeapDumpProject(heapDumpFile);

        String fileName = heapDumpFile.getName();
        String fileNamePart;
        int extensionIndex = fileName.lastIndexOf("."); // NOI18N

        if (extensionIndex == -1) {
            fileNamePart = fileName;
        } else {
            fileNamePart = fileName.substring(0, extensionIndex);

            if (fileNamePart.startsWith("heapdump-")) { // NOI18N

                String time = fileNamePart.substring("heapdump-".length(), fileNamePart.length()); // NOI18N

                try {
                    long timeStamp = Long.parseLong(time);
                    fileNamePart = StringUtils.formatUserDate(new Date(timeStamp));
                } catch (NumberFormatException e) {
                    fileNamePart = fileName;
                }
            } else {
                fileNamePart = fileName;
            }
        }

        heapWalkerName = MessageFormat.format(HEAPWALKER_CUSTOM_NAME, new Object[] { fileNamePart });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public File getHeapDumpFile() {
        return heapDumpFile;
    }

    public Project getHeapDumpProject() {
        return heapDumpProject;
    }

    // --- Internal interface ----------------------------------------------------
    public HeapFragmentWalker getMainHeapWalker() {
        return mainHeapWalker;
    }

    public String getName() {
        return heapWalkerName;
    }

    // --- Public interface ------------------------------------------------------
    public void open() {
        //    SwingUtilities.invokeLater(new Runnable() {
        //      public void run() {
        //        getTopComponent().open();
        ////        getTopComponent().requestActive(); // For some reason steals focus from Dump Heap button in ProfilerControlPanel2 and causes http://www.netbeans.org/issues/show_bug.cgi?id=92425
        //        getTopComponent().requestVisible(); // Workaround for the above problem
        //      }
        //    });
        HeapWalkerManager.getDefault().openHeapWalker(this);
    }

    TopComponent getTopComponent() {
        if (heapWalkerUI == null) {
            heapWalkerUI = new HeapWalkerUI(this);
        }

        return heapWalkerUI;
    }

    void createMainFragment(Heap heap) {
        mainHeapWalker = new HeapFragmentWalker(heap, this, true);
    }

    void createReachableFragment(Instance instance) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // TODO: Open new tab or select existing one
                }
            });
    }

    void createRetainedFragment(Instance instance) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // TODO: Open new tab or select existing one
                }
            });
    }

    // --- Private implementation ------------------------------------------------
    private static Project computeHeapDumpProject(File heapDumpFile) {
        if (heapDumpFile == null) {
            return null;
        }

        File heapDumpDir = heapDumpFile.getParentFile();

        if (heapDumpDir == null) {
            return null;
        }

        FileObject heapDumpDirObj = FileUtil.toFileObject(heapDumpDir);

        if ((heapDumpDirObj == null) || !heapDumpDirObj.isValid()) {
            return null;
        }

        return IDEUtils.getProjectFromSettingsFolder(heapDumpDirObj);
    }

    private static Heap createHeap(File heapFile) throws FileNotFoundException, IOException {
        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandleFactory.createHandle(LOADING_DUMP_MSG);
            pHandle.setInitialDelay(0);
            pHandle.start(HeapProgress.PROGRESS_MAX*2);
            
            setProgress(pHandle,0);
            Heap heap = HeapFactory.createHeap(heapFile);
            setProgress(pHandle,HeapProgress.PROGRESS_MAX);
            heap.getSummary(); // Precompute HeapSummary within progress

            return heap;
        } finally {
            if (pHandle != null) {
                pHandle.finish();
            }
        }
    }

    private static void setProgress(final ProgressHandle pHandle, final int offset) {
        final BoundedRangeModel progress = HeapProgress.getProgress();
        progress.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                pHandle.progress(progress.getValue()+offset);
            }
        });
    }
}
