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

package com.sun.tools.visualvm.jmx.impl;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.jmx.EnvironmentProvider;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import java.lang.management.RuntimeMXBean;
import javax.management.remote.JMXServiceURL;

/**
 * This type of application represents an application
 * that is built from a {@link JMXServiceURL}.
 * 
 * @author Luis-Miguel Alventosa
 * @author Jiri Sedlacek
 */
public final class JmxApplication extends Application {
    
    private int pid = UNKNOWN_PID;
    private final JMXServiceURL url;
    private EnvironmentProvider envProvider;
    private final Storage storage;
    // since getting JVM for the first time can take a long time
    // hard reference jvm from application so we are sure that it is not garbage collected
    public Jvm jvm;

    // Note: storage may be null, in this case the JmxApplication isn't persistent
    // and creates a temporary storage just like any other regular Application
    public JmxApplication(Host host, JMXServiceURL url, EnvironmentProvider envProvider, Storage storage) {
        super(host, createId(url, envProvider, storage));
        this.url = url;
        this.envProvider = envProvider;
        this.storage = storage;
    }


    public JMXServiceURL getJMXServiceURL() {
        return url;
    }

    public EnvironmentProvider getEnvironmentProvider() {
        return envProvider;
    }

    public int getPid() {
        if (pid == UNKNOWN_PID) {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(this);
            if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                if (mxbeans != null) {
                    RuntimeMXBean rt = mxbeans.getRuntimeMXBean();
                    if (rt != null) {
                        String name = rt.getName();
                        if (name != null && name.indexOf("@") != -1) { // NOI18N
                            name = name.substring(0, name.indexOf("@")); // NOI18N
                            pid = Integer.parseInt(name);
                        }
                    }
                }
            }
        }
        return pid;
    }


    public void setStateImpl(int newState) {
        if (newState != Stateful.STATE_AVAILABLE) {
            pid = UNKNOWN_PID;
            jvm = null;
        }
        setState(newState);
    }


    public boolean supportsUserRemove() {
        return true;
    }
    
    protected Storage createStorage() {
        return storage != null ? storage : super.createStorage();
    }
    
    protected void remove() {
        if (getStorage().directoryExists())
            Utils.delete(getStorage().getDirectory(), true);
    }

    public String toString() {
        return "JmxApplication [id: " + getId() + "]";   // NOI18N
    }

    private static String createId(JMXServiceURL url, EnvironmentProvider envProvider,
                                   Storage storage) {
        // url.toString will always be used
        String urlId = url.toString();
        
        // No envProvider -> return just url.toString()
        if (envProvider == null) return urlId;

        // No environmentID -> return just url.toString()
        String envId = envProvider.getEnvironmentId(storage);
        if (envId == null || "".equals(envId)) return urlId; // NOI18N

        // Defined environmentID -> use 'environmentID-url.toString()'
        // Typically 'username-service:jmx:rmi:///jndi/rmi://hostName:portNum/jmxrmi'
        return envId + "-" + urlId; // NOI18N
    }
}
