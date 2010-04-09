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

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.SessionInitializationException;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProgressObject;
import com.sun.tools.visualvm.modules.tracer.dynamic.spi.ItemDescriptorProvider;
import com.sun.tools.visualvm.modules.tracer.dynamic.spi.DeployerImpl;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.swing.Icon;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jaroslav Bachorik
 */
class DynamicPackage extends TracerPackage.SessionAware<Application> {

    private static final Logger LOGGER = Logger.getLogger(DynamicPackage.class.getName());

    private TracerProgressObject progress;

    private final boolean available;
    private final FileObject cfgRoot;

    final private Set<DeployerImpl> applicableDeployers = new HashSet<DeployerImpl>();

    DynamicPackage(FileObject cfgRoot, String name, String desc, Icon icon, int position, boolean available) {
        super(name, desc, icon, position);
        this.available = available;
        this.cfgRoot = cfgRoot;
    }

    final Map<TracerProbeDescriptor, FileObject> probeCfgMap = new HashMap<TracerProbeDescriptor, FileObject>();

    @Override
    public TracerProbeDescriptor[] getProbeDescriptors() {
        FileObject probesRoot = cfgRoot.getFileObject("probes"); // NOI18N

        List<TracerProbeDescriptor> descriptors = new ArrayList<TracerProbeDescriptor>();

        for(FileObject probe : probesRoot.getChildren()) {
            if (probe.isFolder()) {
               Object name = probe.getAttribute("displayName"); // NOI18N
               Object desc = probe.getAttribute("desc"); // NOI18N
               Object position = probe.getAttribute("position"); // NOI18N

               TracerProbeDescriptor tpd = new TracerProbeDescriptor((String)name, (String)desc, getIcon(), ((Integer)position).intValue(), available);
               probeCfgMap.put(tpd, probe);
               descriptors.add(tpd);
            }
        }

        return descriptors.toArray(new TracerProbeDescriptor[descriptors.size()]);
    }

    @Override
    public TracerProbe<Application> getProbe(TracerProbeDescriptor descriptor) {
        FileObject probeCfg = probeCfgMap.get(descriptor);
        List<String> attrNames = new ArrayList<String>();
        List<ProbeItemDescriptor> itemDescs = new ArrayList<ProbeItemDescriptor>();

        FileObject propsFolder = probeCfg.getFileObject("properties"); // NOI18N
        if (propsFolder != null) {
            Object descProviderObj = propsFolder.getAttribute("descriptorProvider"); // NOI18N
            ItemDescriptorProvider idp = (descProviderObj instanceof ItemDescriptorProvider) ? (ItemDescriptorProvider)descProviderObj : null;

            FileObject[] children = propsFolder.getChildren();

            Arrays.sort(children, new Comparator<FileObject>() {

                @Override
                public int compare(FileObject o1, FileObject o2) {
                    Integer p1 = (Integer)o1.getAttribute("position");
                    Integer p2 = (Integer)o2.getAttribute("position");
                    return (p1 != null && p2 != null) ? p1.compareTo(p2) : 0;
                }
            });

            for(FileObject prop : children) {
                if (prop.isData()) {
                    attrNames.add(prop.getName());

                    ProbeItemDescriptor pid = null;
                    if (idp != null) {
                        pid = idp.create(prop.getName(), getAttributes(prop));
                    }
                    if (pid == null) {
                        pid = ProbeItemDescriptor.lineItem((String)prop.getAttribute("displayName"), (String)prop.getAttribute("desc")); // NOI18N
                    }
                    itemDescs.add(pid);
                }
            }
        }
        return new DynamicProbe(itemDescs.toArray(new ProbeItemDescriptor[itemDescs.size()]), attrNames.toArray(new String[attrNames.size()]), probeCfg);
    }

    @Override
    protected TracerProgressObject sessionInitializing(TracerProbe<Application>[] probes, final Application dataSource, int refresh) {
        for(TracerProbe probe : probes) {
            DynamicProbe dp = (DynamicProbe)probe;
            DeployerImpl di = dp.getDeployment().getDeployer();
            di.applyConfig(dataSource, dp.getDeployment().getConfig());
            applicableDeployers.add(di);
        }

        progress = new TracerProgressObject(applicableDeployers.size() * 50 + probes.length + 5, "");

        return progress;
    }

    @Override
    protected void sessionStarting(TracerProbe<Application>[] probes, Application application)
                throws SessionInitializationException {
        MBeanServerConnection mbsc = null;
        try {
            for(DeployerImpl di : applicableDeployers) {
                if (!di.deploy(application, progress, 50)) {
                    throw new SessionInitializationException("Error deploying BTrace probes"); // NOI18N
                }
            }
            for(TracerProbe probe : probes) {
                if (probe instanceof DynamicProbe) {
                    if (mbsc == null) {
                        progress.addStep("Initializing JMX connection");
                        mbsc = getConnection(application);
                        if (mbsc == null) {
                            throw new SessionInitializationException("Unable to create JMX connection",
                                    "Unable to create JMX connection to " + application); // NOI18N);
                        }
                    }
                    ((DynamicProbe)probe).setConnection(mbsc);
                }
            }
        } finally {
            if (progress != null) progress.finish();
        }
    }

    @Override
    protected void sessionStopping(TracerProbe<Application>[] probes, Application application) {
        for(DeployerImpl d : applicableDeployers) {
            d.undeploy(application);
        }
        applicableDeployers.clear();
    }


    private static MBeanServerConnection getConnection(Application application) {
        MBeanServerConnection connection = null;

        try {
            JmxModel model = JmxModelFactory.getJmxModelFor(application);
            connection = model != null ? model.getMBeanServerConnection() : null;
            if (connection == null) {
                LOGGER.info("Failed to resolve JMX connection."); // NOI18N
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Exception when resolving JMX connection.", e); // NOI18N
        }

        return connection;
    }

    private static Map<String, Object> getAttributes(FileObject file) {
        return getAttributes(file, null);
    }

    private static Map<String, Object> getAttributes(FileObject file, Set<String> excluded) {
        if (file == null) return Collections.EMPTY_MAP;

        Map<String, Object> attrs = new HashMap<String, Object>();
        Enumeration<String> attrKeys = file.getAttributes();
        while (attrKeys.hasMoreElements()) {
            String key = attrKeys.nextElement();
            if (excluded != null && excluded.contains(key)) continue;
            
            attrs.put(key, file.getAttribute(key));
        }
        return attrs;
    }

    
}
