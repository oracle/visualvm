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

package org.graalvm.visualvm.application.views.overview;

import java.util.Set;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.snapshot.ApplicationSnapshot;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public class ApplicationOverviewViewProvider extends PluggableDataSourceViewProvider<Application> {
    
    protected boolean supportsViewFor(Application application) {
        return true;
    }

    protected DataSourceView createView(Application application) {
        return new ApplicationOverviewView(application, ApplicationOverviewModel.create(application));
    }
    
    public Set<Integer> getPluggableLocations(DataSourceView view) {
        return ALL_LOCATIONS;
    }

    protected boolean supportsSaveViewFor(Application application, Class<? extends Snapshot> snapshotClass) {
        return ApplicationSnapshot.class.isAssignableFrom(snapshotClass);
    }
    
    protected void saveView(Application application, Snapshot snapshot) {
        ApplicationOverviewView view = (ApplicationOverviewView)getCachedView(application);
        if (view != null) view.getModel().save(snapshot);
        else ApplicationOverviewModel.create(application).save(snapshot);
    }

}
