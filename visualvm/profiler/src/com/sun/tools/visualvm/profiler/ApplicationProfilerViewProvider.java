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

package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.JVM;
import com.sun.tools.visualvm.application.JVMFactory;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationProfilerViewProvider implements DataSourceViewsProvider<Application>{
    
    private final Map<Application, DataSourceView> viewsCache = new HashMap();
    

    public boolean supportsViewsFor(Application application) {
        if (Application.CURRENT_APPLICATION.equals(application) || application.getHost() != Host.LOCALHOST) return false;
        JVM jvm = JVMFactory.getJVMFor(application);
        return jvm.isAttachable() && !jvm.is14() && !jvm.is15();
    }

    public synchronized Set<? extends DataSourceView> getViews(final Application application) {
        DataSourceView view = viewsCache.get(application);
        if (view == null) {
            view = new ApplicationProfilerView(application) {
                public void removed() {
                    super.removed();
                    viewsCache.remove(application);
                }
            };
            viewsCache.put(application, view);
        }
        return Collections.singleton(view);
    }

    void initialize() {
        DataSourceViewsFactory.sharedInstance().addViewProvider(this, Application.class);
    }

}
