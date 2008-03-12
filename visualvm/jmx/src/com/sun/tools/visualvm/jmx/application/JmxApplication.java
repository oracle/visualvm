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

import com.sun.tools.visualvm.application.AbstractApplication;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.core.datasupport.Storage;
import com.sun.tools.visualvm.jmx.model.JvmJmxModel;
import com.sun.tools.visualvm.jmx.model.JvmJmxModelFactory;
import java.lang.management.RuntimeMXBean;
import javax.management.remote.JMXServiceURL;

/**
 * This type of application represents an application
 * that is built from a {@link JMXServiceURL}.
 * 
 * @author Luis-Miguel Alventosa
 */
public final class JmxApplication extends AbstractApplication {
    
    private int pid = -1;
    private final JMXServiceURL url;
    private final Storage storage;

    public JmxApplication(Host host, JMXServiceURL url, Storage storage) {
        super(host, url.toString());
        this.url = url;
        this.storage = storage;
    }

    public JMXServiceURL getJMXServiceURL() {
        return url;
    }

    @Override
    public int getPid() {
        if (pid == -1) {
            JvmJmxModel jmxModel = JvmJmxModelFactory.getJvmJmxModelFor(this);
            if (jmxModel != null) {
                RuntimeMXBean rt = jmxModel.getRuntimeMXBean();
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
    
    
    protected Storage createStorage() {
        return storage;
    }
    
    
    void finished() {
        setState(DataSource.STATE_FINISHED);
    }
}
