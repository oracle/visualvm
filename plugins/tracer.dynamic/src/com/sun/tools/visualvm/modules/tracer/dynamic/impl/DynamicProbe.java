/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.modules.tracer.dynamic.impl;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import com.sun.tools.visualvm.modules.tracer.dynamic.spi.DeployerImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jb198685
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
