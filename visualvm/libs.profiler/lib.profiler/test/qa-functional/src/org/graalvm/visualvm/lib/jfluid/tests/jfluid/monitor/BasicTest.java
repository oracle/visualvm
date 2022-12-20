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

package org.graalvm.visualvm.lib.jfluid.tests.jfluid.monitor;

import junit.framework.Test;
import junit.textui.TestRunner;
import org.netbeans.junit.NbModuleSuite;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;


/**
 *
 * @author ehucka
 */
public class BasicTest extends MonitorTestCase {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of BasicTest */
    public BasicTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return NbModuleSuite.create(
            NbModuleSuite.createConfiguration(BasicTest.class).addTest(
            "testBasic",
            "testBasicCPU",
            "testBasicMemory",
            "testCascadeThreads",
            "testCascadeThreadsCPU",
            "testCascadeThreadsMemory").honorAutoloadEager(true).enableModules(".*").clusters(".*").gui(false));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void testBasic() {
        ProfilerEngineSettings settings = initMonitorTest("j2se-simple", "simple.Monitor");
        startMonitorTest(settings, 12, 1000, new String[] { "main", "Consumer", "Producer" },
                         new byte[][] {
                             { ST_SLEEPING | ST_WAIT | ST_RUNNING | ST_MONITOR },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_WAIT | ST_RUNNING | ST_MONITOR },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_WAIT | ST_RUNNING | ST_SLEEPING | ST_MONITOR }
                         }, MONITOR_ONLY);
    }

    public void testBasicCPU() {
        ProfilerEngineSettings settings = initMonitorTest("j2se-simple", "simple.Monitor");
        startMonitorTest(settings, 12, 1000, new String[] { "main", "Consumer", "Producer" },
                         new byte[][] {
                             { ST_SLEEPING | ST_WAIT | ST_RUNNING | ST_MONITOR },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_WAIT | ST_RUNNING | ST_MONITOR },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_WAIT | ST_RUNNING | ST_SLEEPING | ST_MONITOR }
                         }, WITH_CPU);
    }

    public void testBasicMemory() {
        ProfilerEngineSettings settings = initMonitorTest("j2se-simple", "simple.Monitor");
        startMonitorTest(settings, 12, 1000, new String[] { "main", "Consumer", "Producer" },
                         new byte[][] {
                             { ST_SLEEPING | ST_WAIT | ST_RUNNING | ST_MONITOR },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_WAIT | ST_RUNNING | ST_MONITOR },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_WAIT | ST_RUNNING | ST_SLEEPING | ST_MONITOR }
                         }, WITH_MEMORY);
    }

    public void testCascadeThreads() {
        ProfilerEngineSettings settings = initMonitorTest("j2se-simple", "simple.monitor.Monitor1");
        startMonitorTest(settings, 12, 1000, new String[] { "main", "Cascade" },
                         new byte[][] {
                             { ST_SLEEPING | ST_WAIT | ST_RUNNING | ST_MONITOR, ST_ZOMBIE },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_RUNNING, ST_ZOMBIE }
                         }, MONITOR_ONLY);
    }

    public void testCascadeThreadsCPU() {
        ProfilerEngineSettings settings = initMonitorTest("j2se-simple", "simple.monitor.Monitor1");
        startMonitorTest(settings, 12, 1000, new String[] { "main", "Cascade" },
                         new byte[][] {
                             { ST_SLEEPING | ST_WAIT | ST_RUNNING | ST_MONITOR, ST_ZOMBIE },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_RUNNING, ST_ZOMBIE }
                         }, WITH_CPU);
    }

    public void testCascadeThreadsMemory() {
        ProfilerEngineSettings settings = initMonitorTest("j2se-simple", "simple.monitor.Monitor1");
        startMonitorTest(settings, 12, 1000, new String[] { "main", "Cascade" },
                         new byte[][] {
                             { ST_UNKNOWN | ST_SLEEPING | ST_WAIT | ST_RUNNING | ST_MONITOR, ST_ZOMBIE },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_RUNNING, ST_ZOMBIE }
                         }, WITH_MEMORY);
    }

    /*public void testGUICPU() {
       ProfilerEngineSettings settings = initMonitorTest("j2se-java2demo", "java2d.Intro");
       startMonitorTest(settings, 30, 1000, new String[] {"main"},
               new byte[][] {{ST_UNKNOWN|ST_ZOMBIE, ST_SLEEPING|ST_WAIT|ST_RUNNING|ST_MONITOR,ST_UNKNOWN|ST_ZOMBIE}}, WITH_CPU);
       }*/
}
