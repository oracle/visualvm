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
import org.netbeans.modules.profiler.heapwalk.ui.ClassesControllerUI;
import javax.swing.AbstractButton;
import javax.swing.JPanel;


/**
 *
 * @author Jiri Sedlacek
 */
public class ClassesController extends AbstractTopLevelController implements FieldsBrowserController.Handler,
                                                                             NavigationHistoryManager.NavigationHistoryCapable {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class Configuration extends NavigationHistoryManager.Configuration {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private long javaClassID;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Configuration(long javaClassID) {
            this.javaClassID = javaClassID;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public long getJavaClassID() {
            return javaClassID;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ClassesListController classesListController;
    private FieldsBrowserController staticFieldsBrowserController;
    private HeapFragmentWalker heapFragmentWalker;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public ClassesController(HeapFragmentWalker heapFragmentWalker) {
        this.heapFragmentWalker = heapFragmentWalker;

        classesListController = new ClassesListController(this);
        staticFieldsBrowserController = new FieldsBrowserController(this, FieldsBrowserController.ROOT_CLASS);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Internal interface ----------------------------------------------------
    public ClassesListController getClassesListController() {
        return classesListController;
    }

    // --- NavigationHistoryManager.NavigationHistoryCapable implementation ------
    public Configuration getCurrentConfiguration() {
        // Selected class
        long selectedClassID = -1;
        JavaClass selectedClass = getSelectedClass();

        if (selectedClass != null) {
            selectedClassID = selectedClass.getJavaClassId();
        }

        return new Configuration(selectedClassID);
    }

    // --- Public interface ------------------------------------------------------
    public HeapFragmentWalker getHeapFragmentWalker() {
        return heapFragmentWalker;
    }

    public JavaClass getSelectedClass() {
        return classesListController.getSelectedClass();
    }

    public FieldsBrowserController getStaticFieldsBrowserController() {
        return staticFieldsBrowserController;
    }

    public void classSelected() {
        JavaClass selectedClass = getSelectedClass();
        staticFieldsBrowserController.setJavaClass(selectedClass);
    }

    public void configure(NavigationHistoryManager.Configuration configuration) {
        if (configuration instanceof Configuration) {
            Configuration c = (Configuration) configuration;

            heapFragmentWalker.switchToHistoryClassesView();

            // Selected class
            JavaClass selectedClass = null;
            long selectedClassID = c.getJavaClassID();

            if (selectedClassID != -1) {
                selectedClass = heapFragmentWalker.getHeapFragment().getJavaClassByID(selectedClassID);
            }

            if (selectedClass != null) {
                classesListController.selectClass(selectedClass);
            }
        } else {
            throw new IllegalArgumentException("Unsupported configuration: " + configuration); // NOI18N
        }
    }

    public void showClass(JavaClass javaClass) {
        heapFragmentWalker.switchToClassesView();

        if (!classesListController.getPanel().isVisible()) {
            classesListController.getPanel().setVisible(true);
        }

        classesListController.selectClass(javaClass);
    }

    // --- FieldsBrowserController.Handler implementation ------------------------
    public void showInstance(Instance instance) {
        heapFragmentWalker.getInstancesController().showInstance(instance);
    }

    protected AbstractButton[] createClientPresenters() {
        return new AbstractButton[] { classesListController.getPresenter(), staticFieldsBrowserController.getPresenter() };
    }

    protected AbstractButton createControllerPresenter() {
        return ((ClassesControllerUI) getPanel()).getPresenter();
    }

    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new ClassesControllerUI(this);
    }
}
