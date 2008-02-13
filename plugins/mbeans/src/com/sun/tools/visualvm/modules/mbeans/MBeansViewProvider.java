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

package com.sun.tools.visualvm.modules.mbeans;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import com.sun.tools.visualvm.core.model.jmx.JmxModelFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceWindowFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServerConnection;

/**
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
class MBeansViewProvider implements DataSourceViewProvider<Application> {

    private Map<Application, DataSourceView> viewsCache = new HashMap();

    private MBeansViewProvider() {
    }

    public boolean supportsViewFor(Application application) {
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        MBeanServerConnection mbsc = jmx.getMBeanServerConnection();
        return mbsc != null;
    }

    public synchronized Set<? extends DataSourceView> getViews(Application application) {
        DataSourceView view = viewsCache.get(application);
        if (view == null) {
            view = new MBeansView(application);
        }
        return Collections.singleton(view);
    }

    static void initialize() {
        DataSourceWindowFactory.sharedInstance().addViewProvider(new MBeansViewProvider(), Application.class);
    }
}
