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

package com.sun.tools.visualvm.jmx.application;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
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
 */
public final class JmxApplication extends Application {
    
    private int pid;
    private final JMXServiceURL url;
    private final String username;
    private final String password;
    private final boolean saveCredentials;
    private final Storage storage;
    // since getting JVM for the first time can take a long time
    // hard reference jvm from application so we are sure that it is not garbage collected
    Jvm jvm;

    public JmxApplication(Host host, JMXServiceURL url, String username,
            String password, boolean saveCredentials, Storage storage) {
        super(host, url.toString() + (username == null || username.isEmpty() ? "" : " (" + username + ")"));
        pid = UNKNOWN_PID;
        this.url = url;
        this.username = username;
        this.password = password;
        this.saveCredentials = saveCredentials;
        this.storage = storage;
    }

    public JMXServiceURL getJMXServiceURL() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean getSaveCredentialsFlag() {
        return saveCredentials;
    }

    @Override
    public int getPid() {
        if (pid == UNKNOWN_PID) {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(this);
            if (jmxModel != null) {
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                RuntimeMXBean rt = mxbeans.getRuntimeMXBean();
                if (rt != null) {
                    String name = rt.getName();
                    if (name != null && name.indexOf("@") != -1) {
                        name = name.substring(0, name.indexOf("@"));
                        pid = Integer.parseInt(name);
                    }
                }
            }
        }
        return pid;
    }

    @Override
    public boolean supportsUserRemove() {
        return true;
    }
    
    @Override
    protected Storage createStorage() {
        return storage;
    }

}
