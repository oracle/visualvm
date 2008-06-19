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
package org.netbeans.modules.profiler.ui.stats.drilldown;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.results.cpu.cct.CCTResultsFilter;
import org.netbeans.lib.profiler.marker.Mark;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.lib.profiler.results.cpu.cct.TimeCollector;
import org.netbeans.modules.profiler.categories.Categorization;
import org.netbeans.modules.profiler.categories.Category;
import org.netbeans.modules.profiler.utilities.Visitable;
import org.netbeans.modules.profiler.utilities.Visitor;
import org.openide.util.Lookup;

/**
 *
 * @author Jaroslav Bachorik
 */
public class DrillDown implements CCTResultsFilter.Evaluator {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------
    private static class TimeTouple {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------
        public static final TimeTouple ZERO = new TimeTouple(0, 0);        //~ Instance fields ------------------------------------------------------------------------------------------------------
        final long time0;
        final long time1;

        //~ Constructors ---------------------------------------------------------------------------------------------------------
        public TimeTouple(final long time0, final long time1) {
            this.time0 = time0;
            this.time1 = time1;
        }
    }    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private final List<Category> ddPath = new ArrayList<Category>(5);
    private final Map netTimeMap = new HashMap();
    private final Map timeMap = new HashMap();
    private final Set listeners = Collections.synchronizedSet(new HashSet());
    
    private ProfilerClient client;
    private boolean secondTime;
    private boolean validFlag;
    private boolean isSelf = false;
    private Category currentCategory;
    private Categorization categorization;
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    public DrillDown() {
        this.validFlag = false;
    }

    final public void configure(Lookup lookup, final ProfilerClient client) {
        configure(lookup, client, false);
    }

    final public void configure(Lookup lookup, final ProfilerClient client, final boolean secondTimeStamp) {
        categorization = lookup.lookup(Categorization.class);

        this.secondTime = secondTimeStamp;
        this.client = client;
        reset();

        this.validFlag = !categorization.getRoot().getSubcategories().isEmpty();
    }
    
