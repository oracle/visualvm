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
import com.sun.tools.visualvm.modules.tracer.dynamic.impl.ValueProvider;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanAttributeInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 *
 * @author Jaroslav Bachorik
 */
public class JMXValueProvider implements ValueProvider {
    final private static Logger LOG = Logger.getLogger(JMXValueProvider.class.getName());
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
            LOG.log(Level.FINE, "NULL({0}#{1}) @ {2}", new Object[]{on, attributeName, timestamp});
        }
        return val == null ? 0 : val;
    }

    public MBeanAttributeInfo getInfo() {
        return cache.getInfo(on, attributeName);
    }
}
