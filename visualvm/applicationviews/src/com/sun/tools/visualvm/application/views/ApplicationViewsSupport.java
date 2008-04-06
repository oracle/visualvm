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
import com.sun.tools.visualvm.application.ApplicationSnapshot;
import com.sun.tools.visualvm.application.views.monitor.ApplicationMonitorViewProvider;
import com.sun.tools.visualvm.application.views.overview.ApplicationOverviewViewProvider;
import com.sun.tools.visualvm.application.views.overview.ApplicationSnapshotOverviewViewProvider;
import com.sun.tools.visualvm.application.views.threads.ApplicationThreadsViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.PluggableDataSourceViewProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ApplicationViewsSupport {
    
    private static ApplicationViewsSupport sharedInstance;
    
    private ApplicationSnapshotOverviewViewProvider applicationSnapshotOverviewView = new ApplicationSnapshotOverviewViewProvider();
    private ApplicationOverviewViewProvider overviewPluggableView = new ApplicationOverviewViewProvider();
    private ApplicationMonitorViewProvider monitorPluggableView = new ApplicationMonitorViewProvider();
    private ApplicationThreadsViewProvider threadsPluggableView = new ApplicationThreadsViewProvider();
    
    
    public static synchronized ApplicationViewsSupport sharedInstance() {
        if (sharedInstance == null) sharedInstance = new ApplicationViewsSupport();
        return sharedInstance;
    }
    
    
    public PluggableDataSourceViewProvider<Application> getOverviewView() {
        return overviewPluggableView;
    }
    
    public PluggableDataSourceViewProvider<ApplicationSnapshot> getSnapshotOverviewView() {
        return applicationSnapshotOverviewView;
    }
    
    public PluggableDataSourceViewProvider getMonitorView() {
        return monitorPluggableView;
    }
    
    public PluggableDataSourceViewProvider getThreadsView() {
        return threadsPluggableView;
    }
    
    
    private ApplicationViewsSupport() {
        DataSourceViewsManager.sharedInstance().addViewsProvider(overviewPluggableView, Application.class);
        DataSourceViewsManager.sharedInstance().addViewsProvider(applicationSnapshotOverviewView, ApplicationSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewsProvider(monitorPluggableView, Application.class);
        DataSourceViewsManager.sharedInstance().addViewsProvider(threadsPluggableView, Application.class);
    }
    
}
