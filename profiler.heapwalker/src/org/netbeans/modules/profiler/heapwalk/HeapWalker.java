/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.heapwalk.ui.HeapWalkerUI;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.api.ProfilerStorage;
import org.openide.util.Lookup;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ClassesListController_HeapWalkerDefaultName=HeapWalker",
    "ClassesListController_LoadingDumpMsg=Loading Heap Dump..."
})
public class HeapWalker {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private File heapDumpFile;
    private HeapFragmentWalker mainHeapWalker;
    private HeapWalkerUI heapWalkerUI;
    private Lookup.Provider heapDumpProject;
    private String heapWalkerName;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public HeapWalker(Heap heap) {
        heapWalkerName = Bundle.ClassesListController_HeapWalkerDefaultName();
        createMainFragment(heap);
    }

    public HeapWalker(File heapFile) throws FileNotFoundException, IOException {
        this(createHeap(heapFile));

        heapDumpFile = heapFile;
        heapDumpProject = computeHeapDumpProject(heapDumpFile);

        String fileName = heapDumpFile.getName();
        int dotIndex = fileName.lastIndexOf('.'); // NOI18N
        if (dotIndex > 0 && dotIndex <= fileName.length() - 2)
            fileName = fileName.substring(0, dotIndex);
        heapWalkerName = ResultsManager.getDefault().getHeapDumpDisplayName(fileName);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public File getHeapDumpFile() {
        return heapDumpFile;
    }

    public Lookup.Provider getHeapDumpProject() {
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

    public TopComponent getTopComponent() {
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
    private static Lookup.Provider computeHeapDumpProject(File heapDumpFile) {
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

        return ProfilerStorage.getProjectFromFolder(heapDumpDirObj);
    }

    private static Heap createHeap(File heapFile) throws FileNotFoundException, IOException {
        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandle.createHandle(Bundle.ClassesListController_LoadingDumpMsg());
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
