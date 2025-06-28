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

package org.graalvm.visualvm.core.ui;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;

/**
 * Plugin to an existing DataSourceView which adds an additional view.
 *
 * @author Jiri Sedlacek
 */
public abstract class DataSourceViewPlugin {
    
    private DataSource dataSource;
    private DataSourceViewPluginProvider controller;
    
    
    /**
     * Creates new DataSourceViewPlugin for a DataSource.
     * 
     * @param dataSource DataSource, for which to add the plugin.
     */
    public DataSourceViewPlugin(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    
    /**
     * Returns DataSource for the plugin.
     * 
     * @return DataSource for the plugin.
     */
    public final DataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Creates DataViewComponent.DetailsView which will be plugged into the DataSourceView.
     * 
     * @param location location where the DataViewComponent.DetailsView will be added.
     * @return DataViewComponent.DetailsView of the plugin for the location or null.
     */
    public abstract DataViewComponent.DetailsView createView(int location);
    

    /**
     * Notification when the view is about to be added to DataSourceWindow.
     * This notification comes from a thread other than EDT and its main intention
     * is to provide a possibility to do some models inits before the actual UI is displayed.
     * This call is blocking (blocks opening the view, progress bar is shown) but long-running initializations should
     * still use separate thread and update the UI after the models are ready.
     */
    protected void willBeAdded() {
    }
    
    /**
     * Notification when the view has been added to DataSourceWindow.
     * This notification comes from a thread other than EDT.
     */
    protected void added() {
    }
    
    /**
     * Notification when the view has been either programatically removed from tabbed pane or closed by the user by clicking the X.
     * This notification comes from a thread other than EDT
     */
    protected void removed() {
    }
    
    
    void pluginWillBeAdded() {
        willBeAdded();
        controller.pluginWillBeAdded(this);
    }
    
    void pluginAdded() {
        added();
        controller.pluginAdded(this);
    }
    
    void pluginRemoved() {
        removed();
        controller.pluginRemoved(this);
    }
    
    void setController(DataSourceViewPluginProvider controller) {
        this.controller = controller;
    }

}
