/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.modules.tracer.dynamic.jmx;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;

/**
 * A per application cached MBean attribute values provider<br/>
 * It uses a timestamp to decide whether fresh values should be retrieved
 * from the server. It also tries to batch the retrieval process to reduce
 * the network utilization overhead.
 *
 * @author Jaroslav Bachorik
 */
final public class JMXValueCache {
    final private static Map<Application, JMXValueCache> instanceMap = new WeakHashMap<Application, JMXValueCache>();
    final private Map<ObjectName, Collection<String>> attributeMap = new HashMap<ObjectName, Collection<String>>();
    final private Map<String, Object> valueMap = new HashMap<String, Object>();

    final private MBeanServerConnection connection;
    volatile private long lastTimestamp = 0L;

    private JMXValueCache(Application app) {
        JmxModel model = JmxModelFactory.getJmxModelFor(app);
        connection = model.getMBeanServerConnection();
        
    }

    public static JMXValueCache forApplication(Application app) {
        synchronized(instanceMap) {
            JMXValueCache cvp = instanceMap.get(app);
            if (cvp == null) {
                cvp = new JMXValueCache(app);
                instanceMap.put(app, cvp);
            }
            return cvp;
        }
    }

    public JMXValueCache register(ObjectName name, String attribute) {
        register(name, Collections.singleton(attribute));
        return this;
    }

    public JMXValueCache register(ObjectName name, Collection<String> attributes) {
        synchronized(attributeMap) {
            Collection<String> existingAttribs = attributeMap.get(name);
            if (existingAttribs == null) {
                existingAttribs = new ArrayList<String>();
                attributeMap.put(name, existingAttribs);
            }
            existingAttribs.addAll(attributes);
            lastTimestamp = -1; // need to clear the timestamp so the cache is loaded at the next getValue() request
        }
        return this;
    }

    public JMXValueCache unregister(ObjectName name, String attribute) {
        unregister(name, Collections.singleton(attribute));
        return this;
    }

    public JMXValueCache unregister(ObjectName name, Collection<String> attributes) {
        synchronized(attributeMap) {
            Collection<String> existingAttribs = attributeMap.get(name);
            if (existingAttribs != null) {
                existingAttribs.removeAll(attributes);
            }
        }
        return this;
    }

    public MBeanAttributeInfo getInfo(ObjectName on, String attribute) {
        try {
            MBeanInfo mbeanInfo = connection.getMBeanInfo(on);
            for (MBeanAttributeInfo mbAttrInfo : mbeanInfo.getAttributes()) {
                if (mbAttrInfo.getName().equals(attribute)) {
                    return mbAttrInfo;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Object getValue(ObjectName name, String attribute, long timestamp) {
        refreshCache(timestamp);
        return valueMap.get(getId(name, attribute));
    }
    
    private void refreshCache(long timestamp) {
        if (lastTimestamp == timestamp) {
            return;
        }

        lastTimestamp = timestamp;

        synchronized(attributeMap) {
            for(Map.Entry<ObjectName, Collection<String>> entry : attributeMap.entrySet()) {
                try {
                    AttributeList al = connection.getAttributes(entry.getKey(), entry.getValue().toArray(new String[entry.getValue().size()]));
                    for(Attribute a : al.asList()) {
                        valueMap.put(getId(entry.getKey(), a.getName()), a.getValue());
                    }
                    continue;
                } catch (RuntimeMBeanException ex) {
                } catch (ReflectionException ex) {
                } catch (IOException ex) {
                } catch (InstanceNotFoundException e) {
                }
                for(String an : entry.getValue()) {
                    String id = getId(entry.getKey(), an);
                    if (!valueMap.containsKey(id)) valueMap.put(id, 0);
                }
            }
        }
    }

    private String getId(ObjectName on, String attrName) {
        return on.toString() + "#" + attrName;
    }
}
