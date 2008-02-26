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

package com.sun.tools.visualvm.core.application;

import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasource.Host;
import java.util.HashSet;
import java.util.Set;
import javax.management.remote.JMXServiceURL;

/**
 * A provider for Applications added as JMX connections.
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
public class JmxApplicationProvider extends DefaultDataSourceProvider<JmxApplication> {

    public void processNewJmxApplication(Host host, JMXServiceURL url) {
        JmxApplication jmxapp = new JmxApplication(url);
        Set<JmxApplication> jmxapps = new HashSet<JmxApplication>();
        jmxapps.add(jmxapp);
        host.getRepository().addDataSources(jmxapps);
        registerDataSources(jmxapps);
    }

    void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
    }
}
