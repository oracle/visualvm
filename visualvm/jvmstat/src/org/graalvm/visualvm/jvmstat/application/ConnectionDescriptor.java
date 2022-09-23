/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jvmstat.application;

import java.net.URISyntaxException;
import java.rmi.registry.Registry;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.host.Host;
import org.openide.util.Exceptions;
import sun.jvmstat.monitor.HostIdentifier;

/**
 *
 * @author Jiri Sedlacek
 */
class ConnectionDescriptor {

    static final ConnectionDescriptor DEFAULT_LOCAL_DESCRIPTOR =
            new ConnectionDescriptor(-1, GlobalPreferences.sharedInstance().getMonitoredHostPoll());

    private int port;
    private double refreshRate;


    ConnectionDescriptor(int port, double refreshRate) {
        setPort(port);
        setRefreshRate(refreshRate);
    }


    static ConnectionDescriptor createDefault() {
        return new ConnectionDescriptor(Registry.REGISTRY_PORT, GlobalPreferences.sharedInstance().getMonitoredHostPoll());
    }


    final void setPort(int port) { this.port = port; }

    final int getPort() { return port; }

    final void setRefreshRate(double refreshRate) { this.refreshRate = refreshRate; }

    /**
     * monitored host refresh rate
     * @return refresh rate in seconds
     */ 
    final double getRefreshRate() { return refreshRate; }


    final HostIdentifier createHostIdentifier(Host host) {
        String hostId = null;
        if (this != DEFAULT_LOCAL_DESCRIPTOR) {
            hostId = "rmi://" + host.getHostName(); // NOI18N
            if (port != Registry.REGISTRY_PORT) hostId += ":" + port; // NOI18N
        }
        try {
            return new HostIdentifier(hostId);
        } catch (URISyntaxException e) {
            Exceptions.printStackTrace(e);
            return null;
        }
    }


    public boolean equals(Object o) {
        if (!(o instanceof ConnectionDescriptor)) return false;
        return port == ((ConnectionDescriptor)o).port;
    }

    public int hashCode() {
        return port;
    }

    public String toString() {
        return "Port: " + port + ", Refresh Rate: " + refreshRate; // NOI18N
    }

}
