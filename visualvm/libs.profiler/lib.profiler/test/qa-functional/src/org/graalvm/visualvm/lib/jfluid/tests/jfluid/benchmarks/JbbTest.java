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

package org.graalvm.visualvm.lib.jfluid.tests.jfluid.benchmarks;

import junit.framework.Test;
import junit.textui.TestRunner;
import org.netbeans.junit.NbModuleSuite;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;


/**
 *
 * @author ehucka
 */
public class JbbTest extends JbbTestType {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of JbbTest
     */
    public JbbTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return NbModuleSuite.create(
            NbModuleSuite.createConfiguration(JbbTest.class).addTest(
            "testBasic",
            "testDefaultEntire",
            "testDefaultPart",
            "testInstrumentEager",
            "testInstrumentSampledLazy",
            "testInstrumentSampledTotal").enableModules(".*").clusters(".*").gui(false));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void testBasic() {
        ProfilerEngineSettings settings = initCpuTest("jbb", "spec.jbb.JBBmain");
        startBenchmarkTest(settings, 20);
    }

    public void testDefaultEntire() {
        ProfilerEngineSettings settings = initCpuTest("jbb", "spec.jbb.JBBmain");
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_TOTAL);
        settings.setInstrumentEmptyMethods(false);
        settings.setInstrumentGetterSetterMethods(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentSpawnedThreads(true);
        settings.setExcludeWaitTime(true);
        startBenchmarkTest(settings, 170);
    }

    public void testDefaultPart() {
        ProfilerEngineSettings settings = initCpuTest("jbb", "spec.jbb.JBBmain");
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);
        settings.setInstrumentEmptyMethods(false);
        settings.setInstrumentGetterSetterMethods(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentSpawnedThreads(false);
        settings.setExcludeWaitTime(true);
        startBenchmarkTest(settings, 10);
    }

    public void testInstrumentEager() {
        ProfilerEngineSettings settings = initCpuTest("jbb", "spec.jbb.JBBmain");
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_EAGER);
        settings.setInstrumentEmptyMethods(false);
        settings.setInstrumentGetterSetterMethods(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentSpawnedThreads(true);
        settings.setExcludeWaitTime(true);
        startBenchmarkTest(settings, 165);
    }

    public void testInstrumentSampledLazy() {
        ProfilerEngineSettings settings = initCpuTest("jbb", "spec.jbb.JBBmain");
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_SAMPLED);
        settings.setSamplingInterval(10);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);
        settings.setInstrumentEmptyMethods(false);
        settings.setInstrumentGetterSetterMethods(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentSpawnedThreads(true);
        settings.setExcludeWaitTime(true);
        startBenchmarkTest(settings, 30);
    }

    public void testInstrumentSampledTotal() {
        ProfilerEngineSettings settings = initCpuTest("jbb", "spec.jbb.JBBmain");
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_SAMPLED);
        settings.setSamplingInterval(10);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_TOTAL);
        settings.setInstrumentEmptyMethods(false);
        settings.setInstrumentGetterSetterMethods(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentSpawnedThreads(true);
        settings.setExcludeWaitTime(true);
        startBenchmarkTest(settings, 35);
    }
}
