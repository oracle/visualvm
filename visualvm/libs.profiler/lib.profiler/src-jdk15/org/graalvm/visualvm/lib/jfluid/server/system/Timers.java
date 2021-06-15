/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
import javax.management.JMRuntimeException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;


/**
 * Provides methods for obtaining various high-resolution system times.
 * A version for JDK 1.5
 *
 * @author  Misha Dmitriev
 */
public class Timers {

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static MBeanServerConnection conn;
    private static final String PROCESS_CPU_TIME_ATTR = "ProcessCpuTime"; // NOI18N

    /** IBM/OpenJ9 only. */
    private static final String PROCESS_CPU_TIME_NS_ATTR = "ProcessCpuTimeByNS"; // NOI18N

    /**
     * IBM/OpenJ9 only.
     * Returns the collective processing capacity available to the VM
     * in units of 1% of a physical processor. In environments without
     * some kind of virtual partitioning, this will simply be the number
     * of CPUs * 100.
     */
    private static final String PROCESSING_CAPACITY_ATTR = "ProcessingCapacity"; // NOI18N
    private static ObjectName osName;
    private static double processingCapacity;
    private static int processorsCount;
    private static boolean processCPUTimeAttribute;
    private static boolean initialized;
    private static String processCpuTimeAttr;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * "counts" instead of nanoseconds in this method are for compatibility with the previous
     * versions of JFluid, that call a native method for system timer, which, in turn, returns
     * the result in sub-microsecond "counts" on Windows.
     */
    public static long getCurrentTimeInCounts() {
        return System.nanoTime();
    }

    public static long getNoOfCountsInSecond() {
        return 1000000000;
    }

    public static native long getThreadCPUTimeInNanos();

    /**
     * Returns the approximate accumulated process CPU elapsed time
     * in nanoseconds. Note that the time is normalized to one processor.
     * This method returns <tt>-1</tt> if the collection
     * elapsed time is undefined for this collector.
     *
     * @return the approximate accumulated process CPU elapsed time
     * in nanoseconds.
     */
    public static long getProcessCpuTime() {
        initializeProcessCPUTime();
        if (processCPUTimeAttribute) {
             try {
                 Long cputime = (Long)conn.getAttribute(osName,processCpuTimeAttr);

                 return (long)(cputime.longValue()/processingCapacity/processorsCount);
             } catch (Exception ex) {
                 ex.printStackTrace();
             }
        }
        return -1;
    }

    /**
     * This is relevant only on Solaris. By default, the resolution of the thread local CPU timer is 10 ms. If we enable
     * micro state accounting, it enables significantly (but possibly at a price of some overhead). So I turn it on only
     * when thread CPU timestamps are really collected.
     */
    public static native void enableMicrostateAccounting(boolean v);

    /** Should be called at earliest possible time */
    public static void initialize() {
        ManagementFactory.getThreadMXBean();
        getThreadCPUTimeInNanos();
        initializeProcessCPUTime();
    }

    private static void initializeProcessCPUTime() {
        if (initialized) {
            return;
        }

        initialized = true;
        try {
            MBeanAttributeInfo[] attrs;
            
            conn = ManagementFactory.getPlatformMBeanServer();
            osName = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
            attrs = conn.getMBeanInfo(osName).getAttributes();
            processorsCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
            for (int i = 0; i < attrs.length; i++) {
                String name = attrs[i].getName();
                
                if (PROCESS_CPU_TIME_ATTR.equals(name) && !processCPUTimeAttribute) {
                    processCPUTimeAttribute = Boolean.TRUE;
                    processCpuTimeAttr = name;
                }
                if (PROCESS_CPU_TIME_NS_ATTR.equals(name)) {
                    processCPUTimeAttribute = Boolean.TRUE;
                    processCpuTimeAttr = name;
                }
                if (PROCESSING_CAPACITY_ATTR.equals(name)) {
                    Number mul = (Number) conn.getAttribute(osName,PROCESSING_CAPACITY_ATTR);
                    processingCapacity = mul.longValue()/100.0/processorsCount;
                }
            }
        } catch (JMRuntimeException ex) {
            // Glassfish: if ManagementFactory.getPlatformMBeanServer() is called too early it will throw JMRuntimeException
            // in such case initialization will be rerun later as part of getProcessCpuTime()
            System.err.println(ex.getLocalizedMessage());
            initialized = false;       
            return;   
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * WORKS ONLY ON UNIX, calls nanosleep(). On Solaris, this is more precise than the built-in Thread.sleep() call
     * implementation that, at least in JDK 1.4.2, goes to select(3C). On Linux, it should be more precise, but it
     * turns out that nanosleep() in this OS, at least in version 7.3 that I tested, has a resolution of at least 20ms.
     * This seems to be a known issue; hopefully they fix it in future.
     */
    public static native void osSleep(int ns);
}
