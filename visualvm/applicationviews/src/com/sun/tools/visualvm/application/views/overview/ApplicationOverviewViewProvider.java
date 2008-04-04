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

package com.sun.tools.visualvm.application.views.overview;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.views.ApplicationViewsSupport;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public class ApplicationOverviewViewProvider implements DataSourceViewsProvider<Application> {
    
    private final Map<Application, ApplicationOverviewView> viewsCache = new HashMap();
    

    public boolean supportsViewsFor(Application application) {
        return true;
    }

    public Set<? extends DataSourceView> getViews(final Application application) {
        synchronized(viewsCache) {
            ApplicationOverviewView view = viewsCache.get(application);
        if (view == null) {
                view = new ApplicationOverviewView(application, ApplicationOverviewModel.create(application)) {
                    DataViewComponent createViewComponent() {
                        DataViewComponent viewComponent = super.createViewComponent();
                        ApplicationOverviewPluggableView pluggableView = (ApplicationOverviewPluggableView)ApplicationViewsSupport.sharedInstance().getOverviewView();
                        pluggableView.makeCustomizations(viewComponent, application);
                        return viewComponent;
                    }
                public void removed() {
                    super.removed();
                    viewsCache.remove(application);
                }
            };
            viewsCache.put(application, view);
        }
        return Collections.singleton(view);
    }
    }

    public boolean supportsSaveViewsFor(Application dataSource) {
        return true;
    }
    
    public void saveViews(Application dataSource, Snapshot snapshot) {
        synchronized(viewsCache) {
            ApplicationOverviewView view = viewsCache.get(dataSource);
            if (view != null) view.getModel().save(snapshot);
            else ApplicationOverviewModel.create(dataSource).save(snapshot);
        }
    }
    

    public void initialize() {
        DataSourceViewsManager.sharedInstance().addViewsProvider(this, Application.class);
    }

}
