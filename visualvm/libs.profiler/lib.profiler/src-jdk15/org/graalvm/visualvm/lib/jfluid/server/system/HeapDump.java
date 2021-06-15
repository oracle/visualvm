/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.ManagementFactory;
import javax.management.InstanceNotFoundException;
import javax.management.JMRuntimeException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;


/**
 *
 * @author Tomas Hurka
 */
public class HeapDump {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static MBeanServer mserver;
    private static ObjectName hotspotDiag;
    private static boolean initialized;
    private static boolean runningOnJdk15;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private HeapDump() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void initialize(boolean jdk15) {
        runningOnJdk15 = jdk15;
        if (runningOnJdk15) {
            initialize15();
        } else {
            initialize16();
        }
    }

    public static String takeHeapDump(String outputFile) {
        if (runningOnJdk15) {
            return takeHeapDump15(outputFile);
        }

        return takeHeapDump16(outputFile);
    }

    private static native void initialize15();

    private static void initialize16() {
        if (initialized) {
            return;
        }

        initialized = true;

        try {
            mserver = ManagementFactory.getPlatformMBeanServer();
        } catch (JMRuntimeException ex) {
            // Glassfish: if ManagementFactory.getPlatformMBeanServer() is called too early it will throw JMRuntimeException
            // in such case initialization will be rerun later as part of takeHeapDump()
            System.err.println(ex.getLocalizedMessage());
            initialized = false;

            return;
        }

        try {
            hotspotDiag = new ObjectName("com.sun.management:type=HotSpotDiagnostic");   // NOI18N
            mserver.getObjectInstance(hotspotDiag);
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
        } catch (InstanceNotFoundException ex) {
            System.err.println("Heap Dump is not available"); // NOI18N
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    private static String takeHeapDump15(String outputFile) {
        int error = -1;

        try {
            error = takeHeapDump15Native(outputFile);
        } catch (Exception ex) {
            return ex.getLocalizedMessage();
        }

        if (error == -1) {
            return "Take heap dump is not available."; // NOI18N
        }

        return null;
    }

    private static native int takeHeapDump15Native(String outputFile);

    private static String takeHeapDump16(String outputFile) {
        String error = null;
        initialize16();

        if ((mserver == null) || (hotspotDiag == null)) {
            return "Take heap dump is not available."; // NOI18N
        }

        try {
            mserver.invoke(hotspotDiag, "dumpHeap", new Object[] {outputFile, true}, new String[] {String.class.getName(), boolean.class.getName()} );  // NOI18N
        } catch (IllegalArgumentException ex) {
            error = ex.getLocalizedMessage();
        } catch (InstanceNotFoundException ex) {
            error = ex.getLocalizedMessage();
        } catch (MBeanException ex) {
            error = ex.getLocalizedMessage();
        } catch (ReflectionException ex) {
            error = ex.getLocalizedMessage();
        }

        return error;
    }
}
