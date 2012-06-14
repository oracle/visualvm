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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.heapwalk.ui.HeapFragmentWalkerUI;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapFragmentWalker_ComputeRetainedMsg=<html><b>Retained sizes will be computed.</b><br><br>For large heap dumps this operation can take a significant<br>amount of time. Do you want to continue?</html>",
    "HeapFragmentWalker_ComputeRetainedCaption=Compute Retained Sizes",
    "HeapFragmentWalker_ComputingRetainedMsg=Computing retained sizes...",
    "HeapFragmentWalker_ComputingRetainedCaption=Computing Retained Sizes"
})
public class HeapFragmentWalker {
    public static final int RETAINED_SIZES_UNSUPPORTED = -1;
    public static final int RETAINED_SIZES_UNKNOWN = 0;
    public static final int RETAINED_SIZES_CANCELLED = 1;
    public static final int RETAINED_SIZES_COMPUTING = 2;
    public static final int RETAINED_SIZES_COMPUTED = 3;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AnalysisController analysisController;
    private OQLController oqlController;
    private ClassesController classesController;
    private Heap heapFragment; // TODO: Should be HeapFragment
    private HeapFragmentWalkerUI walkerUI;
    private HeapWalker heapWalker;
    private InstancesController instancesController;
    private NavigationHistoryManager navigationHistoryManager;
    private SummaryController summaryController;

    private List<StateListener> stateListeners;
    private int retainedSizesStatus;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public HeapFragmentWalker(Heap heapFragment, HeapWalker heapWalker) {
        this(heapFragment, heapWalker, false);
    }

