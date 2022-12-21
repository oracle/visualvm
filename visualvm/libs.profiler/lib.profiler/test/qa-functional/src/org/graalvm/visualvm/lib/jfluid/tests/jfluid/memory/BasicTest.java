/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.tests.jfluid.memory;

import junit.framework.Test;
import junit.textui.TestRunner;
import org.netbeans.junit.NbModuleSuite;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;


/**
 *
 * @author ehucka
 */
public class BasicTest extends MemoryTestCase {
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
            "testSettingsAllocations",
            "testSettingsAllocationsServer",
            "testSettingsAllocationsStackTraces",
            "testSettingsAllocationsStackTracesServer",
            "testSettingsDefault",
            "testSettingsLiveness",
            "testSettingsLivenessServer",
            "testSettingsLivenessStackTraces",
            "testSettingsLivenessStackTracesServer")
            .honorAutoloadEager(true).enableModules(".*").clusters(".*").gui(false));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void testSettingsAllocations() {
        ProfilerEngineSettings settings = initMemoryTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(0);
        startMemoryTest(settings, CommonConstants.INSTR_OBJECT_ALLOCATIONS, new String[] { "simple" });
    }

    public void testSettingsAllocationsServer() {
        ProfilerEngineSettings settings = initMemoryTest("j2se-simple", "simple.memory.Memory1");
        addJVMArgs(settings, "-server");
        settings.setAllocStackTraceLimit(0);
        startMemoryTest(settings, CommonConstants.INSTR_OBJECT_ALLOCATIONS, new String[] { "simple" });
    }

    public void testSettingsAllocationsStackTraces() {
        ProfilerEngineSettings settings = initMemoryTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(-1);
        startMemoryTest(settings, CommonConstants.INSTR_OBJECT_ALLOCATIONS, new String[] { "simple" });
    }

    public void testSettingsAllocationsStackTracesServer() {
        ProfilerEngineSettings settings = initMemoryTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(-1);
        addJVMArgs(settings, "-server");
        startMemoryTest(settings, CommonConstants.INSTR_OBJECT_ALLOCATIONS, new String[] { "simple" });
    }

    public void testSettingsDefault() {
        ProfilerEngineSettings settings = initMemoryTest("j2se-simple", "simple.Memory");
        startMemoryTest(settings, CommonConstants.INSTR_OBJECT_ALLOCATIONS, new String[] { "simple" });
    }

    public void testSettingsLiveness() {
        ProfilerEngineSettings settings = initMemoryTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(0);
        startMemoryTest(settings, CommonConstants.INSTR_OBJECT_LIVENESS, new String[] { "simple" });
    }

    public void testSettingsLivenessServer() {
        ProfilerEngineSettings settings = initMemoryTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(0);
        addJVMArgs(settings, "-server");
        startMemoryTest(settings, CommonConstants.INSTR_OBJECT_LIVENESS, new String[] { "simple" });
    }

    public void testSettingsLivenessStackTraces() {
        ProfilerEngineSettings settings = initMemoryTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(-1);
        startMemoryTest(settings, CommonConstants.INSTR_OBJECT_LIVENESS, new String[] { "simple" });
    }

    public void testSettingsLivenessStackTracesServer() {
        ProfilerEngineSettings settings = initMemoryTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(-1);
        addJVMArgs(settings, "-server");
        startMemoryTest(settings, CommonConstants.INSTR_OBJECT_LIVENESS, new String[] { "simple" });
    }
}
