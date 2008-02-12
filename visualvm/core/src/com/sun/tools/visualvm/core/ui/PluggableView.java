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

/**
 * Definition of a view which can be modified by ViewPlugins.
 *
 * @author Jiri Sedlacek
 */
public interface PluggableView<A extends DataSource> {

    /**
     * Registers new ViewPlugin which may modify the View.
     * 
     * @param plugin ViewPlugin to add.
     * @param scope scope of DataSource types for which the ViewPlugin can modify the view.
     */
    public <X extends A> void addPlugin(ViewPlugin<X> plugin, Class<X> scope);

    /**
     * Unregisters a ViewPlugin.
     * 
     * @param plugin ViewPlugin to remove.
     */
    public <X extends A> void removePlugin(ViewPlugin<X> plugin);

    /**
     * Returns true if the view allows defining new area of its DataViewComponent, false otherwise.
     * 
     * @param dataSource DataSource instance for which to check the new area,
     * @param location location of the area to check,
     * @return true if the view supports defining new area of its DataViewComponent, false otherwise.
     */
    public <X extends A> boolean allowsNewArea(X dataSource, int location);

    /**
     * Returns true if the view allows adding new view to given area, false otherwise.
     * 
     * @param dataSource DataSourceInstance for which to check the new view,
     * @param location location of the view to check,
     * @return true if the view allows adding new view to given area, false otherwise.
     */
    public <X extends A> boolean allowsNewView(X dataSource, int location);

}
