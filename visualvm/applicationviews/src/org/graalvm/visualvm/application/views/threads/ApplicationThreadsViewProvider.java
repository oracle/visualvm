/*
 * Copyright (c) 2007, 2024, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.views.threads;

import java.lang.management.ThreadMXBean;
import java.util.Set;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.snapshot.ApplicationSnapshot;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;

/**
 *
 * @author Jiri Sedlacek
 */
public class ApplicationThreadsViewProvider extends PluggableDataSourceViewProvider<Application> {

    protected boolean supportsViewFor(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE)
            return getCachedView(application) != null;
        return resolveThreads(application) != null;
    }

    protected DataSourceView createView(Application application) {
        return new ApplicationThreadsView(application);
    }
    
    public Set<Integer> getPluggableLocations(DataSourceView view) {
        return ALL_LOCATIONS;
    }

    protected boolean supportsSaveViewFor(Application application, Class<? extends Snapshot> snapshotClass) {
        return ApplicationSnapshot.class.isAssignableFrom(snapshotClass);
    }

    protected void saveView(Application application, Snapshot snapshot) {
        VisualVMThreadsDataManager tmanager = null;
        ApplicationThreadsView view = (ApplicationThreadsView)getCachedView(application);
        if (view != null) {
            tmanager = view.getDataManager();
        } else {
            ThreadMXBean tbean = resolveThreads(application);
            if (tbean != null) {
                tmanager = new ThreadMXBeanDataManager(tbean);
                ((ThreadMXBeanDataManager)tmanager).refreshThreadsSync();
//                try { Thread.sleep(50); } catch (Exception e) {} // Collect some data
//                ((ThreadMXBeanDataManager)tmanager).refreshThreadsSync();
            }
        }

        if (tmanager != null) PersistenceSupport.saveDataManager(tmanager, snapshot.getStorage());
    }

    static ThreadMXBean resolveThreads(Application application) {
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
            if (jmxModel.isTakeThreadDumpSupported()) {
                JvmMXBeans mxbeans = jmxModel.getJvmMXBeans();
                return mxbeans == null ? null : mxbeans.getThreadMXBean();
            }
        }
        return null;
    }
    
}