    final public void deconfigure() {
        this.validFlag = false;
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public boolean isCurrent(final Category category) {
        return currentCategory.equals(category);
    }

    public Category getCurrentCategory() {
        return (currentCategory != null) ? currentCategory : null;
    }

    public long getCurrentTime(final boolean net) {
        return getCategoryTime(currentCategory, net);
    }

    public List<Category> getDrillDownPath() {
        List<Category> rslt = new ArrayList(ddPath);

        if (isSelf) {
//            rslt.add(new Category("SELF", "Self")); // NOI18N
        }

        return rslt;
    }

    public boolean isInSelf() {
        return isSelf;
    }

    public long getCategoryTime(final Category category, final boolean net) {
        TimeCollector tc = Lookup.getDefault().lookup(TimeCollector.class);
        TimeTouple time = null;
        try {
            tc.beginTrans(false);
            if (net) {
                time = new TimeTouple(tc.getNetTime0(category.getAssignedMark()), tc.getNetTime1(category.getAssignedMark()));
            } else {
                long time0 = 0L;
                long time1 = 0L;
                for (Mark mark : categorization.getAllMarks(category)) {
                    time0 += tc.getNetTime0(mark);
                    time1 += tc.getNetTime1(mark);
                }
                time = new TimeTouple(time0, time1);
            }
            return (time != null) ? (secondTime ? time.time1 : time.time0) : 0;
        } finally {
            tc.endTrans();
        }
    }

    public List<Category> getSubCategories() {
        return new ArrayList(currentCategory.getSubcategories());
//        if (isSelf) {
//            return Arrays.asList(new HierarchicalMark[] { currentMark });
//        } else {
//            List rslt = new ArrayList(currentMark.getChildren());
//            rslt.add(currentMark);
//
//            return rslt;
//        }
    }

    public Category getTopCategory() {
        return categorization.getRoot();
    }

    public long getTopTime(final boolean net) {
        return getCategoryTime(getTopCategory(), net);
    }

    public boolean isValid() {
        return validFlag;
    }

    public void addListener(DrillDownListener drillDownListener) {
        listeners.add(drillDownListener);
    }

    public boolean canDrilldown(Category category) {
//        if (mark == null) {
//            return false;
//        }
//
//        if (isSelf) {
//            return false;
//        }
//
//        if (isCurrent(mark)) { // special "SELF" category
//
//            if (mark instanceof HierarchicalMark) {
//                return ((HierarchicalMark) mark).getChildren().size() > 1;
//            }
//        }

        return true;
    }

    public void drilldown(String catId) {

//        if (mark.equals(currentMark)) {
//            isSelf = true;
//            fireDrillDownChange();
//        } else {
        isSelf = false;

        for (Category category : currentCategory.getSubcategories()) {
            if (category.getId().equals(catId)) {
                if (canDrilldown(category)) {
                    currentCategory = category;
                    ddPath.add(currentCategory);
                    fireDrillDownChange();
                }

                break;
            }
        }
//        }
    }

    public void drillup() {
        if (ddPath.size() == 1) {
            return;
        }

        ddPath.remove(ddPath.size() - 1);
        currentCategory = ddPath.get(ddPath.size() - 1);
        fireDrillDownChange();
    }

    public void drillup(String catId) {
//        isSelf = false;
        boolean found = false;
        for (Category catInPath : ddPath) {
            if (catInPath.getId().equals(catId)) {
                currentCategory = catInPath;
                found = true;
                break;
            }
        }
        if (found) {
            ddPath.remove(currentCategory);
            fireDrillDownChange();
        }
    }

    public boolean evaluate(Mark categoryMark) {
        if ((currentCategory == null)) {
            return true;
        }

//        if (currentMark.isDefault && categoryMark.isDefault) {
//            return true;
//        }

        Boolean passed = currentCategory.accept(new Visitor<Visitable<Category>, Boolean, Mark>() {

            public Boolean visit(Visitable<Category> visitable, Mark parameter) {
                if (visitable.getValue().getAssignedMark().equals(parameter)) {
                    return Boolean.TRUE;
                }

                return null;
            }
        }, categoryMark);
        return passed != null ? passed.booleanValue() : false;
    }

//    public void refresh() {
//        TimeCollector tc = Lookup.getDefault().lookup(TimeCollector.class);
//        if (tc == null) {
//            return;
//        }
//
//        clearTimeMaps();
//
//
//
//        tc.beginTrans(false);
//
//        categorization.getRoot().accept(new Visitor<Visitable<Category>, Void, Stack<Category>>() {
//
//            public Void visit(Visitable<Category> visitable, Stack<Category> stack) {
//            }
//        }, new Stack<Category>());
//
//        try {
//            getTime(root);
//        } finally {
//            client.getTimeCollector().endTrans();
//        }
//
//        fireDataChange();
//    }

    public void removeListener(DrillDownListener drillDownListener) {
        listeners.remove(drillDownListener);
    }

    public void reset() {
        ddPath.clear();
        ddPath.add(categorization.getRoot());
        currentCategory = categorization.getRoot();
        isSelf = false;
        fireDrillDownChange();
    }

//    private TimeTouple getTime(Category category) {
//        final TimeCollector tc = Lookup.getDefault().lookup(TimeCollector.class);
//        
//        long netTime0 = 0L;
//        long netTime1 = 0L;
//        final long accuTime0[] = new long[1];
//        final long accuTime1[] = new long[1];
//                
//        netTime0 += tc.getNetTime0(category.getAssignedMark());
//        netTime1 += tc.getNetTime1(category.getAssignedMark());
//        
//        if (netTime1 == 0 && netTime1 == 0) {
//            netTimeMap.put(category.getAssignedMark(), TimeTouple.ZERO);
//            timeMap.put(category.getAssignedMark(), TimeTouple.ZERO);
//
//            return TimeTouple.ZERO;
//        }
//
//        category.accept(new Visitor<Category, Void, CategoryDefinitionProcessor>() {
//            public Void visit(Category visitedCategory) {
//                accuTime0[0] += tc.getNetTime0(visitedCategory.getAssignedMark());
//                accuTime1[0] += tc.getNetTime1(visitedCategory.getAssignedMark());
//            }
//            
//        }, null);
//
//
//        TimeTouple netTime = new TimeTouple(netTime0, netTime1);
//        TimeTouple accTime = new TimeTouple(accuTime0[0], accuTime1[0]);
//        netTimeMap.put(category.getAssignedMark(), netTime);
//        timeMap.put(category.getAssignedMark(), accTime);
//
//        return accTime;
//        return null;
//    }

    private void clearTimeMaps() {
        netTimeMap.clear();
        timeMap.clear();
    }

    private void fireDataChange() {
        Set tmpListeners = new HashSet(listeners);

        for (Iterator iter = tmpListeners.iterator(); iter.hasNext();) {
            ((DrillDownListener) iter.next()).dataChanged();
        }
    }

    private void fireDrillDownChange() {
        Set tmpListeners = new HashSet(listeners);

        for (Iterator iter = tmpListeners.iterator(); iter.hasNext();) {
            ((DrillDownListener) iter.next()).drillDownPathChanged(getDrillDownPath());
        }
    }
}
