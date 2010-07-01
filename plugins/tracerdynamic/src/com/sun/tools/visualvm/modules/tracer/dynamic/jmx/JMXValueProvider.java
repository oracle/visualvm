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

package com.sun.tools.visualvm.modules.tracer.dynamic.jmx;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.tracer.dynamic.impl.ValueProvider;
import javax.management.MBeanAttributeInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 *
 * @author Jaroslav Bachorik
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
