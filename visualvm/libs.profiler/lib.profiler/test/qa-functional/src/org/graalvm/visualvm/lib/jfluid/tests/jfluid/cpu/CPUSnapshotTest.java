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

package org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu;

import junit.framework.Test;
import junit.textui.TestRunner;
import org.netbeans.junit.NbModuleSuite;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;


/**
 *
 * @author ehucka
 */
public class CPUSnapshotTest extends CPUSnapshotTestCase {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of BasicTest */
    public CPUSnapshotTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return NbModuleSuite.create(
            NbModuleSuite.createConfiguration(CPUSnapshotTest.class).addTest(
            "testMethods",
            "testMethodsServer",
            "testNoThreads",
            "testSimple",
            "testSimpleServer",
            "testThreads",
            "testThreadsServer",
            "testWaits",
            "testWaitsServer").enableModules(".*").clusters(".*").gui(false));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void testMethods() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Methods", null);
        startSnapshotTest(settings, new String[] { "simple.cpu.Methods", "method27", "()V" }, 0, 1.0,
                          new String[] { "sun", "java" });
    }

    public void testMethodsServer() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Methods", null);
        addJVMArgs(settings, "-server");
        startSnapshotTest(settings, new String[] { "simple.cpu.Methods", "method27", "()V" }, 0, 1.0,
                          new String[] { "sun", "java" });
    }

    public void testNoThreads() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Region", null);
        settings.setInstrumentSpawnedThreads(false);
        startSnapshotTest(settings, new String[] { "simple.cpu.Region", "run100", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testSimple() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.CPU", null);
        startSnapshotTest(settings, new String[] { "simple.CPU", "test20", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testSimpleServer() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.CPU", null);
        addJVMArgs(settings, "-server");
        startSnapshotTest(settings, new String[] { "simple.CPU", "test20", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testThreads() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Region", null);
        settings.setInstrumentSpawnedThreads(true);
        startSnapshotTest(settings, new String[] { "simple.cpu.Region", "run100", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testThreadsServer() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Region", null);
        addJVMArgs(settings, "-server");
        settings.setInstrumentSpawnedThreads(true);
        startSnapshotTest(settings, new String[] { "simple.cpu.Region", "run100", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testWaits() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.WaitingTest", null);
        startSnapshotTest(settings, new String[] { "simple.cpu.WaitingTest", "method1000", "()V" }, 0, 1.0,
                          new String[] { "sun", "java" });
    }

    public void testWaitsServer() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.WaitingTest", null);
        addJVMArgs(settings, "-server");
        startSnapshotTest(settings, new String[] { "simple.cpu.WaitingTest", "method1000", "()V" }, 0, 1.0,
                          new String[] { "sun", "java" });
    }
}