    public HeapFragmentWalker(Heap heapFragment, HeapWalker heapWalker, boolean supportsRetainedSizes) {
        this.heapFragment = heapFragment;
        this.heapWalker = heapWalker;

        this.retainedSizesStatus = supportsRetainedSizes ? RETAINED_SIZES_UNKNOWN :
                                                        RETAINED_SIZES_UNSUPPORTED;

        summaryController = new SummaryController(this);
        classesController = new ClassesController(this);
        instancesController = new InstancesController(this);
        analysisController = new AnalysisController(this);

        navigationHistoryManager = new NavigationHistoryManager(this);
        oqlController = new OQLController(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public synchronized final int computeRetainedSizes(boolean masterAction) {

        if (retainedSizesStatus != RETAINED_SIZES_UNSUPPORTED &&
            retainedSizesStatus != RETAINED_SIZES_COMPUTED) {

            if (!ProfilerDialogs.displayConfirmationDNSA(
                    Bundle.HeapFragmentWalker_ComputeRetainedMsg(), 
                    Bundle.HeapFragmentWalker_ComputeRetainedCaption(),
                    null, "HeapFragmentWalker.computeRetainedSizes", false)) { //NOI18N
                changeState(RETAINED_SIZES_CANCELLED, masterAction);
            } else {
                changeState(RETAINED_SIZES_COMPUTING, masterAction);
                List<JavaClass> classes = heapFragment.getAllClasses();
                for (JavaClass jclass : classes) {
                    List<Instance> instances = jclass.getInstances();
                    if (instances.size() > 0) {
                        Dialog progress = showProgress(
                            Bundle.HeapFragmentWalker_ComputingRetainedCaption(),
                            Bundle.HeapFragmentWalker_ComputingRetainedMsg());
                        instances.get(0).getRetainedSize();
                        if (progress != null) {
                            progress.setVisible(false);
                            progress.dispose();
                        }
                        break;
                    }
                }
                changeState(RETAINED_SIZES_COMPUTED, masterAction);
            }
        }

        return retainedSizesStatus;
    }
    
    private static Dialog showProgress(String caption, String message) {
        final CountDownLatch latch = new CountDownLatch(1); // create a latch to prevent race condition while displaying the progress

        final Dialog progress = createProgressPanel(caption, message);
        progress.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    latch.countDown(); // window has changed the state to "SHOWING" - can leave the "nonResponding()" method"
                }
            }
        });
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                progress.setVisible(true);
            }
        });

        try {
            latch.await(); // wait till the progress dialog has been displayed
        } catch (InterruptedException e) {
            // TODO move to SwingWorker
        }
            
        return progress;
    }

    private static Dialog createProgressPanel(String caption, String message) {
        Dialog dialog;
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.add(new JLabel(message), BorderLayout.NORTH);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        panel.add(progress, BorderLayout.SOUTH);

        Dimension ps = panel.getPreferredSize();
        ps.setSize(Math.max(ps.getWidth(), 350), Math.max(ps.getHeight(), 50));
        panel.setPreferredSize(ps);

        dialog = DialogDisplayer.getDefault().createDialog(new DialogDescriptor(panel, caption, true, new Object[] {  },
                                                           DialogDescriptor.CANCEL_OPTION, DialogDescriptor.RIGHT_ALIGN,
                                                           null, null));

        return dialog;
    }

    public synchronized final int getRetainedSizesStatus() {
        return retainedSizesStatus;
    }

    public final void addStateListener(StateListener listener) {
        if (stateListeners == null) stateListeners = new ArrayList();
        if (!stateListeners.contains(listener)) stateListeners.add(listener);
    }

    public final void removeStateListener(StateListener listener) {
        if (stateListeners == null || !stateListeners.contains(listener)) return;
        stateListeners.remove(listener);
        if (stateListeners.size() == 0) stateListeners = null;
    }

    private void changeState(final int newState, final boolean masterChange) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                retainedSizesStatus = newState;
                if (stateListeners == null) return;
                StateEvent e = new StateEvent(retainedSizesStatus, masterChange);
                for (StateListener listener : stateListeners)
                    listener.stateChanged(e);
            }
        });
    }


    public AbstractTopLevelController getActiveController() {
        HeapFragmentWalkerUI ui = (HeapFragmentWalkerUI) getPanel();

        if (ui == null) {
            return null; // Debugger overrides getPanel() and always returns null
        }

        if (ui.isSummaryViewActive()) {
            return summaryController;
        } else if (ui.isClassesViewActive()) {
            return classesController;
        } else if (ui.isInstancesViewActive()) {
            return instancesController;
        } else if (ui.isAnalysisViewActive()) {
            return analysisController;
        } else if (ui.isOQLViewActive()) {
            return oqlController;
        }

        return null;
    }

    public OQLController getOQLController() {
        return oqlController;
    }

    public AnalysisController getAnalysisController() {
        return analysisController;
    }

    public ClassesController getClassesController() {
        return classesController;
    }

    // --- Public interface ------------------------------------------------------
    public File getHeapDumpFile() {
        return heapWalker.getHeapDumpFile();
    }

    public Lookup.Provider getHeapDumpProject() {
        return heapWalker.getHeapDumpProject();
    }

    public Heap getHeapFragment() {
        return heapFragment;
    }

    public InstancesController getInstancesController() {
        return instancesController;
    }

    public boolean isNavigationBackAvailable() {
        return navigationHistoryManager.isNavigationBackAvailable();
    }

    public boolean isNavigationForwardAvailable() {
        return navigationHistoryManager.isNavigationForwardAvailable();
    }

    public JPanel getPanel() {
        if (walkerUI == null) {
            walkerUI = new HeapFragmentWalkerUI(this);
        }

        return walkerUI;
    }

    public SummaryController getSummaryController() {
        return summaryController;
    }

    public long getTotalLiveBytes() {
        return heapFragment.getSummary().getTotalLiveBytes();
    }

    public long getTotalLiveInstances() {
        return heapFragment.getSummary().getTotalLiveInstances();
    }

    // --- Navigation history support
    public void createNavigationHistoryPoint() {
        HeapFragmentWalkerUI ui = (HeapFragmentWalkerUI) getPanel();

        if (ui == null) {
            return; // Debugger overrides getPanel() and always returns null
        }

        navigationHistoryManager.createNavigationHistoryPoint();
        ui.updateNavigationActions();
    }

    public void navigateBack() {
        HeapFragmentWalkerUI ui = (HeapFragmentWalkerUI) getPanel();

        if (ui == null) {
            return; // Debugger overrides getPanel() and always returns null
        }

        navigationHistoryManager.navigateBack();
        ui.updateNavigationActions();
    }

    public void navigateForward() {
        HeapFragmentWalkerUI ui = (HeapFragmentWalkerUI) getPanel();

        if (ui == null) {
            return; // Debugger overrides getPanel() and always returns null
        }

        navigationHistoryManager.navigateForward();
        ui.updateNavigationActions();
    }

    public void showInstancesForClass(JavaClass jClass) {
        switchToInstancesView();
        instancesController.setClass(jClass);
    }

    public void switchToOQLView() {
        ((HeapFragmentWalkerUI) getPanel()).showOQLView();
    }

    public void switchToAnalysisView() {
        ((HeapFragmentWalkerUI) getPanel()).showAnalysisView();
    }

    public void switchToClassesView() {
        ((HeapFragmentWalkerUI) getPanel()).showClassesView();
    }

    public void switchToHistoryOQLView() {
        ((HeapFragmentWalkerUI) getPanel()).showHistoryOQLView();
    }

    public void switchToHistoryAnalysisView() {
        ((HeapFragmentWalkerUI) getPanel()).showHistoryAnalysisView();
    }

    public void switchToHistoryClassesView() {
        ((HeapFragmentWalkerUI) getPanel()).showHistoryClassesView();
    }

    public void switchToHistoryInstancesView() {
        ((HeapFragmentWalkerUI) getPanel()).showHistoryInstancesView();
    }

    public void switchToHistorySummaryView() {
        ((HeapFragmentWalkerUI) getPanel()).showHistorySummaryView();
    }

    public void switchToInstancesView() {
        ((HeapFragmentWalkerUI) getPanel()).showInstancesView();
    }

    public void switchToSummaryView() {
        ((HeapFragmentWalkerUI) getPanel()).showSummaryView();
    }

    // ---

    // --- Internal interface ----------------------------------------------------
    NavigationHistoryManager.NavigationHistoryCapable getNavigationHistorySource() {
        AbstractTopLevelController activeController = getActiveController();

        if (activeController instanceof NavigationHistoryManager.NavigationHistoryCapable) {
            return (NavigationHistoryManager.NavigationHistoryCapable) activeController;
        }

        return null;
    }


    public static interface StateListener {

        public void stateChanged(StateEvent e);

    }

    public static final class StateEvent {

        private int retainedSizesStatus;
        private boolean masterChange;


        StateEvent(int retainedSizesStatus) {
            this(retainedSizesStatus, false);
        }

        StateEvent(int retainedSizesStatus, boolean masterChange) {
            this.retainedSizesStatus = retainedSizesStatus;
            this.masterChange = masterChange;
        }

        public int getRetainedSizesStatus() { return retainedSizesStatus; }

        public boolean isMasterChange() { return masterChange; }

    }

}
