/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.heapwalk.ui.InstancesControllerUI;
import javax.swing.AbstractButton;
import javax.swing.JPanel;


/**
 *
 * @author Jiri Sedlacek
 */
public class InstancesController extends AbstractTopLevelController implements FieldsBrowserController.Handler,
                                                                               ReferencesBrowserController.Handler,
                                                                               NavigationHistoryManager.NavigationHistoryCapable {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class Configuration extends NavigationHistoryManager.Configuration {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private long instanceID;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Configuration(long instanceID) {
            this.instanceID = instanceID;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public long getInstanceID() {
            return instanceID;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ClassPresenterPanel classPresenter;
    private FieldsBrowserController fieldsBrowserController;
    private HeapFragmentWalker heapFragmentWalker;
    private InstancesListController instancesListController;
    private JavaClass selectedClass;
    private ReferencesBrowserController referencesBrowserController;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public InstancesController(HeapFragmentWalker heapFragmentWalker) {
        this.heapFragmentWalker = heapFragmentWalker;

        classPresenter = new ClassPresenterPanel();
        instancesListController = new InstancesListController(this);
        fieldsBrowserController = new FieldsBrowserController(this, FieldsBrowserController.ROOT_INSTANCE);
        referencesBrowserController = new ReferencesBrowserController(this);

        classPresenter.setHeapFragmentWalker(heapFragmentWalker);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setClass(final JavaClass jClass) {
        if (jClass == selectedClass) {
            return;
        }

        selectedClass = jClass;
        ((InstancesControllerUI) getPanel()).update();
        updateClientPresentersEnabling(getClientPresenters()); // update enabling when no class selected

        classPresenter.setJavaClass(jClass);

        if (!instancesListController.getPanel().isVisible()) {
            instancesListController.getPanel().setVisible(true); // must be opened first to propagate selection to Fields & References browser. Anyway, doesn't make much sense at all when closed.
        }

        instancesListController.scheduleFirstInstanceSelection();
        instancesListController.setClass(jClass);
    }

    // --- Internal interface ----------------------------------------------------
    public ClassPresenterPanel getClassPresenterPanel() {
        return classPresenter;
    }

    // --- NavigationHistoryManager.NavigationHistoryCapable implementation ------

    //  public NavigationHistoryManager.Configuration getCurrentConfiguration() {
    //    return new NavigationHistoryManager.Configuration();
    //  }
    //
    //  public void configure(NavigationHistoryManager.Configuration configuration) {
    //    heapFragmentWalker.switchToHistoryInstancesView();
    //  }
    public Configuration getCurrentConfiguration() {
        // Selected instance
        long selectedInstanceID = -1;
        Instance selectedInstance = getSelectedInstance();

        if (selectedInstance != null) {
            selectedInstanceID = selectedInstance.getInstanceId();
        }

        return new Configuration(selectedInstanceID);
    }

    public FieldsBrowserController getFieldsBrowserController() {
        return fieldsBrowserController;
    }

    // --- Public interface ------------------------------------------------------
    public HeapFragmentWalker getHeapFragmentWalker() {
        return heapFragmentWalker;
    }

    public InstancesListController getInstancesListController() {
        return instancesListController;
    }

    public ReferencesBrowserController getReferencesBrowserController() {
        return referencesBrowserController;
    }

    public JavaClass getSelectedClass() {
        return selectedClass;
    }

    public Instance getSelectedInstance() {
        return instancesListController.getSelectedInstance();
    }

    public void configure(NavigationHistoryManager.Configuration configuration) {
        if (configuration instanceof Configuration) {
            Configuration c = (Configuration) configuration;

            // Selected instance
            Instance selectedInstance = null;
            long selectedInstanceID = c.getInstanceID();

            if (selectedInstanceID != -1) {
                selectedInstance = heapFragmentWalker.getHeapFragment().getInstanceByID(selectedInstanceID);
            }

            if (selectedInstance != null) {
                final JavaClass jClass = selectedInstance.getJavaClass();

                selectedClass = jClass;
                ((InstancesControllerUI) getPanel()).update();
                updateClientPresentersEnabling(getClientPresenters()); // update enabling when no class selected

                heapFragmentWalker.switchToHistoryInstancesView();

                classPresenter.setJavaClass(jClass);
                instancesListController.showInstance(selectedInstance);
            } else {
                heapFragmentWalker.switchToHistoryInstancesView();
            }
        } else {
            throw new IllegalArgumentException("Unsupported configuration: " + configuration); // NOI18N
        }
    }

    public void instanceSelected() {
        Instance selectedInstance = getSelectedInstance();
        fieldsBrowserController.setInstance(selectedInstance);
        referencesBrowserController.setInstance(selectedInstance);
    }

    public void showClass(JavaClass javaClass) {
        heapFragmentWalker.getClassesController().showClass(javaClass);
    }

    // --- FieldsBrowserController.Handler implementation ------------------------
    public void showInstance(final Instance instance) {
        final JavaClass jClass = instance.getJavaClass();

        selectedClass = jClass;
        ((InstancesControllerUI) getPanel()).update();
        updateClientPresentersEnabling(getClientPresenters()); // update enabling when no class selected

        heapFragmentWalker.switchToInstancesView();

        //    BrowserUtils.performTask(new Runnable() {
        //      public void run() {
        classPresenter.setJavaClass(jClass);
        instancesListController.showInstance(instance);

        //      }
        //    });
    }

    protected AbstractButton[] createClientPresenters() {
        return new AbstractButton[] {
                   instancesListController.getPresenter(), fieldsBrowserController.getPresenter(),
                   referencesBrowserController.getPresenter()
               };
    }

    protected AbstractButton createControllerPresenter() {
        return ((InstancesControllerUI) getPanel()).getPresenter();
    }

    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new InstancesControllerUI(this);
    }

    protected void updateClientPresentersEnabling(AbstractButton[] clientPresenters) {
        if (selectedClass == null) {
            for (int i = 0; i < clientPresenters.length; i++) {
                clientPresenters[i].setVisible(false);
            }
        } else {
            for (int i = 0; i < clientPresenters.length; i++) {
                clientPresenters[i].setVisible(true);
            }

            super.updateClientPresentersEnabling(clientPresenters);
        }
    }
}
