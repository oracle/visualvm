/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
import org.netbeans.junit.NbModuleSuite;

/**
 *
 * @author tester
 */
public class ProfilerStableTestSuite {
    public static Test suite() {
    return NbModuleSuite.create(
      NbModuleSuite.emptyConfiguration()
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.BasicTest.class)
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.perf.InstrumentationTest.class)
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.wireio.BasicTest.class)
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.monitor.BasicTest.class)
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.memory.BasicTest.class)
	.addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.memory.MemorySnapshotTest.class)
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.others.MeasureDiffsTest.class)
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu.CPUSnapshotTest.class)
	.addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu.BasicTest.class, org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu.BasicTest.tests)
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu.BasicTest.class, org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu.BasicTest.tests2)
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu.BasicTest.class, org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu.BasicTest.tests3)
        .addTest(org.graalvm.visualvm.lib.jfluid.tests.jfluid.benchmarks.JbbTest.class)
    );
  }


}
