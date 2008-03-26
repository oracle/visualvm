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

import com.sun.tools.visualvm.application.ApplicationSnapshot;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public class ApplicationSnapshotOverviewViewProvider implements DataSourceViewsProvider<ApplicationSnapshot> {
    
    private final Map<ApplicationSnapshot, ApplicationOverviewView> viewsCache = new HashMap();
    

    public boolean supportsViewsFor(ApplicationSnapshot snapshot) {
        return snapshot.getStorage().getCustomProperty(ApplicationOverviewModel.SNAPSHOT_VERSION) != null;
    }

    public Set<? extends DataSourceView> getViews(final ApplicationSnapshot snapshot) {
        synchronized(viewsCache) {
            ApplicationOverviewView view = viewsCache.get(snapshot);
            if (view == null) {
                view = new ApplicationOverviewView(snapshot, ApplicationOverviewModel.create(snapshot)) {
                    public void removed() {
                        super.removed();
                        viewsCache.remove(snapshot);
                    }
                };
                viewsCache.put(snapshot, view);
            }
            return Collections.singleton(view);
        }
    }
    
    public boolean supportsSaveViewsFor(ApplicationSnapshot snapshot) {
        return false;
    }
    
    public void saveViews(ApplicationSnapshot appSnapshot, Snapshot snapshot) {
        throw new UnsupportedOperationException("Cannot save snapshot views");
    }
    

    public void initialize() {
        DataSourceViewsManager.sharedInstance().addViewProvider(this, ApplicationSnapshot.class);
    }

}
