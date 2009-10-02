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

package org.netbeans.modules.profiler.ui.stats.drilldown;

import org.netbeans.lib.profiler.ui.charts.AbstractPieChartModel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.netbeans.modules.profiler.categories.Category;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class DrillDownPieChartModel extends AbstractPieChartModel implements DrillDownListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    final static protected Logger LOGGER = Logger.getLogger(DrillDownPieChartModel.class.getName());
    
    private static Color[] COLORS = new Color[] {
                                        new Color(0x99ff99), new Color(0x99cc99), new Color(0x666633), new Color(0x336666),
                                        new Color(0x6699cc), new Color(0x9999cc), new Color(0xffccff), new Color(0xcc9999),
                                        new Color(0x660099), new Color(0x006600)
                                    };

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected DrillDown drillDown;
    private Object itemMapLock = new Object();
    private int[] itemMap = null;
    private int itemCount = 0;
    // @GuardedBy itemMapLock
    private List<Category> subCategories;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of DrillDownPieChartModel */
    public DrillDownPieChartModel(DrillDown model) {
        setDrillDown(model);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setDrillDown(DrillDown model) {
        if (drillDown != null) {
            drillDown.removeListener(this);
        }

        drillDown = model;
        synchronized(itemMapLock) {
            updateItemMap();
        }
        drillDown.addListener(this);
    }

    @Override
    public Color getItemColor(int index) {
        if (index == -1) {
            return Color.BLACK; // illegal index
        }

        return COLORS[index % COLORS.length];
    }

    @Override
    public int getItemCount() {
        synchronized (itemMapLock) {
            return itemCount;
        }
    }

    public String getItemDescription(int index) {
        if ((index == -1) || (getMappedIndex(index) == -1)) {
            return ""; // illegal index // NOI18N
        }

        synchronized(itemMapLock) {
            if (subCategories != null) {
                return subCategories.get(getMappedIndex(index)).getLabel();
            } else {
                return "";
            }
        }
    }

    public void dataChanged() {
        synchronized (itemMapLock) {
            updateItemMap();
        }

        fireChartDataChanged();
    }

    public void drillDownPathChanged(List newDrillDownPath) {
        synchronized (itemMapLock) {
            updateItemMap();
        }
        
        fireChartDataChanged();
    }

    public void drilldown(int index) {
        if ((index == -1) || (getMappedIndex(index) == -1)) {
            return; // illegal index
        }
        
        Category toDrill = null;
        synchronized(itemMapLock) {
            if (subCategories == null || subCategories.isEmpty()) {
                return;
            }
            toDrill = subCategories.get(getMappedIndex(index));
        }

        if (toDrill != null) {
            drillDown.drilldown(toDrill.getId());
        }
    }

    public void drillup() {
        drillDown.drillup();
    }

    public void drillup(int index) {
        if ((index == -1) || (getMappedIndex(index) == -1)) {
            return; // illegal index
        }

        Category toDrill = null;
        synchronized(itemMapLock) {
            if (subCategories == null || subCategories.isEmpty()) {
                return;
            }
            toDrill = subCategories.get(getMappedIndex(index));
        }
        
        drillDown.drillup(toDrill.getId());
    }

    @Override
    public boolean hasData() {
        synchronized (itemMapLock) {
            return (drillDown != null) && (itemCount > 0);
        }
    }

    protected List<Category> getSubCategories() {
        List<Category> cats = new ArrayList<Category>();
        synchronized(itemMapLock) {
            if (subCategories != null) {
                cats.addAll(subCategories);
            }
        }
        return cats;
    }
    
    protected Category getCategoryAt(int index) {
        synchronized(itemMapLock) {
            if (subCategories == null || subCategories.isEmpty()) {
                return Category.DEFAULT;
            }
            return subCategories.get(getMappedIndex(index));
        }
    }
    
    protected int getMappedIndex(int index) {
        synchronized (itemMapLock) {
            if ((index < 0) || (index >= itemMap.length)) {
                return -1; // check for boundaries
            }

            return itemMap[index];
        }
    }

    private void updateItemMap() {
        synchronized (itemMapLock) {
            subCategories = drillDown.getSubCategories();
            if (subCategories == null || subCategories.isEmpty()) return;
            int counter = 0;
            int mapCounter = 0;
            int[] map = new int[subCategories.size()];

            for (Category category : subCategories) {
                if (drillDown.getCategoryTime(category, false) > 0) {
                    map[mapCounter++] = counter;
                }
                counter++;
            }

            itemCount = (mapCounter > 0) ? mapCounter : 0;
            itemMap = map;
        }
    }
}
