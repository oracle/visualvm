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

package org.graalvm.visualvm.lib.profiler.heapwalk;

import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.MemoryLint;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.Rule;
import org.graalvm.visualvm.lib.profiler.heapwalk.model.BrowserUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.AnalysisControllerUI;
import org.openide.util.NbBundle;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BoundedRangeModel;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.ErrorManager;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "AnalysisController_CannotResolveClassMsg=Cannot resolve class {0}",
    "AnalysisController_CannotResolveInstanceMsg=Cannot resolve instance #{0} of class {1}"
})
public class AnalysisController extends AbstractTopLevelController implements NavigationHistoryManager.NavigationHistoryCapable {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HeapFragmentWalker heapFragmentWalker;
    private List<Rule> rules = null;
    private MemoryLint runningMemoryLint;
    private boolean analysisRunning = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public AnalysisController(HeapFragmentWalker heapFragmentWalker) {
        this.heapFragmentWalker = heapFragmentWalker;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isAnalysisRunning() {
        return analysisRunning;
    }

    // --- NavigationHistoryManager.NavigationHistoryCapable implementation ------
    public NavigationHistoryManager.Configuration getCurrentConfiguration() {
        return new NavigationHistoryManager.Configuration();
    }

    // --- Public interface ------------------------------------------------------
    public HeapFragmentWalker getHeapFragmentWalker() {
        return heapFragmentWalker;
    }

    public List<Rule> getRules() {
        if (rules == null) {
            rules = new ArrayList(MemoryLint.createRules());
        }

        return rules;
    }

    public void cancelAnalysis() {
        if (runningMemoryLint != null) {
            runningMemoryLint.interrupt();
            analysisRunning = false;
            runningMemoryLint = null;
        }
    }

    public void configure(NavigationHistoryManager.Configuration configuration) {
        heapFragmentWalker.switchToHistoryAnalysisView();
    }

    public BoundedRangeModel performAnalysis(boolean[] rulesSelection) {
        final List<Rule> selectedRules = new ArrayList();
        final List<Rule> allRules = getRules();

        for (int i = 0; i < rulesSelection.length; i++) {
            if (rulesSelection[i]) {
                selectedRules.add(allRules.get(i));
            }
        }

        if (selectedRules.size() > 0) {
            analysisRunning = true;

            final MemoryLint ml = new MemoryLint(heapFragmentWalker.getHeapFragment());
            runningMemoryLint = ml;
            BrowserUtils.performTask(new Runnable() {
                    public void run() {
                        try {
                            ml.process(selectedRules);
                        } catch (Exception e) {
                            ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
                        }
                        rules = null;
                        analysisRunning = false;
                        runningMemoryLint = null;

                        AnalysisControllerUI ui = (AnalysisControllerUI)getPanel();
                        ui.displayNewRules();
                        if (!ml.isInterrupted()) ui.setResult(ml.getResults());
                    }
                });

            return ml.getGlobalProgress();
        } else {
            return null;
        }
    }

    public void showURL(URL url) {
        String urls = url.toString();

        if (urls.startsWith("file://instance/")) { // NOI18N
            urls = urls.substring("file://instance/".length()); // NOI18N

            String[] id = urls.split("/"); // NOI18N
            JavaClass c = heapFragmentWalker.getHeapFragment().getJavaClassByName(id[0]);

            if (c != null) {
                List<Instance> instances = c.getInstances();
                Instance i = null;
                int instanceNumber = Integer.parseInt(id[1]);
                if (instanceNumber <= instances.size()) i = instances.get(instanceNumber - 1);

                if (i != null) {
                    heapFragmentWalker.getClassesController().showInstance(i);
                } else {
                    ProfilerDialogs.displayError(Bundle.AnalysisController_CannotResolveInstanceMsg(id[1], c.getName()));
                }
            } else {
                ProfilerDialogs.displayError(Bundle.AnalysisController_CannotResolveClassMsg(id[0]));
            }
        } else if (urls.startsWith("file://class/")) { // NOI18N
            urls = urls.substring("file://class/".length()); // NOI18N

            JavaClass c = heapFragmentWalker.getHeapFragment().getJavaClassByName(urls);

            if (c != null) {
                heapFragmentWalker.getClassesController().showClass(c);
            } else {
                ProfilerDialogs.displayError(Bundle.AnalysisController_CannotResolveClassMsg(urls));
            }
        }
    }

    protected AbstractButton[] createClientPresenters() {
        return new AbstractButton[0];
    }

    protected AbstractButton createControllerPresenter() {
        return ((AnalysisControllerUI) getPanel()).getPresenter();
    }

    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new AnalysisControllerUI(this);
    }
}
