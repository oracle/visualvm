/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.tests.jfluid.monitor;

import junit.framework.Test;
import junit.textui.TestRunner;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.lib.profiler.ProfilerEngineSettings;


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
            "testCascadeThreadsMemory").enableModules(".*").clusters(".*").gui(false));
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
                             { ST_SLEEPING | ST_WAIT | ST_RUNNING | ST_MONITOR, ST_ZOMBIE },
                             { ST_UNKNOWN | ST_ZOMBIE, ST_RUNNING, ST_ZOMBIE }
                         }, WITH_MEMORY);
    }

    /*public void testGUICPU() {
       ProfilerEngineSettings settings = initMonitorTest("j2se-java2demo", "java2d.Intro");
       startMonitorTest(settings, 30, 1000, new String[] {"main"},
               new byte[][] {{ST_UNKNOWN|ST_ZOMBIE, ST_SLEEPING|ST_WAIT|ST_RUNNING|ST_MONITOR,ST_UNKNOWN|ST_ZOMBIE}}, WITH_CPU);
       }*/
}
