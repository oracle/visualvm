/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.server.system;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 *
 * @author Tomas Hurka
 */
class Histogram19 {
    private static final String DIAGNOSTIC_COMMAND_MXBEAN_NAME =
            "com.sun.management:type=DiagnosticCommand";    // NOI18N
    private static final String ALL_OBJECTS_OPTION = "-all";    // NOI18N
    private static final String HISTOGRAM_COMMAND = "gcClassHistogram";       // NOI18N
    private static MBeanServer mserver;
    private static ObjectName hotspotDiag;

    static boolean initialize() {
        boolean initok = false;
        try {
            mserver = ManagementFactory.getPlatformMBeanServer();
            hotspotDiag = new ObjectName(DIAGNOSTIC_COMMAND_MXBEAN_NAME);
            mserver.getObjectInstance(hotspotDiag);
            initok = true;
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
        } catch (InstanceNotFoundException ex) {
            System.err.println("Heap Histogram is not available"); // NOI18N
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return initok;
    }

    static InputStream getRawHistogram() {
        try {
            Object histo = mserver.invoke(hotspotDiag,
                    HISTOGRAM_COMMAND,
                    new Object[] {new String[] {ALL_OBJECTS_OPTION}},
                    new String[] {String[].class.getName()}
            );
            if (histo instanceof String) {
                return new ByteArrayInputStream(((String)histo).getBytes(StandardCharsets.UTF_8));
            }
        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
        } catch (MBeanException ex) {
            ex.printStackTrace();
        } catch (ReflectionException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
}
