/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.ui;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.util.Set;

/**
 * Definition of a plugin which can modify a PluggableView.
 *
 * @author Jiri Sedlacek
 */
public interface ViewPlugin<X extends DataSource> {

    /**
     * Returns set of requested areas to be created for in PluggableView for given DataSource.
     * 
     * @param dataSource DataSource for which to create the areas.
     * @return set of requested areas to be created in PluggableView for given DataSource.
     */
    public Set<? extends AreaDescriptor> getAreasFor(X dataSource);

    /**
     * Returns set of views to be added to PluggableView for given DataSource.
     * 
     * @param dataSource DataSource for which to add the views.
     * @return set of views to be added to PluggableView for given DataSource.
     */
    public Set<? extends ViewDescriptor> getViewsFor(X dataSource);


    /**
     * Descriptor of the Area to be created in PluggableView.
     */
    public static interface AreaDescriptor {
        /**
         * Returns area to be created in PluggableView.
         * 
         * @return area to be created in PluggableView.
         */
        public DataViewComponent.DetailsAreaConfiguration getArea();
        /**
         * Returns location of the area to be created in PluggableView.
         * 
         * @return location of the area to be created in PluggableView.
         */
        public int getLocation();
    }

    /**
     * Descriptor of the view to be added to PluggableView.
     */
    public static interface ViewDescriptor {
        /**
         * Returns view to be added to PluggableView.
         * 
         * @return view to be added to PluggableView.
         */
        public DataViewComponent.DetailsView getView();
        /**
         * Returns location of the view to be added to PluggableView.
         * 
         * @return location of the view to be added to PluggableView.
         */
        public int getLocation();
        
        public int getPreferredPosition();
    }

}
