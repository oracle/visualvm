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

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.heapwalk.ui.ClassesListControllerUI;
import org.openide.util.NbBundle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import org.netbeans.modules.profiler.projectsupport.utilities.SourceUtils;


/**
 *
 * @author Jiri Sedlacek
 */
public class ClassesListController extends AbstractController {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class ClassesComparator implements Comparator {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean sortingOrder;
        private int sortingColumn;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ClassesComparator(int sortingColumn, boolean sortingOrder) {
            this.sortingColumn = sortingColumn;
            this.sortingOrder = sortingOrder;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int compare(Object o1, Object o2) {
            JavaClass jClass1 = sortingOrder ? (JavaClass) o1 : (JavaClass) o2;
            JavaClass jClass2 = sortingOrder ? (JavaClass) o2 : (JavaClass) o1;

            switch (sortingColumn) {
                case 0:
                    return jClass1.getName().compareTo(jClass2.getName());
                case 1:
                case 2:
                    return new Integer(jClass1.getInstancesCount()).compareTo(new Integer(jClass2.getInstancesCount()));
                case 3:
                    return new Long(jClass1.getAllInstancesSize()).compareTo(jClass2.getAllInstancesSize());
                default:
                    throw new RuntimeException("Unsupported compare operation for " + o1 + ", " + o2); // NOI18N
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ANALYZING_CLASSES_MSG = NbBundle.getMessage(ClassesListController.class,
                                                                            "ClassesListController_AnalyzingClassesMsg"); // NOI18N
                                                                                                                          // -----
    public static final int FILTER_SUBCLASS = 1001;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ClassesController classesController;
    private JavaClass selectedClass;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ClassesListController(ClassesController classesController) {
        this.classesController = classesController;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public ClassesController getClassesController() {
        return classesController;
    }

    public void setColumnVisibility(int column, boolean isColumnVisible) {
        ((ClassesListControllerUI) getPanel()).setColumnVisibility(column, isColumnVisible);
    }

    // --- Internal interface ----------------------------------------------------
    public List getFilteredSortedClasses(String[] filterStrings, int filterType, boolean showZeroInstances, boolean showZeroSize,
                                         int sortingColumn, boolean sortingOrder) {
        HeapFragmentWalker fragmentWalker = classesController.getHeapFragmentWalker();
        Heap heap = fragmentWalker.getHeapFragment();
        List filteredClasses;

        if ((filterType == FILTER_SUBCLASS) && !((filterStrings == null) || filterStrings[0].equals(""))) { // NOI18N
            filteredClasses = getFilteredClasses(getSubclasses(heap, filterStrings, fragmentWalker.getHeapDumpProject()), null,
                                                 CommonConstants.FILTER_NONE, showZeroInstances, showZeroSize);
        } else {
            filteredClasses = getFilteredClasses(heap.getAllClasses(), filterStrings, filterType, showZeroInstances, showZeroSize);
        }

        return getSortedClasses(filteredClasses, sortingColumn, sortingOrder);
    }

    public JavaClass getSelectedClass() {
        return selectedClass;
    }

    public void classSelected(JavaClass javaClass) {
        if (selectedClass == javaClass) {
            return;
        }

        selectedClass = javaClass;
        classesController.classSelected();
    }

    public static boolean matchesFilter(JavaClass jClass, String[] filterStrings, int filterType, boolean showZeroInstances,
                                        boolean showZeroSize) {
        int instancesCount = jClass.getInstancesCount();
        int instanceSize = jClass.getInstanceSize();

        if (!showZeroInstances && (instancesCount == 0)) {
            return false;
        }

        if (!showZeroSize && ((instancesCount == 0) || (instanceSize == 0))) {
            return false;
        }

        if ((filterType == CommonConstants.FILTER_NONE) || (filterStrings == null) || filterStrings[0].equals("")) {
            return true; // NOI18N
        }

        return passesFilters(jClass.getName(), filterStrings, filterType);
    }

    public void selectClass(JavaClass javaClass) {
        ((ClassesListControllerUI) getPanel()).ensureWillBeVisible(javaClass);
        ((ClassesListControllerUI) getPanel()).selectClass(javaClass);
    }

    public void updateData() {
        ((ClassesListControllerUI) getPanel()).updateData();
    }

    protected AbstractButton createControllerPresenter() {
        return ((ClassesListControllerUI) getPanel()).getPresenter();
    }

    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new ClassesListControllerUI(this);
    }

    private static Collection getContextSubclasses(Heap heap, String className, Project project) {
        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandleFactory.createHandle(ANALYZING_CLASSES_MSG);
            pHandle.setInitialDelay(0);
            pHandle.start();

            HashSet subclasses = new HashSet();

            String[] subclassesNames = SourceUtils.getSubclassesNames(className, project);

            for (int i = 0; i < subclassesNames.length; i++) {
                JavaClass jClass = heap.getJavaClassByName(subclassesNames[i]);

                if ((jClass != null) && subclasses.add(jClass)) { // instanceof approach rather than subclassof
                    subclasses.addAll(jClass.getSubClasses());
                }
            }

            return subclasses;
        } finally {
            if (pHandle != null) {
                pHandle.finish();
            }
        }
    }

    private static List getFilteredClasses(List classes, String[] filterStrings, int filterType, boolean showZeroInstances,
                                           boolean showZeroSize) {
        ArrayList filteredClasses = new ArrayList();

        Iterator classesIterator = classes.iterator();

        while (classesIterator.hasNext()) {
            JavaClass jClass = (JavaClass) classesIterator.next();

            if (matchesFilter(jClass, filterStrings, filterType, showZeroInstances, showZeroSize)) {
                filteredClasses.add(jClass);
            }
        }

        return filteredClasses;
    }

    // --- Private implementation ------------------------------------------------
    private static List getSortedClasses(List filteredClasses, int sortingColumn, boolean sortingOrder) {
        Collections.sort(filteredClasses, new ClassesComparator(sortingColumn, sortingOrder));

        return filteredClasses;
    }

    private static List getSubclasses(Heap heap, String[] filterStrings, Project project) {
        HashSet subclasses = new HashSet();

        for (int i = 0; i < filterStrings.length; i++) {
            JavaClass jClass = heap.getJavaClassByName(filterStrings[i]);

            if (jClass != null) {
                Collection subclassesCol = jClass.getSubClasses();
                subclasses.add(jClass); // instanceof approach rather than subclassof

                if (subclassesCol.size() > 0) {
                    // jClass is a class and some subclasses found in heapdump
                    subclasses.addAll(subclassesCol);
                } else {
                    // jClass may be an interface and subclasses will be obtained from IDE infrastructure
                    // if heapdump has a project context
                    if (project != null) {
                        subclasses.addAll(getContextSubclasses(heap, filterStrings[i], project));
                    }
                }
            }
        }

        return new ArrayList(subclasses);
    }

    private static boolean passesFilter(String value, String filter, int type) {
        // Case sensitive comparison:
        /*switch (type) {
           case CommonConstants.FILTER_STARTS_WITH:
             return value.startsWith(filter);
           case CommonConstants.FILTER_CONTAINS:
             return value.indexOf(filter) != -1;
           case CommonConstants.FILTER_ENDS_WITH:
             return value.endsWith(filter);
           case CommonConstants.FILTER_EQUALS:
             return value.equals(filter);
           case CommonConstants.FILTER_REGEXP:
             return value.matches(filter);
           }*/

        // Case insensitive comparison (except regexp):
        switch (type) {
            case CommonConstants.FILTER_STARTS_WITH:
                return value.regionMatches(true, 0, filter, 0, filter.length()); // case insensitive startsWith, optimized
            case CommonConstants.FILTER_CONTAINS:
                return value.toLowerCase().indexOf(filter.toLowerCase()) != -1; // case insensitive indexOf, NOT OPTIMIZED
            case CommonConstants.FILTER_ENDS_WITH:

                // case insensitive endsWith, optimized
                return value.regionMatches(true, value.length() - filter.length(), filter, 0, filter.length());
            case CommonConstants.FILTER_EQUALS:
                return value.equalsIgnoreCase(filter); // case insensitive equals
            case CommonConstants.FILTER_REGEXP:

                try {
                    return value.matches(filter); // still case sensitive!
                } catch (java.util.regex.PatternSyntaxException e) {
                    return false;
                }
        }

        return false;
    }

    private static boolean passesFilters(String value, String[] filters, int type) {
        for (int i = 0; i < filters.length; i++) {
            if (passesFilter(value, filters[i], type)) {
                return true;
            }
        }

        return false;
    }
}
