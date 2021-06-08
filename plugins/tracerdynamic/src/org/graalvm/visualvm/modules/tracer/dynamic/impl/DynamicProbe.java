/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.tracer.dynamic.impl;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.modules.tracer.ProbeItemDescriptor;
import org.graalvm.visualvm.modules.tracer.TracerProbe;
import org.graalvm.visualvm.modules.tracer.TracerProbeDescriptor;
import org.graalvm.visualvm.modules.tracer.dynamic.spi.DeployerImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jaroslav Bachorik
 */
public class DynamicProbe extends TracerProbe<Application>{
    private TracerProbeDescriptor descriptor;
    private List<ValueProvider> valueProviders;

    private Map<DeployerImpl, Map<String, Object>>  deployers = new HashMap<DeployerImpl, Map<String, Object>>();

    public DynamicProbe(List<ProbeItemDescriptor> itemDescriptors, List<ValueProvider> valueProviders) {
        super(itemDescriptors.toArray(new ProbeItemDescriptor[itemDescriptors.size()]));
        this.valueProviders = valueProviders;
    }

    public TracerProbeDescriptor getProbeDescriptor() {
        return descriptor;
    }

    public void setProbeDescriptor(TracerProbeDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public void addDeployment(DeployerImpl d, Map<String, Object> attribs) {
        deployers.put(d, attribs);
    }

    public Set<DeployerImpl> applyDeployerConfigs(Application app) {
        for(Map.Entry<DeployerImpl, Map<String, Object>> entry : deployers.entrySet()) {
            entry.getKey().applyConfig(app, entry.getValue());
        }
        return deployers.keySet();
    }

    @Override
    public long[] getItemValues(long timestamp) {
        long[] vals = new long[valueProviders.size()];
        int index = 0;
        for(ValueProvider vp : valueProviders) {
            vals[index++] = vp.getValue(timestamp);
        }
        return vals;
    }
}
