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

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.heapwalk.ui.HeapFragmentWalkerUI;
import java.io.File;
import javax.swing.JPanel;


/**
 *
 * @author Jiri Sedlacek
 */
public class HeapFragmentWalker {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AnalysisController analysisController;
    private ClassesController classesController;
    private Heap heapFragment; // TODO: Should be HeapFragment
    private HeapFragmentWalkerUI walkerUI;
    private HeapWalker heapWalker;
    private InstancesController instancesController;
    private NavigationHistoryManager navigationHistoryManager;
    private SummaryController summaryController;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public HeapFragmentWalker(Heap heapFragment, HeapWalker heapWalker) {
        this.heapFragment = heapFragment;
        this.heapWalker = heapWalker;

        summaryController = new SummaryController(this);
        classesController = new ClassesController(this);
        instancesController = new InstancesController(this);
        analysisController = new AnalysisController(this);

        navigationHistoryManager = new NavigationHistoryManager(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

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
        }

        return null;
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

    public Project getHeapDumpProject() {
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

    public int getTotalLiveBytes() {
        return heapFragment.getSummary().getTotalLiveBytes();
    }

    public int getTotalLiveInstances() {
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

    public void switchToAnalysisView() {
        ((HeapFragmentWalkerUI) getPanel()).showAnalysisView();
    }

    public void switchToClassesView() {
        ((HeapFragmentWalkerUI) getPanel()).showClassesView();
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
}
