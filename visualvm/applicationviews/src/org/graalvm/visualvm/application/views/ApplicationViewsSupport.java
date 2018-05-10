/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.views;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.snapshot.ApplicationSnapshot;
import org.graalvm.visualvm.application.views.monitor.ApplicationMonitorViewProvider;
import org.graalvm.visualvm.application.views.monitor.ApplicationSnapshotMonitorViewProvider;
import org.graalvm.visualvm.application.views.overview.ApplicationOverviewViewProvider;
import org.graalvm.visualvm.application.views.overview.ApplicationSnapshotOverviewViewProvider;
import org.graalvm.visualvm.application.views.threads.ApplicationSnapshotThreadsViewProvider;
import org.graalvm.visualvm.application.views.threads.ApplicationThreadsViewProvider;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;

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
    private ApplicationSnapshotMonitorViewProvider applicationSnapshotMonitorView = new ApplicationSnapshotMonitorViewProvider();
    private ApplicationMonitorViewProvider monitorPluggableView = new ApplicationMonitorViewProvider();
    private ApplicationSnapshotThreadsViewProvider applicationSnapshotThreadsView = new ApplicationSnapshotThreadsViewProvider();
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
     * Returns PluggableDataSourceViewProvider for Monitor application snapshot subtab.
     *
     * @return PluggableDataSourceViewProvider for Monitor application snapshot subtab.
     */
    public PluggableDataSourceViewProvider<ApplicationSnapshot> getSnapshotMonitorView() {
        return applicationSnapshotMonitorView;
    }
    
    /**
     * Returns PluggableDataSourceViewProvider for Threads application subtab.
     * 
     * @return PluggableDataSourceViewProvider for Threads application subtab.
     */
    public PluggableDataSourceViewProvider getThreadsView() {
        return threadsPluggableView;
    }

    /**
     * Returns PluggableDataSourceViewProvider for Threads application snapshot subtab.
     *
     * @return PluggableDataSourceViewProvider for Threads application snapshot subtab.
     */
    public PluggableDataSourceViewProvider<ApplicationSnapshot> getSnapshotThreadsView() {
        return applicationSnapshotThreadsView;
    }
    
    
    private ApplicationViewsSupport() {
        DataSourceViewsManager.sharedInstance().addViewProvider(overviewPluggableView, Application.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(applicationSnapshotOverviewView, ApplicationSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(monitorPluggableView, Application.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(applicationSnapshotMonitorView, ApplicationSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(threadsPluggableView, Application.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(applicationSnapshotThreadsView, ApplicationSnapshot.class);
    }
    
}
