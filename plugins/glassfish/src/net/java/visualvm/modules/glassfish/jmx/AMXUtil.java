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

package net.java.visualvm.modules.glassfish.jmx;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.SystemInfo;
import com.sun.appserv.management.client.ProxyFactory;
import com.sun.appserv.management.monitor.MonitoringRoot;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import javax.management.MBeanServerConnection;

/**
 *
 * @author Jaroslav Bachorik
 */
public class AMXUtil {
    private static ProxyFactory amxProxyFactory = null;
    private static DomainRoot domainRoot = null;
    private static MonitoringRoot monitoringRoot = null;

    public static MonitoringRoot getMonitoringRoot(MBeanServerConnection connection) throws Exception {
        DomainRoot dr = getDomainRoot(connection);
        if (dr == null) return null;
        return monitoringRoot ==  null ? dr.getMonitoringRoot() : monitoringRoot;
    }

    public static DomainRoot getDomainRoot(JmxModel model) {
        try {
            return getDomainRoot(model.getMBeanServerConnection());
        } catch (Exception e) {
            return null;
        }
    }
    
    public static DomainRoot getDomainRoot(MBeanServerConnection connection) throws Exception {
        try {
            return domainRoot == null ? getAMXProxyFactory(connection).getDomainRoot() : domainRoot;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static ProxyFactory getAMXProxyFactory(MBeanServerConnection connection) throws Exception {
        return amxProxyFactory == null ? 
        ProxyFactory.getInstance(connection) : amxProxyFactory;
    }
       
    public static boolean isEE(DomainRoot dr) {
        SystemInfo systemInfo = dr.getSystemInfo();
        return systemInfo.supportsFeature(SystemInfo.CLUSTERS_FEATURE);
    }
}
