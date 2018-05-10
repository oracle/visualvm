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

package net.java.visualvm.modules.glassfish.jmx;

import org.graalvm.visualvm.tools.jmx.JmxModel;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.openide.util.Exceptions;

/**
 *
 * @author Jaroslav Bachorik
 */
public class JMXUtil {
    private static final Logger LOGGER = Logger.getLogger(JMXUtil.class.getName());
    
    public static final String getServerName(JmxModel jmx) {
        try {
            Object serverNameObj = jmx.getMBeanServerConnection().getAttribute(new ObjectName("com.sun.appserv:j2eeType=J2EEServer,name=server,category=runtime"), "J2EEServer");
            return serverNameObj != null ? serverNameObj.toString() : null;
        } catch (MalformedObjectNameException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerName", ex);
        } catch (NullPointerException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerName", ex);
        } catch (MBeanException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerName", ex);
        } catch (AttributeNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerName", ex);
        } catch (InstanceNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerName", ex);
        } catch (ReflectionException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerName", ex);
        } catch (IOException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerName", ex);
        }
        return null;
    }
    
    public static final String getServerConfig(JmxModel jmx) {
        try {
            Object serverConfObj = jmx.getMBeanServerConnection().getAttribute(new ObjectName("com.sun.appserv:j2eeType=J2EEServer,name=server,category=runtime"), "config-ref");
            return serverConfObj != null ? serverConfObj.toString() : null;
        } catch (MalformedObjectNameException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfig", ex);
        } catch (NullPointerException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfig", ex);
        } catch (MBeanException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfig", ex);
        } catch (AttributeNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfig", ex);
        } catch (InstanceNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfig", ex);
        } catch (ReflectionException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfig", ex);
        } catch (IOException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfig", ex);
        }
        return null;
    }
    
    public static final String getServerConfigDir(JmxModel jmx) {
        try {
            Object serverConfDirObj = jmx.getMBeanServerConnection().invoke(new ObjectName("com.sun.appserv:type=domain,category=config"), "getConfigDir", null, null);
            return serverConfDirObj != null ? serverConfDirObj.toString() : null;
        } catch (MalformedObjectNameException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfigDir", ex);
        } catch (NullPointerException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfigDir", ex);
        } catch (MBeanException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfigDir", ex);
        } catch (InstanceNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfigDir", ex);
        } catch (ReflectionException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfigDir", ex);
        } catch (IOException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerConfigDir", ex);
        }
        return null;
    }
    
    public static final String getServerDomain(JmxModel jmx) {
        try {
            Object serverDomainObj = jmx.getMBeanServerConnection().invoke(new ObjectName("com.sun.appserv:type=domain,category=config"), "getName", null, null);
            return serverDomainObj != null ? serverDomainObj.toString() : null;
        } catch (MalformedObjectNameException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerDomain", ex);
        } catch (NullPointerException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerDomain", ex);
        } catch (MBeanException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerDomain", ex);
        } catch (InstanceNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerDomain", ex);
        } catch (ReflectionException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerDomain", ex);
        } catch (IOException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getServerDomain", ex);
        }
        return null;
    }
    
    public static final String getObjectName(String type, String moduleUniqueName, JmxModel jmx) {
        try {
            for(String deplObjName : getDeployedObjects(jmx)) {
                if (deplObjName.startsWith("com.sun.appserv:j2eeType=" + type + ",name=" + moduleUniqueName)) {
                    return deplObjName;
                }
            }
        } catch (NullPointerException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getObjectName", ex);
        }
        return moduleUniqueName;
    }
    
    public static final String getJ2EEAppName(String objectName) {
        int startIndex = objectName.indexOf(",J2EEApplication=");
        int stopIndex = -1;
        if (startIndex > -1) {
            stopIndex = objectName.indexOf(",", startIndex + 1);
        }
        if (startIndex > -1 && stopIndex > -1 && stopIndex > startIndex) {
            String appName = objectName.substring(startIndex + 17, stopIndex - 1);
            if (appName == null || appName.startsWith("nul")) return null;
            return new String(appName);
        } else {
            return null;
        }
    }
    
    public static final String getWebModuleName(String objectName, JmxModel jmx) {
        try {
            return (String) jmx.getMBeanServerConnection().getAttribute(new ObjectName(objectName), "name");
        } catch (MBeanException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (AttributeNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (InstanceNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (ReflectionException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (IOException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (MalformedObjectNameException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (NullPointerException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        }
        return null;
    }
    
    public static final String getWebModuleName(String objectName, JmxModel jmx, Map<String, String> context2name) {
        try {
            String ctxMapping = (String) jmx.getMBeanServerConnection().getAttribute(new ObjectName(objectName), "name");
            if (!ctxMapping.startsWith("/")) ctxMapping = "/" + ctxMapping;
            return context2name.get(ctxMapping);
        } catch (MBeanException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (AttributeNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (InstanceNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (ReflectionException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (IOException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (MalformedObjectNameException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        } catch (NullPointerException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getWebModuleName", ex);
        }
        return null;
    }
    
    public static final String[] getDeployedObjects(JmxModel jmx) {
        try {
            ObjectName on = new ObjectName("com.sun.appserv:j2eeType=J2EEServer,name=server,category=runtime");
            return (String[]) jmx.getMBeanServerConnection().getAttribute(on, "deployedObjects");
        } catch (MBeanException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getDeployedObjects", ex);
        } catch (AttributeNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getDeployedObjects", ex);
        } catch (InstanceNotFoundException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getDeployedObjects", ex);
        } catch (ReflectionException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getDeployedObjects", ex);
        } catch (IOException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getDeployedObjects", ex);
        } catch (MalformedObjectNameException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getDeployedObjects", ex);
        } catch (NullPointerException ex) {
            LOGGER.throwing(JMXUtil.class.getName(), "getDeployedObjects", ex);
        }
        return new String[0];
    }
}
