/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application;

import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.properties.PropertiesSupport;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.host.LocalHostDescriptor;
import java.lang.management.ManagementFactory;
import org.graalvm.visualvm.core.VisualVM;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
final class ApplicationSupport {

    private static ApplicationSupport instance;

    public static synchronized ApplicationSupport getInstance() {
        if (instance == null) instance = new ApplicationSupport();
        return instance;
    }
    
    Application createCurrentApplication() {
        String selfName = ManagementFactory.getRuntimeMXBean().getName();
        final Integer selfPid = Integer.valueOf(selfName.substring(0, selfName.indexOf('@')));
        return new CurrentApplication(selfPid, Host.LOCALHOST, Host.LOCALHOST.getHostName() + "-" + selfPid);
    }
    
    private void initCurrentApplication() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                VisualVM.getInstance().runTask(new Runnable() {
                    public void run() {
                        // Initialize sorting
                        DataSourceDescriptor localHostDescriptor =
                                DataSourceDescriptorFactory.getDescriptor(Host.LOCALHOST);
                        if (localHostDescriptor instanceof LocalHostDescriptor) {
                            ((LocalHostDescriptor)localHostDescriptor).setChildrenComparator(
                                    ApplicationsSorting.instance().getInitialSorting());
                        }
                        
                        Host.LOCALHOST.getRepository().addDataSource(Application.CURRENT_APPLICATION);
                    }
                });
            }
        });
    }

    private ApplicationSupport() {
        DataSourceDescriptorFactory descriptorFactory = DataSourceDescriptorFactory.getDefault();
        descriptorFactory.registerProvider(new ApplicationDescriptorProvider());
        PropertiesSupport.sharedInstance().registerPropertiesProvider(
                new GeneralPropertiesProvider(), CurrentApplication.class);
        initCurrentApplication();
    }

    class CurrentApplication extends Application {

        private int selfPid;
        
        private CurrentApplication(int selfPid, Host host, String id) {
            super(host, id);
            this.selfPid = selfPid;
        }

        public int getPid() {
            return selfPid;
        }
    }
}
