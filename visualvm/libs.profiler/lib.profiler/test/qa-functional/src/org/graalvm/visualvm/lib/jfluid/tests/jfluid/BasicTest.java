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

package org.graalvm.visualvm.lib.jfluid.tests.jfluid;

import junit.framework.Test;
import junit.textui.TestRunner;
import org.netbeans.junit.NbModuleSuite;
import org.graalvm.visualvm.lib.jfluid.*;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils.*;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils.TestProfilerAppHandler;


public class BasicTest extends CommonProfilerTestCase {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public BasicTest(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return NbModuleSuite.create(
            NbModuleSuite.createConfiguration(BasicTest.class).addTest(
            "testCalibrate").enableModules(".*").clusters(".*").gui(false));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void testCalibrate() {
        ProfilerEngineSettings settings;
        settings = new ProfilerEngineSettings();
        setTargetVM(settings);
        settings.setPortNo(5140);
        settings.setSeparateConsole(false);
        setStatus(STATUS_NONE);

        setProfilerHome(settings);

        TargetAppRunner runner = new TargetAppRunner(settings, new TestProfilerAppHandler(this),
                                                     new TestProfilingPointsProcessor());
        runner.addProfilingEventListener(Utils.createProfilingListener(this));

        try {
            assertTrue("Error in calibration", runner.calibrateInstrumentationCode());
        } catch (Exception ex) {
            ex.printStackTrace();
            assertFalse("Error in calibration", true);
        } finally {
            runner.terminateTargetJVM();

            //            waitForStatus(STATUS_FINISHED, 60 * 1000);
        }
    }
}
