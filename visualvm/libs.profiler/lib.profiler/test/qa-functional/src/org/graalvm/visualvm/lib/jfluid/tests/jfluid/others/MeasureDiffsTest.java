/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.tests.jfluid.others;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.netbeans.junit.NbModuleSuite;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;


/**
 *
 * @author ehucka
 */
public class MeasureDiffsTest extends MeasureDiffsTestCase {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MeasureDiffsTest
     */
    public MeasureDiffsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return NbModuleSuite.create(
            NbModuleSuite.createConfiguration(MeasureDiffsTest.class).addTest(
            "testSettingsInstrumentAllEager",
            "testSettingsInstrumentAllEagerServer",
            "testSettingsInstrumentAllLazy",
            "testSettingsInstrumentAllLazyServer",
            "testSettingsInstrumentAllTotal",
            "testSettingsInstrumentAllTotalServer")
            .honorAutoloadEager(true).enableModules(".*").clusters(".*").gui(false));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void testSettingsInstrumentAllEager() {
        temptestSettingsInstrumentAll(CommonConstants.INSTRSCHEME_EAGER, false);
    }

    public void testSettingsInstrumentAllEagerServer() {
        temptestSettingsInstrumentAll(CommonConstants.INSTRSCHEME_EAGER, true);
    }

    public void testSettingsInstrumentAllLazy() {
        temptestSettingsInstrumentAll(CommonConstants.INSTRSCHEME_LAZY, false);
    }

    public void testSettingsInstrumentAllLazyServer() {
        temptestSettingsInstrumentAll(CommonConstants.INSTRSCHEME_LAZY, true);
    }

    public void testSettingsInstrumentAllTotal() {
        temptestSettingsInstrumentAll(CommonConstants.INSTRSCHEME_TOTAL, false);
    }

    public void testSettingsInstrumentAllTotalServer() {
        temptestSettingsInstrumentAll(CommonConstants.INSTRSCHEME_TOTAL, true);
    }

    protected void temptestSettingsInstrumentAll(int instrScheme, boolean server) {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.Measure");
        settings.setInstrScheme(instrScheme);

        if (server) {
            addJVMArgs(settings, "-server");
        }

        startCPUTest(settings, new String[] { "simple.cpu.Measure.run" });
    }
}
