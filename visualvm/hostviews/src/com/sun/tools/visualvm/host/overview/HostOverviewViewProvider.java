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

package com.sun.tools.visualvm.host.overview;

import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.host.model.HostOverviewFactory;
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
public class HostOverviewViewProvider implements DataSourceViewsProvider<Host>{
    
    private final Map<Host, DataSourceView> viewsCache = new HashMap();
    

    public boolean supportsViewsFor(Host host) {
        return HostOverviewFactory.getSystemOverviewFor(host) != null;
    }

    public synchronized Set<? extends DataSourceView> getViews(final Host host) {
        DataSourceView view = viewsCache.get(host);
        if (view == null) {
            view = new HostOverviewView(host) {
                public void removed() {
                    super.removed();
                    viewsCache.remove(host);
                }
            };
            viewsCache.put(host, view);
        }
        return Collections.singleton(view);
    }

    public boolean supportsSaveViewsFor(Host dataSource) {
        return false;
    }
    
    public void saveViews(Host dataSource, Snapshot snapshot) {
        
    }
    

    public void initialize() {
        DataSourceViewsManager.sharedInstance().addViewsProvider(this, Host.class);
    }

}
