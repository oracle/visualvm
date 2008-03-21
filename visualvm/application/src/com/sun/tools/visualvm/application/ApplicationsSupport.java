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

package com.sun.tools.visualvm.application;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.host.Host;
import java.lang.management.ManagementFactory;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ApplicationsSupport {

    private static ApplicationsSupport instance;

    public static synchronized ApplicationsSupport getInstance() {
        if (instance == null) {
            instance = new ApplicationsSupport();
        }
        return instance;
    }
    
    Application createCurrentApplication() {
        String selfName = ManagementFactory.getRuntimeMXBean().getName();
        final int selfPid = Integer.valueOf(selfName.substring(0, selfName.indexOf('@')));
        Application currentApplication = new Application(Host.LOCALHOST, Host.LOCALHOST.getHostName() + "-" + selfPid) {
            public int getPid() { return selfPid; }
        };
        
        return currentApplication;
    }
    
    private void initCurrentApplication() {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                Host.LOCALHOST.getRepository().addDataSource(Application.CURRENT_APPLICATION);
            }
        });
    }

    private ApplicationsSupport() {
        DataSourceDescriptorFactory.getDefault().registerFactory(new ApplicationDescriptorProvider());
        ApplicationActionsProvider.initialize();
        initCurrentApplication();
    }
}
