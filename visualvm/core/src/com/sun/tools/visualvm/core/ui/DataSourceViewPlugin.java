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

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class DataSourceViewPlugin {
    
    private DataSource dataSource;
    private DataSourceViewPluginProvider controller;
    
    
    public DataSourceViewPlugin(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    
    public final DataSource getDataSource() {
        return dataSource;
    }
    
    public abstract DataViewComponent.DetailsView createView(int location);
    

    protected void willBeAdded() {
    }
    
    protected void added() {
    }
    
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
