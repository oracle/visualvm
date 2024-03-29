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
package org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu;

import junit.framework.Test;
import junit.textui.TestRunner;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.netbeans.junit.NbModuleSuite;

/**
 *
 * @author ehucka
 */
public class BasicTest extends CPUTestCase {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final long MAX_DELAY = 25000L;

    public static final String[] tests = new String[]{
        /*"testLiveResultsAll",
        "testLiveResultsBasic",
        "testLiveResultsWaitEager",
        "testLiveResultsWaitLazy",
        "testLiveResultsWaitSampled", // not stable */
        "testLiveResultsWaitServer",
        "testLiveResultsWaitTotal",
        "testMethodWithWaitEager",
        "testMethodWithWaitEagerServer",
        "testMethodWithWaitExcludeWEager",
        "testMethodWithWaitExcludeWLazy",
        "testMethodWithWaitExcludeWTotal",
        "testMethodWithWaitLazy"
    };
    public static final String[] tests2 = new String[]{
        "testMethodWithWaitLazyServer",
        "testMethodWithWaitTotal",
        "testMethodWithWaitTotalServer",
        "testSettingsDefault", // not stable
        "testSettingsInstrumenManyMethodsLazy",
        "testSettingsInstrumentAllEager",
        "testSettingsInstrumentAllEagerServer",
        "testSettingsInstrumentAllLazy",
        "testSettingsInstrumentAllLazyServer",
        "testSettingsInstrumentAllTotal",
        "testSettingsInstrumentAllTotalServer",
        "testSettingsInstrumentExcludeJavas",
        "testSettingsInstrumentExcludeJavasServer"
    };
    public static final String[] tests3 = new String[]{
        "testSettingsInstrumentManyMethodsTotal",
        "testSettingsInstrumentNotSpawnedThreads",
        "testSettingsInstrumentNotSpawnedThreadsServer",
        "testSettingsInstrumentRootMethod",
        "testSettingsInstrumentRootMethodServer",
        "testSettingsLimitedThreads",
        "testSettingsLimitedThreadsServer",
        "testSettingsSampledProfilingEager",
        "testSettingsSampledProfilingLazy",
        "testSettingsSampledProfilingServerEager",
        "testSettingsSampledProfilingServerLazy",
        "testSettingsSampledProfilingServerTotal",
        "testSettingsSampledProfilingTotal"
    };

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
                "testLiveResultsAll",
                "testLiveResultsBasic",
                "testLiveResultsWaitEager",
                "testLiveResultsWaitLazy",
                "testLiveResultsWaitSampled",
                "testLiveResultsWaitServer",
                "testLiveResultsWaitTotal",
                "testMethodWithWaitEager",
                "testMethodWithWaitEagerServer",
                "testMethodWithWaitExcludeWEager",
                "testMethodWithWaitExcludeWLazy",
                "testMethodWithWaitExcludeWTotal",
                "testMethodWithWaitLazy",
                 "testMethodWithWaitLazyServer",
                "testMethodWithWaitTotal",
                "testMethodWithWaitTotalServer",
                "testSettingsDefault",
//                "testSettingsInstrumenManyMethodsLazy", JVM crash 
                "testSettingsInstrumentAllEager",
                "testSettingsInstrumentAllEagerServer",
                "testSettingsInstrumentAllLazy",
                "testSettingsInstrumentAllLazyServer",
                "testSettingsInstrumentAllTotal",
                "testSettingsInstrumentAllTotalServer",
                "testSettingsInstrumentExcludeJavas",
                "testSettingsInstrumentExcludeJavasServer",
                "testSettingsInstrumentManyMethodsTotal",
                "testSettingsInstrumentNotSpawnedThreads",
                "testSettingsInstrumentNotSpawnedThreadsServer",
                "testSettingsInstrumentRootMethod",
                "testSettingsInstrumentRootMethodServer",
                "testSettingsLimitedThreads",
                "testSettingsLimitedThreadsServer",
                "testSettingsSampledProfilingEager",
                "testSettingsSampledProfilingLazy",
                "testSettingsSampledProfilingServerEager",
                "testSettingsSampledProfilingServerLazy",
                "testSettingsSampledProfilingServerTotal",
                "testSettingsSampledProfilingTotal"
                ).enableModules(".*").clusters(".*").gui(false));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public void temptestSettingsInstrumentRootMethod(boolean server) {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.CPU1",
                new String[][]{
                    {"simple.cpu.CPUThread", "run", "()V"}
                });
        settings.setInstrumentSpawnedThreads(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentGetterSetterMethods(true);
        settings.setInstrumentEmptyMethods(true);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);

        if (server) {
            addJVMArgs(settings, "-server");
        }

        startCPUTest(settings, new String[]{"simple.cpu.CPUThread.run512()"}, new long[]{512L}, 40.0,
                new String[]{"simple"}, ALL_INV_ERROR_METHOD);
    }

    public void testLiveResultsAll() {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.CPU1");
        startCPUTest(settings, new String[]{""}, 1000, MAX_DELAY);
    }

    public void testLiveResultsBasic() {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.CPU");
        startCPUTest(settings, new String[]{"simple"}, 1500, MAX_DELAY);
    }

    public void testLiveResultsWaitEager() {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.WaitingTest");
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_EAGER);
        startCPUTest(settings, new String[]{"simple"}, 1000, MAX_DELAY);
    }

    public void testLiveResultsWaitLazy() {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.WaitingTest");
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);
        startCPUTest(settings, new String[]{"simple"}, 1000, MAX_DELAY);
    }

    public void testLiveResultsWaitSampled() {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.WaitingTest");
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_SAMPLED);
        settings.setSamplingInterval(1);
        startCPUTest(settings, new String[]{"simple"}, 1000, MAX_DELAY);
    }

    public void testLiveResultsWaitServer() {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.WaitingTest");
        addJVMArgs(settings, "-server");
        startCPUTest(settings, new String[]{"simple"}, 1000, MAX_DELAY);
    }

    public void testLiveResultsWaitTotal() {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.WaitingTest");
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_TOTAL);
        startCPUTest(settings, new String[]{"simple"}, 1000, MAX_DELAY);
    }

    public void testMethodWithWaitEager() {
        temptestMethodWithWait(CommonConstants.INSTRSCHEME_EAGER, false, false, 2000L);
    }

    public void testMethodWithWaitEagerServer() {
        temptestMethodWithWait(CommonConstants.INSTRSCHEME_EAGER, false, true, 2000L);
    }

    public void testMethodWithWaitExcludeWEager() {
        temptestMethodWithWait(CommonConstants.INSTRSCHEME_EAGER, true, false, 4000L);
    }

    public void testMethodWithWaitExcludeWLazy() {
        temptestMethodWithWait(CommonConstants.INSTRSCHEME_LAZY, true, false, 4000L);
    }

    public void testMethodWithWaitExcludeWTotal() {
        temptestMethodWithWait(CommonConstants.INSTRSCHEME_TOTAL, true, false, 4000L);
    }

    public void testMethodWithWaitLazy() {
        temptestMethodWithWait(CommonConstants.INSTRSCHEME_LAZY, false, false, 2000L);
    }

    public void testMethodWithWaitLazyServer() {
        temptestMethodWithWait(CommonConstants.INSTRSCHEME_LAZY, false, true, 2000L);
    }

    public void testMethodWithWaitTotal() {
        temptestMethodWithWait(CommonConstants.INSTRSCHEME_TOTAL, false, false, 2000L);
    }

    public void testMethodWithWaitTotalServer() {
        temptestMethodWithWait(CommonConstants.INSTRSCHEME_TOTAL, false, true, 2000L);
    }

    public void testSettingsDefault() {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.CPU1");
        startCPUTest(settings,
                new String[]{
                    "simple.cpu.Bean.run20()", "simple.cpu.Bean.run100()", "simple.cpu.Bean.run1000()",
                    "simple.cpu.CPUThread.run512()"
                }, new long[]{20L, 100L, 1000L, 512L}, 40.0, new String[]{"simple"}, ALL_INV_ERROR_METHOD);
    }

    public void testSettingsInstrumenManyMethodsLazy() {
        temptestSettingsInstrumentManyMethods(CommonConstants.INSTRSCHEME_LAZY);
    }

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

    public void testSettingsInstrumentExcludeJavas() {
        temptestSettingsInstrumentExcludeJavas(false);
    }

    public void testSettingsInstrumentExcludeJavasServer() {
        temptestSettingsInstrumentExcludeJavas(true);
    }

    public void testSettingsInstrumentManyMethodsTotal() {
        temptestSettingsInstrumentManyMethods(CommonConstants.INSTRSCHEME_TOTAL);
    }

    public void testSettingsInstrumentNotSpawnedThreads() {
        temptestSettingsInstrumentNotSpawnedThreads(false);
    }

    public void testSettingsInstrumentNotSpawnedThreadsServer() {
        temptestSettingsInstrumentNotSpawnedThreads(true);
    }

    public void testSettingsInstrumentRootMethod() {
        temptestSettingsInstrumentRootMethod(false);
    }

    public void testSettingsInstrumentRootMethodServer() {
        temptestSettingsInstrumentRootMethod(true);
    }

    public void testSettingsLimitedThreads() {
        temptestSettingsLimitedThreads(false);
    }

    public void testSettingsLimitedThreadsServer() {
        temptestSettingsLimitedThreads(true);
    }

    public void testSettingsSampledProfilingEager() {
        temptestSettingsSampledProfiling(false, CommonConstants.INSTRSCHEME_EAGER);
    }

    public void testSettingsSampledProfilingLazy() {
        temptestSettingsSampledProfiling(false, CommonConstants.INSTRSCHEME_LAZY);
    }

    public void testSettingsSampledProfilingServerEager() {
        temptestSettingsSampledProfiling(true, CommonConstants.INSTRSCHEME_EAGER);
    }

    public void testSettingsSampledProfilingServerLazy() {
        temptestSettingsSampledProfiling(true, CommonConstants.INSTRSCHEME_LAZY);
    }

    public void testSettingsSampledProfilingServerTotal() {
        temptestSettingsSampledProfiling(true, CommonConstants.INSTRSCHEME_TOTAL);
    }

    public void testSettingsSampledProfilingTotal() {
        temptestSettingsSampledProfiling(false, CommonConstants.INSTRSCHEME_TOTAL);
    }

    protected void temptestMethodWithWait(int instrscheme, boolean withwaits, boolean server, long idealtime) {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.WaitingTest",
                new String[][]{
                    {"simple.cpu.WaitingTest", "method1000", "()V"}
                });
        settings.setInstrScheme(instrscheme);
        settings.setExcludeWaitTime(!withwaits);

        if (server) {
            addJVMArgs(settings, "-server");
        }

        startCPUTest(settings, new String[]{"simple.cpu.WaitingTest.method1000()"}, new long[]{idealtime}, 40.0,
                new String[]{"simple"}, ALL_INV_ERROR_METHOD);
    }

    protected void temptestSettingsInstrumentAll(int instrScheme, boolean server) {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.CPU1");
        settings.setInstrumentSpawnedThreads(true);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentGetterSetterMethods(true);
        settings.setInstrumentEmptyMethods(true);
        settings.setInstrScheme(instrScheme);

        if (server) {
            addJVMArgs(settings, "-server");
        }

        startCPUTest(settings,
                new String[]{
                    "simple.cpu.Bean.run20()", "simple.cpu.Bean.run100()", "simple.cpu.Bean.run1000()",
                    "simple.cpu.CPUThread.run512()"
                }, new long[]{20L, 100L, 1000L, 512L}, 40.0, new String[]{"simple"}, ALL_INV_ERROR_METHOD);
    }

    protected void temptestSettingsInstrumentExcludeJavas(boolean server) {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.CPU1");
        settings.setInstrumentSpawnedThreads(true);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentGetterSetterMethods(false);
        settings.setInstrumentEmptyMethods(false);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);

        if (server) {
            addJVMArgs(settings, "-server");
        }

        // TODO: fix for the new filters!
        InstrumentationFilter filter = new InstrumentationFilter();
        filter.setType(InstrumentationFilter.TYPE_EXCLUSIVE);
        filter.setValue("java");
        settings.setInstrumentationFilter(filter);
        startCPUTest(settings,
                new String[]{
                    "simple.cpu.Bean.run20()", "simple.cpu.Bean.run100()", "simple.cpu.Bean.run1000()",
                    "simple.cpu.CPUThread.run512()"
                }, new long[]{20L, 100L, 1000L, 512L}, 40.0, new String[]{"simple", "java"}, ALL_INV_ERROR_METHOD);
    }

    protected void temptestSettingsInstrumentManyMethods(int instrscheme) {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.Methods2");
        settings.setInstrumentSpawnedThreads(true);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentGetterSetterMethods(true);
        settings.setInstrumentEmptyMethods(true);
        settings.setInstrScheme(instrscheme);
        startCPUTest(settings, new String[]{"simple.cpu.Methods2.method0()"}, new long[]{400L}, 40.0,
                new String[]{"simple.cpu.Methods2.method1"}, ALL_INV_ERROR_METHOD);
    }

    protected void temptestSettingsInstrumentNotSpawnedThreads(boolean server) {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.CPU1");
        settings.setInstrumentSpawnedThreads(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentGetterSetterMethods(true);
        settings.setInstrumentEmptyMethods(true);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);

        if (server) {
            addJVMArgs(settings, "-server");
        }

        startCPUTest(settings,
                new String[]{
                    "simple.cpu.Bean.run20()", "simple.cpu.Bean.run100()", "simple.cpu.Bean.run1000()",
                    "simple.cpu.CPUThread.run512()"
                }, new long[]{20L, 100L, 1000L, 512L}, 40.0, new String[]{"simple"}, ALL_INV_ERROR_METHOD);
    }

    protected void temptestSettingsLimitedThreads(boolean server) {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.CPU1");
        settings.setInstrumentSpawnedThreads(true);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentGetterSetterMethods(true);
        settings.setInstrumentEmptyMethods(true);
        settings.setNProfiledThreadsLimit(1);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);

        if (server) {
            addJVMArgs(settings, "-server");
        }

        startCPUTest(settings,
                new String[]{
                    "simple.cpu.Bean.run20()", "simple.cpu.Bean.run100()", "simple.cpu.Bean.run1000()",
                    "simple.cpu.CPUThread.run512()"
                }, new long[]{20L, 100L, 1000L, 512L}, 40.0, new String[]{"simple"}, ALL_INV_ERROR_METHOD);
    }

    protected void temptestSettingsSampledProfiling(boolean server, int instrScheme) {
        ProfilerEngineSettings settings = initCpuTest("j2se-simple", "simple.cpu.CPU1");
        settings.setInstrumentSpawnedThreads(true);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentGetterSetterMethods(true);
        settings.setInstrumentEmptyMethods(true);
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_SAMPLED);
        settings.setSamplingInterval(5);
        settings.setInstrScheme(instrScheme);

        if (server) {
            addJVMArgs(settings, "-server");
        }

        startCPUTest(settings,
                new String[]{
                    "simple.cpu.Bean.run20()", "simple.cpu.Bean.run100()", "simple.cpu.Bean.run1000()",
                    "simple.cpu.CPUThread.run512()"
                }, new long[]{20L, 100L, 1000L, 512L}, 40.0, new String[]{"simple"}, ALL_INV_ERROR_METHOD);
    }
}
