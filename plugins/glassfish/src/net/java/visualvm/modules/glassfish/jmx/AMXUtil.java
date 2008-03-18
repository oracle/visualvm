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
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.ModuleMonitoringLevelsConfig;
import com.sun.appserv.management.monitor.MonitoringRoot;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import javax.management.MBeanServerConnection;

/**
 *
 * @author Jaroslav Bachorik
 */
public class AMXUtil {
    private static final Map<MBeanServerConnection, WeakReference<ProxyFactory>> proxyMap = new WeakHashMap<MBeanServerConnection, WeakReference<ProxyFactory>>();
    
    public static MonitoringRoot getMonitoringRoot(MBeanServerConnection connection) throws Exception {
        DomainRoot dr = getDomainRoot(connection);
        if (dr == null) return null;
        return dr.getMonitoringRoot();
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
            DomainRoot domainRoot = getAMXProxyFactory(connection).getDomainRoot();
            domainRoot.waitAMXReady();
            return domainRoot;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static ModuleMonitoringLevelsConfig getMonitoringConfig(JmxModel jmxModel) {
        ConfigConfig cc = getDomainRoot(jmxModel).getDomainConfig().getConfigConfigMap().get(JMXUtil.getServerConfig(jmxModel));
        return cc.getMonitoringServiceConfig().getModuleMonitoringLevelsConfig();
    }
    
    public static ProxyFactory getAMXProxyFactory(MBeanServerConnection connection) throws Exception {
        WeakReference<ProxyFactory> pfref = proxyMap.get(connection);
        ProxyFactory pf = null;
        if (pfref == null || pfref.get() == null) {
            pf = ProxyFactory.getInstance(connection);
            proxyMap.put(connection, new WeakReference<ProxyFactory>(pf));
        } else {
            pf = pfref.get();
        }
        return pf;
    }
       
    public static boolean isEE(DomainRoot dr) {
        SystemInfo systemInfo = dr.getSystemInfo();
        return systemInfo.supportsFeature(SystemInfo.CLUSTERS_FEATURE);
    }
}
