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

import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import java.io.IOException;
import java.util.Map;
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
    public static final String getServerName(JmxModel jmx) {
        try {
            Object serverNameObj = jmx.getMBeanServerConnection().getAttribute(new ObjectName("com.sun.appserv:j2eeType=J2EEServer,name=server,category=runtime"), "J2EEServer");
            return serverNameObj != null ? serverNameObj.toString() : null;
        } catch (MalformedObjectNameException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NullPointerException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MBeanException ex) {
            Exceptions.printStackTrace(ex);
        } catch (AttributeNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstanceNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ReflectionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
    public static final String getServerConfig(JmxModel jmx) {
        try {
            Object serverConfObj = jmx.getMBeanServerConnection().getAttribute(new ObjectName("com.sun.appserv:j2eeType=J2EEServer,name=server,category=runtime"), "config-ref");
            return serverConfObj != null ? serverConfObj.toString() : null;
        } catch (MalformedObjectNameException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NullPointerException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MBeanException ex) {
            Exceptions.printStackTrace(ex);
        } catch (AttributeNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstanceNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ReflectionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
    public static final String getServerConfigDir(JmxModel jmx) {
        try {
            Object serverConfDirObj = jmx.getMBeanServerConnection().invoke(new ObjectName("com.sun.appserv:type=domain,category=config"), "getConfigDir", null, null);
            return serverConfDirObj != null ? serverConfDirObj.toString() : null;
        } catch (MalformedObjectNameException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NullPointerException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MBeanException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstanceNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ReflectionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
    public static final String getServerDomain(JmxModel jmx) {
        try {
            Object serverDomainObj = jmx.getMBeanServerConnection().invoke(new ObjectName("com.sun.appserv:type=domain,category=config"), "getName", null, null);
            return serverDomainObj != null ? serverDomainObj.toString() : null;
        } catch (MalformedObjectNameException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NullPointerException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MBeanException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstanceNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ReflectionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
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
            Exceptions.printStackTrace(ex);
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
            Exceptions.printStackTrace(ex);
        } catch (AttributeNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstanceNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ReflectionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MalformedObjectNameException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NullPointerException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
    public static final String getWebModuleName(String objectName, JmxModel jmx, Map<String, String> context2name) {
        try {
            String ctxMapping = (String) jmx.getMBeanServerConnection().getAttribute(new ObjectName(objectName), "name");
            if (!ctxMapping.startsWith("/")) ctxMapping = "/" + ctxMapping;
            return context2name.get(ctxMapping);
        } catch (MBeanException ex) {
            Exceptions.printStackTrace(ex);
        } catch (AttributeNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstanceNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ReflectionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MalformedObjectNameException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NullPointerException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
    public static final String[] getDeployedObjects(JmxModel jmx) {
        try {
            ObjectName on = new ObjectName("com.sun.appserv:j2eeType=J2EEServer,name=server,category=runtime");
            return (String[]) jmx.getMBeanServerConnection().getAttribute(on, "deployedObjects");
        } catch (MBeanException ex) {
            Exceptions.printStackTrace(ex);
        } catch (AttributeNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstanceNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ReflectionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MalformedObjectNameException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NullPointerException ex) {
            Exceptions.printStackTrace(ex);
        }
        return new String[0];
    }
}
