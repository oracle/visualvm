/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.visualvm.samples.filtersortsample;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Positionable;
import org.graalvm.visualvm.core.model.AbstractModelProvider;
import java.awt.Image;
import org.openide.util.Utilities;

/**
 * 
 * @author Jiri Sedlacek
 */
public class FilterSortNode extends DataSource {
    
    private static FilterSortDescriptorProvider descriptorProvider;
    
    
    private FilterSortNode() {}
    
    
    static synchronized void initialize() {
        if (descriptorProvider != null) return;
        descriptorProvider = new FilterSortDescriptorProvider();
        DataSourceDescriptorFactory.getDefault().registerProvider(descriptorProvider);
        for (int i = 0; i < 10; i++)
            FilterSortRootNode.sharedInstance().getRepository().addDataSource(new FilterSortNode());
    }
    
    static synchronized void uninitialize() {
        if (descriptorProvider == null) return;
        FilterSortRootNode.sharedInstance().getRepository().removeDataSources(
                FilterSortRootNode.sharedInstance().getRepository().getDataSources());
        DataSourceDescriptorFactory.getDefault().unregisterProvider(descriptorProvider);
        descriptorProvider = null;
    }
    
    
    static class FilterSortDescriptorProvider extends AbstractModelProvider<DataSourceDescriptor, DataSource> {
    
        public DataSourceDescriptor createModelFor(DataSource ds) {
            if (ds instanceof FilterSortNode) return new FilterSortDescriptor((FilterSortNode)ds);
            else return null;
        }

        static class FilterSortDescriptor extends DataSourceDescriptor {
            private static final Image NODE_ICON =
                    Utilities.loadImage("org/graalvm/visualvm/core/ui/resources/busy-icon5.png", true);  // NOI18N

            FilterSortDescriptor(FilterSortNode node) {
                super(node, "FilterSortDemo @" + node.hashCode(), null, NODE_ICON,
                        Positionable.POSITION_LAST, EXPAND_ON_EACH_NEW_CHILD); // NOI18N
            }
            
            void changePreferredPosition(int position) {
                setPreferredPosition(position);
            }

        }

    }
    
}
