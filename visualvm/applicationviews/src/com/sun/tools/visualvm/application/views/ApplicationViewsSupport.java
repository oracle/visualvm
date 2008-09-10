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

package com.sun.tools.visualvm.application.views;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.snapshot.ApplicationSnapshot;
import com.sun.tools.visualvm.application.views.monitor.ApplicationMonitorViewProvider;
import com.sun.tools.visualvm.application.views.overview.ApplicationOverviewViewProvider;
import com.sun.tools.visualvm.application.views.overview.ApplicationSnapshotOverviewViewProvider;
import com.sun.tools.visualvm.application.views.threads.ApplicationThreadsViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.PluggableDataSourceViewProvider;

/**
 * Support for built-in application views in VisualVM.
 * Currently publishes Overview, Monitor and Threads subtabs for Application and
 * Overview subtab for ApplicationSnapshot.
 *
 * @author Jiri Sedlacek
 */
public final class ApplicationViewsSupport {
    
    private static ApplicationViewsSupport sharedInstance;
    
    private ApplicationSnapshotOverviewViewProvider applicationSnapshotOverviewView = new ApplicationSnapshotOverviewViewProvider();
    private ApplicationOverviewViewProvider overviewPluggableView = new ApplicationOverviewViewProvider();
    private ApplicationMonitorViewProvider monitorPluggableView = new ApplicationMonitorViewProvider();
    private ApplicationThreadsViewProvider threadsPluggableView = new ApplicationThreadsViewProvider();
    
    
    /**
     * Returns singleton instance of ApplicationViewsSupport.
     * 
     * @return singleton instance of ApplicationViewsSupport.
     */
    public static synchronized ApplicationViewsSupport sharedInstance() {
        if (sharedInstance == null) sharedInstance = new ApplicationViewsSupport();
        return sharedInstance;
    }
    
    
    /**
     * Returns PluggableDataSourceViewProvider for Overview application subtab.
     * 
     * @return PluggableDataSourceViewProvider for Overview application subtab.
     */
    public PluggableDataSourceViewProvider<Application> getOverviewView() {
        return overviewPluggableView;
    }
    
    /**
     * Returns PluggableDataSourceViewProvider for Overview application snapshot subtab.
     * 
     * @return PluggableDataSourceViewProvider for Overview application snapshot subtab.
     */
    public PluggableDataSourceViewProvider<ApplicationSnapshot> getSnapshotOverviewView() {
        return applicationSnapshotOverviewView;
    }
    
    /**
     * Returns PluggableDataSourceViewProvider for Monitor application subtab.
     * 
     * @return PluggableDataSourceViewProvider for Monitor application subtab.
     */
    public PluggableDataSourceViewProvider getMonitorView() {
        return monitorPluggableView;
    }
    
    /**
     * Returns PluggableDataSourceViewProvider for Threads application subtab.
     * 
     * @return PluggableDataSourceViewProvider for Threads application subtab.
     */
    public PluggableDataSourceViewProvider getThreadsView() {
        return threadsPluggableView;
    }
    
    
    private ApplicationViewsSupport() {
        DataSourceViewsManager.sharedInstance().addViewProvider(overviewPluggableView, Application.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(applicationSnapshotOverviewView, ApplicationSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(monitorPluggableView, Application.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(threadsPluggableView, Application.class);
    }
    
}
