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

import java.text.NumberFormat;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.heapwalk.ui.ClassesListControllerUI;
import org.openide.util.NbBundle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.java.ProfilerTypeUtils;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.Lookup;


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
                    return Integer.valueOf(jClass1.getInstancesCount()).compareTo(Integer.valueOf(jClass2.getInstancesCount()));
                case 3:
                    return Long.valueOf(jClass1.getAllInstancesSize()).compareTo(jClass2.getAllInstancesSize());
                case 4:
                    return Long.valueOf(jClass1.getRetainedSizeByClass()).compareTo(jClass2.getRetainedSizeByClass());
                default:
                    throw new RuntimeException("Unsupported compare operation for " + o1 + ", " + o2); // NOI18N
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

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

    private static final NumberFormat percentFormat = NumberFormat.getPercentInstance();
    private static final NumberFormat numberFormat = NumberFormat.getInstance();
    static {
        percentFormat.setMaximumFractionDigits(1);
        percentFormat.setMinimumFractionDigits(0);
    }
    
    public long minDiff;
    public long maxDiff;

    // --- Internal interface ----------------------------------------------------
    @NbBundle.Messages({"ClassesListController_ResultNotAvailableString=N/A",
                        "ClassesListController_CompareFailed=Failed to load the heap dump to compare."})
    public Object[][] getData(String[] filterStrings, int filterType, boolean showZeroInstances, boolean showZeroSize,
                                         int sortingColumn, boolean sortingOrder, int columnCount) {
        boolean diff = isDiff();
        boolean retained = classesController.getHeapFragmentWalker().getRetainedSizesStatus() == HeapFragmentWalker.RETAINED_SIZES_COMPUTED;
        
        long totalLiveInstances = classesController.getHeapFragmentWalker().getTotalLiveInstances();
        long totalLiveBytes = classesController.getHeapFragmentWalker().getTotalLiveBytes();

        List classes = getFilteredSortedClasses(filterStrings, filterType,
                showZeroInstances, showZeroSize, sortingColumn, sortingOrder);
        Object[][] data = new Object[classes.size()][columnCount + 1];
        
        minDiff = Long.MAX_VALUE;
        maxDiff = Long.MIN_VALUE;

        for (int i = 0; i < classes.size(); i++) {
            JavaClass jClass = (JavaClass) classes.get(i);
            
            int instancesCount = jClass.getInstancesCount();
//                            int instanceSize = jClass.getInstanceSize();
            long allInstancesSize = jClass.getAllInstancesSize();
            long retainedSizeByClass = -1;

            if (retained) {
                retainedSizeByClass = jClass.getRetainedSizeByClass();
            }
            data[i][0] = jClass.getName();
            
            if (diff) { 
                minDiff = Math.min(minDiff, instancesCount);
                maxDiff = Math.max(maxDiff, instancesCount);
                data[i][1] = new Long(instancesCount);
                data[i][2] = (instancesCount > 0 ? "+" : "") + numberFormat.format(instancesCount); // NOI18N
                data[i][3] = (allInstancesSize > 0 ? "+" : "") + numberFormat.format(allInstancesSize); // NOI18N
                if (retained) {
                    if (!compareRetained) data[i][4] = Bundle.ClassesListController_ResultNotAvailableString();
                    else data[i][4] = (retainedSizeByClass > 0 ? "+" : "") + numberFormat.format(retainedSizeByClass); // NOI18N 
                }
            } else {
                data[i][1] = new Double((double) instancesCount /
                                     (double) totalLiveInstances * 100);
                data[i][2] = numberFormat.format(instancesCount) + " (" // NOI18N
                                     + percentFormat.format((double) instancesCount /
                                     (double) totalLiveInstances) + ")"; // NOI18N
                data[i][3] = (allInstancesSize < 0) ? Bundle.ClassesListController_ResultNotAvailableString()
                                      : (numberFormat.format(allInstancesSize) + " (" // NOI18N
                                      + percentFormat.format((double) allInstancesSize /
                                      (double) totalLiveBytes) + ")"); // NOI18N
                if (retained) {
                    data[i][4] = (retainedSizeByClass < 0) ? Bundle.ClassesListController_ResultNotAvailableString()
                                      : (numberFormat.format(retainedSizeByClass) + " (" // NOI18N
                                      + percentFormat.format((double) retainedSizeByClass /
                                      (double) totalLiveBytes) + ")"); // NOI18N
                }
            }
            
            data[i][columnCount] = diff ? ((DiffJavaClass)jClass).getJavaClass() : jClass;
        }
        
        if ((minDiff > 0) && (maxDiff > 0)) {
            minDiff = 0;
        } else if ((minDiff < 0) && (maxDiff < 0)) {
            maxDiff = 0;
        }
        
        return data;
    }
    
    private static final class DiffJavaClass implements JavaClass {
        
        private final String name;
        private String id;
        private long allInstancesSize;
        private int instanceSize;
        private int instancesCount;
        private long retainedSizeByClass;
        private JavaClass real;
        
        static DiffJavaClass createExternal(JavaClass jc, boolean compareRetained) {
            return new DiffJavaClass(jc, false, compareRetained);
        }
        
        static DiffJavaClass createReal(JavaClass jc, boolean compareRetained) {
            return new DiffJavaClass(jc, true, compareRetained);
        }
        
        private DiffJavaClass(JavaClass jc, boolean realClass, boolean compareRetained) {
            name = jc.getName();
            
            if (realClass) {
                instancesCount = jc.getInstancesCount();
                instanceSize = jc.getInstanceSize();
                allInstancesSize = jc.getAllInstancesSize();
                retainedSizeByClass = compareRetained ? jc.getRetainedSizeByClass() : -1;
                real = jc;
            } else {
                instancesCount = -jc.getInstancesCount();
                instanceSize = -jc.getInstanceSize();
                allInstancesSize = -jc.getAllInstancesSize();
                retainedSizeByClass = compareRetained ? -jc.getRetainedSizeByClass() : -1;
                real = null;
            }
        }
        
        static String createID(JavaClass jc) {
            String id = jc.getName();
            try {
                id += jc.getClassLoader().getJavaClass().getName();
                // TODO: use more precise identification, URLClassLoaders have unique fields etc.
            } catch (Exception e) {
                
            }
            if (jc instanceof DiffJavaClass)
                ((DiffJavaClass)jc).setID(id);
            return id;
        }
        
        private synchronized void setID(String id) {
            this.id = id;
        }
        
        synchronized String getID() {
            if (id == null) createID(this);
            return id;
        }
        
        JavaClass getJavaClass() {
            return real;
        }
        
        void diff(DiffJavaClass djc) {
            instancesCount += djc.instancesCount;
            instanceSize += djc.instanceSize;
            allInstancesSize += djc.allInstancesSize;
            retainedSizeByClass += djc.retainedSizeByClass;
            real = djc.real;
        }
        
        public boolean equals(Object o) {
            if (o instanceof DiffJavaClass)
                return getID().equals(((DiffJavaClass)o).getID());
            else return false;
        }
        
        public int hashCode() {
            return getID().hashCode();
        }

        public Object getValueOfStaticField(String name) {
            // Not implemented
            return null;
        }

        public long getAllInstancesSize() {
            return allInstancesSize;
        }

        public boolean isArray() {
            // Not implemented
            return false;
        }

        public Instance getClassLoader() {
            // Not implemented
            return null;
        }

        public List getFields() {
            // Not implemented
            return null;
        }

        public int getInstanceSize() {
            return instanceSize;
        }

        public List getInstances() {
            // Not implemented
            return null;
        }

        public int getInstancesCount() {
            return instancesCount;
        }

        public long getJavaClassId() {
            // Not implemented
            return -1;
        }

        public String getName() {
            return name;
        }

        public List getStaticFieldValues() {
            // Not implemented
            return null;
        }

        public Collection getSubClasses() {
            // Not implemented
            return null;
        }

        public JavaClass getSuperClass() {
            // Not implemented
            return null;
        }

        public long getRetainedSizeByClass() {
            return retainedSizeByClass;
        }
        
    }
    
    public List getFilteredSortedClasses(String[] filterStrings, int filterType, boolean showZeroInstances, boolean showZeroSize,
                                         int sortingColumn, boolean sortingOrder) {
        HeapFragmentWalker fragmentWalker = classesController.getHeapFragmentWalker();
        Heap heap = fragmentWalker.getHeapFragment();
        
        List filteredClasses;

        if ((filterType == FILTER_SUBCLASS) && !((filterStrings == null) || filterStrings[0].isEmpty())) { // NOI18N
            filteredClasses = getFilteredClasses(getSubclasses(heap, diffClasses, filterStrings, fragmentWalker.getHeapDumpProject()), null,
                                                 CommonConstants.FILTER_NONE, showZeroInstances, showZeroSize);
        } else {
            List classes = diffClasses == null ? heap.getAllClasses() : diffClasses;
            filteredClasses = getFilteredClasses(classes, filterStrings, filterType, showZeroInstances, showZeroSize);
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

        if ((filterType == CommonConstants.FILTER_NONE) || (filterStrings == null) || filterStrings[0].isEmpty()) {
            return true; // NOI18N
        }

        return passesFilters(jClass.getName(), filterStrings, filterType);
    }

    public void selectClass(JavaClass javaClass) {
        ((ClassesListControllerUI) getPanel()).selectClass(javaClass);
    }

    public void updateData() {
        ((ClassesListControllerUI) getPanel()).updateData();
    }
    
    private void showDiffProgress() {
        ((ClassesListControllerUI) getPanel()).showDiffProgress();
    }
    
    private void hideDiffProgress() {
        ((ClassesListControllerUI) getPanel()).hideDiffProgress();
    }
    
    private List diffClasses;
    private boolean comparingSnapshot = false;
    private boolean compareRetained;
    public void compareAction() {
        if (comparingSnapshot) return;
        comparingSnapshot = true;
        BrowserUtils.performTask(new Runnable() {
            public void run() {
                try {
                    HeapFragmentWalker hfw = classesController.getHeapFragmentWalker();
                    CompareSnapshotsHelper.Result result = CompareSnapshotsHelper.selectSnapshot(
                            hfw, ((ClassesListControllerUI)getPanel()).isRetainedVisible());
                    if (result != null) {
                        try {
                            showDiffProgress();
                            Heap currentHeap = hfw.getHeapFragment();
                            Heap diffHeap = HeapFactory.createHeap(result.getFile());
                            compareRetained = result.compareRetained();
                            diffClasses = createDiffClasses(diffHeap, currentHeap);
                        } catch (Exception e) {
                            ProfilerDialogs.displayError(Bundle.ClassesListController_CompareFailed());
                            ProfilerLogger.log(e);
                        } finally {
                            hideDiffProgress();
                        }
                        updateData();
                    }
                } finally {
                    comparingSnapshot = false;
                }
            }
        });
    }
    
    public boolean isDiff() {
        return diffClasses != null;
    }
    
    public boolean compareRetained() {
        return compareRetained;
    }
    
    private List createDiffClasses(Heap h1, Heap h2) {
        if (compareRetained) classesController.getHeapFragmentWalker().
                             computeRetainedSizes(false, false);
        
        Map<String, DiffJavaClass> classes = new HashMap();
        
        List<JavaClass> classes1 = h1.getAllClasses();
        for (JavaClass jc1 : classes1) {
            String id1 = DiffJavaClass.createID(jc1);
            DiffJavaClass djc1 = DiffJavaClass.createExternal(jc1, compareRetained);
            classes.put(id1, djc1);
        }
        
        List<JavaClass> classes2 = h2.getAllClasses();
        for (JavaClass jc2 : classes2) {
            String id2 = DiffJavaClass.createID(jc2);
            DiffJavaClass djc2 = DiffJavaClass.createReal(jc2, compareRetained);
            DiffJavaClass djc1 = classes.get(id2);
            if (djc1 != null) djc1.diff(djc2);
            else classes.put(id2, djc2);
        }
        
        return new ArrayList(classes.values());
    }
    
    public void resetDiffAction() {
        if (diffClasses != null) {
            diffClasses.clear();
            diffClasses = null;
        }
        hideDiffProgress();
        updateData();
    }

    protected AbstractButton createControllerPresenter() {
        return ((ClassesListControllerUI) getPanel()).getPresenter();
    }

    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new ClassesListControllerUI(this);
    }

    @NbBundle.Messages("ClassesListController_AnalyzingClassesMsg=Analyzing classes...")
    private static Collection getContextSubclasses(Heap heap, String className, Lookup.Provider project) {
        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandleFactory.createHandle(Bundle.ClassesListController_AnalyzingClassesMsg());
            pHandle.setInitialDelay(0);
            pHandle.start();

            HashSet subclasses = new HashSet();

            SourceClassInfo sci = ProfilerTypeUtils.resolveClass(className, project);
            Collection<SourceClassInfo> impls = sci != null ? sci.getSubclasses() : Collections.EMPTY_LIST;

            for (SourceClassInfo ci : impls) {
                JavaClass jClass = heap.getJavaClassByName(ci.getQualifiedName());

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

    private static List getSubclasses(Heap heap, List diffClasses, String[] filterStrings, Lookup.Provider project) {
        HashSet subclasses = new HashSet();

        for (int i = 0; i < filterStrings.length; i++) {
            Collection<JavaClass> jClasses = heap.getJavaClassesByRegExp(filterStrings[i].replace(".", "\\."));

            for (JavaClass jClass : jClasses) {
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
        
        if (diffClasses != null) {
            ArrayList ret = new ArrayList();
            for (Object o : subclasses) {
                int i = diffClasses.indexOf(
                        DiffJavaClass.createExternal((JavaClass)o, false));
                if (i != -1) ret.add(diffClasses.get(i));
            }
            return ret;
        } else {
            return new ArrayList(subclasses);
        }
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
//            case CommonConstants.FILTER_STARTS_WITH:
//                return value.regionMatches(true, 0, filter, 0, filter.length()); // case insensitive startsWith, optimized
            case CommonConstants.FILTER_CONTAINS:
                return value.toLowerCase().contains(filter); // case insensitive indexOf, NOT OPTIMIZED
            case CommonConstants.FILTER_NOT_CONTAINS:
                return !value.toLowerCase().contains(filter);
//            case CommonConstants.FILTER_ENDS_WITH:
//                // case insensitive endsWith, optimized
//                return value.regionMatches(true, value.length() - filter.length(), filter, 0, filter.length());
//            case CommonConstants.FILTER_EQUALS:
//                return value.equalsIgnoreCase(filter); // case insensitive equals
            case CommonConstants.FILTER_REGEXP:
                try {
                    return value.matches(filter); //  case sensitive!
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
