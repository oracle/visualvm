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
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.modules.tracer.SessionInitializationException;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProgressObject;
import com.sun.tools.visualvm.modules.tracer.dynamic.spi.DeployerImpl;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.swing.Icon;

/**
 *
 * @author Jaroslav Bachorik
 */
public class DynamicPackage extends TracerPackage.SessionAware<Application> {

    private static final Logger LOGGER = Logger.getLogger(DynamicPackage.class.getName());

    private TracerProgressObject progress;

    final private Map<TracerProbeDescriptor, DynamicProbe> probeMap = new TreeMap<TracerProbeDescriptor, DynamicProbe>(Positionable.COMPARATOR);

    final private Set<DeployerImpl> deployers = new HashSet<DeployerImpl>();

    public DynamicPackage(String name, String desc, Icon icon, int position) {
        super(name, desc, icon, position);
    }

    public void addProbe(DynamicProbe probe) {
        probeMap.put(probe.getProbeDescriptor(), probe);
    }

    @Override
    public TracerProbeDescriptor[] getProbeDescriptors() {
        return probeMap.keySet().toArray(new TracerProbeDescriptor[probeMap.keySet().size()]);
    }

    @Override
    public TracerProbe<Application> getProbe(TracerProbeDescriptor descriptor) {
        return probeMap.get(descriptor);
    }

    @Override
    protected TracerProgressObject sessionInitializing(TracerProbe<Application>[] probes, final Application dataSource, int refresh) {
        for(TracerProbe probe : probes) {
            DynamicProbe dp = (DynamicProbe)probe;
            deployers.addAll(dp.applyDeployerConfigs(dataSource));
//            DeployerImpl di = dp.getDeployment() != null ? dp.getDeployment().getDeployer() : null;
//            if (di != null) {
//                di.applyConfig(dataSource, dp.getDeployment().getConfig());
//                deployers.add(di);
//            }
        }

        progress = new TracerProgressObject(deployers.size() * 50 + probes.length + 5, "");

        return progress;
    }

    @Override
    protected void sessionStarting(TracerProbe<Application>[] probes, Application application)
                throws SessionInitializationException {
        try {
            for(DeployerImpl di : deployers) {
                if (!di.deploy(application, progress, 50)) {
                    throw new SessionInitializationException("Error deploying dynamic probe (" + di.getClass().getName() + ")"); // NOI18N
                }
            }
        } finally {
            if (progress != null) progress.finish();
        }
    }

    @Override
    protected void sessionStopping(TracerProbe<Application>[] probes, Application application) {
        for(DeployerImpl d : deployers) {
            d.undeploy(application);
        }
        deployers.clear();
    }
}
