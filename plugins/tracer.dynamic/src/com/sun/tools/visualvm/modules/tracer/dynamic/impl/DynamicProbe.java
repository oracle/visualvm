/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */
package com.sun.tools.visualvm.modules.tracer.dynamic.impl;

import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jaroslav Bachorik
 */
class DynamicProbe extends TracerProbe {
    private static final Logger LOGGER = Logger.getLogger(DynamicProbe.class.getName());

    private final String[] attrs;
    private Deployment deployment;
    private MBeanServerConnection connection;
    private ObjectName mbean;

    DynamicProbe(ProbeItemDescriptor[] itemDescriptors, String[] attrs, FileObject probeCfgRoot) {
        super(itemDescriptors);
        this.attrs = attrs;
        processConfiguration(probeCfgRoot);
    }

    private void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }

    Deployment getDeployment() {
        return deployment;
    }

    private void setMBean(String mbeanName) {
        try {
            this.mbean = ObjectName.getInstance(mbeanName);
        } catch (Exception e) {
            this.mbean = null;
        }
    }

    synchronized final void setConnection(MBeanServerConnection connection) {
        if (mbean == null) return;

        this.connection = connection;

        try {
            boolean ready = false;
            for (int i = 0; i < 5; i++) {
                ready = !connection.queryNames(mbean, null).isEmpty();
                if (ready) break;
                Thread.sleep(500);
            }
            if (!ready) {
                this.connection = null;
                LOGGER.info("Timeout initializing JMX connection."); // NOI18N
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Exception when querying JMX connection.", e); // NOI18N
        }
    }

    synchronized final void resetConnection() {
        connection = null;
    }

    @Override
    public synchronized final long[] getItemValues(long timestamp) {
        if (connection != null) {
            try {
                List<Attribute> attrList = connection.getAttributes(mbean, attrs).asList();
                long[] values = new long[attrList.size()];
                int index = 0;
                for(Attribute a : attrList) {
                    Object v = a.getValue();
                    if (v instanceof Number) {
                        values[index++] = ((Number)v).longValue();
                    }
                }
                return values;
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Failed to read JMX attributes", t); // NOI18N
            }
        }

        long[] values = new long[attrs.length];
        Arrays.fill(values, 0);
        return values;
    }

    private void processConfiguration(FileObject probeCfg) {
        setMBean((String)probeCfg.getAttribute("mbean")); // NOI18N
        setDeployment(Deployment.forProbeConfig(probeCfg));
    }
}
