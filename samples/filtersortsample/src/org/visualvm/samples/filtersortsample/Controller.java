/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.visualvm.samples.filtersortsample;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.ui.actions.DataSourceAction;
import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public class Controller {
    
    private static Controller instance;
    
    private ShowHideDSAction showHideDSAction;
    private boolean dataSourcesHidden;
    
    private SortDSAction sortDSAction;
    private boolean currentSortOrder;
    
    
    public static synchronized Controller instance() {
        if (instance == null) instance = new Controller();
        return instance;
    }
    
    public static DataSourceAction showHideDSAction() { return instance().showHideAction(); }
    
    public static DataSourceAction sortDSAction() { return instance().sortAction(); }

    
    private synchronized DataSourceAction showHideAction() {
        if (showHideDSAction == null) showHideDSAction = new ShowHideDSAction();
        return showHideDSAction;
    }
    
    private synchronized DataSourceAction sortAction() {
        if (sortDSAction == null) sortDSAction = new SortDSAction();
        return sortDSAction;
    }
    
    
    private static void filterDataSources(boolean visible) {
        Set<DataSource> dataSources = DataSource.ROOT.getRepository().getDataSources();
        for (DataSource dataSource : dataSources)
            if (!(dataSource instanceof FilterSortRootNode))
                dataSource.setVisible(visible);
    }
    
    private static void sortDataSources(final boolean sortOrder) {
        Set<DataSource> dataSources = FilterSortRootNode.sharedInstance().getRepository().getDataSources();
        List<FilterSortNode.FilterSortDescriptorProvider.FilterSortDescriptor> descriptors = new ArrayList(dataSources.size());
        for (DataSource dataSource : dataSources)
            descriptors.add((FilterSortNode.FilterSortDescriptorProvider.FilterSortDescriptor)DataSourceDescriptorFactory.getDescriptor(dataSource));
        Collections.sort(descriptors, new Comparator<DataSourceDescriptor>() {
            public int compare(DataSourceDescriptor dsd1, DataSourceDescriptor dsd2) {
                String name1 = dsd1.getName();
                Integer int1 = Integer.decode(name1.substring(name1.lastIndexOf("@") + 1));
                String name2 = dsd2.getName();
                Integer int2 = Integer.decode(name2.substring(name1.lastIndexOf("@") + 1));
                return sortOrder ? int2.compareTo(int1) : int1.compareTo(int2);
            }
        });
        for (int i = 0; i < descriptors.size(); i++) descriptors.get(i).changePreferredPosition(i);
    }
    
    
    
    private class ShowHideDSAction extends SingleDataSourceAction<FilterSortRootNode> {

        protected void actionPerformed(FilterSortRootNode arg0, ActionEvent arg1) {
            filterDataSources(dataSourcesHidden);
            dataSourcesHidden = !dataSourcesHidden;
            updateName();
        }

        protected boolean isEnabled(FilterSortRootNode arg0) { return true; }
        
        
        private void updateName() {
            putValue(NAME, dataSourcesHidden ? "Reset Filter" : "Filter Out DataSources");
        }


        ShowHideDSAction() {
            super(FilterSortRootNode.class);
            updateName();
        }

    }
    
    private class SortDSAction extends SingleDataSourceAction<FilterSortRootNode> {

        protected void actionPerformed(FilterSortRootNode arg0, ActionEvent arg1) {
            sortDataSources(currentSortOrder);
            currentSortOrder = !currentSortOrder;
            updateName();
        }

        protected boolean isEnabled(FilterSortRootNode arg0) { return true; }
        
        
        private void updateName() {
            putValue(NAME, currentSortOrder ? "Sort Descending" : "Sort Ascending");
        }


        SortDSAction() {
            super(FilterSortRootNode.class);
            updateName();
        }

    }

}
