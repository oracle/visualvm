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

package com.sun.tools.visualvm.jvmstat.application;

import com.sun.tools.visualvm.core.options.GlobalPreferences;
import java.rmi.registry.Registry;

/**
 *
 * @author Jiri Sedlacek
 */
class ConnectionDescriptor {

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


    public boolean equals(Object o) {
        if (!(o instanceof ConnectionDescriptor)) return false;
        return port == ((ConnectionDescriptor)o).port;
    }

    public int hashCode() {
        return port;
    }

    public String toString() {
        return "Port: " + port + "   Refresh Rate: " + refreshRate; // NOI18N
    }

}
