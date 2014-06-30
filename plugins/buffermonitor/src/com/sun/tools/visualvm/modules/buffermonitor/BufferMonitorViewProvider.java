/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.buffermonitor;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import java.io.IOException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 *
 * @author Tomas Hurka
 */
public class BufferMonitorViewProvider extends DataSourceViewProvider<Application> {

    static final String DIRECT_BUFFER_NAME = "java.nio:type=BufferPool,name=direct";
    static final String MAPPED_BUFFER_NAME = "java.nio:type=BufferPool,name=mapped";
    
    protected boolean supportsViewFor(Application application) {
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        if (jmx != null && jmx.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
            MBeanServerConnection connection = jmx.getMBeanServerConnection();
            try {
                if (connection.isRegistered(new ObjectName(DIRECT_BUFFER_NAME))) {
                    return true;
                }
            } catch (MalformedObjectNameException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    protected DataSourceView createView(Application application) {
        return new BufferMonitorView(application);
    }

    public void initialize() {
        DataSourceViewsManager.sharedInstance().addViewProvider(this, Application.class);
    }
}
