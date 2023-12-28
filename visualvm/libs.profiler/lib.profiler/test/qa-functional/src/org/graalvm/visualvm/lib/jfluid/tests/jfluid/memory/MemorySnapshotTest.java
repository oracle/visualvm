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
public class MemorySnapshotTest extends MemorySnapshotTestCase {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of BasicTest */
    public MemorySnapshotTest(String name) {
        super(name);
    }
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return NbModuleSuite.create(
            NbModuleSuite.createConfiguration(MemorySnapshotTest.class).addTest(
            "testSettingsAllocations",
            "testSettingsAllocationsServer",
            "testSettingsAllocationsStackTraces",
            "testSettingsAllocationsStackTracesServer",
            "testSettingsLiveness",
            "testSettingsLivenessServer",
            "testSettingsLivenessStackTraces",
            "testSettingsLivenessStackTracesServer")
            .honorAutoloadEager(true).enableModules(".*").clusters(".*").gui(false));
    }
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void testSettingsAllocations() {
        ProfilerEngineSettings settings = initMemorySnapshotTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(0);
        startMemorySnapshotTest(settings, CommonConstants.INSTR_OBJECT_ALLOCATIONS, new String[] { "simple" },
                                "simple.memory.Bean");
    }

    public void testSettingsAllocationsServer() {
        ProfilerEngineSettings settings = initMemorySnapshotTest("j2se-simple", "simple.memory.Memory1");
        addJVMArgs(settings, "-server");
        settings.setAllocStackTraceLimit(0);
        startMemorySnapshotTest(settings, CommonConstants.INSTR_OBJECT_ALLOCATIONS, new String[] { "simple" },
                                "simple.memory.Bean");
    }

    public void testSettingsAllocationsStackTraces() {
        ProfilerEngineSettings settings = initMemorySnapshotTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(-1);
        startMemorySnapshotTest(settings, CommonConstants.INSTR_OBJECT_ALLOCATIONS, new String[] { "simple" },
                                "simple.memory.Bean");
    }

    public void testSettingsAllocationsStackTracesServer() {
        ProfilerEngineSettings settings = initMemorySnapshotTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(-1);
        addJVMArgs(settings, "-server");
        startMemorySnapshotTest(settings, CommonConstants.INSTR_OBJECT_ALLOCATIONS, new String[] { "simple" },
                                "simple.memory.Bean");
    }

    public void testSettingsLiveness() {
        ProfilerEngineSettings settings = initMemorySnapshotTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(0);
        startMemorySnapshotTest(settings, CommonConstants.INSTR_OBJECT_LIVENESS, new String[] { "simple" }, "simple.memory.Bean");
    }

    public void testSettingsLivenessServer() {
        ProfilerEngineSettings settings = initMemorySnapshotTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(0);
        addJVMArgs(settings, "-server");
        startMemorySnapshotTest(settings, CommonConstants.INSTR_OBJECT_LIVENESS, new String[] { "simple" }, "simple.memory.Bean");
    }

    public void testSettingsLivenessStackTraces() {
        ProfilerEngineSettings settings = initMemorySnapshotTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(-1);
        startMemorySnapshotTest(settings, CommonConstants.INSTR_OBJECT_LIVENESS, new String[] { "simple" }, "simple.memory.Bean");
    }

    public void testSettingsLivenessStackTracesServer() {
        ProfilerEngineSettings settings = initMemorySnapshotTest("j2se-simple", "simple.memory.Memory1");
        settings.setAllocStackTraceLimit(-1);
        addJVMArgs(settings, "-server");
        startMemorySnapshotTest(settings, CommonConstants.INSTR_OBJECT_LIVENESS, new String[] { "simple" }, "simple.memory.Bean");
    }
}
