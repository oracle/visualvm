/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.modules.tracer.dynamic.jmx;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.tracer.dynamic.impl.ValueProvider;
import javax.management.MBeanAttributeInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 *
 * @author jb198685
 */
public class JMXValueProvider implements ValueProvider {
    private ObjectName on;
    private String attributeName;
    private JMXValueCache cache;

    public JMXValueProvider(String objectName, String attributeName, Application app) throws MalformedObjectNameException {
        this.on = ObjectName.getInstance(objectName);
        this.attributeName = attributeName;
        cache = JMXValueCache.forApplication(app).register(on, attributeName);
    }

    @Override
    public long getValue(long timestamp) {
        Object val = value(timestamp);
        if (val instanceof Number) {
            return ((Number)val).longValue();
        }
        return 0L;
    }

    public Object value(long timestamp) {
        Object val = cache.getValue(on, attributeName, timestamp);
        if (val == null) {
            System.err.println("NULL @ " + timestamp);
        }
        return val == null ? 0 : val;
    }

    public MBeanAttributeInfo getInfo() {
        return cache.getInfo(on, attributeName);
    }
}
