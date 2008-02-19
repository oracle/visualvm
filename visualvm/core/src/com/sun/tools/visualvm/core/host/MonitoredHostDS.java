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

package com.sun.tools.visualvm.core.host;

import com.sun.tools.visualvm.core.datasource.AbstractDataSource;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Host;
import java.net.URISyntaxException;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;

/**
 *
 * @author Jiri Sedlacek
 */
public class MonitoredHostDS extends AbstractDataSource {

    private final MonitoredHost monitoredHost;
    private final Host host;


    MonitoredHostDS(Host host) throws MonitorException, URISyntaxException {
        this.host = host;
        monitoredHost = MonitoredHost.getMonitoredHost(host.getHostName());
        setVisible(false);
    }

    public static boolean isAvailableFor(Host host) {
        try {
            MonitoredHost.getMonitoredHost(host.getHostName());
            return true;
        } catch (Exception e) {}
        return false;
    }
    
    public MonitoredHost getMonitoredHost() {
        return monitoredHost;
    }

    void finished() {
        setState(DataSource.STATE_FINISHED);
    }


    public Host getHost() {
        return host;
    }

}
