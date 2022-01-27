/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.sampler.truffle;

import java.io.IOException;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 *
 * @author Tomas Hurka
 */
public class ProxyTruffleMBean {

    private static final String TRUFFLE_OBJECT_NAME = "com.truffle:type=Threading";
    private final ObjectName truffleObjectName;
    private final MBeanServerConnection conn;

    public ProxyTruffleMBean(MBeanServerConnection c) throws MalformedObjectNameException {
        conn = c;
        truffleObjectName = new ObjectName(TRUFFLE_OBJECT_NAME);
    }

    public Map<String, Object>[] dumpAllThreads() throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (Map[]) conn.invoke(truffleObjectName, "dumpAllThreads", null, null);
    }

    public boolean isStackTracesEnabled() throws InstanceNotFoundException, MBeanException, IOException, ReflectionException, AttributeNotFoundException {
        return (boolean) conn.getAttribute(truffleObjectName, "StackTracesEnabled");
    }

    public Map<String, Object>[] heapHistogram() throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (Map[]) conn.invoke(truffleObjectName, "heapHistogram", null, null);
    }

    public boolean isHeapHistogramEnabled() throws InstanceNotFoundException, MBeanException, IOException, ReflectionException, AttributeNotFoundException {
        return (boolean) conn.getAttribute(truffleObjectName, "HeapHistogramEnabled");
    }

    public void setTrackFlags(boolean trackFlags) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
        conn.setAttribute(truffleObjectName, new Attribute("TrackFlags", trackFlags));
    }

    public void setMode(String mode) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException, AttributeNotFoundException, InvalidAttributeValueException {
        conn.setAttribute(truffleObjectName, new Attribute("Mode", mode));
    }

    public boolean isModeAvailable() throws InstanceNotFoundException, MBeanException, IOException, ReflectionException, AttributeNotFoundException {
        return (boolean) conn.getAttribute(truffleObjectName, "ModeAvailable");
    }

    public boolean isRegistered() throws IOException {
        return conn.isRegistered(truffleObjectName);
    }
}
