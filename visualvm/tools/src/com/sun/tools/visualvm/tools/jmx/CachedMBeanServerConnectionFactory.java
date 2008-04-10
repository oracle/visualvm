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

package com.sun.tools.visualvm.tools.jmx;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * The {@code CachedMBeanServerConnectionFactory} class is a factory class that
 * allows to get instances of {@link CachedMBeanServerConnection} for a given
 * {@link MBeanServerConnection} or {@link JmxModel}.
 * 
 * The factory methods allow to supply an interval value at which the cache will
 * be automatically flushed and interested {@link MBeanCacheListener}s notified.
 * 
 * If the factory methods which do not take an interval value are used then
 * no automatic flush is performed and the user will be in charge of flushing
 * the cache by calling {@link CachedMBeanServerConnection#flush()}.
 *
 * @author Eamonn McManus
 * @author Luis-Miguel Alventosa
 */
public final class CachedMBeanServerConnectionFactory {

    private CachedMBeanServerConnectionFactory() {
    }

    /**
     * Factory method for obtaining the {@link CachedMBeanServerConnection} for
     * the given {@link MBeanServerConnection}.
     * 
     * @param mbsc an MBeanServerConnection.
     * 
     * @return a {@link CachedMBeanServerConnection} instance which caches the
     * attribute values of the supplied {@link MBeanServerConnection}.
     */
    public static CachedMBeanServerConnection getCachedMBeanServerConnection(MBeanServerConnection mbsc) {
        return Snapshot.newSnapshot(mbsc);
    }

    /**
     * Factory method for obtaining the {@link CachedMBeanServerConnection} for
     * the given {@link MBeanServerConnection}. The cache will be flushed at the
     * given interval and the interested {@link MBeanCacheListener}s will be notified.
     * 
     * @param mbsc an MBeanServerConnection.
     * @param interval the interval (in milliseconds) at which the cache is flushed.
     * An interval equal to zero means no automatic flush of the MBean cache.
     * 
     * @return a {@link CachedMBeanServerConnection} instance which caches the
     * attribute values of the supplied {@link MBeanServerConnection} and is
     * flushed at the end of every interval period.
     * 
     * @throws IllegalArgumentException if the supplied interval is negative.
     */
    public static CachedMBeanServerConnection
            getCachedMBeanServerConnection(MBeanServerConnection mbsc, int interval)
            throws IllegalArgumentException {
        if (interval < 0) {
            throw new IllegalArgumentException("interval cannot be negative");  // NOI18N
        }
        return Snapshot.newSnapshot(mbsc);
    }

    /**
     * Factory method for obtaining the {@link CachedMBeanServerConnection} for
     * the given {@link JmxModel}.
     * 
     * @param jmx a JmxModel.
     * 
     * @return a {@link CachedMBeanServerConnection} instance which caches the
     * attribute values of the supplied {@link JmxModel}.
     */
    public static CachedMBeanServerConnection getCachedMBeanServerConnection(JmxModel jmx) {
        return Snapshot.newSnapshot(jmx.getMBeanServerConnection());
    }

    /**
     * Factory method for obtaining the {@link CachedMBeanServerConnection} for
     * the given {@link JmxModel}. The cache will be flushed at the given interval
     * and the interested {@link MBeanCacheListener}s will be notified.
     * 
     * @param jmx a JmxModel.
     * @param interval the interval (in milliseconds) at which the cache is flushed.
     * An interval equal to zero means no automatic flush of the MBean cache.
     * 
     * @return a {@link CachedMBeanServerConnection} instance which caches the
     * attribute values of the supplied {@link JmxModel} and is flushed at the
     * end of every interval period.
     *
     * @throws IllegalArgumentException if the supplied interval is negative.
     */
    public static CachedMBeanServerConnection
            getCachedMBeanServerConnection(JmxModel jmx, int interval)
            throws IllegalArgumentException {
        if (interval < 0) {
            throw new IllegalArgumentException("interval cannot be negative");  // NOI18N
        }
        return Snapshot.newSnapshot(jmx.getMBeanServerConnection());
    }

    static class Snapshot {

        private Snapshot() {
        }

        public static CachedMBeanServerConnection newSnapshot(MBeanServerConnection mbsc) {
            final InvocationHandler ih = new SnapshotInvocationHandler(mbsc);
            return (CachedMBeanServerConnection) Proxy.newProxyInstance(
                    Snapshot.class.getClassLoader(),
                    new Class[]{CachedMBeanServerConnection.class},
                    ih);
        }
    }

    static class SnapshotInvocationHandler implements InvocationHandler {

        private final MBeanServerConnection conn;
        private Map<ObjectName, NameValueMap> cachedValues = newMap();
        private Map<ObjectName, Set<String>> cachedNames = newMap();

        @SuppressWarnings("serial")
        private static final class NameValueMap
                extends HashMap<String, Object> {
        }

        SnapshotInvocationHandler(MBeanServerConnection conn) {
            this.conn = conn;
        }

        synchronized void flush() {
            cachedValues = newMap();
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            final String methodName = method.getName();
            if (methodName.equals("getAttribute")) {    // NOI18N
                return getAttribute((ObjectName) args[0], (String) args[1]);
            } else if (methodName.equals("getAttributes")) {    // NOI18N
                return getAttributes((ObjectName) args[0], (String[]) args[1]);
            } else if (methodName.equals("flush")) {    // NOI18N
                flush();
                return null;
            } else {
                try {
                    return method.invoke(conn, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        private Object getAttribute(ObjectName objName, String attrName)
                throws MBeanException, InstanceNotFoundException,
                AttributeNotFoundException, ReflectionException, IOException {
            final NameValueMap values = getCachedAttributes(
                    objName, Collections.singleton(attrName));
            Object value = values.get(attrName);
            if (value != null || values.containsKey(attrName)) {
                return value;
            }
            // Not in cache, presumably because it was omitted from the
            // getAttributes result because of an exception.  Following
            // call will probably provoke the same exception.
            return conn.getAttribute(objName, attrName);
        }

        private AttributeList getAttributes(
                ObjectName objName, String[] attrNames) throws
                InstanceNotFoundException, ReflectionException, IOException {
            final NameValueMap values = getCachedAttributes(
                    objName,
                    new TreeSet<String>(Arrays.asList(attrNames)));
            final AttributeList list = new AttributeList();
            for (String attrName : attrNames) {
                final Object value = values.get(attrName);
                if (value != null || values.containsKey(attrName)) {
                    list.add(new Attribute(attrName, value));
                }
            }
            return list;
        }

        private synchronized NameValueMap getCachedAttributes(
                ObjectName objName, Set<String> attrNames) throws
                InstanceNotFoundException, ReflectionException, IOException {
            NameValueMap values = cachedValues.get(objName);
            if (values != null && values.keySet().containsAll(attrNames)) {
                return values;
            }
            attrNames = new TreeSet<String>(attrNames);
            Set<String> oldNames = cachedNames.get(objName);
            if (oldNames != null) {
                attrNames.addAll(oldNames);
            }
            values = new NameValueMap();
            final AttributeList attrs = conn.getAttributes(
                    objName,
                    attrNames.toArray(new String[attrNames.size()]));
            for (Attribute attr : attrs.asList()) {
                values.put(attr.getName(), attr.getValue());
            }
            cachedValues.put(objName, values);
            cachedNames.put(objName, attrNames);
            return values;
        }

        // See http://www.artima.com/weblogs/viewpost.jsp?thread=79394
        private static <K, V> Map<K, V> newMap() {
            return new HashMap<K, V>();
        }
    }
}
