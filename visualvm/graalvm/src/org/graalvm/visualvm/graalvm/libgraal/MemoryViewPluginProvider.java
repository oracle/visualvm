/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.graalvm.libgraal;

import java.lang.management.ManagementFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.snapshot.ApplicationSnapshot;
import org.graalvm.visualvm.application.views.ApplicationViewsSupport;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.ui.DataSourceViewPlugin;
import org.graalvm.visualvm.core.ui.DataSourceViewPluginProvider;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;

/**
 *
 * @author Tomas Hurka
 */
public class MemoryViewPluginProvider extends DataSourceViewPluginProvider<Application> {

    private static final String JVMCINativeLibraryFlag = "UseJVMCINativeLibrary";    // NOI18N
    static final String LIBGRAAL_HEAP = "Libgraal";  // NOI18N

    private final ObjectName libgraalName;

    @Override
    protected DataSourceViewPlugin createPlugin(Application application) {
        return new MemoryViewPlugin(application, MemoryModel.create(application, LIBGRAAL_HEAP, libgraalName));
    }

    @Override
    protected boolean supportsPluginFor(Application t) {
        return isSupported(t);
    }

    @Override
    protected boolean supportsSavePluginFor(Application application, Class<? extends Snapshot> snapshotClass) {
        return ApplicationSnapshot.class.isAssignableFrom(snapshotClass);
    }

    @Override
    protected void savePlugin(Application application, Snapshot snapshot) {
        MemoryViewPlugin view = (MemoryViewPlugin) getCachedPlugin(application);
        if (view != null) {
            view.getModel().save(snapshot);
        } else {
            MemoryModel.create(application, LIBGRAAL_HEAP).save(snapshot);
        }
    }

    private MemoryViewPluginProvider() {
        libgraalName = getLibgraalName();
    }

    public static void initialize() {
        ApplicationViewsSupport.sharedInstance().getMonitorView().
                registerPluginProvider(new MemoryViewPluginProvider());
    }

    private static ObjectName getLibgraalName() {
        try {
            return new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",name=" + LIBGRAAL_HEAP);
        } catch (MalformedObjectNameException ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean isSupported(Application app) {
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
        if (jmxModel != null) {
            String val = jmxModel.getFlagValue(JVMCINativeLibraryFlag);
            return Boolean.valueOf(val);
        }
        return false;
    }
}
