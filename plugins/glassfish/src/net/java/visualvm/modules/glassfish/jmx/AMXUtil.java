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

package net.java.visualvm.modules.glassfish.jmx;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.SystemInfo;
import com.sun.appserv.management.client.ProxyFactory;
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.ModuleMonitoringLevelsConfig;
import com.sun.appserv.management.monitor.MonitoringRoot;
import com.sun.appserv.management.util.jmx.MBeanServerConnectionConnectionSource;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;

/**
 *
 * @author Jaroslav Bachorik
 */
public class AMXUtil {
    private static final Logger LOGGER = Logger.getLogger(AMXUtil.class.getName());
    
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
            LOGGER.log(Level.FINER, "", e);
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
        if (pfref != null && pfref.get() != null && pfref.get().getDomainRoot() != null) {
            pf = pfref.get();
            try {
                pf.getDomainRoot().getAMXReady();
                return pf;
            } catch (Exception e) {}
        }
        pf = ProxyFactory.getInstance(new MBeanServerConnectionConnectionSource(connection), false);
        proxyMap.put(connection, new WeakReference<ProxyFactory>(pf));
        return pf;
    }
       
    public static boolean isEE(DomainRoot dr) {
        SystemInfo systemInfo = dr.getSystemInfo();
        return systemInfo.supportsFeature(SystemInfo.CLUSTERS_FEATURE);
    }
}
